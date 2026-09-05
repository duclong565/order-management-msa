package com.example.product.service;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import com.example.product.common.ErrorCode;
import com.example.product.common.StockStatus;
import com.example.product.dto.AdminProductListItemResponse;
import com.example.product.dto.CategoryResponse;
import com.example.product.dto.CreateCategoryRequest;
import com.example.product.dto.CreateProductRequest;
import com.example.product.dto.CreateProductVariantRequest;
import com.example.product.dto.ProductExportRow;
import com.example.product.dto.ProductResponse;
import com.example.product.dto.ProductVariantSummaryResponse;
import com.example.product.dto.StockAdjustmentRequest;
import com.example.product.dto.StockAdjustmentResponse;
import com.example.product.entity.Category;
import com.example.product.entity.Inventory;
import com.example.product.entity.InventoryTransaction;
import com.example.product.entity.Product;
import com.example.product.entity.ProductVariant;
import com.example.product.entity.Warehouse;
import com.example.product.exception.ApplicationException;
import com.example.product.pricing.PricingCalculator;
import com.example.product.repository.CategoryRepository;
import com.example.product.repository.InventoryRepository;
import com.example.product.repository.InventoryTransactionRepository;
import com.example.product.repository.ProductRepository;
import com.example.product.repository.WarehouseRepository;
import com.example.product.repository.ProductVariantRepository;
import com.example.product.repository.ProductVariantRowProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final WarehouseRepository warehouseRepository;
    private final PricingCalculator pricingCalculator;

    @Transactional(readOnly = true)
    public Page<AdminProductListItemResponse> getProducts(UUID categoryId,
                                                          StockStatus stockStatus,
                                                          String search,
                                                          Pageable pageable) {
        return productVariantRepository
                .searchForAdmin(categoryId,
                                stockStatus == null ? null : stockStatus.name(),
                                normalizeSearch(search),
                                pricingCalculator.getLowStockThreshold(),
                                pageable)
                .map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .filter(c -> !c.isDeleted())
                .sorted(Comparator.comparing(Category::getName))
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription()))
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Slug already exists: " + request.getSlug());
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        Category saved = categoryRepository.save(category);

        return new CategoryResponse(saved.getId(), saved.getName(), saved.getSlug(), saved.getDescription());
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Category not found"));
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        Product saved = productRepository.save(product);

        return new ProductResponse(saved.getId(), saved.getName(), saved.getDescription(),
                saved.getImageUrl(), category != null ? category.getId() : null);
    }

    @Transactional
    public ProductVariantSummaryResponse createVariant(UUID productId, CreateProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Product not found"));

        if (request.getSku() != null && productVariantRepository.existsBySku(request.getSku())) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "SKU already exists: " + request.getSku());
        }

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setName(request.getName());
        variant.setSku(request.getSku());
        variant.setPrice(request.getPrice());
        ProductVariant saved = productVariantRepository.save(variant);

        return new ProductVariantSummaryResponse(saved.getId(), product.getId(), saved.getName(),
                saved.getSku(), saved.getPrice());
    }

    private AdminProductListItemResponse toListItem(ProductVariantRowProjection row) {
        return new AdminProductListItemResponse(
                row.getVariantId(),
                row.getProductId(),
                row.getProductName(),
                row.getVariantName(),
                row.getSku(),
                row.getImageUrl(),
                row.getCategoryId(),
                row.getCategoryName(),
                row.getPrice(),
                row.getTotalQuantity(),
                pricingCalculator.resolveStockStatus(row.getTotalQuantity())
        );
    }

    private String normalizeSearch(String search) {
        return (search == null || search.isBlank()) ? null : search.trim();
    }

    @Transactional
    public StockAdjustmentResponse adjustStock(UUID variantId, StockAdjustmentRequest request) {
        int delta = request.getQuantityDelta();
        if (delta == 0) {
            throw new ApplicationException(ErrorCode.ZERO_STOCK_ADJUSTMENT);
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.WAREHOUSE_NOT_FOUND));

        Inventory inventory = inventoryRepository
                .findForUpdate(variantId, warehouse.getId())
                .orElseGet(() -> newInventoryRow(variant, warehouse));

        int before = inventory.getQuantity();
        int after = before + delta;
        if (after < 0) {
            throw new ApplicationException(ErrorCode.STOCK_WOULD_GO_NEGATIVE,
                    "Warehouse " + warehouse.getName() + " has " + before
                            + ", cannot apply " + delta);
        }

        inventory.setQuantity(after);
        inventoryRepository.save(inventory);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setProductVariant(variant);
        tx.setWarehouse(warehouse);
        tx.setQuantityDelta(delta);
        tx.setQuantityAfter(after);
        tx.setReason(request.getReason());
        tx.setNote(request.getNote());
        InventoryTransaction savedTx = inventoryTransactionRepository.save(tx);

        long totalAllWarehouses = inventoryRepository.totalStock(variantId);

        return new StockAdjustmentResponse(
                savedTx.getId(),
                variant.getId(),
                variant.getSku(),
                warehouse.getId(),
                warehouse.getName(),
                delta,
                before,
                after,
                totalAllWarehouses,
                pricingCalculator.resolveStockStatus(totalAllWarehouses),
                request.getReason(),
                request.getNote(),
                savedTx.getCreatedAt()
        );
    }

    private Inventory newInventoryRow(ProductVariant variant, Warehouse warehouse) {
        Inventory inventory = new Inventory();
        inventory.setProductVariant(variant);
        inventory.setWarehouse(warehouse);
        inventory.setQuantity(0);
        return inventory;
    }

    private static final int EXPORT_BATCH_SIZE = 500;

    //check lai, batch processing
    @Transactional(readOnly = true)
    public void writeExcel(UUID categoryId, StockStatus stockStatus, String search, OutputStream outputStream) {
        try (ExcelWriter writer = FastExcel.write(outputStream, ProductExportRow.class).build();
             Stream<ProductVariantRowProjection> rows = productVariantRepository.streamForExport(
                     categoryId,
                     stockStatus == null ? null : stockStatus.name(),
                     normalizeSearch(search),
                     pricingCalculator.getLowStockThreshold())) {

            WriteSheet sheet = EasyExcel.writerSheet("San pham").build();

            List<ProductExportRow> batch = new ArrayList<>(EXPORT_BATCH_SIZE);
            Iterator<ProductVariantRowProjection> iterator = rows.iterator();

            while (iterator.hasNext()) {
                batch.add(toExportRow(iterator.next()));
                if (batch.size() >= EXPORT_BATCH_SIZE) {
                    writer.write(batch, sheet);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                writer.write(batch, sheet);
            }
        }
    }

    private ProductExportRow toExportRow(ProductVariantRowProjection row) {
        BigDecimal stockValue = row.getPrice().multiply(BigDecimal.valueOf(row.getTotalQuantity()));
        return new ProductExportRow(
                row.getSku(),
                row.getProductName(),
                row.getVariantName(),
                row.getCategoryName(),
                row.getPrice(),
                row.getTotalQuantity(),
                pricingCalculator.resolveStockStatus(row.getTotalQuantity()).name(),
                stockValue
        );
    }
}
