# 10 — Transaction phân tán

> Vì sao `@Transactional` không hoạt động xuyên service, và các pattern xử lý.
> ⭐ Chủ đề nâng cao — **biết keyword là điểm cộng lớn khi phỏng vấn**.
> 📎 Nguồn: buổi 6, 8, 10

---

## 1. ⚠️ `@Transactional` KHÔNG hoạt động xuyên Microservice

### Tình huống

Câu hỏi thường gặp: *"Sao consume lỗi mà order vẫn được tạo? Em kỳ vọng nó rollback."*

**SAI HOÀN TOÀN.**

```java
Order created = orderRepository.save(order);        // ← ĐÃ COMMIT xuống DB tại đây
orderItemRepository.saveAll(items);                 // ← ĐÃ COMMIT
kafkaTemplate.send("order-created", event);         // ← publish sau
return created;                                      // ← trả về user
// Product Service consume lỗi ở bước sau → KHÔNG thể rollback order nữa
```

| | Monolithic | Microservice |
|---|---|---|
| DB | Order + Product **cùng 1 DB** | **2 DB độc lập** |
| `@Transactional` | ✅ Rollback được cả hai | ❌ Chỉ rollback trong **1 service** |

> Order là **một hệ thống độc lập**, Product là **một hệ thống độc lập**. Hai thằng không liên quan gì đến nhau.
> Trước thời điểm publish message, dữ liệu **đã persist, đã commit** xuống DB. **Không có cách nào rollback.**

---

## 2. Rollback trong Microservice = Compensating Transaction

> "Rollback" ở đây **không phải là xóa khỏi DB**.

Khi Product publish `product-lock-failed` và Order consume rồi đổi `status = CANCELLED` — **đó chính là một hình thức rollback**.

| Cách | Đánh giá |
|---|---|
| Xóa cứng (`DELETE`) | ❌ Hệ thống thật không được xóa cứng |
| Xóa mềm (`is_deleted = 1`) | Được |
| **Đổi status sang `CANCELLED`/`FAILED`** | ✅ Cách dùng trong lớp |

### Trường hợp Sync cũng gặp vấn đề tương tự

```
Order gọi Product (sync) → Product lock xong, ĐÃ COMMIT → trả về Order
Order làm bước tiếp theo → LỖI
→ Product đã bị lock nhưng KHÔNG ai báo Product unlock
→ Cần cơ chế bù trừ (compensating) để rollback bên Product
```

---

## 3. ⚠️ Kafka KHÔNG có transaction chung với Database

Câu hỏi trên lớp: *"Kafka có nằm trong transaction không?"*

> **KHÔNG.** Kafka là một **server riêng**, Database là một **server riêng** — hai thực thể độc lập.
> Không có cơ chế nào đảm bảo transaction xuyên hai thực thể này.
> (Kafka có transaction **nội tại** của nó, nhưng không liên quan tới DB transaction.)

### Hệ quả nguy hiểm

```java
@Transactional
public Order create(...) {
    Order order = orderRepository.save(order);   // ① ghi DB
    kafkaTemplate.send("order-created", order);  // ② publish Kafka
    // Nếu ① rollback sau ② → DB không có order, nhưng Kafka ĐÃ có event → SAI LỆCH
}
```

---

## 4. Saga Pattern

> Pattern chuẩn cho transaction phân tán.
>
> **Thầy KHÔNG dạy** vì quá phức tạp với level lớp.
> Thực tế **ít công ty triển khai** — kể cả team thầy cũng không, vì chi phí vận hành cao hơn lợi ích.
>
> ⭐ **Nên biết keyword để phỏng vấn.**

### Hai kiểu Saga

| Kiểu | Cách hoạt động |
|---|---|
| **Choreography** | Các service tự lắng nghe event của nhau, **không có điều phối viên** ← luồng lớp đang làm gần giống kiểu này |
| **Orchestration** | Có một **Saga Orchestrator** trung tâm điều phối từng bước và gọi compensating khi lỗi |

### Minh họa Choreography (luồng lớp đang làm)

```
Order Service ──publish "order-created"──►
                                          Product Service consume → lock stock
                                                    │
                        ┌───────── thành công ──────┴────── thất bại ─────────┐
                        ▼                                                     ▼
              publish "product-locked"                        publish "product-lock-failed"
                        │                                                     │
                        ▼                                                     ▼
        Order consume → status = CONFIRMED              Order consume → status = CANCELLED
                                                                    ↑
                                                        đây là COMPENSATING
```

---

## 5. Outbox Pattern

> Giải pháp cho bài toán ở mục 3 (DB commit và Kafka publish không cùng transaction).

### Ý tưởng

```
① Trong CÙNG một transaction:
      - INSERT vào bảng orders
      - INSERT event vào bảng outbox
   → cả hai cùng commit hoặc cùng rollback ✅

② Một tiến trình riêng đọc bảng outbox → publish lên Kafka → đánh dấu đã gửi
```

```sql
CREATE TABLE outbox (
    id            VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(50),      -- "Order"
    aggregate_id   VARCHAR(36),      -- orderId
    event_type     VARCHAR(50),      -- "OrderCreated"
    payload        JSON,
    status         VARCHAR(20),      -- PENDING / SENT
    created_at     DATETIME
);
```

- ✅ **DB và event luôn nhất quán**
- ✅ Đội dev **chủ động hoàn toàn**, không phụ thuộc DBA
- ❌ Phải tự viết tiến trình đọc outbox và publish

> ⭐ *"Bạn nào biết cái này đi phỏng vấn nó sẽ là một điểm cộng rất lớn."*

---

## 6. CDC — Change Data Capture

### CDC là gì

> **Change Data Capture.** Công cụ đọc **binlog** của MySQL để bắt được record nào vừa **tạo / sửa / xóa**.

**Công cụ điển hình: Debezium.**

### Hai use case

```
                    ┌──► [Elasticsearch]  → phục vụ SEARCH nhanh
[MySQL binlog] ──CDC┤
                    └──► [Kafka topic]    → service khác consume
```

**① Đẩy lên Elasticsearch để search**
> Quy mô Shopee: một câu query JOIN rất nhiều bảng product, dữ liệu cực lớn → **rất chậm**.
> Giải pháp: dùng Elasticsearch — sinh ra chuyên để searching, **siêu nhanh**.
> CDC bắt sự kiện sản phẩm tạo/sửa/xóa → đẩy lên Elasticsearch → search luôn ở đó.

**② Đẩy lên Kafka**
> Bắt được dữ liệu đơn hàng mới → publish lên topic cho service khác consume.
> → Thay thế cho việc code Outbox Pattern thủ công.

### ⚠️ Thực tế: rất khó áp dụng

> *"Bên anh đã từng **offer giải pháp này** vào ứng dụng rồi. Ở **Viettel** và cả **OneMount** — **cả hai đều bị REJECT**."*

**Hai lý do:**
1. **Không ai chịu maintain công cụ mới.** Nó là open source, đội dev không maintain nổi
2. **Phải có quyền access vào tận binlog** của database. DBA làm rất chặt, không cho

> → Cuối cùng chỉ apply được **Outbox Pattern**, vì đội dev **chủ động hoàn toàn**.

> 💬 *"Thực tế mình sẽ ít lựa chọn hơn là trên lý thuyết. Trên lý thuyết CDC rất hiệu quả và effort bỏ ra rất ít, nhưng thực tế bọn anh không thể triển khai được."*

---

## 7. So sánh Outbox vs CDC

| | **Outbox Pattern** | **CDC (Debezium)** |
|---|---|---|
| Cách hoạt động | Ghi event vào bảng `outbox` trong cùng transaction | Đọc **binlog** của DB |
| Code phải viết | Có — tiến trình đọc outbox và publish | Gần như không |
| Cần quyền đặc biệt | ❌ Không | ✅ **Quyền đọc binlog** — DBA thường từ chối |
| Cần maintain công cụ mới | ❌ Không | ✅ Có — Debezium |
| Thực tế áp dụng được | ✅ **Có** | ❌ Thường bị reject |

---

## 8. Tóm tắt các khái niệm

| Khái niệm | Ý nghĩa |
|---|---|
| **Eventual Consistency** | Dữ liệu nhất quán *sau một khoảng thời gian*, không tức thì |
| **Compensating Transaction** | Hành động bù trừ để "undo" — đổi status, không xóa |
| **Saga** | Pattern quản lý chuỗi transaction phân tán |
| **Outbox** | Ghi event cùng transaction với data, tiến trình riêng publish |
| **CDC** | Đọc binlog để bắt thay đổi |
| **Idempotency** | Gọi API nhiều lần cho kết quả như gọi 1 lần — quan trọng khi có retry |

---

## 📚 Đọc tiếp

- [04 — Kafka](04-kafka.md) — retry, DLT
- [05 — Concurrency & Locking](05-concurrency-va-locking.md)
