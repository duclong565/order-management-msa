# ⭐ Câu hỏi Phỏng vấn

> Tổng hợp mọi chỗ thầy nói *"đi phỏng vấn họ sẽ hỏi cái này"* trong suốt 14 buổi,
> kèm gợi ý trả lời và link tới phần kiến thức đầy đủ.

---

## 🔥 Nhóm 1 — Kiến trúc (cơ bản, chắc chắn bị hỏi)

### Q1. Microservice khác Monolithic thế nào? Khi nào nên dùng cái nào?

**Ý chính:**
- Monolithic: 1 codebase, 1 DB. Microservice: nhiều service độc lập, mỗi service 1 DB
- **Điểm mấu chốt: scale lệch.** Monolithic scale phải nhân bản **toàn bộ**; Microservice scale được **từng phần**
- **Monolithic KHÔNG xấu** — dự án nhỏ/startup thì nên dùng
- Microservice đổi lại: network latency, distributed transaction, debug khó, vận hành phức tạp

> 💡 Trả lời hay: nêu được **trade-off**, không phải "Microservice tốt hơn".

📎 [01 — Kiến trúc & Nền tảng](hoc-theo-chu-de/01-kien-truc-nen-tang.md)

---

### Q2. Scale up và Scale out khác nhau thế nào?

- **Scale up (vertical)**: tăng CPU/RAM cho 1 máy. Có trần cứng, cần restart, chi phí phi tuyến
- **Scale out (horizontal)**: thêm máy. Gần như vô hạn, không downtime, nhưng **app phải stateless**

> 💡 Nói thêm: Microservice ưu tiên scale out → nên state phải đẩy ra DB/Redis/JWT.

---

### Q3. Database per Service là gì? Có nhất thiết mỗi service một DB server riêng không?

- Mỗi service **sở hữu** database riêng, không share
- ❌ **Không cần** server vật lý riêng — thực tế dùng chung cụm (~3 node HA), **mỗi service một schema**
- Lý do: 10 service × 3 server = 30 server, không công ty nào chịu nổi

---

### Q4. Service A muốn lấy dữ liệu của Service B thì làm thế nào?

**Phải gọi qua API của B. Tuyệt đối không chọc thẳng vào DB của B.**

**Vì sao:**
- A phụ thuộc **cấu trúc bảng** của B → B đổi cột thì A vỡ mà B không biết
- **Phá vỡ business rule** của B (validate/tính toán nằm trong code, không trong bảng)
- Mất khả năng deploy độc lập
- Encapsulation ở cấp kiến trúc: API là hợp đồng, DB schema là chi tiết nội bộ

---

### Q5. Chia microservice thế nào cho đúng?

- Chia theo **business capability**, không theo bảng dữ liệu
- **Bounded Context** (DDD)
- High cohesion, loose coupling
- 1 team sở hữu 1 service
- A gọi B ở mọi request → nên gộp
- 2 module luôn scale cùng nhịp → tách vô ích

> ⚠️ Bẫy: "chia càng nhỏ càng tốt" là **SAI**. Chia sai tạo ra **distributed monolith** — tệ nhất.

---

## 🔥 Nhóm 2 — Concurrency (THẦY NÓI HAY HỎI NHẤT)

### Q6. ⭐ Có 2 request cùng lúc cập nhật một bản ghi thì sao?

> *"Bạn nào từ level fresher/junior trở lên đi phỏng vấn họ sẽ hỏi cái này."*

**Vấn đề — Race Condition:**
```
stock = 10
Request A: đọc 10 → trừ 2 → save 8
Request B: đọc 10 → trừ 3 → save 7    ← CÙNG LÚC
Kết quả: 7. Đúng phải là 5. ❌
```

**Giải pháp 1 — Pessimistic Lock:**
```sql
SELECT * FROM products WHERE id = 'A' FOR UPDATE;
```
Spring: `@Lock(LockModeType.PESSIMISTIC_WRITE)`

**Giải pháp 2 — Optimistic Lock:** `@Version`, phát hiện xung đột lúc update rồi retry

| | Pessimistic | Optimistic |
|---|---|---|
| Phù hợp | Tranh chấp **nhiều** (flash sale) | Tranh chấp **ít** |

> 💡 Nói thêm: **không** gắn `@Lock` lên hàm `findById` dùng chung — nhiều chỗ chỉ search sẽ bị khóa oan.

📎 [05 — Concurrency & Locking](hoc-theo-chu-de/05-concurrency-va-locking.md)

---

### Q7. ⭐ Distributed Lock là gì? Khi nào cần?

- `SELECT FOR UPDATE` **vẫn work** với nhiều instance (mỗi instance = 1 transaction, cùng 1 DB)
- Nhưng **CHẬM** khi hàng chục nghìn/triệu request đồng thời
- → Dùng **Redis** làm trọng tài: mọi instance phải xin "cờ" từ Redis trước khi vào DB
- Redis phù hợp vì **single-threaded** + lưu RAM + O(1)

**Redisson:**
```java
RLock lock = redissonClient.getLock(key);
lock.tryLock(10, 5, TimeUnit.SECONDS);   // chờ tối đa 10s, giữ lock tối đa 5s
```

> ⚠️ **Bẫy hay bị hỏi tiếp:** vì sao cần `leaseTime`?
> → Phòng instance lấy lock rồi **chết**, không gọi được `unlock()` → key nằm lại vĩnh viễn → deadlock.

---

### Q8. ⭐ Lock nhiều bản ghi thì có vấn đề gì?

**Bẫy 1 — key khác nhau do thứ tự:**
```
Instance 1: [1,2] → key "product:1,2"
Instance 2: [2,1] → key "product:2,1"    ← KHÁC NHAU → cả hai cùng vào DB ❌
```
→ **Phải SORT ID** trước khi tạo key.

**Bẫy 2 — danh sách giao nhau một phần:**
```
A lock [1,2,3] → key "1,2,3"
B lock [1,2]   → key "1,2"      ← vẫn khác nhau ❌
```
→ Phải lock **từng product một**.

**Bẫy 3 — Deadlock:**
```
A giữ P1, chờ P2
B giữ P2, chờ P1     → chờ nhau vĩnh viễn
```
→ **Luôn lock theo thứ tự đã sort** thì không bao giờ deadlock.

---

## 🔥 Nhóm 3 — Kafka

### Q9. Kafka khác database thế nào?

| | MySQL | Kafka |
|---|---|---|
| Sinh ra để | Searching/filtering | **Hàng đợi** |
| Query | `WHERE`, `JOIN` | ❌ Không |
| Lưu trữ | Vĩnh viễn | Tạm thời (retention) |

> Chính vì không support searching nên Kafka **rất nhanh**.

---

### Q10. Giải thích Topic, Partition, Offset, Consumer Group

| Kafka | ≈ MySQL |
|---|---|
| Topic | Table |
| Message | Record |
| **(Partition, Offset)** | **Primary Key** |

> ⚠️ **Bẫy:** Offset **một mình** không phải primary key — mỗi partition đều có offset từ 0.
> Phải là **cặp (partition, offset)**.

**Partition** = "đường ray", cho phép xử lý **song song**.
**Trade-off: mất tính ordering toàn cục** — chỉ giữ thứ tự trong từng partition.

**Cách giữ ordering:** dùng **message key**. `partition = hash(key) % n` → set `key = orderId` thì mọi event của cùng đơn hàng vào cùng partition.

---

### Q11. Consumer Group để làm gì?

- Nhiều instance cùng service → nếu mỗi thằng đều consume message #1 thì 1 việc làm 10 lần
- Cùng `group-id` → Kafka hiểu cùng nhóm → **chia tải, không trùng lặp**

**Hai luật:**
1. Một partition **CHỈ** được đọc bởi **MỘT** consumer trong group
2. Một consumer **CÓ THỂ** đọc nhiều partition

**Câu hỏi tình huống:** 3 partition + 4 consumer → **1 thằng ngồi không**.

---

### Q12. `auto-offset-reset` hoạt động thế nào?

`earliest` = đọc từ đầu; `latest` = đọc từ mới nhất.

> ⚠️ **Bẫy:** chỉ có tác dụng **LẦN ĐẦU group đăng ký**.
> Group đã đọc tới offset 101, chết 15 ngày, sống lại → đọc từ **102**, không phải 0 cũng không phải mới nhất.

---

### Q13. `acks` là gì?

| `acks` | Chờ ai | Rủi ro |
|---|---|---|
| `0` | Không chờ ai | **Mất message** |
| `1` | Chỉ Leader | Leader chết trước khi follower sao chép → mất |
| `all` | Leader + tất cả Follower | **An toàn nhất** (mặc định) |

---

### Q14. Replication Factor? Leader/Follower?

- Replica = số bản sao. **`replica` ≤ số `broker`**
- 1 broker mà replica=2 là **vô nghĩa** (bản chính và sao cùng máy, máy chết là mất hết)
- **Leader** giữ bản chính, **Follower** chỉ sao chép
- ⚠️ **Mọi thao tác đọc VÀ ghi đều với Leader.** Follower không phục vụ đọc

---

### Q15. ⭐ Retry Blocking vs Non-Blocking

**Blocking (mặc định):** retry ngay tại chỗ → **chặn** message phía sau. Retry 100 lần × 1s = message 2 chờ 100 giây.

**Non-Blocking:** đẩy message lỗi sang topic riêng (`-retry-2000`, `-retry-4000`, `-retry-8000`) → message 2 chạy ngay.

```java
@RetryableTopic(attempts = "4",
    backoff = @Backoff(delay = 2000, multiplier = 2.0),
    exclude = { BusinessException.class })
```

**Hai loại lỗi:**
- Retry được: timeout, DB lỗi, mạng
- **Không** retry được: business exception (`id` null thì retry 1 tỷ lần vẫn null)

**DLT:** hết retry → vào Dead Letter Topic → **xử lý bằng tay**.

> 💡 *"Kiến thức này nhiều Junior không biết. Trả lời được thì người ta đánh giá rất tốt."*

---

## 🔥 Nhóm 4 — Bảo mật & JWT

### Q16. ⭐ Access Token an toàn hơn Basic Auth ở chỗ nào?

> ⚠️ **Bẫy:** nhiều người trả lời "vì nó dài, khó đọc" — **chưa đủ**.

**Cả hai đều bị đánh cắp được** (vì đều truyền ở mọi request).

**Điểm khác biệt QUYẾT ĐỊNH: HẾT HẠN.**
- Basic Auth bị cắp → dùng **đến cuối đời** (nếu không đổi password)
- Access Token bị cắp → chỉ dùng được **5 phút**

---

### Q17. ⭐ Cấu trúc JWT? Signature sinh ra thế nào?

```
header . payload . signature
```

| Phần | Chứa |
|---|---|
| Header | **Thuật toán** ký (RS256) |
| Payload | Nội dung — **decode được, KHÔNG bí mật** |
| Signature | Chữ ký |

**Signature = f(header, payload, thuật toán, PRIVATE KEY)** — **4 thành phần**, thiếu một là không ra.

**Vì sao không giả mạo được:**
Hacker sửa payload → signature phải khác → nhưng **không có private key** để ký lại → verify bằng public key phát hiện ngay.

> 💡 Nói thêm: vì payload decode được nên **không bao giờ để dữ liệu nhạy cảm** (password, số thẻ) vào.

📎 [08 — Gateway & Bảo mật](hoc-theo-chu-de/08-gateway-va-bao-mat.md)

---

### Q18. Mã hóa đối xứng và bất đối xứng?

- **Đối xứng**: chung một key cho cả mã hóa và giải mã (AES, DES)
- **Bất đối xứng**: private key ký, public key verify (RSA, ECDSA)

> ⚠️ **Base64 KHÔNG phải mã hóa** — nó là encoding, không có key, ai cũng decode được.

**Vì sao bất đối xứng là bước ngoặt:** đối xứng phải truyền key cho nhau trước → có thể bị chặn. Bất đối xứng thì public key **cứ để lộ thoải mái**. Đây là nền tảng của HTTPS/TLS.

---

### Q19. Xác thực giữa các microservice có cần không?

**3 options, không có đúng/sai:**

| Option | Nhược điểm |
|---|---|
| Không xác thực | **Không audit được ai làm gì** — chỉ ghi `anonymous` |
| Basic Auth | Quá nhiều người biết password → audit vô nghĩa |
| Access Token | Tốn CPU giải mã; public key rotate phải lấy lại |

**Lý lẽ cho việc không cần:** từ ngoài vào bắt buộc qua Gateway đã có token; bên trong là network nội bộ.

> 💡 Trả lời hay: nêu được vấn đề thật là **AUDIT**, không phải bảo mật.

---

### Q20. Frontend có được gọi thẳng Keycloak không?

**KHÔNG.** `client_secret` là password của hệ thống — để ở frontend là **lộ ngay**.

→ Phải qua **Auth Service** giữ secret ở phía server. Đây chính là lý do Auth Service tồn tại.

---

## 🔥 Nhóm 5 — Transaction phân tán

### Q21. ⭐ `@Transactional` có rollback được xuyên microservice không?

**KHÔNG.** Order và Product là 2 DB độc lập, 2 hệ thống độc lập.
Trước khi publish message, dữ liệu **đã commit** → không rollback được.

**"Rollback" trong Microservice = Compensating Transaction** — đổi status sang `CANCELLED`, **không xóa**.

---

### Q22. Kafka có nằm trong transaction với DB không?

**KHÔNG.** Kafka và DB là 2 server độc lập. Không có cơ chế transaction xuyên hai thực thể.

**Hệ quả nguy hiểm:**
```java
@Transactional
public Order create(...) {
    orderRepository.save(order);                 // ① ghi DB
    kafkaTemplate.send("order-created", order);  // ② publish
    // ① rollback sau ② → DB không có order nhưng Kafka ĐÃ có event
}
```

→ Giải pháp: **Outbox Pattern**.

---

### Q23. ⭐ Outbox Pattern là gì?

Ghi event vào bảng `outbox` **trong cùng transaction** với data → tiến trình riêng đọc `outbox` và publish lên Kafka.

→ DB và event **luôn nhất quán**.

> 💡 *"Bạn nào biết cái này đi phỏng vấn nó sẽ là một điểm cộng rất lớn."*

---

### Q24. Saga Pattern?

Pattern quản lý transaction phân tán. **Hai kiểu:**
- **Choreography**: service tự lắng nghe event của nhau, không có điều phối viên
- **Orchestration**: có Saga Orchestrator trung tâm

> Thực tế **ít công ty triển khai** vì quá phức tạp — biết keyword là đủ.

---

### Q25. CDC là gì? Vì sao thực tế khó áp dụng?

**Change Data Capture** — đọc **binlog** MySQL bắt thay đổi (công cụ: **Debezium**).

**Vì sao bị reject** (thầy kể team đã offer ở Viettel và OneMount, cả hai bị từ chối):
1. Không ai chịu maintain công cụ mới
2. Phải có **quyền đọc binlog** — DBA làm rất chặt

---

## 🔥 Nhóm 6 — Caching

### Q26. Local Cache vs Global Cache?

| | Local (Caffeine) | Global (Redis) |
|---|---|---|
| Tốc độ | ⚡ Nhanh hơn (không qua mạng) | Chậm hơn |
| Share giữa instance | ❌ | ✅ |
| Mất khi restart | ✅ | ❌ |
| Chi phí | Không | Phải maintain server |

**Ví dụ then chốt:** 10 instance, 10 request → Global cache tính **1 lần**; Local cache tính **10 lần**.

---

### Q27. Redis cũng qua mạng, khác gì query MySQL?

- **Redis**: RAM, key-value, **O(1)** — sinh ra chỉ để làm việc đó
- **MySQL**: disk, bảng quan hệ, phải **JOIN**

---

### Q28. Cache Invalidation xử lý thế nào?

> *"Hai thứ khó nhất trong khoa học máy tính: **naming** và **cache invalidation**."*

- **TTL** (`expireAfterWrite`) — giới hạn mức độ outdated (vd tối đa 5 phút)
- **`@CacheEvict`** trên hàm update/delete
- Cache theo ID → evict theo key; cache list → `allEntries = true`

> ⚠️ Cache trên API **listing là anti-pattern** — nên cache `getById`.

---

## 🔥 Nhóm 7 — Service Discovery

### Q29. Vì sao cần Service Discovery?

- 2 app không thể cùng port → scale = nhiều port khác nhau
- Hardcode `localhost:8888` → 9 instance kia **vô nghĩa**
- Danh sách instance **thay đổi liên tục** (scale up/down, instance chết)
- → Cần "cuốn sổ" ghi instance nào đang sống

**Eureka**: service **tự báo cáo** (push). **K8s**: registry **đi hỏi** (pull).

---

### Q30. Gateway có cần nhiều instance không?

**CÓ.** Gateway **cũng là một service**, cũng là ứng dụng đang chạy.
Ẩn dụ: xây thành thì xây một cổng hay nhiều cổng? → Nhiều.

→ Gateway **cũng phải cài Eureka Client**.

---

## 🔥 Nhóm 8 — Thiết kế Database

### Q31. Giá sản phẩm thay đổi thì đơn hàng cũ tính thế nào?

**Bài toán:** mua áo 100k, mai shop tăng lên 150k → shipper thu bao nhiêu? → **100k**.

**Hai pattern:**
- **Snapshot**: copy `price` vào `order_items` lúc tạo đơn ← đơn giản, dùng khi ít trường
- **Versioning**: mỗi lần sửa tạo bản ghi mới + tăng version ← khi nhiều trường / cần full lịch sử

> ⚠️ Versioning: PK vẫn **chỉ là `id`**, không dùng composite `(id, version)`.

---

### Q32. Thiết kế category nhiều tầng?

**Adjacency List** — cột `parent_id` tự tham chiếu.

**3 nhược điểm:** xóa cha phải đệ quy, đọc cây phải đệ quy, **chỉ có 1 cha**.

**Nhiều cha:** bảng trung gian `(left_id, right_id, relationship)`.

**4 pattern:** Adjacency List / Path Enumeration / Nested Set / **Closure Table**.

---

### Q33. Vì sao dùng UUID thay auto-increment?

Chủ yếu vì **auto-increment dễ đoán** (`/order/1`, `/order/2`). Ngoài ra: merge nhiều DB không đụng, sinh được ở client.

Đánh đổi: nặng hơn, index chậm hơn.

---

## 🔥 Nhóm 9 — Hiệu năng

### Q34. Vì sao không được gọi I/O trong vòng lặp?

**Ví dụ bao cát:** chạy VN→Mỹ 10 lần mỗi lần 1 bao, hay 1 lần 10 bao?

I/O = gọi service khác, query DB, đọc file — **tốn chi phí gấp hàng nghìn lần** tính toán trong RAM.

→ **Gom nhóm (batch)**: truyền list ID, `findAllById`, `saveAll`.

---

### Q35. Tra cứu trong list vs map?

```java
for (item : items) {          // n vòng
    products.stream().filter(...)   // lại duyệt n
}   // → O(n²)  ❌

Map<String, Product> map = products.stream()
    .collect(Collectors.toMap(Product::getId, p -> p));   // O(n)
for (item : items) { map.get(item.getProductId()); }      // O(1)
// → O(n)  ✅
```

---

## 💡 Lời khuyên chung khi trả lời

1. **Trả lời generic trước, chi tiết sau.** Hỏi "2 service giao tiếp thế nào" → nói sync/async trước, rồi mới WebClient/Feign/Kafka.

2. **Luôn nêu trade-off.** Thầy nhắc đi nhắc lại: *"Đi làm không có đúng và sai."*
   Nói được "cái này nhanh hơn nhưng tốn X, phù hợp khi Y" > nói "cái này tốt hơn".

3. **Phân biệt "biết" và "đã làm".**
   > *"Có thể sản phẩm em không cần đến mức đó nên em không triển khai, nhưng em biết."*
   Đây là câu trả lời thầy gợi ý — vừa trung thực vừa thể hiện hiểu biết.

4. **Kể được vấn đề thực tế.** Ví dụ: *"Không xác thực nội bộ thì audit chỉ ghi `anonymous`, một tháng sau không truy vết được ai sửa dữ liệu."*

---

## 📚 Xem thêm

- [Danh sách file theo chủ đề](README.md)
- [Thuật ngữ](THUAT-NGU.md)
