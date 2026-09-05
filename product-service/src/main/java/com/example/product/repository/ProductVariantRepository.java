package com.example.product.repository;

import com.example.product.entity.ProductVariant;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    boolean existsBySku(String sku);

    Optional<ProductVariant> findBySku(String sku);

    String ROW_SELECT = """
        select pv.id                                  as "variantId",
               p.id                                   as "productId",
               p.name                                 as "productName",
               pv.name                                as "variantName",
               pv.sku                                 as "sku",
               p.image_url                            as "imageUrl",
               c.id                                   as "categoryId",
               c.name                                 as "categoryName",
               pv.price                               as "price",
               coalesce(sum(i.quantity), 0)           as "totalQuantity"
        """;

    String ROW_FROM = """
        from product_variants pv
        join products p on p.id = pv.product_id and p.deleted = false
        left join categories c on c.id = p.category_id and c.deleted = false
        left join inventories i on i.product_variant_id = pv.id and i.deleted = false
        where pv.deleted = false
          and (cast(:categoryId as uuid) is null or p.category_id = cast(:categoryId as uuid))
          and (cast(:search as varchar) is null
               or lower(p.name)  like lower('%' || cast(:search as varchar) || '%')
               or lower(pv.name) like lower('%' || cast(:search as varchar) || '%')
               or lower(coalesce(pv.sku, '')) like lower('%' || cast(:search as varchar) || '%'))
        group by pv.id, p.id, c.id
        having (cast(:stockStatus as varchar) is null
                or (cast(:stockStatus as varchar) = 'OUT_OF_STOCK'
                    and coalesce(sum(i.quantity), 0) <= 0)
                or (cast(:stockStatus as varchar) = 'LIMITED_STOCK'
                    and coalesce(sum(i.quantity), 0) > 0
                    and coalesce(sum(i.quantity), 0) <= :lowStockThreshold)
                or (cast(:stockStatus as varchar) = 'IN_STOCK'
                    and coalesce(sum(i.quantity), 0) > :lowStockThreshold))
        """;

    @Query(value = ROW_SELECT + ROW_FROM + " order by p.name, pv.name",
           countQuery = "select count(*) from (select pv.id " + ROW_FROM + ") t",
           nativeQuery = true)
    Page<ProductVariantRowProjection> searchForAdmin(@Param("categoryId") UUID categoryId,
                                                     @Param("stockStatus") String stockStatus,
                                                     @Param("search") String search,
                                                     @Param("lowStockThreshold") int lowStockThreshold,
                                                     Pageable pageable);

    @Query(value = ROW_SELECT + """
        from product_variants pv
        join products p on p.id = pv.product_id
        left join categories c on c.id = p.category_id
        left join inventories i on i.product_variant_id = pv.id and i.deleted = false
        where pv.id = :variantId
          and pv.deleted = false
        group by pv.id, p.id, c.id
        """, nativeQuery = true)
    Optional<ProductVariantRowProjection> findRowById(@Param("variantId") UUID variantId);

    @Query(value = ROW_SELECT + """
        from product_variants pv
        join products p on p.id = pv.product_id
        left join categories c on c.id = p.category_id
        left join inventories i on i.product_variant_id = pv.id and i.deleted = false
        where pv.id in (:variantIds)
          and pv.deleted = false
        group by pv.id, p.id, c.id
        """, nativeQuery = true)
    List<ProductVariantRowProjection> findRowsByIds(@Param("variantIds") List<UUID> variantIds);

    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "500"))
    @Query(value = ROW_SELECT + ROW_FROM + " order by p.name, pv.name", nativeQuery = true)
    Stream<ProductVariantRowProjection> streamForExport(@Param("categoryId") UUID categoryId,
                                                        @Param("stockStatus") String stockStatus,
                                                        @Param("search") String search,
                                                        @Param("lowStockThreshold") int lowStockThreshold);
}
