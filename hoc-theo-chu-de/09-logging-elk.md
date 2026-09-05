# 09 — Logging & ELK Stack

> Centralized logging cho hệ thống Microservice.
> 📎 Nguồn: buổi 13

---

## 1. Vì sao Logging quan trọng

### Log là gì
> Các dòng thông tin ứng dụng in ra để **truy vết xem ứng dụng đã chạy như nào**.
> Không chỉ khi lỗi — chạy bình thường cũng có log.

### 🎯 Tình huống quyết định

> Bạn làm hệ thống thương mại điện tử. Khách hàng gọi điện:
> *"Tối hôm qua lúc 8 giờ, tôi đang thanh toán thì hệ thống bị lỗi."*
>
> Bạn có phải xử lý không?

> **Cho dù lỗi do em hay do ai thì em cũng phải xử lý hết.** Em là người phát triển sản phẩm này.

**Vấn đề:** chuyện đã xảy ra hôm qua. Không debug lại được.
→ **Thứ duy nhất còn lại là LOG.**

### Hệ quả

| Câu hỏi | Trả lời |
|---|---|
| Có cần lưu trữ log không? | ✅ **Chắc chắn.** Log quan trọng như dữ liệu, **không thể bị mất** |
| Lưu bao lâu? | 30 ngày / 60 ngày / 6 tháng / 1 năm. Cũ hơn thì xóa vì **log rất nhiều** |
| Có cần lưu log của TẤT CẢ microservice? | ✅ Có |

→ Log của mọi service phải **đổ về một chỗ tập trung**.

---

## 2. ELK Stack

**ELK** = 3 công cụ liên quan nhau. **Từ khóa xuất hiện nhiều trong JD tuyển dụng.**

| Chữ | Công cụ | Vai trò | Tương đương |
|---|---|---|---|
| **E** | **Elasticsearch** | "Cơ sở dữ liệu" chứa log | ≈ **MySQL** |
| **L** | **Logstash** | Đứng giữa — nhận log, filter/biến đổi rồi đẩy vào ES | — |
| **K** | **Kibana** | Giao diện xem log trực quan | ≈ **MySQL Workbench** |

### Vì sao cần Logstash ở giữa?

> Đẩy log trực tiếp về Elasticsearch **đôi khi không ổn** — vì vẫn cần **filter** hoặc **biến đổi** log.
> Logstash xử lý nhanh hơn và cho phép transform dữ liệu.

```
[Order Service]   ─┐
[Product Service] ─┼──► [ Logstash ] ──► [ Elasticsearch ] ◄── [ Kibana ]
[Auth Service]    ─┤      (nhận,           (lưu trữ)            (xem)
[API Gateway]     ─┘       transform)
```

### Ánh xạ Elasticsearch ↔ MySQL

| MySQL | Elasticsearch |
|---|---|
| **Table** | **Index** |
| **Record** | **Document** (doc) |

📌 Elasticsearch không chỉ để chứa log — nó là **search engine** (full-text search, inverted index).
Trong e-commerce thường được dùng làm **Search Service**.

---

## 3. ⚠️ Ai dựng ELK?

> Khi đi làm, **Elasticsearch, Logstash và Kibana do đội hạ tầng (DevOps) dựng**, không phải dev.
> Dù họ dựng bằng container hay máy vật lý thì cũng là việc của họ.
>
> **Nhiệm vụ của mình:** làm sao **đẩy được log từ ứng dụng vào Logstash**, rồi vào Kibana xem.

---

## 4. Docker Compose

```yaml
services:
  elasticsearch:
    container_name: elasticsearch
    image: docker.elastic.co/elasticsearch/elasticsearch:7.17.10
    ports:
      - "9200:9200"
      - "9300:9300"
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    networks:
      - optimize-network

  logstash:
    container_name: logstash
    image: docker.elastic.co/logstash/logstash:7.17.10
    ports:
      - "5044:5044"      # port mặc định của Logstash
      - "5000:5000/tcp"  # ← ứng dụng đẩy log vào ĐÂY
      - "5000:5000/udp"
    volumes:
      - ./logstash/config/logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    networks:
      - optimize-network

  kibana:
    container_name: kibana
    image: docker.elastic.co/kibana/kibana:7.17.10
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    networks:
      - optimize-network

networks:
  optimize-network:
    driver: bridge
```

### Các port cần nhớ

| Service | Port | Ghi chú |
|---|---|---|
| Elasticsearch | `9200`, `9300` | |
| Logstash | `5044` (mặc định), **`5000` TCP/UDP** | ứng dụng đẩy log vào 5000 |
| **Kibana** | **`5601`** | vào bằng trình duyệt |

> Ba service cần chung một **network**.
> Logstash chạy mặc định `5044` nhưng hoàn toàn có thể chìa thêm port khác.

⚠️ **ELK ngốn RAM.** Máy yếu thì Elasticsearch **không start lên được**, kéo theo Kibana không kết nối được.
Start cũng khá lâu, cần kiên nhẫn.

---

## 5. File `logstash.conf`

Mount từ host vào `/usr/share/logstash/pipeline/logstash.conf`. Logstash đọc lúc khởi động.

```ruby
input {
  tcp {
    port  => 5000
    codec => json          # dữ liệu vào ở dạng JSON
  }
}

output {
  if [app_name] {
    # Có app_name → đẩy vào index riêng theo tên service + ngày
    elasticsearch {
      hosts => ["http://elasticsearch:9200"]
      index => "%{app_name}-log-%{+YYYY.MM.dd}"
    }
  } else {
    # Không có app_name → index chung
    elasticsearch {
      hosts => ["http://elasticsearch:9200"]
      index => "unknown-service"
    }
  }

  stdout { codec => rubydebug }    # in ra console để dễ theo dõi
}
```

| Phần | Ý nghĩa |
|---|---|
| **input** | Log nhận vào **qua đâu** — TCP port `5000`, dạng JSON |
| **output** | Log đẩy đi **đâu** — Elasticsearch, vào index nào |

**Quy tắc đặt tên index:**
```
order-service-log-2026.01.15
product-service-log-2026.01.15
```
→ Mỗi service một index, **mỗi ngày một index riêng**.

📌 *Vì sao tách theo ngày*: dễ xóa log cũ (drop index của ngày cũ), dễ áp policy retention, query nhanh hơn.

---

## 6. Code Spring Boot

### Dependency
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### File `logback-spring.xml`

Đặt trong `src/main/resources/`.

```xml
<configuration>

    <!-- ① Appender CONSOLE — hiển thị khi chạy local -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- ② Appender LOGSTASH — đẩy log về Logstash -->
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>localhost:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app_name":"order-service"}</customFields>
        </encoder>
    </appender>

    <!-- ③ Root — dùng CẢ HAI appender, level INFO -->
    <root level="info">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>

</configuration>
```

**Giải thích format CONSOLE:**
```
2026-01-15 14:32:10  INFO   c.v.order.OrderServiceImpl - Receive new request creating order
└─── thời gian ───┘ └level┘ └────── class ──────────┘   └───────── message ──────────┘
```

**LOGSTASH appender:**
- `destination` — nơi đẩy log tới: `localhost:5000`
- ⚠️ `customFields` — **thêm trường `app_name` vào JSON** → chính là trường mà `logstash.conf` dùng để quyết định index

### Log đẩy lên trông như thế nào

Console chỉ là **một dòng**, nhưng đẩy sang Logstash là một **JSON**:

```json
{
  "@timestamp": "2026-01-15T14:32:10.123Z",
  "level": "INFO",
  "logger_name": "com.vti.order.OrderServiceImpl",
  "message": "Receive new request creating order",
  "thread_name": "http-nio-8080-exec-1",
  "app_name": "order-service"
}
```

→ Logstash thấy `app_name = "order-service"` → đẩy vào index `order-service-log-2026.01.15`.

---

## 7. Cách viết log tốt

### Vì sao phải log?

> Ở local em **debug được**. Nhưng triển khai lên **production thì KHÔNG debug được**.

### Ví dụ trên luồng Create Order

```java
@Service
@RequiredArgsConstructor
@Slf4j                                    // ← Lombok
public class OrderServiceImpl implements OrderService {

    public Order create(CreateOrderRequest request) {

        log.info("Receive new request creating order: {}", request.toString());

        log.info("Product IDs: {}", productIds);

        List<ProductDTO> products = productClient.getProductsByIds(productIds);
        log.info("Finish calling product from product service: {}", products);

        Order createdOrder = orderRepository.save(order);
        log.info("Save created order: {}", createdOrder.getId());   // ⚠️ chỉ ID

        kafkaTemplate.send(TOPIC_ORDER_CREATED, event);
        log.info("Publish new order success");

        return createdOrder;
    }
}
```

**Nguyên tắc:**

| Điểm log | Mục đích |
|---|---|
| Đầu API — log **input** | Biết client gửi gì lên |
| Trước/sau khi gọi service khác | Biết đã gọi được chưa, nhận về gì |
| Sau khi save DB | Xác nhận đã lưu, ID là gì |
| Sau khi publish Kafka | Xác nhận event đã đi |

> ⚠️ Khi log entity vừa save, **chỉ log `getId()`**, không log full object.

📌 *Vì sao*: object đầy đủ rất dài làm log phình to; và quan trọng hơn — có thể chứa **dữ liệu nhạy cảm** (thông tin khách hàng, địa chỉ). Log là nơi nhiều người đọc được, **đừng đưa PII/mật khẩu/token vào**.

### Các level log

`ERROR` (lỗi cần xử lý) > `WARN` (bất thường nhưng chưa lỗi) > `INFO` (mốc nghiệp vụ quan trọng — mức đang dùng) > `DEBUG` > `TRACE`

Production thường để `INFO`, tạm bật `DEBUG` khi cần điều tra.

### Áp dụng cho toàn bộ service

> Apply cả **4 service**: Order, Product, Auth, API Gateway.

---

## 8. Xem log trên Kibana

### Truy cập
`http://localhost:5601` → **Explore on my own**

### Xem index đã có
```
☰ → Stack Management → Index Management
```
Thấy các index như `order-service-log-2026.01.15` với số lượng document.

### Tạo Index Pattern
```
☰ → Stack Management → Index Patterns → Create index pattern
```

| Field | Giá trị |
|---|---|
| Pattern | `*-service-*` |
| Time field | `@timestamp` |

> Pattern `*-service-*` khớp **mọi index chứa chữ `service`** → gom log của tất cả microservice vào một chỗ.

### Xem log
```
☰ → Discover
```

- Chọn index pattern vừa tạo
- **Chọn field để hiển thị**, nếu không rất khó đọc. Nên chọn: **`level`**, **`app_name`**, **`message`**
- Phía trên có **dashboard thống kê** số lượng log theo thời gian
- Mặc định **15 phút gần nhất** — có thể nới ra 7 ngày, xem log quá khứ

> Đây chính là thứ giải quyết bài toán ban đầu: khách báo lỗi hôm qua lúc 8h tối → vào Kibana, lọc theo thời gian, tìm nguyên nhân.

---

## 📚 Đọc tiếp

- [08 — Gateway & Bảo mật](08-gateway-va-bao-mat.md)
- [12 — Project cuối khóa](12-project-cuoi-khoa.md)
