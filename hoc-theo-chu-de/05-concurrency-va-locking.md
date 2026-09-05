# 05 — Concurrency & Locking

> Race condition, SELECT FOR UPDATE, Redis Distributed Lock, Deadlock.
> ⭐ **Chủ đề hay bị hỏi phỏng vấn nhất** từ level fresher/junior trở lên.
> 📎 Nguồn: buổi 7, 8, 9

---

## 1. ⚠️ RACE CONDITION — bài toán gốc

### Đoạn code hầu hết mọi người viết

```java
List<Product> products = productRepository.findAllById(ids);
for (...) { product.setStock(product.getStock() - quantity); }
productRepository.saveAll(products);
```

### Hai request đồng thời

```
stock ban đầu = 10

Request A                          Request B
─────────────                      ─────────────
đọc stock = 10                     đọc stock = 10      ← CÙNG LÚC
tính 10 - 2 = 8                    tính 10 - 3 = 7
save stock = 8                     save stock = 7

Kết quả cuối: stock = 7
Đúng phải là:  10 - 2 - 3 = 5     ❌ SAI
```

> **Race Condition** — hai tiến trình cùng đọc và cùng ghi một record.

### Vì sao phải "lock" stock ngay khi đặt hàng

> `stock = 10`. Đơn A mua 9, đơn B mua 2.
> Nếu không trừ stock ngay khi tạo đơn A → đơn B vẫn thấy `stock = 10` và tạo thành công.
> → **Oversell.** Phải trừ ngay để "giữ chỗ".

---

## 2. Giải pháp 1 — SELECT FOR UPDATE (Pessimistic Lock)

```sql
SELECT * FROM products WHERE id = 'A' FOR UPDATE;
```

> Câu select này nằm trong một **transaction** và báo cho DB biết transaction đó **sẽ update** record.
> DB **khóa record** lại. Transaction khác muốn đọc phải **CHỜ** đến khi transaction đầu **commit**.

```
Request A: SELECT FOR UPDATE → đọc 10 → trừ 2 → save 8 → COMMIT
Request B:                     ⏳ CHỜ ...                → đọc 8 → trừ 3 → save 5 ✅
```

### Triển khai Spring Data JPA

```java
public interface ProductRepository extends JpaRepository<Product, String> {

    // Hàm thường — dùng cho search, KHÔNG lock
    List<Product> findAllByIdIn(List<String> ids);

    // Hàm có lock — CHỈ dùng khi thực sự update
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIdInForUpdate(@Param("ids") List<String> ids);
}
```

> ⚠️ **Không** gắn `@Lock` lên hàm `findById` dùng chung.
> Nhiều chỗ chỉ **search** mà không update — gắn lock vào sẽ khóa oan, làm chậm toàn hệ thống.
> **Tách thành hàm riêng.**

Verify: bật `show-sql: true` → thấy `... for update` ở cuối câu SQL.

### Optimistic Lock — giải pháp thay thế

```java
@Entity
public class Product {
    @Version
    private Long version;   // JPA tự tăng mỗi lần update
}
```

Không khóa record. Khi update, JPA thêm `WHERE version = ?`.
Nếu ai đó đã sửa trước (version đã đổi) → `OptimisticLockException` → app tự retry.

| | Pessimistic (`FOR UPDATE`) | Optimistic (`@Version`) |
|---|---|---|
| Cơ chế | Khóa record, thằng khác chờ | Không khóa, phát hiện xung đột lúc update |
| Phù hợp khi | **Tranh chấp nhiều** (flash sale) | **Tranh chấp ít** — nhanh hơn vì không khóa |

### Khi nào cần?

> Thực tế xác suất 2 request tranh chấp cùng lúc **không nhiều**, khá hiếm.
> Sản phẩm bình thường có thể bỏ qua. Nhưng **sản phẩm tài chính thì cực kỳ chặt chẽ**.
>
> ⭐ **Đi phỏng vấn thì luôn bị hỏi** — dù công ty bạn có làm hay không.
> Phải biết, chỉ là có triển khai hay không.

---

## 3. ⚠️ Vấn đề của SELECT FOR UPDATE khi nhiều instance

### Bối cảnh

```
[Product instance 1] ┐
[Product instance 2] ├──► [ 1 Product Database ]
[...]                │
[Product instance 10]┘
```

**Mỗi instance có DB riêng được không?**
> ❌ **KHÔNG.** Product A lưu DB này, Product B lưu DB khác thì dữ liệu **không nhất quán**.
> Tất cả instance **bắt buộc** cùng nối vào **một** DB.

**`SELECT FOR UPDATE` có còn work với 10 instance không?**
> ✅ **CÓ, vẫn work tốt.** Vì 10 instance = **10 transaction khác nhau** trên **cùng 1 DB**.
> Cơ chế lock của DB vẫn đảm bảo transaction này commit xong transaction khác mới đọc được.

**Vậy vấn đề là gì?**
> **CHẬM.** Lock trong database rất chậm khi có **hàng triệu / hàng chục nghìn request đồng thời** (flash sale).
> Nếu chỉ vài nghìn request thì `SELECT FOR UPDATE` **không phải vấn đề**.

---

## 4. Giải pháp 2 — REDIS DISTRIBUTED LOCK

### 4.1 — Redis là gì

| Đặc điểm | Ý nghĩa |
|---|---|
| **Key-Value store** | Giống `Map` trong Java |
| **Lưu trên RAM** | ⚡ Cực nhanh |
| **Độ phức tạp `O(1)`** | Tra cứu theo key |
| **SINGLE-THREADED** | ⚠️ Chỉ xử lý **một** lệnh tại một thời điểm |

> Chính vì **single-threaded** mà Redis phù hợp làm **lock**: đảm bảo tại một thời điểm chỉ một request được xử lý.
> (Database thì **multi-threaded**.)

### 4.2 — Ý tưởng

> Thay vì để **database** tự lock, nhờ **một thằng thứ ba đứng ngoài** điều phối.
> Mọi instance trước khi vào DB **phải đi hỏi Redis**: *"tao có được phép vào không?"*

```
[instance 1] ┐
[instance 2] ├──► [ REDIS ]  ──(chỉ 1 thằng được qua)──► [ DATABASE ]
[instance N] ┘     điều phối
```

**Gọi là "distributed" vì việc lock diễn ra trên NHIỀU instance phân tán.**

### 4.3 — Cơ chế: xí chỗ bằng key

```
instance nào đến trước → PUT một key vào Redis   (xí chỗ)
instance đến sau       → hỏi Redis "mày có key này không?"
                          ├── CÓ  → có thằng đang vào DB → CHỜ
                          └── KHÔNG → được phép vào DB
instance đầu xong      → XÓA key  → thằng tiếp theo được vào
```

### 4.4 — Docker Compose

```yaml
services:
  redis:
    image: redis:latest
    ports: ["6379:6379"]

  redis-insight:            # UI, giống Kafka UI / MySQL Workbench
    image: redis/redisinsight:latest
    ports: ["8001:8001"]
```

Vào `localhost:8001` → Add connection → Host: `redis`, Port: `6379`.

### 4.5 — Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>
```

- `spring-boot-starter-data-redis` — kết nối Redis, insert/update/delete cơ bản
- `redisson` — cung cấp `RLock` cho distributed lock

### 4.6 — ⚠️ PHẢI SORT ID KHI TẠO KEY

Đây là **bẫy** quan trọng nhất:

```
KHÔNG sort:
  Instance 1 update product [1, 2]  →  key = "product:1,2"
  Instance 2 update product [2, 1]  →  key = "product:2,1"

  → Hai key KHÁC NHAU (chỉ là 2 chuỗi text khác nhau)
  → CẢ HAI đều được phép vào DB
  → ❌ SAI LOGIC — cùng cập nhật product 1 và 2!

CÓ sort:
  Instance 1: [1, 2] → sort → key = "product:1,2"
  Instance 2: [2, 1] → sort → key = "product:1,2"   ← GIỐNG NHAU
  → Chặn được nhau ✅
```

### 4.7 — Code

```java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final RedissonClient redissonClient;
    private final ProductRepository productRepository;

    public void lockProduct(LockProductRequest request) {
        // 1. Tạo key — BẮT BUỘC SORT
        String key = "lock:product:" + request.getItems().stream()
                .map(LockProductItem::getProductId)
                .sorted()                                    // ⚠️ SORT
                .collect(Collectors.joining(","));

        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            // 2. Xin lock: chờ tối đa 10s, giữ lock tối đa 5s
            acquired = lock.tryLock(10, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new ApplicationException("Không lấy được lock, vui lòng thử lại");
            }

            // 3. Logic — KHÔNG cần SELECT FOR UPDATE nữa
            List<String> ids = request.getItems().stream()
                    .map(LockProductItem::getProductId).toList();
            List<Product> products = productRepository.findAllById(ids);
            // ... trừ stock ...
            productRepository.saveAll(products);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("Interrupted");
        } finally {
            // 4. LUÔN giải phóng lock
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 4.8 — Hai tham số của `tryLock(waitTime, leaseTime, unit)`

| Tham số | Giá trị | Ý nghĩa |
|---|---|---|
| `waitTime` = **10s** | Thời gian **CHỜ** để lấy lock | *"Tao chờ tối đa 10 giây. Không được thì bỏ đi, báo lỗi."* |
| `leaseTime` = **5s** | Thời gian **GIỮ** lock tối đa | *"Cho tao tối đa 5 giây. Sau đó mà tao không trả lời, mày cứ xóa key đi."* |

**Vì sao cần `leaseTime`?**
> Phòng trường hợp instance **lấy được lock rồi CHẾT**. Nó không thể gọi `unlock()` được nữa.
> Không có `leaseTime` → key nằm lại vĩnh viễn → **deadlock**, không ai vào được nữa.

### 4.9 — Demo: sao không thấy key trên Redis Insight

> Code chạy chỉ vài mili-giây → key tạo rồi xóa ngay.
> Muốn thấy phải **giả lập chậm**: `Thread.sleep(4000)` bên trong vùng lock.

---

## 5. ⚠️ Lỗ hổng của lock theo danh sách ID

Buổi 8 chỉ giải quyết trường hợp **hai danh sách giống hệt nhau nhưng khác thứ tự**.
Trường hợp **danh sách giao nhau một phần** thì sort không cứu được:

```
Request A muốn lock [1, 2, 3]  →  key = "product:1,2,3"
Request B muốn lock [1, 2]     →  key = "product:1,2"

→ HAI KEY KHÁC NHAU
→ CẢ HAI cùng vào DB
→ ❌ Vẫn tranh chấp trên product 1 và 2!
```

### Giải pháp: lock đến TỪNG product

```
Thay vì:  lock "product:1,2,3"           (1 key cho cả list)
Làm:      lock "product:1"
          lock "product:2"               (3 key riêng)
          lock "product:3"
```

> Cách này **giống hệt cơ chế của database** — DB cũng lock đến từng **record**, không lock cả nhóm.

📌 Redisson có `RedissonMultiLock` gom nhiều lock thành một — thầy nói **chưa từng dùng**, cho về research.

---

## 6. ⚠️ DEADLOCK

Lock từng cái sinh vấn đề mới:

### Option A — Lock hết rồi mới save
```
lock P1 → lock P2 → lock P3 → save tất cả → unlock tất cả
```
- ✅ Atomic
- ❌ **RỦI RO DEADLOCK**

```
Request A: đã lock P1, đang CHỜ P2
Request B: đã lock P2, đang CHỜ P1
→ Hai thằng chờ nhau vĩnh viễn = DEADLOCK
```

### Option B — Lock từng cái, save, nhả ngay
```
lock P1 → save P1 → unlock P1
lock P2 → save P2 → unlock P2
```
- ✅ Không deadlock
- ❌ **Không atomic** — P1 đã trừ stock mà P2 lỗi thì dữ liệu lệch

### Chi phí
> Lock từng product → mỗi lần lock lại **gọi DB một lần**. 10 sản phẩm = 10 lần gọi DB.
> (Ngược với nguyên tắc "gom nhóm I/O" — đây là cái giá phải trả cho tính đúng đắn.)

### 📌 Cách tránh deadlock chuẩn

> **Luôn lock theo THỨ TỰ ĐÃ SORT.**
> Nếu mọi request đều lock theo thứ tự tăng dần của ID, sẽ không bao giờ có chuyện
> A giữ P1 chờ P2 còn B giữ P2 chờ P1 — vì B cũng phải lấy P1 trước.
>
> Đây là lý do việc **sort** quan trọng hơn ta tưởng — nó vừa giải quyết key trùng, vừa chống deadlock.

---

## 7. So sánh & lựa chọn

| | `SELECT FOR UPDATE` | **Redis Distributed Lock** |
|---|---|---|
| Cần công cụ thứ 3 | ❌ Không | ✅ Cần Redis |
| Tốc độ | Chậm khi tải rất cao | ⚡ Nhanh |
| Độ phức tạp code | Thấp | Cao hơn |
| Phù hợp | Vài nghìn request | Hàng chục nghìn / triệu request |

> **Thực tế đi làm:** dùng `SELECT FOR UPDATE` là **đã ok rồi**.
> Hệ thống chưa chắc nhiều request đến mức phải dùng Redis, mà Redis làm **tăng độ phức tạp code**.
>
> ⭐ **Nhưng đi phỏng vấn chắc chắn họ sẽ hỏi.** Phải hiểu và tự cân nhắc được khi nào cần.

---

## 📚 Đọc tiếp

- [06 — Caching](06-caching.md) — Redis dùng cho mục đích khác
- [10 — Transaction phân tán](10-transaction-phan-tan.md)
