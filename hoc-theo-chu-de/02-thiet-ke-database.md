# 02 — Thiết kế Database

> Quy ước thiết kế DB thực chiến + các pattern xử lý dữ liệu thay đổi theo thời gian.
> 📎 Nguồn: buổi 2, 3

---

## 1. Quy ước thiết kế DB thực chiến

### 1.1 — ID dùng UUID, không dùng auto-increment

```sql
id VARCHAR(36) PRIMARY KEY
```

| | INT AUTO_INCREMENT | UUID (36 ký tự) |
|---|---|---|
| Đoán được? | ✅ Rất dễ (`/order/1`, `/order/2`) | ❌ Gần như không thể |
| Merge nhiều DB | Đụng ID | Không đụng |
| Sinh ở client trước khi insert | ❌ | ✅ |
| Dung lượng / tốc độ index | Nhẹ, nhanh | Nặng hơn |

> Thực tế đi làm **ít khi dùng auto-increment**, chủ yếu vì dễ đoán.
> Java: dùng kiểu `String`, **không** dùng `java.util.UUID` (bản chất vẫn là chuỗi).

### 1.2 — Không đặt prefix tên bảng vào tên cột

```
❌ categories.category_name     ❌ orders.order_code    ❌ orders.order_id
✅ categories.name              ✅ orders.code          ✅ orders.id
```
Lý do: truy vấn luôn viết `categories.name` — prefix là thừa.

### 1.3 — Soft delete (xóa mềm) — bắt buộc

```sql
is_deleted BIT DEFAULT 0
```
- Nhiều công ty **must-have**, cấm xóa cứng (`DELETE`)
- ⚠️ **Mọi query tìm kiếm phải lọc thêm `is_deleted = 0`** — lỗi hay gặp là quên điều kiện này

### 1.4 — Auditing — 4 cột bắt buộc trên mọi bảng

```sql
created_at        DATETIME
created_by        VARCHAR(255)
last_modified_at  DATETIME
last_modified_by  VARCHAR(255)
```
Trả lời: *bản ghi này được ai tạo, lúc nào; sửa lần cuối bởi ai, lúc nào.*

> Java dùng kiểu `Instant` (thay `Date`) — mốc từ **1970-01-01 UTC**, không mang timezone, hợp cho hệ thống phân tán.
> Cách tự động điền 4 cột này: xem [file 11 — JPA Auditing](11-spring-boot-cheatsheet.md#jpa-auditing).

### 1.5 — ID vs CODE — hai trường khác nhau

Nhầm lẫn phổ biến: `id` và `code` **không trùng lặp**, cả hai đều unique nhưng mục đích khác nhau.

| | `id` | `code` |
|---|---|---|
| Dạng | UUID 36 ký tự | Chuỗi ngắn 6–10 ký tự |
| Ví dụ | `9f8c...e21a` | `ORD250830A7` |
| Dành cho | **Hệ thống** giao tiếp với nhau | **Người dùng** đọc, nhớ, tra cứu |

### 1.6 — Các quy tắc đặt tên khác

| ❌ Sai | ✅ Đúng | Lý do |
|---|---|---|
| Bảng tên `item` | `order_item` | `item` không rõ item của cái gì |
| `orders.promotion_code` | Bảng riêng `order_promotions` | **1 đơn có thể áp NHIỀU promotion** |
| PK = `(id, version)` | PK = `id`, UNIQUE `(root_id, version)` | Thực tế hiếm dùng composite PK |
| `status` kiểu `ENUM` | `VARCHAR` | Dễ mở rộng trạng thái mà không `ALTER TABLE` |

---

## 2. Category lồng nhau — Adjacency List Pattern

Yêu cầu: danh mục nhiều tầng như Shopee.
`Đồ điện tử` → `Điện thoại` → `Điện thoại cũ` / `Điện thoại mới`

**Pattern phổ biến nhất: `parent_id` tự tham chiếu.**

```sql
CREATE TABLE categories (
    id                 VARCHAR(36) PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    parent_id          VARCHAR(36) NULL,
    is_deleted         BIT DEFAULT 0,
    created_at         DATETIME,
    created_by         VARCHAR(255),
    last_modified_at   DATETIME,
    last_modified_by   VARCHAR(255),
    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id) REFERENCES categories(id)
);
```

| id | name | parent_id |
|---|---|---|
| `uuid-A` | Đồ điện tử | `NULL` ← root |
| `uuid-B` | Điện thoại | `uuid-A` |
| `uuid-C` | Điện thoại cũ | `uuid-B` |

### ⚠️ Ba nhược điểm

1. **Xóa cha phải đệ quy xóa con** — không tự động
2. **Search/lấy toàn bộ cây phải đệ quy** — query mệt, có thể N+1
3. **Một category chỉ có ĐÚNG MỘT cha**

### Query cây trong MySQL 8+ (Recursive CTE)

```sql
WITH RECURSIVE tree AS (
    SELECT id, name, parent_id, 0 AS depth
    FROM categories WHERE id = 'uuid-A'
    UNION ALL
    SELECT c.id, c.name, c.parent_id, t.depth + 1
    FROM categories c JOIN tree t ON c.parent_id = t.id
)
SELECT * FROM tree;
```

### Nhiều cha — Bảng quan hệ trung gian

Yêu cầu mở rộng: `Điện thoại cũ` vừa là con của `Điện thoại`, vừa là con của `Đồ cũ`.

```sql
CREATE TABLE category_relationships (
    id           VARCHAR(36) PRIMARY KEY,
    left_id      VARCHAR(36) NOT NULL,   -- vai trò "cha"
    right_id     VARCHAR(36) NOT NULL,   -- vai trò "con"
    relationship VARCHAR(50) NOT NULL,   -- PARENT_CHILD, ANCESTOR, ...
    FOREIGN KEY (left_id)  REFERENCES categories(id),
    FOREIGN KEY (right_id) REFERENCES categories(id)
);
```

- Cả `left_id` và `right_id` đều **reference về `categories.id`**
- Dùng `left/right` + cột `relationship` thay `parent/child` cứng → **generic hơn**, lưu được cả quan hệ ông–cháu

> Lớp **dùng `parent_id`** (đơn giản, phổ biến nhất). Phần nhiều cha chỉ để mở rộng — **hay bị hỏi khi phỏng vấn**.

### 4 pattern lưu cây — đối chiếu

| Pattern | Cách lưu | Đọc cây | Ghi | Nhiều cha |
|---|---|---|---|---|
| **Adjacency List** ← lớp dùng | `parent_id` | Chậm (đệ quy) | Nhanh | ❌ |
| Path Enumeration | `path = "/A/B/C/"` | Nhanh (LIKE) | Vừa | ❌ |
| Nested Set | `left`, `right` | Rất nhanh | Rất chậm | ❌ |
| **Closure Table** | Bảng quan hệ riêng | Nhanh | Vừa | ✅ |

---

## 3. ⚠️ Dữ liệu thay đổi theo thời gian — Snapshot vs Versioning

### Bài toán cốt lõi

> Hôm nay bạn mua áo giá 100.000đ, đơn đã confirm nhưng chưa ship.
> Ngày mai shop tăng giá lên 150.000đ.
> **Shipper thu bạn 100.000 hay 150.000?**

→ **100.000đ.** Đơn hàng phải giữ giá **tại thời điểm mua**.

> Nếu `order_items` chỉ lưu `product_id` rồi JOIN sang `products` lấy giá → **SAI**, vì giá đã đổi.

Đây là bài toán chung: *bảng A tham chiếu bảng B, dữ liệu B thay đổi theo thời gian, nhưng A phải giữ giá trị tại thời điểm giao dịch.*

### Pattern 1 — SNAPSHOT (lớp dùng)

Copy giá trị cần giữ vào chính bảng `order_items` lúc tạo đơn.

```sql
CREATE TABLE order_items (
    id          VARCHAR(36) PRIMARY KEY,
    order_id    VARCHAR(36) NOT NULL,
    product_id  VARCHAR(36) NOT NULL,   -- chỉ lưu ID, KHÔNG có FK (khác DB)
    price       INT NOT NULL,           -- ← SNAPSHOT giá lúc mua
    quantity    INT NOT NULL
);
```

- ✅ Đơn giản, đọc nhanh, không cần JOIN
- ❌ **Càng ngày càng phình cột** — ngoài `price` còn cần snapshot `product_name`, `color`, `size`...

*Biến thể:* gom hết vào **1 cột JSON** (`product_snapshot JSON`). JSON hay tách cột **đều vẫn là snapshot**, chỉ khác cách lưu.

### Pattern 2 — VERSIONING

Mỗi lần sửa product → **tạo bản ghi MỚI**, tăng `version`, không update bản cũ.

```
products:
| id       | name | price   | version | parent_id  |
|----------|------|---------|---------|------------|
| uuid-v1  | A    | 100.000 | 1       | NULL       |  ← root
| uuid-v2  | A2   | 100.000 | 2       | uuid-v1    |  ← đổi tên
| uuid-v3  | A2   | 150.000 | 3       | uuid-v2    |  ← đổi giá

order_items.product_id → trỏ CHÍNH XÁC tới uuid-v2 (version tại lúc mua)
```

⚠️ **Primary key vẫn CHỈ là `id`** (UUID mới cho mỗi version). **Không** dùng composite key `(id, version)`.
Muốn ràng buộc thì thêm **UNIQUE KEY** trên `(root_id, version)`.

- ✅ Giữ toàn bộ lịch sử, không phình cột, audit tốt
- ❌ Bảng product phình số dòng, query "sản phẩm hiện tại" phải lọc version mới nhất

### Chọn cái nào?

| Tình huống | Nên dùng |
|---|---|
| Cần giữ ít trường (2–3 cột) | **Snapshot** ← lớp chọn |
| Cần giữ nhiều trường / full lịch sử | **Versioning** |

> *"Không có đúng và sai. Cả hai đều rất phổ biến."*

*Pattern thứ 3:* **Event Sourcing** — không lưu trạng thái mà lưu chuỗi sự kiện, dựng lại bằng replay. Mạnh nhất nhưng phức tạp nhất.

---

## 4. ⚠️ Không có Foreign Key xuyên service

```sql
CREATE TABLE orders (
    id           VARCHAR(36) PRIMARY KEY,
    customer_id  VARCHAR(36) NOT NULL,  -- ❌ KHÔNG FK → auth_db.users
    status       VARCHAR(50) NOT NULL,
    total_amount INT NOT NULL
);
```

| | Monolithic | Microservice |
|---|---|---|
| DB | `orders` và `users` **cùng 1 DB** | **2 DB độc lập** |
| FK | ✅ Khai được | ❌ **Không thể** |

Chỉ lưu ID trần; ràng buộc do **application** đảm bảo.

---

## 5. Schema thực tế của project

### `auth_db`
```sql
users            (id, username, email, phone, password_hash, status, created_at, updated_at)
roles            (id, name, description)
permissions      (id, code, description)
user_roles       (user_id, role_id)
role_permissions (role_id, permission_id)
user_profiles    (user_id, full_name, avatar, dob, gender)
addresses        (id, user_id, receiver_name, phone, province, district, ward, detail, is_default)
sellers          (id, user_id, shop_name, status, bank_account, approved_at)
refresh_tokens   (id, user_id, token, expires_at, revoked)
```

### `product_db`
```sql
categories        (id, parent_id, name, slug, level)
products          (id, seller_id, category_id, name, description, price, stock, status)
product_variants  (id, product_id, sku, price, stock, attributes_json)
product_images    (id, product_id, variant_id, url, sort_order)
promotions        (id, code, type, value, start_at, end_at, quota, used_count, status)
promotion_products(promotion_id, product_id)
flash_sales       (id, name, start_at, end_at, status)
flash_sale_items  (flash_sale_id, variant_id, sale_price, quota, sold)
```

### `order_db` — CHỐT minimum 2 bảng
```sql
CREATE TABLE orders (
    id                VARCHAR(36) PRIMARY KEY,
    customer_id       VARCHAR(36) NOT NULL,   -- không FK (khác DB)
    status            VARCHAR(50) NOT NULL,   -- VARCHAR để dễ mở rộng
    total_amount      INT NOT NULL,
    is_deleted        BIT DEFAULT 0,
    created_at        DATETIME,
    created_by        VARCHAR(255),
    last_modified_at  DATETIME,
    last_modified_by  VARCHAR(255)
);

CREATE TABLE order_items (
    id                VARCHAR(36) PRIMARY KEY,
    order_id          VARCHAR(36) NOT NULL,   -- FK OK (cùng DB)
    product_id        VARCHAR(36) NOT NULL,   -- không FK (khác DB)
    price             INT NOT NULL,           -- SNAPSHOT
    quantity          INT NOT NULL,
    is_deleted        BIT DEFAULT 0,
    created_at        DATETIME,
    created_by        VARCHAR(255),
    last_modified_at  DATETIME,
    last_modified_by  VARCHAR(255),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
```

---

## 6. Bàn về Cart (giỏ hàng) — lưu ở đâu

| Nơi lưu | Đánh giá |
|---|---|
| **Client** (localStorage) | ❌ Không đồng bộ giữa thiết bị — Shopee trên điện thoại và máy tính phải thấy cùng giỏ |
| **Redis / Cache** | ❌ Cart không nhiều dữ liệu; cache lỗi → **mất sạch cart** |
| **Database** | ✅ **Chọn cái này** |
| **Firebase** | ❌ Thầy đi làm 5+ năm chưa gặp dự án nào dùng làm DB chính |

> **Quy tắc:** dữ liệu có cần đồng bộ giữa các thiết bị không? Có → phải lưu server.

---

## 📚 Đọc tiếp

- [01 — Kiến trúc & Nền tảng](01-kien-truc-nen-tang.md)
- [11 — Spring Boot Cheatsheet](11-spring-boot-cheatsheet.md) — cách code entity, auditing
