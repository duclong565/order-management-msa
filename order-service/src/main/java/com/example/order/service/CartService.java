package com.example.order.service;

import com.example.order.client.DiscountResponse;
import com.example.order.client.ProductClient;
import com.example.order.client.ProductVariantResponse;
import com.example.order.common.ErrorCode;
import com.example.order.common.StockStatus;
import com.example.order.dto.CartItemResponse;
import com.example.order.dto.CartResponse;
import com.example.order.dto.OrderSummaryResponse;
import com.example.order.entity.Cart;
import com.example.order.entity.CartItem;
import com.example.order.exception.ApplicationException;
import com.example.order.pricing.PricingCalculator;
import com.example.order.repository.CartItemRepository;
import com.example.order.repository.CartRepository;
import com.example.order.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final PricingCalculator pricingCalculator;
    private final CurrentUserProvider currentUserProvider;

    private Cart findCartByUserId(UUID userId) {
        return cartRepository.findByUserId(userId).orElseThrow(() ->
                new ApplicationException(ErrorCode.CART_NOT_FOUND));
    }

    private Cart findOrCreateCartByUserId(UUID userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUserId(userId);
            return cartRepository.save(cart);
        });
    }

    // gom hết ID cần thiết, gọi product-service đúng 1 lần, dựng Map để tra cứu O(1)
    // - không gọi ProductClient bên trong vòng lặp.
    private Map<UUID, ProductVariantResponse> fetchVariantsByCartItems(List<CartItem> cartItems) {
        List<UUID> variantIds = cartItems.stream().map(CartItem::getProductVariantId).distinct().toList();
        return productClient.getVariantsByIds(variantIds).stream()
                .collect(Collectors.toMap(ProductVariantResponse::getVariantId, Function.identity()));
    }

    private CartItemResponse toItemResponse(CartItem cartItem, Map<UUID, ProductVariantResponse> variants) {
        ProductVariantResponse variant = variants.get(cartItem.getProductVariantId());
        long stockQuantity = variant != null ? variant.getTotalQuantity() : 0;
        StockStatus status = pricingCalculator.resolveStockStatus(stockQuantity, cartItem.getQuantity());
        Integer available = status == StockStatus.LIMITED_STOCK ? (int) stockQuantity : null;

        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getProductVariantId(),
                variant != null ? variant.getProductName() : null,
                variant != null ? variant.getVariantName() : null,
                variant != null ? variant.getPrice() : BigDecimal.ZERO,
                cartItem.getQuantity(),
                status,
                available
        );
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        Map<UUID, ProductVariantResponse> variants = fetchVariantsByCartItems(cartItems);

        List<CartItemResponse> items = cartItems.stream()
                .map(item -> toItemResponse(item, variants))
                .toList();

        return new CartResponse(cart.getId(), cart.getUserId(), items);
    }

    @Transactional(readOnly = true)
    public CartResponse getMyCart() {
        UUID userId = currentUserProvider.getUserId();
        Cart cart = findCartByUserId(userId);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addItem(UUID productVariantId, int quantity) {
        UUID userId = currentUserProvider.getUserId();
        Cart cart = findOrCreateCartByUserId(userId);

        List<ProductVariantResponse> variants = productClient.getVariantsByIds(List.of(productVariantId));
        if (variants.isEmpty()) {
            throw new ApplicationException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }
        ProductVariantResponse variant = variants.get(0);

        CartItem existing = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), productVariantId)
                .orElse(null);
        int wantedQuantity = quantity + (existing != null ? existing.getQuantity() : 0);

        if (variant.getTotalQuantity() < wantedQuantity) {
            throw new ApplicationException(ErrorCode.INSUFFICIENT_STOCK,
                    "Insufficient stock. Available quantity: " + variant.getTotalQuantity());
        }

        if (existing != null) {
            existing.setQuantity(wantedQuantity);
            cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProductVariantId(productVariantId);
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return buildCartResponse(cart);
    }

    @Transactional
    public void updateItemQuantity(UUID cartItemId, Integer quantity) {
        UUID userId = currentUserProvider.getUserId();

        CartItem cartItem = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.CART_ITEM_NOT_FOUND));

        long availableStock = productClient.getStock(cartItem.getProductVariantId());

        if (availableStock < quantity) {
            throw new ApplicationException(ErrorCode.INSUFFICIENT_STOCK,
                    "Insufficient stock. Available quantity: " + availableStock);
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void removeItem(UUID cartItemId) {
        UUID userId = currentUserProvider.getUserId();
        CartItem cartItem = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.CART_ITEM_NOT_FOUND));
        cartItemRepository.delete(cartItem);
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse getOrderSummary(UUID discountId) {
        UUID userId = currentUserProvider.getUserId();
        Cart cart = findCartByUserId(userId);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        Map<UUID, ProductVariantResponse> variants = fetchVariantsByCartItems(cartItems);

        BigDecimal subtotal = cartItems.stream()
                .map(item -> variants.get(item.getProductVariantId()).getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DiscountResponse discount = null;
        if (discountId != null) {
            discount = productClient.getDiscountById(discountId);
            pricingCalculator.validateDiscountActive(discount);
        }

        BigDecimal shippingFee = pricingCalculator.getShippingFee();
        BigDecimal discountAmount = pricingCalculator.calculateDiscountAmount(discount, subtotal);
        BigDecimal totalPrice = subtotal.subtract(discountAmount).add(shippingFee);

        return new OrderSummaryResponse(
                subtotal,
                discountAmount,
                shippingFee,
                totalPrice
        );
    }
}
