# 04 — Kafka

> Toàn bộ kiến thức Kafka: khái niệm nền, Producer/Consumer, Consumer Group, Retry, Replication, acks.
> 📎 Nguồn: buổi 4, 6, 7, 8

---

## 1. Kafka là gì

> **Coi Kafka như một database.** Có thằng ghi dữ liệu vào, có thằng đọc dữ liệu ra.

| | MySQL | Kafka |
|---|---|---|
| Sinh ra để | **Searching / filtering** | **Hàng đợi (queue)** |
| Query | `WHERE`, `JOIN`, `ORDER BY` | ❌ Không hỗ trợ |
| Đọc | Chọn lọc bản ghi bất kỳ | **Đọc cả cục, tuần tự** |
| Lưu trữ | Vĩnh viễn | **Tạm thời** (retention time) |
| Tốc độ | Nhanh | **Cực nhanh** (dùng được cho real-time) |

> *"Tao quăng cả cục vào, mày nhận cả cục. Tao không sinh ra để support searching. Muốn search thì lưu vào chỗ khác."*
> Chính vì không phải support searching nên nó **rất nhanh**.

---

## 2. ⚠️ Ánh xạ khái niệm Kafka ↔ MySQL

| MySQL | Kafka | Ý nghĩa |
|---|---|---|
| **Table** | **Topic** | Nơi chứa dữ liệu cùng loại |
| **Record** (dòng) | **Message** | Một đơn vị dữ liệu |
| **Primary Key** | **(Partition, Offset)** ⚠️ | Định danh duy nhất một message |
| Server | **Broker** | Một node Kafka |
| Cụm server | **Cluster** | Nhiều broker gộp lại — backup cho nhau |

> **Event = Message = Data** — ba cách nói **giống hệt nhau**.
> "Publish một event" = "publish một message" = "publish data".

### Producer / Consumer

| Hành động | Bên ghi | Bên đọc |
|---|---|---|
| Tên gọi 1 | **Publisher** | **Subscriber** |
| Tên gọi 2 | **Producer** | **Consumer** |
| Động từ | **Produce** | **Consume** |

### Message gồm gì

| Thành phần | Ghi chú |
|---|---|
| **Key** | Quyết định partition |
| **Value** | Nội dung — thường là **JSON** |
| **Partition** | Có thể tự chọn, hoặc để Kafka điều phối |
| **Timestamp** | Thời điểm publish |
| **Offset** | Thứ tự trong partition |

---

## 3. ⚠️ Partition — "đường ray"

### Vấn đề với 1 partition duy nhất

```
Partition 0:  [1] → [2] → [3] → [4] → [5]
```
Phải đọc xong `1` mới được đọc `2`... **Tuần tự, chậm.**
Thuê 3 người đọc cũng **vô nghĩa** — người 2 vẫn phải chờ người 1 xong.

### Giải pháp: nhiều "đường ray"

```
Topic "order-created" (5 partitions)

Partition 0:  [msg] [msg] [msg]  ← consumer 1 đọc
Partition 1:  [msg] [msg]        ← consumer 2 đọc   ← ĐỌC SONG SONG
Partition 2:  [msg] [msg] [msg]  ← consumer 3 đọc
Partition 3:  [msg]              ← consumer 4 đọc
Partition 4:  [msg] [msg]        ← consumer 5 đọc
```

- **Ghi nhanh hơn**: không phải chờ message trước ghi xong
- **Đọc nhanh hơn**: nhiều consumer đọc song song
- Trong **cùng 1 partition**, thứ tự vẫn được giữ tuyệt đối
- Producer **không cần chỉ định** partition — để Kafka tự rải đều

📌 Số partition thường cấu hình **3 hoặc 6**, không nên quá nhiều.

### 🎯 Offset

**Offset = THỨ TỰ của message trong partition** (không phải ID). Giống xếp hàng — mỗi người một số.

⚠️ **Offset MỘT MÌNH không tương đương primary key.** Mỗi partition đều có offset bắt đầu từ 0 —
partition 1 có offset 1, partition 5 cũng có offset 1.

→ Primary key thật sự là cặp **`(partition, offset)`**.

### ⚠️ TRADE-OFF: Partition làm MẤT THỨ TỰ TOÀN CỤC

> *"Để ý không kỹ là sau này code rất mệt."*

```
Sự kiện thứ tự đúng:  ORDER-1 CREATED → UPDATED → CANCELLED

Khi rải partition:
Partition 1:  [ORDER-1 CREATED]
Partition 3:  [ORDER-1 UPDATED]
Partition 4:  [ORDER-1 CANCELLED]

→ 3 consumer đọc song song
→ Consumer partition 4 có thể xử lý CANCELLED TRƯỚC khi CREATED
→ ❌ LỖI LOGIC
```

| | 1 partition | Nhiều partition |
|---|---|---|
| Thứ tự | ✅ Đảm bảo tuyệt đối | ❌ Chỉ trong từng partition |
| Tốc độ | Chậm | ✅ Nhanh |

📌 **Cách giải quyết thực tế: dùng message KEY.**
Kafka đảm bảo cùng một `key` luôn vào cùng một partition (`partition = hash(key) % numPartitions`).
Set `key = orderId` → mọi sự kiện của cùng một đơn hàng vào cùng 1 partition → **giữ đúng thứ tự cho đơn đó**,
vẫn song song được giữa các đơn khác nhau. Đây chính là lý do trường `key` tồn tại.

📌 Với các công việc **độc lập** (100 job không phụ thuộc nhau) thì không cần lo thứ tự.

---

## 4. Retention time — Kafka KHÔNG lưu vĩnh viễn

> Kafka **không phải nơi recommend để lưu dữ liệu vĩnh viễn.** Muốn lưu lâu và query → dùng MySQL.

Cấu hình `Time to retain data`, ví dụ `1 day` → message quá 1 ngày bị **xóa tự động**.

---

## 5. Setup — Docker Compose

- Thầy gửi file `docker-compose.yml` trong chat lớp
- Comment lại service **MySQL** trong file đó (máy đã có MySQL riêng)

```bash
docker compose up -d
```

- Kafka UI chạy ở **cổng 8081** — tạo topic, publish/consume thủ công, xem partition & offset

| Field khi tạo topic | Ý nghĩa |
|---|---|
| Topic name | vd `order-created` |
| Number of partitions | Số "đường ray" |
| Replication factor | Số bản sao (xem mục 10) |
| Time to retain data | Giữ message bao lâu |

---

## 6. Kafka PRODUCER

### Dependency
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### Config
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
```

**Vì sao cần serializer?**
> Java làm việc với **Object**. Kafka chỉ nhận **byte[]**.

| | Producer | Consumer |
|---|---|---|
| Chiều | Object → byte[] | byte[] → Object |
| Tên | **Serializer** | **Deserializer** |
| Key (String) | `StringSerializer` | `StringDeserializer` |
| Value (JSON) | `JsonSerializer` | `JsonDeserializer` |

⚠️ `JsonSerializer` thuộc package `org.springframework.kafka.support.serializer` (Spring Kafka),
**khác** package của `StringSerializer` (`org.apache.kafka.common.serialization` — Kafka gốc).

### Publish message

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_ORDER_CREATED = "order-created";

    public Order create(CreateOrderRequest request) {
        Order createdOrder = orderRepository.save(order);
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

        // Dùng EVENT class riêng, không publish thẳng entity
        OrderCreatedEvent event = orderMapper.toEvent(createdOrder);
        event.setOrderItems(savedItems);

        kafkaTemplate.send(TOPIC_ORDER_CREATED, event);
        log.info("Publish new order success");
        return createdOrder;
    }
}
```

`send()` có nhiều overload:
- `send(topic, value)` — bỏ qua key
- `send(topic, key, value)` — có key (key quyết định partition)

### ⚠️ Tách class Event riêng

**Vấn đề:** entity `Order` **không chứa** `order items`. Product Service cần items để biết lock gì.

```java
// package com.vti.order.event
@Getter @Setter
public class OrderCreatedEvent extends Order {
    private List<OrderItem> orderItems;
}
```

Bên **Product Service** cũng phải tạo class `OrderCreatedEvent` + `Order` + `OrderItem` tương ứng để parse.

---

## 7. Kafka CONSUMER

### Config
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: product-service
      auto-offset-reset: latest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

### Listener
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final ObjectMapper objectMapper;      // ⚠️ INJECT bean, không new mỗi lần
    private final ProductService productService;

    @KafkaListener(topics = "order-created")
    public void handle(String message) throws JsonProcessingException {
        OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

        List<LockProductItem> items = event.getOrderItems().stream()
                .map(i -> new LockProductItem(i.getProductId(), i.getQuantity()))
                .toList();

        productService.lockProduct(new LockProductRequest(items));   // gọi SERVICE
    }
}
```

> ⚠️ **Consumer đóng vai trò như Controller** — chỉ gọi xuống service.
> **Không viết business logic trong consumer.** Toàn bộ logic tập trung ở tầng service.

```java
@Getter @Setter @ToString
@JsonIgnoreProperties(ignoreUnknown = true)    // ⚠️ bắt buộc
public class Order { ... }
```

---

## 8. ⚠️ CONSUMER GROUP

### Vì sao cần

Khi tải lớn, ta chạy **nhiều instance** của cùng một service.
Nếu mỗi instance đều consume message #1 → **1 công việc bị làm 10 lần**.

**Giải pháp:** các instance cùng khai `group-id` → Kafka hiểu chúng **cùng một nhóm**.
Message #1 đã được instance A consume → instance B..J **không consume nữa**.

### Hai tầng quan hệ

```
Topic "order-created"
│
├── Consumer Group "product-service"          ← offset riêng, đọc tới msg 102
│     ├── consumer 1 (server 1)
│     ├── consumer 2 (server 2)
│     └── consumer 3 (server 3)               ← chia nhau message trong group
│
└── Consumer Group "payment-service"          ← offset riêng, đọc tới msg 200
      ├── consumer 1
      └── consumer 2
```

1. **Một topic → nhiều consumer group.** Các group đọc **hoàn toàn độc lập**, offset riêng.
2. **Một consumer group → nhiều consumer.** Các consumer trong group **chia sẻ** công việc.

### 🍰 Ẩn dụ "cái bánh"

> **1 partition = 1 cái bánh. 1 consumer = 1 người ăn.**

| Tình huống | Kết quả |
|---|---|
| 3 bánh, 1 người | 1 người ăn **cả 3 cái**, ăn **song song**, không hề chậm |
| 3 bánh, 2 người | 1 người ăn 2 cái, 1 người ăn 1 cái |
| 3 bánh, 3 người | Mỗi người 1 cái — **tối ưu** |
| 3 bánh, 4 người | 1 người **ngồi đói** |
| Đang ăn thì 1 người chết | Người khác **tiếp quản** (rebalance) |

**Hai luật bất di bất dịch:**
1. **Một partition CHỈ được đọc bởi MỘT consumer** trong cùng group
2. **Một consumer CÓ THỂ đọc nhiều partition** — song song, không chậm

> **Số consumer hoạt động tối đa = số partition.** Thêm nữa là lãng phí.

### Group "chết" khi nào

> Group chỉ chết khi **không còn một consumer nào**. 10 instance, 9 chết còn 1 → group **vẫn sống**.

---

## 9. ⚠️ `auto-offset-reset` — bẫy hay gặp

| Giá trị | Nghĩa |
|---|---|
| `latest` | Đọc từ message **mới nhất** ← thường dùng |
| `earliest` | Đọc từ message **đầu tiên** |

### 🎯 CHỈ có tác dụng LẦN ĐẦU group đăng ký

```
Lần đầu group join Kafka
   → Kafka: "Mày mới, bắt đầu đọc từ đâu?" → đọc auto-offset-reset

Group đã đọc tới offset 101, service CHẾT 15 NGÀY
   → Topic tích lũy tới 200 message

Service SỐNG LẠI cùng group-id
   → Kafka: "15 ngày trước mày đã đọc tới 101 rồi"
   → BẮT ĐẦU TỪ 102, không phải 200, cũng không phải 0
   → auto-offset-reset lúc này VÔ NGHĨA
```

> **Ngoại lệ:** nếu lần đầu đăng ký mà consume **liên tục lỗi**, offset chưa commit → coi như chưa đăng ký thành công → đổi config **vẫn có tác dụng**.

---

## 10. ⚠️ REPLICATION FACTOR

**Replica = số lượng bản sao** của dữ liệu topic.

```
replica = 2, có 2 broker:
   Broker 1: [bản CHÍNH]     ← Leader
   Broker 2: [bản SAO]       ← Follower

Broker 1 chết → vẫn còn bản sao ở Broker 2 ✅
```

### Luật: `replica` ≤ số `broker`

| Broker | Replica | Hợp lệ? | Lý do |
|---|---|---|---|
| 1 | 1 | ✅ | |
| 1 | 2 | ❌ | Bản chính và sao cùng 1 server → server chết là mất hết, **vô nghĩa** |
| 2 | 2 | ✅ | |
| 2 | 3 | ❌ | Không đủ server |
| 5 | 3 | ✅ | 1 chính + 2 sao, 2 server còn lại không chứa gì |

> Docker Compose của lớp chỉ có **1 broker** → chỉ config được `replica = 1`.

### Leader & Follower

```
Broker A: [Partition 0 — LEADER]     ← MỌI thao tác đọc/ghi đều ở đây
Broker B: [Partition 0 — FOLLOWER]   ← chỉ SAO CHÉP
Broker C: [Partition 0 — FOLLOWER]   ← chỉ SAO CHÉP
```

- Leader chết → một Follower được **bầu lên** ngay lập tức
- ⚠️ **Đọc VÀ ghi đều làm việc với LEADER.** Follower chỉ sao chép

---

## 11. ⚠️ ACKS (chỉ cho PRODUCER)

```yaml
spring:
  kafka:
    producer:
      acks: all       # 0 | 1 | all
```

**Câu hỏi `acks` trả lời:** *khi publish, phải có bao nhiêu broker xác nhận thì mới coi là thành công?*

| `acks` | Chờ ai xác nhận | Tốc độ | Rủi ro |
|---|---|---|---|
| **`0`** | **Không chờ ai** — quăng lên rồi kệ | Nhanh nhất | **Mất message** nếu server chết |
| **`1`** | Chỉ **Leader** | Trung bình | Leader confirm xong **chết trước khi follower sao chép** → mất message |
| **`all`** | **Leader + TẤT CẢ Follower** | Chậm nhất | **An toàn nhất — không mất message** |

> **Mặc định là `all`** vì an toàn nhất. Spring Kafka còn có cơ chế **retry khi publish fail**.

---

## 12. ⚠️ RETRY: Blocking vs Non-Blocking

### Hai loại lỗi

| Loại | Ví dụ | Retry có ý nghĩa? |
|---|---|---|
| **Có thể retry** | `TimeoutException`, DB lỗi, service khác bận, mạng chập chờn | ✅ Có |
| **KHÔNG thể retry** | `BusinessException` — "Product không tồn tại", `orderId` null | ❌ Vô nghĩa |

> Nếu `id` bằng null thì retry 1 tỷ lần nó vẫn null.

### Blocking Retry (MẶC ĐỊNH — có vấn đề)

```
Message 1 lỗi → retry ngay → lỗi → retry ngay → ... (10 lần) → bỏ qua
                └──────────────── Message 2 PHẢI CHỜ ────────────────┘
```

> Retry 100 lần, mỗi lần 1 giây → **Message 2 phải chờ 100 giây**.
> Việc retry đang **BLOCK** toàn bộ message phía sau.

Và: retry ngay lập tức thường vô ích. DB chết 1 tiếng thì retry sau 1 mili-giây vẫn chết.

### Non-Blocking Retry (GIẢI PHÁP)

```
topic: order-created
   │ Message 1 lỗi
   ├──────────────► order-created-retry-2000    (retry sau 2 giây)
   │                     │ vẫn lỗi
   │                     ├──────► order-created-retry-4000   (sau 4 giây)
   │                     │             │ vẫn lỗi
   │                     │             ├──────► order-created-retry-8000
   │                     │             │             │ vẫn lỗi
   │                     │             │             └──────► order-created-DLT
   │
   └─ Message 2 được consume NGAY ✅
```

```java
@RetryableTopic(
    attempts = "4",
    backoff = @Backoff(delay = 2000, multiplier = 2.0),  // 2s → 4s → 8s
    exclude = { BusinessException.class }                 // lỗi này KHÔNG retry
)
@KafkaListener(topics = "order-created")
public void handle(String message) { ... }
```

| Tham số | Ý nghĩa |
|---|---|
| `attempts` | Số lần thử |
| `backoff.delay` | Thời gian chờ lần đầu (ms) |
| `backoff.multiplier` | Hệ số nhân — exponential backoff |
| `exclude` | Exception **không** retry |

> **Spring Kafka tự tạo topic, tự đẩy message.** Không phải tự code — nhưng **vẫn phải hiểu concept**.

### Dead Letter Topic (DLT)

Hết số lần retry → message vào `<topic>-DLT`.

> **Vào DLT rồi thì KHÔNG consume nữa.** Nó đã không xử lý đúng logic thì consume tiếp cũng vô nghĩa.
> Lúc đó chỉ có **xử lý bằng tay**.

### ⚠️ Hành vi retry khác nhau tùy chỗ lỗi

```
byte[] ──deserialize──► Object ──► @KafkaListener method
         ▲                              ▲
    lỗi ở đây                      lỗi ở đây
    → retry VÔ HẠN                 → retry 10 lần rồi skip
```

| Lỗi ở đâu | Hành vi |
|---|---|
| **TRƯỚC** khi vào listener (deserialize fail) | **Retry VÔ HẠN** — kẹt vĩnh viễn |
| **TRONG** thân listener (business logic throw) | Retry ~10 lần rồi **bỏ qua**, commit offset, đọc tiếp |

**Và:** consume **thất bại** thì offset **không commit** → restart sẽ **đọc lại message đó**.

---

## 13. ⚠️ Ba lỗi kinh điển

### Lỗi 1 — `The class is not in the trusted packages`

Spring Kafka mặc định không cho deserialize về class bất kỳ (chống deserialization attack).

```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.json.trusted.packages: "*"
```

### Lỗi 2 — `__TypeId__` header — LỖI KINH ĐIỂN CỦA MICROSERVICE

`JsonSerializer` **tự nhét header `__TypeId__`** = tên đầy đủ class bên Producer:

```
__TypeId__ : com.example.vti.order.entities.Order
```

Consumer cố convert về **đúng class đó**. Nhưng Product Service **không có** class đó → lỗi.

**Giải pháp lớp dùng:** nhận value về **`String`**, tự parse bằng `ObjectMapper`.

| Cách khác | Ghi chú |
|---|---|
| `spring.json.use.type.headers: false` + `spring.json.value.default.type` | Bỏ qua header, ép về class chỉ định |
| Tách **shared module** chứa event class dùng chung | Chuẩn nhất, nhưng tạo coupling |
| **Avro + Schema Registry** | Chuẩn công nghiệp cho hệ thống lớn |

### Lỗi 3 — `UnrecognizedPropertyException`

JSON có trường `isDeleted`, `createdAt`... nhưng class Consumer chỉ khai 4 trường.

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order { ... }
```

---

## 14. ⚠️ Đặt tên Topic

```
❌ order-confirm      ← sai
✅ product-locked     ← đúng
```

**Hai lý do:**
1. **Topic đặt theo NGUỒN PHÁT + trạng thái**, không theo hệ quả.
   Message xuất phát từ Product Service → tên bắt đầu bằng `product`.
2. **Không ràng buộc vào một consumer cụ thể.**
   `product-locked` chỉ nói *"tao đã lock xong"* — ai muốn làm gì với thông tin đó là việc của họ.

### Chia topic nhỏ hay gộp?

| Cách | Ưu | Nhược |
|---|---|---|
| 1 topic `order` chung | Ai cần toàn bộ chỉ consume 1 topic | Consume cả những event **không cần** |
| **Nhiều topic** (`order-created`, `order-cancelled`) ← prefer | Dễ quản lý, chỉ nghe cái mình cần | Ai cần toàn bộ phải consume nhiều topic |

---

## 15. Kafka vs RabbitMQ

| | Kafka | RabbitMQ |
|---|---|---|
| Cơ chế | **PULL** — consumer **chủ động kéo** | **PUSH** — broker **chủ động đẩy** |
| Lưu trữ | Ghi xuống **disk**, giữ theo retention | Xóa sau khi consume |
| Quy mô | Rất lớn | Nhỏ hơn |

> **Best practice: LUÔN DÙNG KAFKA.**
> Những gì RabbitMQ làm được thì Kafka cũng làm được.
> Ngân hàng / viễn thông gần như 100% dùng Kafka. JD tuyển dụng cũng phổ biến hơn hẳn.

📌 *Khi nào RabbitMQ vẫn hợp lý*: routing phức tạp (topic/fanout/direct exchange), per-message TTL, priority queue, hệ thống nhỏ không cần replay.

---

## 📚 Đọc tiếp

- [03 — Giao tiếp giữa Service](03-giao-tiep-giua-service.md)
- [10 — Transaction phân tán](10-transaction-phan-tan.md) — Kafka không có transaction chung với DB
