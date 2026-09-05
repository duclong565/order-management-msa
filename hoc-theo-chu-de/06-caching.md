# 06 — Caching

> Local Cache (Caffeine) vs Global Cache (Redis), TTL, Cache Invalidation.
> 📎 Nguồn: buổi 14

---

## 1. Vì sao cần Caching

### Bài toán

```
API lấy chi tiết sản phẩm:
   JOIN products + product_variants + categories + ... nhiều bảng khác

→ Câu query đã CHẬM sẵn
→ Hàng TRIỆU người dùng cùng xem một sản phẩm
→ Cả triệu request cùng hit database, lặp đi lặp lại CÙNG MỘT phép tính
→ Server và database QUÁ TẢI
```

> Nếu sản phẩm **chưa bị thay đổi** thì kết quả người đầu tiên nhận được **giống hệt** người cuối cùng.
> Vậy tính lại cả triệu lần để làm gì?

### Cache là gì

> Cache lưu lại một **bản snapshot** của dữ liệu. Thay vì vào DB lấy, lần sau lấy thẳng từ chỗ đã lưu.

```
User 1: GET product A1  →  query DB (chậm)  →  LƯU LẠI  →  trả về A1
User 2: GET product A1  →  lấy từ CACHE (nhanh)         →  trả về A1
User N: GET product A1  →  lấy từ CACHE (nhanh)         →  trả về A1
```

### "Chi phí" là gì

> Chi phí = **thời gian tính toán** hoặc **bộ nhớ**.
> Cache đáng dùng khi việc lấy dữ liệu **tốn chi phí lớn** và **lặp lại nhiều lần**.

📌 Không chỉ cache query DB. Có thể cache:
- Kết quả **gọi sang service khác** (mất ~1 giây mỗi lần)
- **Cả một hàm** — query 300ms + tính toán 300ms = 600ms → cache cả 600ms đó
- Cache được ở tầng **service, repository hoặc controller**

---

## 2. Demo: cache thủ công bằng HashMap

```java
private final Map<String, Product> productMap = new HashMap<>();

public Product getById(String id) {
    if (productMap.containsKey(id)) {
        return productMap.get(id);          // ← lấy từ cache
    }
    Product product = productRepository.findById(id).orElseThrow();
    productMap.put(id, product);             // ← lưu cache
    return product;
}
```

| Lần gọi | Thời gian |
|---|---|
| Request 1 (query DB) | **305 ms** |
| Request 2 (từ cache) | **7 ms** |

### ⚠️ Nhưng KHÔNG dùng HashMap trong thực tế

| Vấn đề | Giải thích |
|---|---|
| **Giới hạn dung lượng** | Dùng trực tiếp RAM của tiến trình, không kiểm soát được |
| **Coupling chặt** | Logic cache trộn thẳng vào business code |
| **Không share giữa instance** | 10 instance = 10 cache riêng, mỗi thằng tự tính lại |
| **Mất khi restart** | Instance chết là mất sạch cache |
| **Không có TTL, không eviction** | Dữ liệu outdated vĩnh viễn |

---

## 3. 💬 Hai thứ khó nhất trong khoa học máy tính

> *"Trong khoa học máy tính có hai thứ khó nhất:*
> 1. **Naming** *— đặt tên sao cho meaningful*
> 2. **Cache invalidation** *— làm mới cache khi dữ liệu thay đổi"*

Vấn đề: product update giá 300k → 400k nhưng cache vẫn trả 300k → **dữ liệu không đồng bộ với DB**.

> ⚠️ Vấn đề này là **chung cho MỌI công cụ cache** — HashMap, Caffeine, Redis đều gặp.

---

## 4. ⚠️ Local Cache vs Global Cache

```
┌── LOCAL CACHE ────────────────────┐   ┌── GLOBAL CACHE (Distributed) ───┐
│  [instance 1] có cache riêng       │   │  [instance 1] ─┐                 │
│  [instance 2] có cache riêng       │   │  [instance 2] ─┼──► [ REDIS ]    │
│  [instance 3] có cache riêng       │   │  [instance 3] ─┘   (dùng chung)  │
│  Công cụ: CAFFEINE / EhCache       │   │  Công cụ: REDIS                  │
└────────────────────────────────────┘   └──────────────────────────────────┘
```

| | **Local Cache** | **Global Cache** |
|---|---|---|
| Lưu ở đâu | Ngay trên máy chạy instance (heap) | Server riêng (Redis) |
| Tốc độ | ⚡ **Rất nhanh** — không mở TCP, không qua mạng | Chậm hơn — mở kết nối, truyền qua mạng |
| Share giữa instance | ❌ Không | ✅ Có |
| Mất khi restart | ✅ Mất hết | ❌ Không mất |
| Dung lượng | Giới hạn bởi RAM máy đó | Nhiều hơn — server chuyên dụng |
| Tính lại nhiều lần | ✅ Mỗi instance tự tính | ❌ Chỉ instance đầu tiên tính |
| Chi phí vận hành | Không tốn gì | **Maintain thêm hệ thống**, tốn tiền server |

### Ví dụ minh họa

> 10 instance, 10 request đi vào 10 instance khác nhau:
> - **Global cache**: instance 1 tính và cache lên Redis → instance 2→10 chỉ việc lấy về. **Tính 1 lần.**
> - **Local cache**: máy 2 không lấy được cache của máy 1 → **phải tự tính lại**. Tính **10 lần**.

### ❓ Redis cũng qua mạng, khác gì query MySQL?

> - **Redis**: lưu trên **RAM**, dạng **key-value**, tra cứu **O(1)** — sinh ra chỉ để làm việc đó
> - **MySQL**: lưu trên **disk**, dạng **bảng quan hệ**, phải **JOIN**
>
> → Redis nhanh hơn rất nhiều dù cả hai đều truyền gói tin qua mạng.

⚠️ Lưu ý địa lý: Redis ở Mỹ mà server ở Việt Nam thì độ trễ mạng rất lớn. Local cache không có vấn đề này.

---

## 5. Công cụ

| Loại | Công cụ | Ghi chú |
|---|---|---|
| **Local** | **Caffeine** ← lớp dùng | Phổ biến nhất, nhanh nhất cho Java |
| **Local** | **EhCache** | Cũng nổi tiếng; có thể tích hợp bên thứ 3 làm global cache. Ít prefer hơn |
| **Global** | **Redis** ← lớp dùng | Được prefer hơn hẳn EhCache |

> Sau này nghe ai nhắc **"EhCache"** thì hiểu là họ đang dùng cho **local cache**.

📌 *Vì sao Caffeine tốt hơn*: dùng thuật toán eviction **W-TinyLFU** — kết hợp tần suất truy cập (LFU) và thời gian gần đây (LRU), giữ lại entry thực sự "nóng" → hit-rate cao hơn.

---

## 6. Spring Cache Abstraction

> Spring **đã có sẵn cache**, nhưng **chỉ ở tầng interface** — chưa có implementation.
> Implementation do thư viện bên dưới (Caffeine / Redis) đảm nhiệm.

```
        Spring Cache (interface)
        @Cacheable  @CacheEvict  CacheManager
                    ▲
        ┌───────────┴───────────┐
   [Caffeine]                [Redis]
```

**Hệ quả cực kỳ tiện:** đổi từ Caffeine sang Redis chỉ cần **đổi config** — code nghiệp vụ **giữ nguyên hoàn toàn**.

---

## 7. Triển khai Caffeine (Local)

### Dependency
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### Bật cache
```java
@SpringBootApplication
@EnableCaching                       // ← bắt buộc
public class ProductServiceApplication { ... }
```

### Config
```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)   // ① TTL
                .maximumSize(100));                        // ② số entry tối đa
        return cacheManager;
    }
}
```

### ⚠️ TTL — giải pháp cho dữ liệu outdated

> Thay vì cache **vĩnh viễn** (dữ liệu outdated mãi mãi), cho cache **sống 5 phút**.
> Sau 5 phút cache tự xóa → request tiếp theo tính lại và cache mới.
>
> → Dữ liệu **chỉ có thể outdated tối đa 5 phút**, không phải mãi mãi.

### Vì sao cần `maximumSize`

> DB có 1 triệu record — **không thể** cache cả 1 triệu vì quá nhiều bộ nhớ.
> Giới hạn 100 cái. Cái nào cũ thì bị xóa bớt.

---

## 8. `@Cacheable`

```java
@Cacheable(
    value     = "product",              // ① tên vùng cache
    key       = "#productId",           // ② key
    condition = "#productId != null"    // ③ điều kiện mới cache
)
public Product getById(String productId) {
    return productRepository.findById(productId).orElseThrow();
}
```

| Tham số | Ý nghĩa |
|---|---|
| `value` | Tên "vùng nhớ" cache — hiểu đơn giản là **tên của cái HashMap** |
| `key` | Key trong map đó (SpEL: `#tênThamSố`) |
| `condition` | Chỉ cache khi điều kiện đúng |

**Kết quả demo:**

| Lần gọi | Thời gian |
|---|---|
| 1 | **335 ms** |
| 2 | **14 ms** |
| 3 | **12 ms** |

Đọc log thấy rõ: request 1 báo *"no entry for key"* → xuống DB. Sau khi quá TTL → lại xuống DB.

---

## 9. ⚠️ `@CacheEvict` — Cache Invalidation

> Hàm `get*` → **lấy** dữ liệu → `@Cacheable`
> Hàm `update*`, `delete*` → **làm thay đổi** dữ liệu → `@CacheEvict`

```java
@CacheEvict(value = "product", allEntries = true)
public Product update(String id, UpdateProductRequest request) { ... }
```

### `allEntries = true` — khi nào dùng

| Đang cache gì | Cách evict |
|---|---|
| Cache theo **ID** (`getById`) | Evict **theo key**: `@CacheEvict(value="product", key="#id")` |
| Cache một **danh sách** (`search`) | **`allEntries = true`** — xóa **toàn bộ** |

**Vì sao list phải xóa hết:**
> Cache list chứa sản phẩm 1 và 2. Sản phẩm 1 bị đổi.
> Nhưng key của cache là key của **cả list**, **không có key riêng cho sản phẩm 1**.
> → Không xóa lẻ được → phải **xóa toàn bộ**.

**Demo:**
```
① search product  → 284 ms  (query DB)
② search product  → 10 ms   (cache)
③ update product  → log: "invalidate toàn bộ cache"
④ search product  → log: "no entry for key" → QUERY DB LẠI ✅
⑤ search product  → nhanh trở lại (cache mới)
```

### ⚠️ Anti-pattern: cache trên API listing

> Cache ở API **listing/search** **không phải best practice — nó là anti-pattern**, nên tránh.
> Nên cache ở API **`getById`**.

📌 *Vì sao*: list có vô số tổ hợp filter/sort/paging → mỗi tổ hợp một cache entry → hit rate thấp, tốn bộ nhớ, và chỉ cần một bản ghi đổi là phải xóa sạch.

### Các annotation khác

| Annotation | Dùng khi |
|---|---|
| `@Cacheable` | Đọc — có cache trả cache, không thì chạy hàm rồi cache lại |
| `@CacheEvict` | Xóa cache |
| `@CachePut` | **Luôn chạy hàm** rồi **ghi đè** cache — hợp cho `update` khi muốn cập nhật thay vì xóa |
| `@Caching` | Gộp nhiều annotation trên cùng một hàm |

---

## 10. Chuyển Caffeine → Redis

Đây là chỗ thể hiện sức mạnh của Spring Cache abstraction.

### Dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Project giờ có **cả hai** thư viện. Chỉ cần config chọn dùng cái nào.

### Config
```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
```

### Bean với `@Primary`

```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() { ... }        // Caffeine, giữ nguyên

    @Bean
    @Primary                                          // ⚠️ điểm mấu chốt
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60));
        return RedisCacheManager.builder(factory).cacheDefaults(config).build();
    }
}
```

**`@Primary` là gì?**
> Khi có **hai bean cùng kiểu dữ liệu**, bean nào được đánh `@Primary` thì Spring **ưu tiên chọn** bean đó.

### ⚠️ Code nghiệp vụ KHÔNG ĐỔI

> `@Cacheable`, `@CacheEvict` là **interface của Spring**. Chỉ khác implementation bên dưới.
> Đổi từ cache RAM sang cache Redis — **không sửa một dòng business code nào**.

### Xem cache
`http://localhost:8001` (RedisInsight) → Browser → thấy các key cache.

### 🎯 Demo quyết định: cache share giữa instance

```
Chạy 2 instance Product Service: port 8888 và 8887

① Gọi instance 8888  →  query DB, cache lên Redis
② Gọi instance 8887  →  KHÔNG query DB ✅
```

> *"Hai instance khác nhau hoàn toàn. Thằng 8887 chưa được gọi lần nào hết mà vẫn không phải xuống DB."*

Đây chính là điều **local cache không làm được**.

---

## 11. 💬 Trade-off khi đi làm

> *"Đi làm không có đúng và sai. Nó là trade-off.
> Đi học thì mình rất bay bổng, nhưng đi làm phải bám với thực tế."*

### Ba câu hỏi phải trả lời THEO THỨ TỰ

**① Có CẦN cache không?**
- Chỉ 100–1000 người dùng → API chậm cũng không phải vấn đề
- Dữ liệu ít, query không chậm → **không cần cache**
- Cache thêm vào là thêm **độ phức tạp**

**② Nếu cần — Local hay Global?**
- Có đủ tiền/tài nguyên dựng Redis không?
- Mua server vật lý rất tốn kém. **Nhiều công ty không có Redis** vì không dùng cloud
- Cloud thì dễ hơn — dùng bao nhiêu trả bấy nhiêu

**③ Team có sẵn sàng không?**
- **Cache invalidation rất phức tạp**, không hề dễ
- Team nhiều fresher, dự án gấp → cân nhắc

---

## 12. Redis dùng vào việc gì khác

> Redis **rất phổ biến với caching** — "cái đấy ai cũng biết".
> **Distributed lock** mới là cái ít người biết → xem [file 05](05-concurrency-va-locking.md).

Các use case khác: session store, rate limiting, leaderboard (sorted set), pub/sub, counter.

---

## 📚 Đọc tiếp

- [05 — Concurrency & Locking](05-concurrency-va-locking.md) — Redis dùng làm lock
- [11 — Spring Boot Cheatsheet](11-spring-boot-cheatsheet.md)
