# 03 — Giao tiếp giữa các Service

> Sync vs Async, WebClient, và các bài học tối ưu hiệu năng.
> 📎 Nguồn: buổi 4, 5, 6, 8

---

## 1. Hai kiểu giao tiếp

```
Giao tiếp giữa 2 microservice
│
├── SYNC (đồng bộ) ─── Command-based: ra lệnh và CHỜ phản hồi
│     ├── REST/HTTP  → công cụ: WebClient, RestTemplate, FeignClient
│     └── gRPC
│
└── ASYNC (bất đồng bộ) ─── Event-based: phát sự kiện, KHÔNG chờ
      ├── Kafka  ← lớp dùng ([file 04](04-kafka.md))
      └── RabbitMQ / ActiveMQ
```

> Đi phỏng vấn nên trả lời **generic trước** (sync/async, HTTP/gRPC), rồi mới đi vào công cụ cụ thể.
> Đừng nhảy thẳng vào `WebClient`.

### SYNC — Đồng bộ

> Order gọi Product và **ĐỢI** cho đến khi Product phản hồi thì mới đi tiếp.

```
[Order Service] ──── gọi ────► [Product Service]
       │                              │
       │◄──── đợi... phản hồi ────────┘
       ▼ đi tiếp
```

**Vì sao thầy chọn HTTP dù gRPC nhanh hơn?**
> API HTTP viết ra **còn tái sử dụng cho các luồng khác** (frontend gọi, tích hợp bên thứ 3),
> không chỉ để service gọi nhau → giảm effort, không phải viết 2 lớp API.

### ASYNC — Bất đồng bộ

> Order **KHÔNG ĐỢI** Product. Product **lắng nghe sự kiện** và tự hành động.

```
① Order Service: lưu order status = NEW
② Order Service: publish event lên Kafka topic "order-created"
③ Product Service: consume event  →  lock stock
④ Product Service: publish event lên topic "product-locked"
⑤ Order Service: consume event  →  update status = CONFIRMED
```

### Ví dụ dễ hiểu — đặt vé concert

| | Trải nghiệm người dùng |
|---|---|
| **SYNC** | Màn hình quay vòng chờ → *"Đặt vé THÀNH CÔNG"* / *"THẤT BẠI"*. Phải đợi kết quả chính thức |
| **ASYNC** | *"Yêu cầu đã được ghi nhận. Chúng tôi sẽ phản hồi qua email."* → vài phút sau nhận mail |

### So sánh

| | Sync | Async |
|---|---|---|
| Chờ phản hồi | ✅ Có | ❌ Không |
| Coupling | Chặt | Lỏng |
| B chết thì A? | A cũng lỗi | A vẫn chạy, event nằm chờ |
| Chịu tải cao | Kém | Tốt (Kafka làm bộ đệm) |
| Debug | Dễ | Khó (cần tracing) |
| Consistency | Ngay lập tức | **Eventual** |

📌 Async còn giúp **fan-out** — 1 event `order-created` được nhiều service cùng nghe (Product lock stock, Notification gửi mail, Analytics ghi số liệu) mà Order **không cần biết** có bao nhiêu service đang nghe.

**Flash sale, 1 triệu người mua cùng lúc — chọn gì?**
> **Async.** Kafka làm **buffer** — publish lên rồi xử lý dần, đơn ở trạng thái chờ.
> Sync thì mọi người phải đợi nhau, dễ sập.

---

## 2. WebClient — triển khai Sync

### 2.1 — Cấu hình port cho từng service

| Service | Port |
|---|---|
| Order Service | `8080` |
| Product Service | `8888` |
| Keycloak | `8080` ⚠️ *(đụng Order — cần đổi một trong hai)* |

### 2.2 — Tách interface Client

```java
public interface ProductClient {
    List<ProductDTO> getProductsByIds(List<String> productIds);
}

@Component
@RequiredArgsConstructor
public class ProductClientImpl implements ProductClient { ... }
```

> **Vì sao tách:** tầng service chỉ cần biết *"lấy cho tôi danh sách product"*, không cần biết lấy bằng HTTP hay gRPC hay Kafka. Đổi công nghệ chỉ sửa Impl.

### 2.3 — Config Bean

```java
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced                     // ⚠️ cần khi dùng Eureka — xem file 07
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```

### 2.4 — Gọi sang service khác

```java
@Component
@RequiredArgsConstructor
public class ProductClientImpl implements ProductClient {

    private final WebClient.Builder webClientBuilder;

    @Override
    public List<ProductDTO> getProductsByIds(List<String> productIds) {

        BaseResponse<List<ProductDTO>> response = webClientBuilder.build()
                .post()                                              // ① method
                .uri("http://product-service/v1/products/get-by-ids") // ② URI (tên service)
                .bodyValue(productIds)                               // ③ body
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<ProductDTO>>>() {})
                .block();                                            // ④ CHỜ — đây là SYNC

        if (response == null || response.getData() == null) {         // ⑤ validate
            throw new ApplicationException("Dữ liệu truyền sang product service bị sai");
        }
        return response.getData();
    }
}
```

**Năm thứ cần quan tâm:**

| # | Thành phần | Ghi chú |
|---|---|---|
| ① | **Phương thức** | `post()` — truyền list ID, không nhét vừa query param |
| ② | **URI** | Dùng **tên service** (`http://product-service`), không hardcode IP — xem [file 07](07-service-discovery.md) |
| ③ | **Body** | Dữ liệu truyền vào |
| ④ | **Kiểu trả về** | Dùng `ParameterizedTypeReference` vì có generic |
| ⑤ | **Validate response** | `null` → throw |

> 📌 **`.block()`** chính là điểm làm cho lời gọi trở thành **đồng bộ**.

📌 `WebClient` là API mới (reactive core) thay cho `RestTemplate` đã deprecated. Còn có `FeignClient` — khai báo bằng interface + annotation, không phải viết code gọi.

---

## 3. ⚠️ BÀI HỌC LỚN — Không gọi I/O trong vòng lặp

### I/O là gì

Thao tác **đọc/ghi giao tiếp ra ngoài process**:
- Gọi sang service khác
- Query xuống database (DB cũng là một server khác)
- Đọc/ghi file, gọi API bên thứ 3

**I/O cực kỳ tốn chi phí** — chậm hơn tính toán trong RAM hàng nghìn lần.

### Ví dụ "bao cát"

> Server ở Việt Nam, server kia ở Mỹ.
> Chạy **10 lần** từ VN sang Mỹ, mỗi lần ôm **1 bao cát** — hay chạy **1 lần** ôm **10 bao**?
>
> → 1 lần / 10 bao. Nếu là 1000 bao thì càng chênh lệch khủng khiếp.

### ❌ SAI — I/O trong loop

```java
for (OrderItemRequest item : request.getItems()) {
    // ❌ Mỗi vòng lặp = 1 lần gọi mạng
    ProductDTO product = productClient.getProduct(item.getProductId());
}
```

```java
// ❌ Bên Product Service — mỗi vòng lặp 1 lần query DB
for (LockItem item : request.getItems()) {
    Product p = productRepository.findById(item.getProductId()).orElseThrow();
    p.setStock(p.getStock() - item.getQuantity());
    productRepository.save(p);      // ❌ save từng cái
}
```

### ✅ ĐÚNG — Gom nhóm (batch)

```java
// 1 lần gọi mạng cho TẤT CẢ sản phẩm
List<String> productIds = new ArrayList<>(grouped.keySet());
List<ProductDTO> products = productClient.getProductsByIds(productIds);
```

```java
// Bên Product Service — 1 query, 1 saveAll
List<String> ids = request.getItems().stream().map(LockItem::getProductId).toList();
List<Product> products = productRepository.findAllById(ids);
// ... xử lý trong RAM ...
productRepository.saveAll(products);
```

> **Quy tắc:** hạn chế I/O ít nhất có thể. Xử lý theo list thì **gom nhóm lại xử lý một lần**.

---

## 4. ⚠️ Map lookup O(1) thay vì scan List O(n)

Sau khi nhận `List<ProductDTO>`, khi loop order items lại phải **tìm** product tương ứng.

### ❌ SAI — O(n²)

```java
for (OrderItemRequest item : items) {          // n vòng
    ProductDTO product = products.stream()      // lại duyệt n phần tử
            .filter(p -> p.getId().equals(item.getProductId()))
            .findFirst().orElseThrow();
}
```

### ✅ ĐÚNG — O(n)

```java
Map<String, ProductDTO> productMap = products.stream()
        .collect(Collectors.toMap(ProductDTO::getId, p -> p));   // O(n) một lần

for (OrderItemRequest item : items) {
    ProductDTO product = productMap.get(item.getProductId());     // O(1)
    if (product == null) throw new ApplicationException("Product not exist");
}
```

| Cách tra cứu | Độ phức tạp |
|---|---|
| Duyệt `List` | `O(n)` mỗi lần → tổng `O(n²)` |
| Tra `Map` (hash) | `O(1)` mỗi lần → tổng `O(n)` |

---

## 5. Thiết kế API — nguyên tắc bảo mật

### ⚠️ TUYỆT ĐỐI KHÔNG cho client truyền `price`

```json
POST /v1/orders
{
  "customerId": "uuid-customer",
  "items": [
    { "productId": "uuid-product-1", "quantity": 2 }
  ]
}
```

> **Một API không chỉ được gọi từ Frontend. Bất kỳ ai cũng có thể gọi nó bằng Postman.**

Nếu client truyền `price` → hacker gọi API với `price: 1` và mua iPhone giá 1đ.

**Hai nguyên tắc:**
1. **Mọi thứ liên quan đến TIỀN không bao giờ nhận từ client**
2. **Cái gì suy ra được từ dữ liệu server thì không nhận từ client** — có `productId` → server tự query ra `price`

📌 *Danh sách trường không bao giờ tin client*: giá, tổng tiền, số tiền giảm, `userId`/`role` của người gọi (lấy từ token), trạng thái đơn hàng, `isAdmin`, phí ship đã tính.

### Đặt tên DTO cho request

```java
// ❌ SAI — người đọc tưởng phải truyền đủ mọi trường của Product
public ResponseEntity<?> validate(@RequestBody List<ProductDTO> products)

// ✅ ĐÚNG
public ResponseEntity<?> validate(@RequestBody List<ProductValidateRequest> request)
```

> Request DTO phải **phản ánh đúng cái client cần truyền**, không tái dùng DTO to.

### Gom trùng trước khi xử lý

Client (hoặc kẻ tấn công) có thể gửi:
```json
{ "items": [ {"productId":"P1","quantity":1}, {"productId":"P1","quantity":1} ] }
```
Không gộp → trừ stock **2 lần riêng biệt**, validate `quantity <= stock` chạy trên từng dòng nên có thể lọt.

```java
Map<String, Integer> grouped = request.getItems().stream()
        .collect(Collectors.groupingBy(
                OrderItemRequest::getProductId,
                Collectors.summingInt(OrderItemRequest::getQuantity)));
```

---

## 6. Luồng Create Order hoàn chỉnh

### Bản SYNC

```
① Group order items theo productId (gộp trùng, CỘNG DỒN quantity)
② Lấy List<productId>  →  gọi Product Service MỘT LẦN (batch)
③ Chuyển List<ProductDTO>  →  Map<productId, ProductDTO>
④ Tạo order (status = PENDING), save để lấy orderId
⑤ Loop từng order item:
      - Tra Map: productId tồn tại không?
      - quantity <= stock?
      - Tạo OrderItem (snapshot price)
      - Cộng dồn totalAmount
⑥ saveAll order items
⑦ Update lại order.totalAmount
⑧ Gọi Product Service để LOCK (trừ) stock — batch một lần
```

**Hai lần bắt buộc gọi Product Service:** (1) lấy thông tin + validate + lấy giá, (2) lock stock.

### Bản ASYNC

```
① Order Service: validate order
      ⚠️ VẪN PHẢI gọi Product Service (cần lấy giá để tính totalAmount)
② Order Service: save order status = NEW
③ Order Service: PUBLISH "order-created"
④ Product Service: CONSUME  →  lock stock
⑤ Product Service: PUBLISH "product-locked"
⑥ Order Service: CONSUME  →  update status = PREPARING
```

> ⚠️ Bước ① **vẫn là sync**. Không thể async hoàn toàn vì cần giá ngay để trả response.
> Chỉ bước **lock stock** được chuyển sang async.

---

## 7. Câu hỏi mở: đơn hàng nhiều seller

Nếu 1 order chứa sản phẩm của **nhiều seller**?

- Thực tế **phải tách thành nhiều order** — mỗi seller một order
- Lý do: **shipping**. Mỗi seller có địa chỉ lấy hàng khác nhau
- Ngoại lệ: **sản phẩm số** (digital) — không cần vận chuyển
- Không cần client truyền `sellerId` — từ `productId` server tự suy ra
- **Ngoài scope lớp**

---

## 📚 Đọc tiếp

- [04 — Kafka](04-kafka.md) — chi tiết giao tiếp bất đồng bộ
- [07 — Service Discovery](07-service-discovery.md) — vì sao URI dùng tên service
- [10 — Transaction phân tán](10-transaction-phan-tan.md) — khi luồng bị lỗi giữa chừng
