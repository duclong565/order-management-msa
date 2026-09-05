# Workspace Microservice — order-management

> 5 project đã được scaffold, code từ monolith đã copy vào đúng service.
> **Chưa compile được** — đây là danh sách việc cần làm, có thứ tự.
>
> 📖 Lộ trình đầy đủ: `../javaaa/order_management/MICROSERVICE-ROADMAP.md`
> 📚 Tài liệu: [README.md](README.md)

---

## Cấu trúc

```
microservice/
├── README.md, THUAT-NGU.md, CAU-HOI-PHONG-VAN.md   ← tài liệu
├── hoc-theo-chu-de/                                 ← 12 file kiến thức
│
├── settings.gradle, build.gradle, gradlew            ← Gradle multi-project
├── .run/                                            ← run config IntelliJ
│
├── discovery-server/     :8761   Eureka
├── api-gateway/          :8080   Spring Cloud Gateway
├── auth-service/         :8081   User, Address
├── product-service/      :8082   Product, Inventory, Discount, Category, Warehouse
└── order-service/        :8083   Order, Cart, Return, Tracking, Payment, Carrier
```

| Service | File Java | DB |
|---|---|---|
| auth-service | 21 | `auth_db` |
| product-service | 34 | `product_db` |
| order-service | 67 | `order_db` |

**Monolith gốc giữ nguyên** ở `javaaa/order_management` (copy, không move).

### Đã cấu hình sẵn
- Spring Boot **4.1.0** + Spring Cloud **2025.1.0 (Oakwood)**
- Eureka Client trong cả 3 service + Gateway
- Gateway route: `/auth/**` `/product/**` `/order/**`
- Package đã đổi: `com.example.{auth,product,order}`
- `@EnableJpaAuditing`, `@EnableEurekaServer` đã gắn

---

## ✅ TÌNH TRẠNG COMPILE *(đã build thật, không phải phỏng đoán)*

| Service | `./gradlew compileJava` |
|---|---|
| **discovery-server** | ✅ **BUILD SUCCESSFUL** |
| **api-gateway** | ✅ **BUILD SUCCESSFUL** |
| **auth-service** | ✅ **BUILD SUCCESSFUL** |
| **product-service** | ⚠️ **11 lỗi** |
| **order-service** | ❌ **79 lỗi** |

> 🎉 **3/5 project chạy được ngay.**
> Toàn bộ đau đớn dồn vào **order-service** — đúng như dự đoán, vì `placeOrder` chạm 4 domain.

### ⚠️ Hai chỗ đã sửa khác với roadmap

**1. Artifact Gateway đổi tên ở Spring Cloud 2025.1.0**
```gradle
❌ spring-cloud-starter-gateway                     // không tồn tại nữa
✅ spring-cloud-starter-gateway-server-webflux      // Oakwood / Boot 4
```
Roadmap ghi *"xóa chữ server đi"* — đó là lời thầy cho **bản cũ**. Với Spring Cloud 2025.1.0 thì **ngược lại**, phải có `-server-webflux`.

**2. Dependency đã bổ sung**
- `product-service`: `fastexcel` (export Excel) + `spring-boot-starter-security`
- `order-service`: `spring-boot-starter-security` (cho `GlobalExceptionHandler`)

---

# 🔧 VIỆC CẦN LÀM

## A. product-service — 11 lỗi (làm trước, dễ)

| Lỗi | File | Cách sửa |
|---|---|---|
| `CurrentUserProvider`, `CustomUserDetails` | `AuditorAwareImpl.java` (4 lỗi) | Xem mục **C** |
| `Address` | `Warehouse.java:15,26` | `@ManyToOne Address` → `private UUID addressId;` — Address thuộc auth-service |
| `PricingCalculator` | `AdminProductService.java` (3 lỗi) | Kiểm tra có thực sự cần không. Nếu chỉ format giá → viết hàm local; nếu không dùng → xóa import |

---

## B. order-service — 79 lỗi

### Thống kê symbol thiếu *(số liệu thật từ compiler)*

| Class thiếu | Số lỗi | Thuộc service |
|---|---|---|
| `User` | **14** | auth |
| `StockStatus` | **10** | product (enum) |
| `Address` | **9** | auth |
| `ProductVariant` | **7** | product |
| `Discount` | **7** | product |
| `UserRole` | **6** | auth (enum) |
| `CurrentUserProvider` | **6** | — (mục C) |
| `InventoryRepository` | 5 | product |
| `WarehouseRepository` | 3 | product |
| `Warehouse` | 3 | product |
| `DiscountRepository` | 3 | product |
| `AddressRepository` | 1 | auth |

### B1. Enum — copy là xong (**giảm ngay 16 lỗi**)

| Thiếu | Lỗi | Xử lý |
|---|---|---|
| `StockStatus` | 10 | **Copy** từ `product-service/common/` sang |
| `UserRole` | 6 | **Copy** từ `auth-service/common/` sang |

```bash
cp product-service/src/main/java/com/example/product/common/StockStatus.java \
   order-service/src/main/java/com/example/order/common/
cp auth-service/src/main/java/com/example/auth/common/UserRole.java \
   order-service/src/main/java/com/example/order/common/
# rồi sửa dòng package trong 2 file vừa copy
```

> Enum không có state → copy sang cả 2 service là chấp nhận được.
> (Cách "chuẩn" là shared library, nhưng tạo coupling — chưa cần ở giai đoạn này.)

### B2. Entity xuyên service — **bài học chính** (33 lỗi)

| Thiếu | Lỗi | Cách sửa |
|---|---|---|
| `User` | 14 | `order.getUser().getX()` → gọi **auth-service** qua API |
| `Address` | 9 | → **auth-service** |
| `ProductVariant` | 7 | → **product-service** |
| `Discount` | 7 | Nhận `DiscountDTO` thay vì entity |
| `Warehouse` | 3 | → **product-service** |

**Và các entity còn `@ManyToOne` xuyên service** (chưa hiện lỗi import vì cùng package, nhưng sẽ vỡ khi chạy):

```java
// order-service/entity/Order.java
@ManyToOne private User user;              →  private UUID userId;
@ManyToOne private Discount discount;      →  private UUID discountId;
@ManyToOne private Address recipientAddress; →  private UUID recipientAddressId;

// order-service/entity/Cart.java
@ManyToOne private User user;              →  private UUID userId;
@ManyToOne private Discount discount;      →  private UUID discountId;

// order-service/entity/CartItem.java
@ManyToOne private ProductVariant productVariant; → private UUID productVariantId;

// order-service/entity/OrderItem.java
@ManyToOne private ProductVariant productVariant; → private UUID productVariantId;

// order-service/entity/TrackingLog.java, ReturnRequest.java
@ManyToOne private User user;              →  private UUID userId;
```

📎 [RULE 3 — không chọc DB service khác](hoc-theo-chu-de/01-kien-truc-nen-tang.md)

### B3. Repository của service khác (12 lỗi)

| Thiếu | Lỗi | Cách sửa |
|---|---|---|
| `InventoryRepository` | 5 | → `ProductClient.getStock(variantIds)` — **batch** |
| `WarehouseRepository` | 3 | → `ProductClient` |
| `DiscountRepository` | 3 | → `ProductClient.getDiscount(id)` |
| `AddressRepository` | 1 | → `AuthClient.getAddress(id)` |

⚠️ Nhớ **batch** ngay từ đầu, đừng gọi trong vòng lặp.
📎 [Không gọi I/O trong vòng lặp](hoc-theo-chu-de/03-giao-tiep-giua-service.md)

---

## C. `CurrentUserProvider` — vấn đề chung của product + order

`AuditorAwareImpl` cần biết "ai đang thao tác" để điền `created_by`.
Monolith lấy từ `SecurityContextHolder`. Sau khi tách, product/order **không có** Security context.

### Giải pháp: Gateway giải mã token → gắn header

```java
// product-service & order-service: thay CurrentUserProvider
@Component
public class CurrentUserProvider {

    public UUID getUserId() {
        HttpServletRequest req = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        String id = req.getHeader("X-User-Id");
        return id == null ? null : UUID.fromString(id);
    }

    public String getUsername() {
        HttpServletRequest req = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        return req.getHeader("X-Username");
    }
}
```

`AuditorAwareImpl` đọc từ `CurrentUserProvider` này thay vì `CustomUserDetails`.

📎 [Truyền thông tin user qua header](hoc-theo-chu-de/08-gateway-va-bao-mat.md)

> 💥 **Thí nghiệm 4** trong roadmap: chạy thử trước khi làm bước này → `created_by` sẽ là `null`.
> Đó chính là vấn đề audit thầy nói ở buổi 10.

---

## D. Database & Migration

Chưa làm. Cần:

```sql
CREATE DATABASE auth_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
```

Migration `V1__init_schema.sql` … `V6__product_catalog.sql` ở monolith là **một khối chung**.
Phải tách thành 3 bộ, mỗi service một thư mục `src/main/resources/db/migration/`.

Sau khi tách xong, chạy `grep` kiểm tra: **không còn JOIN nào xuyên schema**.

---

## E. Test

Chưa copy sang. Sẽ làm sau khi 3 service compile được.

Monolith có sẵn: `PricingCalculatorTest`, `InventoryRepositoryTest`, `InventoryTransactionRepositoryTest`, `ReturnRequestRepositoryTest` + `TestcontainersConfiguration`.

---

# 📋 Thứ tự làm

```
① discovery-server   → ./gradlew bootRun, vào localhost:8761      [✅ build OK]
② api-gateway        → ./gradlew bootRun, thấy đăng ký Eureka     [✅ build OK]
③ auth-service       → tạo auth_db, bootRun                        [✅ build OK]
④ product-service    → sửa 11 lỗi (mục A + C)                      [11 lỗi]
⑤ order-service B1   → copy 2 enum → còn 63 lỗi                    [dễ, 5 phút]
⑥ order-service C    → sửa CurrentUserProvider → còn 57 lỗi
⑦ order-service B2   → cắt @ManyToOne → UUID                       [BÀI HỌC CHÍNH]
⑧ order-service B3   → viết ProductClient / AuthClient (WebClient)
⑨ Migration + DB     → mục D
⑩ Thí nghiệm 1-8     → theo roadmap
```

> **Đừng làm ⑦ và ⑧ cùng lúc.** Cắt quan hệ trước (⑦) — compile lỗi ở đâu thì
> chỗ đó chính là nơi cần API (⑧). Danh sách lỗi là bản đồ cross-service call.

### Mốc kiểm tra

| Sau bước | Lỗi order-service còn |
|---|---|
| Hiện tại | 79 |
| ⑤ copy enum | ~63 |
| ⑥ CurrentUserProvider | ~57 |
| ⑦ cắt `@ManyToOne` | ~20 (chỉ còn chỗ gọi API) |
| ⑧ viết Client | **0** |

> **Đừng làm ⑤ và ⑥ cùng lúc.** Cắt quan hệ trước (⑤), để compile lỗi ở đâu thì biết chỗ đó cần API (⑥).

---

---

# 💻 Mở trong IntelliJ IDEA

Đây là **Gradle multi-project** — chỉ cần mở folder gốc là thấy cả 5 service.

```
File → Open… → chọn folder  microservice/   → OK
                (chọn FOLDER, không phải file build.gradle)
→ hộp thoại "Trust Gradle project?"  → Trust Project
→ chờ Gradle sync
```

Sau sync, panel Gradle (bên phải) sẽ hiện:

```
order-management-msa
├── api-gateway
├── auth-service
├── discovery-server
├── order-service
└── product-service
```

### Run configuration đã tạo sẵn

Thư mục `.run/` có sẵn 7 config, IntelliJ tự nạp:

| Config | Port |
|---|---|
| 1. discovery-server | 8761 |
| 2. api-gateway | 8080 |
| 3. auth-service | 8081 |
| 4. product-service | 8082 |
| 5. order-service | 8083 |
| 6. product-service (8887) | 8887 ← để test load balancing |
| 7. product-service (8889) | 8889 ← để test load balancing |

Chạy theo thứ tự **1 → 2 → 3 → …** (discovery-server phải lên trước).

> Config 6, 7 dùng cho **Thí nghiệm 5** (hardcode URL) và **Thí nghiệm 6** (consumer group).

### Services Tool Window

`View → Tool Windows → Services` — xem tất cả app đang chạy, log riêng từng cái, start/stop nhanh.

---

## Lệnh hay dùng

Chạy **từ folder gốc**, dùng cú pháp `:service:task`:

```bash
# chạy 1 service
./gradlew :discovery-server:bootRun
./gradlew :order-service:bootRun

# chạy thêm instance khác port (test load balancing)
./gradlew :product-service:bootRun --args='--server.port=8887'

# compile tất cả, không dừng ở lỗi đầu tiên
./gradlew compileJava --continue

# đếm lỗi của 1 service
./gradlew :order-service:compileJava 2>&1 | grep -c "error:"

# build tất cả, bỏ qua test
./gradlew build -x test
```

> ⚠️ Chỉ còn **một** gradle wrapper ở gốc. Đừng `cd` vào service rồi gọi `./gradlew` — không có ở đó nữa.
