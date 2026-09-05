# 12 — Project cuối khóa

> Đề bài, yêu cầu nộp, và checklist tự kiểm tra trước khi bảo vệ.
> 📎 Nguồn: buổi 12, 13, 14

---

## 1. Technical Stack

| Công nghệ | Dùng để | File tham khảo |
|---|---|---|
| **Spring Boot 3.5.x** + Java 21 | Nền tảng | [11](11-spring-boot-cheatsheet.md) |
| **Spring Cloud Gateway** | API Gateway | [08](08-gateway-va-bao-mat.md) |
| **Eureka** | Service Discovery | [07](07-service-discovery.md) |
| **Keycloak** | Xác thực & phân quyền | [08](08-gateway-va-bao-mat.md) |
| **Kafka** | Message broker | [04](04-kafka.md) |
| **Redis** | Distributed lock + Cache | [05](05-concurrency-va-locking.md), [06](06-caching.md) |
| **ELK** | Logging tập trung | [09](09-logging-elk.md) |
| **MySQL** | Database (schema per service) | [02](02-thiet-ke-database.md) |
| ~~RabbitMQ~~ | ❌ **Không cần** — tương tự Kafka | |

> Tất cả đều đã học — không có công nghệ mới.

---

## 2. ⚠️ HAI PAIN POINT bắt buộc phải xử lý

Đây là **phần cốt lõi** của đề bài.

### ① Đáp ứng lượng REQUEST ĐỒNG THỜI lớn

> Ngày **Flash Sale** có **rất rất nhiều request đồng thời**. Hệ thống phải xử lý được.

**Kiến thức áp dụng:**

| Giải pháp | File |
|---|---|
| **Scale out** nhiều instance + Eureka + `@LoadBalanced` | [01](01-kien-truc-nen-tang.md), [07](07-service-discovery.md) |
| **Async qua Kafka** thay vì sync — Kafka làm buffer | [03](03-giao-tiep-giua-service.md), [04](04-kafka.md) |
| **Race condition**: `SELECT FOR UPDATE` hoặc **Redis distributed lock** | [05](05-concurrency-va-locking.md) |
| **Caching** giảm tải DB | [06](06-caching.md) |
| Tránh **I/O trong vòng lặp**, gom batch, dùng `Map` O(1) | [03](03-giao-tiep-giua-service.md) |
| Nhiều **partition** Kafka + nhiều consumer trong group | [04](04-kafka.md) |

### ② Gửi NOTIFICATION cho HÀNG TRIỆU USER

> Đúng 8h sáng Black Friday, gửi thông báo khuyến mãi cho **tất cả user** trong hệ thống.
> *"Bọn em dùng Shopee thì có nhận được thông báo không? Mình có **hàng triệu user**, chứ không phải một user."*

**Hướng giải quyết:**

- ❌ **Không thể** gửi tuần tự trong một vòng lặp — mất hàng giờ và timeout
- ✅ Đẩy job vào **Kafka**, chia **nhiều partition**, nhiều **consumer** trong cùng group xử lý **song song**
- ✅ **Notification Service tách riêng** — ⚠️ **không gộp vào User Service**
- ✅ Thiết kế có **channel** (email / SMS / push) và **retry** cho bản ghi gửi lỗi
- ✅ Xử lý theo **batch**, không từng user một

---

## 3. ⚠️ Các lỗi thiết kế thầy đã sửa

### Lỗi 1 — Tách Auth Service và User Service

> *"Nó bị cồng kềnh quá, em nên gộp thành một thôi. Tách ra maintain nó mệt."*

→ Hệ thống nhỏ thì **gộp Auth + User**.

### Lỗi 2 — Notification nhét vào User Service

> *"Trời ơi, sao Notification lại cho vào User Service? Thiết kế microservice kiểu gì vậy?
> **Hai thằng chẳng liên quan gì với nhau lại gộp thành một. Còn hai thằng liên quan nhau thì lại tách ra.**"*

| | |
|---|---|
| Notification | **Gửi thông báo** |
| User | **Thông tin người dùng** |

→ Hai domain khác nhau hoàn toàn, **không được gộp**.

### Lỗi 3 — System Design không thể hiện công nghệ

> *"Đừng vẽ chỉ mỗi như này — người ta sẽ không biết em có sử dụng Keycloak hay không.
> **Nhìn vào kiến trúc, người ta phải biết em đang sử dụng những công nghệ gì.**"*

→ Phải vẽ đủ: **Keycloak, Kafka, Redis, Eureka, ELK**, API Gateway.

### Lỗi 4 — Thiết kế Notification quá đơn giản

Chỉ có `title`, `content` là chưa đủ. Cần thêm **`channel`** — email, SMS, push.

**Gợi ý bảng:**
```sql
notifications         (id, user_id, type, title, content, status, created_at)
notification_channels (id, notification_id, channel, recipient, status, sent_at, retry_count)
notification_templates(id, code, channel, subject_template, body_template)
```

### Lỗi 5 — Chia service quá nhỏ

> Tách `Subject`, `Major`, `Faculty`, `Tuition`, `Schedule`, `Score` thành 7 service → **quá vụn**.
> Domain đơn giản thì **3–4 service là đủ**.

---

## 4. Sản phẩm phải nộp

| # | Yêu cầu |
|---|---|
| 1 | **Vẽ System Design** — chi tiết, thể hiện đủ **mọi công nghệ** |
| 2 | **Vẽ kiến trúc TRIỂN KHAI** (deployment architecture) |
| 3 | **Thiết kế cơ sở dữ liệu cho TỪNG microservice** |
| 4 | **Liệt kê tất cả luồng nghiệp vụ** (đặt hàng, đăng ký, login...) |
| 5 | **Code** các luồng |
| 6 | **Push lên Git** — tạo group, để **public**, gửi link vào group chat lớp |

### Yêu cầu về chất lượng

> - Vẽ bằng **draw.io**, dùng **icon** cho đẹp — *"draw.io nó có hết đấy"*
> - **Đừng làm sơ sài** — *"cả lớp đang làm sơ sài đến mức không thể sơ sài hơn"*
> - Coi như **dự án cá nhân mang đi phỏng vấn**
> - Thiết kế DB phải **professional** — tham khảo nguồn trên mạng
> - **Tự do sáng tạo**, không có khuôn khổ cố định

---

## 5. ✅ CHECKLIST tự kiểm tra trước khi nộp

### Kiến trúc
- [ ] Vẽ system design đủ 4 service + Gateway + Eureka + Kafka + Redis + Keycloak + ELK
- [ ] Vẽ kiến trúc triển khai (deployment)
- [ ] Không có service nào chia quá vụn
- [ ] Notification **tách riêng**, không gộp vào User
- [ ] Auth + User **gộp** (hệ thống nhỏ)
- [ ] Liệt kê đủ các luồng nghiệp vụ

### Database
- [ ] Mỗi service **một schema riêng**
- [ ] Không có FK xuyên service
- [ ] Dùng UUID làm PK
- [ ] Có `is_deleted` + 4 cột auditing trên mọi bảng
- [ ] `order_items` có **snapshot** `price`
- [ ] Không đặt prefix tên bảng vào tên cột
- [ ] `status` dùng `VARCHAR`, không `ENUM`

### Code
- [ ] Business logic tập trung ở tầng **service** (controller/consumer chỉ gọi xuống)
- [ ] Không gọi **I/O trong vòng lặp** — gom batch
- [ ] Dùng `Map` tra cứu O(1), không scan List
- [ ] **Không** nhận `price` từ client
- [ ] Validate lọc cả `is_deleted = 0`
- [ ] Request DTO đúng nghĩa, không tái dùng DTO to
- [ ] Log ở các điểm quan trọng, **chỉ log ID** không log full object

### Concurrency
- [ ] Có xử lý **race condition** khi trừ stock
- [ ] `@Lock(PESSIMISTIC_WRITE)` tách thành hàm riêng, không gắn lên `findById` chung
- [ ] Nếu dùng Redis lock: **có sort ID** khi tạo key

### Kafka
- [ ] Producer publish **event class riêng** (không publish thẳng entity)
- [ ] Consumer nhận `String` + `ObjectMapper` (tránh lỗi `__TypeId__`)
- [ ] Có `@RetryableTopic` + DLT
- [ ] Đặt tên topic theo **nguồn phát** (`product-locked`, không phải `order-confirm`)
- [ ] `acks: all`

### Bảo mật
- [ ] Frontend ↔ Gateway dùng **Access Token**
- [ ] Frontend **không** gọi trực tiếp Keycloak
- [ ] `client_secret` giữ ở phía server
- [ ] Gateway verify token bằng **JWKS**

### Vận hành
- [ ] ELK chạy được, xem log trên Kibana
- [ ] Eureka dashboard thấy đủ service
- [ ] Chạy được **nhiều instance** mỗi service
- [ ] Docker Compose đủ: MySQL, Kafka, Redis, Keycloak, ELK

### Git
- [ ] Push lên Git, để **public**
- [ ] README mô tả cách chạy
- [ ] Gửi link vào group chat

---

## 6. Lịch thi

| | |
|---|---|
| Trước buổi 15 | Hoàn thiện **~100%** project |
| Buổi 15 | Ôn tập + thầy review, góp ý |
| Thi | **Thứ 7** hoặc **Thứ 3 tuần sau** (2 đợt, chọn 1) |
| Hình thức | Mang chính project đi **bảo vệ** |

> Làm càng đầy đủ càng tốt — từ đó thầy hướng dẫn nên bổ sung gì để thành một **mini project** mang đi phỏng vấn.

---

## 📚 Đọc tiếp

- [CÂU HỎI PHỎNG VẤN](../CAU-HOI-PHONG-VAN.md) — chuẩn bị cho phần bảo vệ
- [THUẬT NGỮ](../THUAT-NGU.md)
