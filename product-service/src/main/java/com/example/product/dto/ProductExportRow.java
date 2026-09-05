package com.example.product.dto;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductExportRow {

    @ExcelProperty("SKU")
    @ColumnWidth(20)
    private String sku;

    @ExcelProperty("Ten san pham")
    @ColumnWidth(30)
    private String productName;

    @ExcelProperty("Bien the")
    @ColumnWidth(24)
    private String variantName;

    @ExcelProperty("Danh muc")
    @ColumnWidth(22)
    private String categoryName;

    @ExcelProperty("Gia (VND)")
    @ColumnWidth(16)
    private BigDecimal price;

    @ExcelProperty("So luong")
    @ColumnWidth(12)
    private Long totalQuantity;

    @ExcelProperty("Trang thai")
    @ColumnWidth(18)
    private String stockStatus;

    @ExcelProperty("Gia tri ton (VND)")
    @ColumnWidth(20)
    private BigDecimal stockValue;
}
