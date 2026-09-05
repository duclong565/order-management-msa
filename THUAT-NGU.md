# Thuật ngữ

> Tra cứu nhanh mọi thuật ngữ trong học phần.


| Thuật ngữ | Nghĩa |
|---|---|
| **Monolithic** | Kiến trúc một khối, toàn bộ app trong 1 codebase/1 deployment |
| **Microservice** | Kiến trúc chia hệ thống thành nhiều service nhỏ, độc lập |
| **Scale up / Vertical** | Tăng CPU/RAM cho 1 máy |
| **Scale out / Horizontal** | Tăng số lượng máy/instance |
| **Stateless** | Service không giữ state giữa các request → scale out được |
| **API Gateway** | Cổng vào duy nhất từ ngoài vào cụm service |
| **Service Discovery** | Cơ chế service tự tìm địa chỉ của nhau (Eureka, Consul) |
| **Load Balancer** | Phân phối request tới nhiều instance |
| **Database per Service** | Mỗi service sở hữu DB riêng, không chia sẻ |
| **Bounded Context** | Ranh giới ngữ nghĩa của một domain (DDD) |
| **Saga** | Pattern xử lý transaction xuyên nhiều service |
| **Eventual Consistency** | Dữ liệu nhất quán *sau một khoảng thời gian*, không tức thì |
| **Circuit Breaker** | Ngắt mạch khi service phụ thuộc lỗi, tránh lan sập |
| **Idempotency** | Gọi API nhiều lần cho kết quả như gọi 1 lần |
| **Distributed Tracing** | Lần vết 1 request qua nhiều service (trace-id) |
| **Distributed Monolith** | Anti-pattern: tách service nhưng vẫn coupling chặt — tệ nhất |
| **JWT** | JSON Web Token — token self-contained dùng cho auth stateless |
| **Snapshot data** | Copy dữ liệu tại thời điểm giao dịch vào bảng của mình |
| **Versioning** | Mỗi lần sửa tạo bản ghi mới + tăng version, không update bản cũ |
| **Adjacency List** | Lưu cây bằng cột `parent_id` tự tham chiếu |
| **Soft delete** | Xóa mềm — đánh dấu `is_deleted` thay vì `DELETE` thật |
| **Auditing** | 4 cột `created_at/by`, `last_modified_at/by` trên mọi bảng |
| **AuditorAware** | Bean nói cho JPA biết "ai" đang thao tác để điền `created_by` |
| **@MappedSuperclass** | Cho phép entity con kế thừa cột từ class cha (BaseEntity) |
| **MapStruct** | Thư viện map DTO↔Entity, sinh code lúc compile (nhanh hơn ModelMapper) |
| **Sync communication** | Service gọi service khác và **đợi** phản hồi (REST/gRPC) |
| **Async communication** | Service phát event, service khác lắng nghe (Kafka) |
| **Kafka Topic** | Tương đương Table trong MySQL |
| **Kafka Message** | Tương đương Record (một dòng dữ liệu) |
| **Partition** | "Đường ray" trong topic — cho phép ghi/đọc song song |
| **Offset** | Thứ tự của message trong một partition |
| **Broker** | Một server Kafka |
| **Producer / Publisher** | Bên ghi dữ liệu vào Kafka |
| **Consumer / Subscriber** | Bên đọc dữ liệu từ Kafka |
| **Retention time** | Thời gian Kafka giữ message trước khi xóa |
| **PULL vs PUSH** | Kafka = consumer chủ động kéo; RabbitMQ = broker chủ động đẩy |
| **Consumer Group** | Nhóm consumer chia sẻ việc đọc 1 topic — mỗi message chỉ 1 consumer trong group xử lý |
| **Replication Factor** | Số bản sao của partition; không được lớn hơn số broker |
| **Leader / Follower** | Leader giữ bản chính (mọi đọc/ghi); Follower chỉ sao chép, sẵn sàng thay thế |
| **acks** | Producer chờ bao nhiêu broker xác nhận: `0` / `1` / `all` (mặc định, an toàn nhất) |
| **DLT** | Dead Letter Topic — nơi chứa message retry hết số lần vẫn fail, phải xử lý tay |
| **Blocking Retry** | Retry ngay tại chỗ, **chặn** các message phía sau |
| **Non-Blocking Retry** | Đẩy message lỗi sang topic retry riêng, message sau vẫn chạy (`@RetryableTopic`) |
| **Race Condition** | Hai tiến trình cùng đọc + ghi một record → dữ liệu sai |
| **Pessimistic Lock** | `SELECT ... FOR UPDATE` — khóa record, transaction khác phải chờ |
| **Optimistic Lock** | `@Version` — không khóa, phát hiện xung đột lúc update rồi retry |
| **Distributed Lock** | Lock qua bên thứ ba (Redis) khi có nhiều instance cùng ghi |
| **Redis** | DB key-value trên RAM, `O(1)`, **single-threaded** → hợp làm lock |
| **Redisson** | Thư viện Java cung cấp `RLock` để làm distributed lock trên Redis |
| **waitTime / leaseTime** | Chờ tối đa bao lâu để lấy lock / giữ lock tối đa bao lâu trước khi Redis tự xóa |
| **Compensating Transaction** | "Rollback" trong microservice — đổi status sang CANCELLED, không xóa dữ liệu |
| **Saga Pattern** | Pattern quản lý transaction phân tán (Choreography / Orchestration) |
| **Outbox Pattern** | Ghi event vào bảng `outbox` cùng transaction với data, tiến trình riêng publish lên Kafka |
| **`__TypeId__`** | Header Spring Kafka nhét vào message — nguyên nhân lỗi deserialize giữa 2 service |
| **I/O** | Thao tác giao tiếp ra ngoài process (gọi service, query DB) — rất tốn chi phí, tránh đặt trong vòng lặp |
| **Instance** | Một bản đang chạy của service; nhiều instance = nhiều port khác nhau |
| **Service Registry** | "Cuốn sổ cái" ghi service nào có instance nào, IP/port, sống hay chết |
| **Service Discovery** | Cơ chế service tìm địa chỉ của service khác qua Registry |
| **Eureka Server** | Ứng dụng giữ sổ cái (port mặc định `8761`) |
| **Eureka Client** | Thư viện nhúng vào mỗi service để tự gửi heartbeat |
| **Heartbeat** | Tín hiệu định kỳ (~5s) báo "tao còn sống"; quá hạn → bị loại khỏi sổ |
| **`@LoadBalanced`** | Annotation Spring Cloud dạy `WebClient` hỏi Eureka và phân tải |
| **Round Robin** | Chọn instance lần lượt 1→2→3→1; mặc định của Spring Cloud LoadBalancer |
| **HPA** | Horizontal Pod Autoscaler — K8s tự scale instance khi CPU/RAM vượt ngưỡng |
| **Deadlock** | A giữ P1 chờ P2, B giữ P2 chờ P1 → chờ nhau vĩnh viễn |
| **Bottleneck** | Nút thắt cổ chai — Gateway là một ví dụ, mọi request đều qua nó |
| **ELK Stack** | Elasticsearch + Logstash + Kibana — bộ lưu trữ & xem log tập trung |
| **Elasticsearch** | "DB" chứa log (≈ MySQL); cũng là search engine |
| **Logstash** | Trung gian nhận log, filter/transform rồi đẩy vào Elasticsearch |
| **Kibana** | GUI xem log (≈ MySQL Workbench), port `5601` |
| **Index** | ≈ Table trong MySQL (Elasticsearch) |
| **Document** | ≈ Record trong MySQL (Elasticsearch) |
| **Index Pattern** | Mẫu gom nhiều index để xem chung, vd `*-service-*` |
| **`logback-spring.xml`** | File cấu hình appender CONSOLE + LOGSTASH |
| **Caching** | Lưu snapshot dữ liệu để lần sau không phải tính lại |
| **Cache Invalidation** | Làm mới/xóa cache khi dữ liệu đổi — 1 trong 2 việc khó nhất ngành |
| **Local Cache** | Cache trên chính máy chạy instance — nhanh, không share được (**Caffeine**) |
| **Global / Distributed Cache** | Cache trên server riêng, share giữa instance (**Redis**) |
| **Caffeine** | Thư viện local cache Java phổ biến nhất |
| **EhCache** | Local cache khác, ít prefer hơn Caffeine |
| **TTL / `expireAfterWrite`** | Thời gian sống của cache — giới hạn mức độ outdated |
| **`maximumSize`** | Số entry tối đa được cache |
| **`@Cacheable`** | Đọc: có cache trả cache, không thì chạy hàm rồi cache |
| **`@CacheEvict`** | Xóa cache; `allEntries = true` xóa toàn bộ |
| **`@CachePut`** | Luôn chạy hàm rồi ghi đè cache |
| **`@Primary`** | Khi có 2 bean cùng kiểu, Spring ưu tiên bean này |
| **Spring Cloud Gateway** | Thư viện dựng API Gateway (`spring-cloud-starter-gateway`) |
| **Basic Auth** | Truyền username/password mọi request — vĩnh viễn, dễ lộ |
| **Access Token** | Chuỗi dài đại diện credential, **có hạn sử dụng** — chuẩn hiện nay |
| **Private / Public key** | Private ký token (Auth Server giữ); public verify chữ ký, có thể rotate |
| **Keycloak** | Open source lo xác thực/phân quyền: quản lý user, cấp token, verify token |
| **CDC** | Change Data Capture — đọc binlog DB để bắt thay đổi (vd **Debezium**) |
| **binlog** | Nhật ký thay đổi của MySQL — nguồn cho CDC đọc |
| **Realm** | "Vương quốc" trong Keycloak — nơi user đăng nhập, chứa public key |
| **Client (Keycloak)** | Đại diện một **hệ thống**: `client_id` + `client_secret` |
| **Mã hóa đối xứng** | Mã hóa & giải mã dùng **chung một key** (AES, DES) |
| **Mã hóa bất đối xứng** | Mã hóa một key, giải mã key khác (RSA, ECDSA) — nền tảng của JWT/HTTPS |
| **JWT** | Token 3 phần: `header.payload.signature` |
| **Signature** | Sinh từ **4 thứ**: header + payload + thuật toán + **private key** |
| **Payload** | Nội dung token — **decode được**, không để dữ liệu nhạy cảm |
| **JWKS** | JSON Web Key Set — endpoint Keycloak expose bộ **public key** để backend verify token |
| **kid** | Key ID trong header token, dùng chọn đúng key trong JWKS |
| **Key rotation** | Keycloak đổi cặp khóa định kỳ → client phải fetch lại JWKS |

---

---

## 📚 Xem thêm

- [Danh sách file theo chủ đề](README.md)
- [Câu hỏi phỏng vấn](CAU-HOI-PHONG-VAN.md)
