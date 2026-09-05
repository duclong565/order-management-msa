# Microservice — Sổ tay kiến thức

> File tổng hợp kiến thức xuyên suốt học phần Microservice (14 buổi).
> Ghi chú từ bài giảng + phần bổ sung để rõ ý (đánh dấu 📌 *Bổ sung*).
> Project xuyên suốt: **hệ thống Thương mại điện tử (E-commerce)**.

---

## Mục lục

- [Buổi 1 — Tổng quan kiến trúc Microservice](#buổi-1--tổng-quan-kiến-trúc-microservice)
  - [1. Monolithic là gì](#1-monolithic-là-gì)
  - [2. Scale — khái niệm nền tảng](#2-scale--khái-niệm-nền-tảng)
  - [3. Điểm yếu của Monolithic](#3-điểm-yếu-của-monolithic)
  - [4. Microservice — giải pháp](#4-microservice--giải-pháp)
  - [5. Database per Service](#5-database-per-service)
  - [6. API Gateway](#6-api-gateway)
  - [7. Auth Service](#7-auth-service)
  - [8. Các nguyên tắc cứng (RULES)](#8-các-nguyên-tắc-cứng-rules)
  - [9. Chia service bao nhiêu là đủ](#9-chia-service-bao-nhiêu-là-đủ)
  - [10. Phân tích domain E-commerce](#10-phân-tích-domain-e-commerce)
  - [11. Scope project của lớp](#11-scope-project-của-lớp)
  - [12. Bài tập về nhà](#12-bài-tập-về-nhà)
- [Buổi 2 — Thiết kế DB & dựng Product Service](#buổi-2--thiết-kế-db--dựng-product-service)
- [Buổi 3 — Order Service & pattern dữ liệu thay đổi theo thời gian](#buổi-3--order-service--pattern-dữ-liệu-thay-đổi-theo-thời-gian)
- [Buổi 4 — JPA Auditing, luồng đặt hàng & Kafka](#buổi-4--jpa-auditing-luồng-đặt-hàng--kafka)
- [Buổi 5 — Thực hành giao tiếp Đồng bộ bằng WebClient](#buổi-5--thực-hành-giao-tiếp-đồng-bộ-bằng-webclient)
- [Buổi 6 — Tối ưu giao tiếp Sync & triển khai Kafka](#buổi-6--tối-ưu-giao-tiếp-sync--triển-khai-kafka)
- [Buổi 7 — Event class, Race Condition & Retry Non-Blocking](#buổi-7--event-class-race-condition--retry-non-blocking)
- [Buổi 8 — Ôn tập, Kafka nâng cao & Redis Distributed Lock](#buổi-8--ôn-tập-kafka-nâng-cao--redis-distributed-lock)
- [Buổi 9 — Service Discovery với Eureka](#buổi-9--service-discovery-với-eureka)
- [Buổi 10 — API Gateway & Keycloak](#buổi-10--api-gateway--keycloak)
- [Buổi 11 — Keycloak, Mã hóa bất đối xứng & Cấu trúc JWT](#buổi-11--keycloak-mã-hóa-bất-đối-xứng--cấu-trúc-jwt)
- [Buổi 12 — JWKS & Đề bài Project cuối khóa](#buổi-12--jwks--đề-bài-project-cuối-khóa)
- [Buổi 13 — Centralized Logging với ELK Stack](#buổi-13--centralized-logging-với-elk-stack)
- [Buổi 14 — Caching (Caffeine & Redis)](#buổi-14--caching-caffeine--redis)
- [Thuật ngữ](#thuật-ngữ)
- [Lộ trình dự kiến các buổi sau](#lộ-trình-dự-kiến-các-buổi-sau)

---

## Công cụ

| Công cụ | Dùng để |
|---|---|
| **draw.io** (diagrams.net) | Vẽ kiến trúc hệ thống — bắt buộc cài |
| Spring Boot / Spring Cloud | Code service (nối tiếp học phần Java Advance) |
| MySQL / PostgreSQL | Database mỗi service |
| Kafka | Giao tiếp bất đồng bộ (sẽ đưa vào sau) |
| Redis | Cache (chưa chắc có trong scope lớp) |

📌 *Bổ sung*: học phần này **thiên về kiến trúc / design hệ thống**, tầng cao hơn tầng coding. Coding chỉ là công cụ để hiện thực hóa bản thiết kế.

---

# Buổi 1 — Tổng quan kiến trúc Microservice

## 1. Monolithic là gì

Kiến trúc truyền thống đã học ở Java Advance:

```
[Frontend] ──────► [ Backend (1 source code) ] ──────► [ 1 Database ]
```

Đặc điểm:
- **Một** source code backend duy nhất.
- Mọi domain nằm chung: Product, Order, Cart, Payment, User, Shipment, Review, Promotion, Notification...
- Mỗi domain chỉ là một (hoặc vài) `Controller` + `Service` + `Repository` trong cùng project.
- **Một** database duy nhất, các domain chỉ khác nhau **tên bảng**.

📌 *Bổ sung — Monolithic KHÔNG phải là xấu*:

| Ưu điểm Monolithic | Nhược điểm Monolithic |
|---|---|
| Dev nhanh, đơn giản | Scale phải scale toàn bộ |
| Debug dễ (1 process, 1 stack trace) | Deploy: sửa 1 dòng → build lại cả hệ thống |
| Transaction ACID gọn (1 DB) | 1 module crash → sập cả app |
| Không tốn chi phí network nội bộ | Codebase phình → build/test chậm dần |
| Chi phí hạ tầng thấp | Nhiều team dẫm chân nhau trên 1 repo |

> **Nguyên tắc thực tế:** dự án nhỏ / startup giai đoạn đầu → **cứ làm Monolithic**. Chỉ tách Microservice khi thực sự có nhu cầu (tải lớn, nhiều team, cần scale lệch).

---

## 2. Scale — khái niệm nền tảng

### Ví dụ quán xôi (thầy dùng)

Bình thường bán 100 gói/ngày. Hôm nay gần nhà có sự kiện → 1000 gói/ngày. Một mình không kham nổi. Hai cách:

1. **Thuê thêm người** → thêm nhiều người cùng làm.
2. **Tự cố gắng hơn** → bỏ nghỉ trưa, uống nước tăng lực, làm nhanh hơn.

### Ánh xạ sang hệ thống

| Cách | Tên kỹ thuật | Nghĩa |
|---|---|---|
| Thuê thêm người | **Scale out** / Horizontal scaling | Thêm **số lượng server/instance**. 1 server = 100 req → 10 server = 1000 req |
| Tự cố gắng hơn | **Scale up** / Vertical scaling | Tăng **CPU / RAM** cho chính server đó |

📌 *Bổ sung — so sánh*:

| | Scale up (dọc) | Scale out (ngang) |
|---|---|---|
| Cách làm | Thêm CPU/RAM vào 1 máy | Thêm nhiều máy |
| Giới hạn | Có trần cứng (phần cứng max) | Gần như vô hạn |
| Downtime | Thường phải restart máy | Không cần (thêm node mới) |
| Chi phí | Tăng phi tuyến (máy càng khủng càng đắt) | Tăng tuyến tính |
| Điều kiện | Không cần gì | App phải **stateless** |
| Điểm chết | Single point of failure | Có redundancy |

> **Microservice ưu tiên scale out.** Muốn scale out được thì service phải **stateless** — không giữ session/state trong RAM của instance, vì request lần sau có thể rơi vào instance khác. State đẩy hết ra DB / Redis / JWT token.

---

## 3. Điểm yếu của Monolithic

### Tình huống Flash Sale

Lúc flash sale, tải phân bố **rất lệch**:

| Module | Tải lúc flash sale |
|---|---|
| Order (đặt hàng) | 🔥🔥🔥 Cực cao |
| Payment (thanh toán) | 🔥🔥🔥 Cực cao |
| Cart (giỏ hàng) | 🔥🔥 Cao |
| Product (xem sản phẩm) | 🔥🔥 Cao |
| Shipment (vận chuyển) | 🙂 Thấp — mai mốt mới ship |
| Blog / News | 🙂 Rất thấp |
| Review / Comment | 🙂 Rất thấp |

**Vấn đề:** Monolithic chỉ có **1 cục source code** → khi scale phải nhân bản **toàn bộ**:

```
Trước:  [Backend]

Sau:    [Backend]  [Backend]  [Backend]   ← nhân bản CẢ CỤC
           ▲          ▲          ▲
           └──────────┴──────────┘
                  Load Balancer
```

Kết quả: mình chỉ cần Order mạnh hơn, nhưng buộc phải trả tiền cho cả Blog, Review, Shipment nhân bản theo. **Lãng phí tài nguyên.**

📌 *Bổ sung — thêm 3 điểm yếu Monolithic nữa*:
1. **Deploy toàn phần** — sửa 1 dòng ở Blog → build + deploy lại cả hệ thống, rủi ro downtime cho Order.
2. **Lỗi lan** — memory leak ở Review làm sập process → mất luôn Order, Payment.
3. **Khóa công nghệ** — cả hệ thống dùng chung 1 ngôn ngữ/framework/version.

---

## 4. Microservice — giải pháp

Phân rã (decompose) backend thành các **service độc lập**:

```
                    ┌──────────────────────┐
                    │    API Gateway       │
                    └──────────┬───────────┘
        ┌──────────┬───────────┼───────────┬──────────┐
        ▼          ▼           ▼           ▼          ▼
   ┌────────┐ ┌────────┐ ┌─────────┐ ┌────────┐ ┌──────────┐
   │Product │ │  Cart  │ │  Order  │ │Payment │ │ Shipment │
   │Service │ │Service │ │ Service │ │Service │ │ Service  │
   └───┬────┘ └───┬────┘ └────┬────┘ └───┬────┘ └────┬─────┘
       ▼          ▼           ▼          ▼           ▼
   ┌────────┐ ┌────────┐ ┌─────────┐ ┌────────┐ ┌──────────┐
   │Prod DB │ │Cart DB │ │Order DB │ │ Pay DB │ │ Ship DB  │
   └────────┘ └────────┘ └─────────┘ └────────┘ └──────────┘
```

Đặc điểm mỗi service:
- **Source code riêng** (repo riêng hoặc module riêng).
- **Deploy độc lập** — sửa Order không cần build Product.
- **Scale độc lập** — điểm mấu chốt.
- **Database riêng**.

### Scale lệch — cái Microservice giải quyết

Flash sale, chỉ Order quá tải:

```
   ┌────────┐ ┌────────┐ ┌─────────┐┌─────────┐┌─────────┐┌─────────┐ ┌──────────┐
   │Product │ │  Cart  │ │  Order  ││  Order  ││  Order  ││  Order  │ │ Shipment │
   │  x1    │ │   x1   │ │   x4 ───┴┴─────────┴┴─────────┴┴──────── │ │    x1    │
   └───┬────┘ └───┬────┘ └────┬──────────────────────────────────── │ └────┬─────┘
       ▼          ▼           ▼                                          ▼
   ┌────────┐ ┌────────┐ ┌──────────────────┐                     ┌──────────┐
   │Prod DB │ │Cart DB │ │ Order DB (scaled)│                     │ Ship DB  │
   └────────┘ └────────┘ └──────────────────┘                     └──────────┘
```

Chỉ Order + Order DB được scale. Các service khác giữ nguyên → **tiết kiệm chi phí**.

📌 *Bổ sung — cái giá phải trả (trade-off)*:

Microservice **KHÔNG miễn phí**. Đổi lại được scale lệch, phải chịu:

| Vấn đề | Mô tả |
|---|---|
| Network latency | Gọi in-process (nano-giây) → gọi HTTP/gRPC (mili-giây) |
| Distributed transaction | Không còn `@Transactional` xuyên service → phải dùng **Saga pattern** |
| Data consistency | Chấp nhận **eventual consistency**, không còn ACID toàn cục |
| Debug khó | 1 request đi qua 5 service → cần **distributed tracing** (Zipkin/Jaeger) |
| Vận hành phức tạp | N service = N pipeline CI/CD, N bộ log, N bộ monitoring |
| Partial failure | Service B chết → A phải xử lý sao? → cần **Circuit Breaker**, retry, timeout |

> Câu nói kinh điển: *"Microservices buy you options at the cost of complexity."*

---

## 5. Database per Service

### Tại sao phải tách DB

Nếu tách service nhưng vẫn chung 1 DB:

```
[Product SVC] ──┐
[Order SVC]   ──┼──► [ 1 DB dùng chung ]   ❌ SAI
[Cart SVC]    ──┘
```

Vấn đề:
- Scale service được nhưng **DB thành nút thắt cổ chai (bottleneck)**.
- Ví dụ nhà hàng của thầy: khách đông → thuê thêm **phục vụ** (scale service) nhưng vẫn giữ **1 đầu bếp** (1 DB) → đầu bếp kiệt sức, vẫn tắc.
- Các service **coupling** qua schema: Order đổi cấu trúc bảng → Product vỡ.

✅ Đúng: **mỗi service một DB riêng.**

### Thực tế triển khai — điều quan trọng

Nếu mỗi service xin **server vật lý riêng** thì:
- Hệ thống production **không bao giờ dùng 1 server cho DB** — tối thiểu **3 server** để backup/failover cho nhau (1 chết còn 2 gánh).
- 10 service × 3 server = **30 server vật lý** → **không công ty nào chịu chi phí đó.**

**Cách làm thật:**

```
        ┌──────────── Cụm DB vật lý (3 node, replication) ─────────────┐
        │                                                              │
        │   schema: product_db   schema: order_db   schema: auth_db   │
        │                                                              │
        └──────────────────────────────────────────────────────────────┘
              ▲                    ▲                    ▲
              │                    │                    │
        [Product SVC]         [Order SVC]          [Auth SVC]
```

- Chung cụm server vật lý (~3 node để HA).
- Mỗi service một **schema/database logic riêng** (trong MySQL: `CREATE DATABASE product_db;` `CREATE DATABASE order_db;`).
- Về mặt logic vẫn là **database per service** — service A tuyệt đối không truy cập schema của B.

> Pattern này gọi là **Database per Service (Schema per Service)**.

📌 *Bổ sung — 3 mức tách DB (từ nhẹ đến nặng)*:

| Mức | Mô tả | Chi phí | Độ cô lập |
|---|---|---|---|
| 1. Private tables | Chung schema, quy ước không đụng bảng của nhau | Rẻ nhất | Yếu (dễ vi phạm) |
| 2. **Schema per service** | Chung DB server, khác schema ← **lớp mình dùng** | Vừa | Tốt |
| 3. DB server per service | Mỗi service 1 cụm DB riêng | Đắt | Mạnh nhất |

### Ghi chú về phần cứng (SSD vs HDD)

Có bạn hỏi: scale DB có phải thêm SSD không?
- Về nguyên tắc SSD luôn nhanh hơn HDD, server lý tưởng luôn dùng SSD.
- Người ta vẫn dùng HDD vì **chi phí** — SSD đắt hơn nhiều.
- Nhưng đây là **scope của DevOps/Infra**, không phải scope của Development team. Dev chỉ quan tâm: scale up (thêm CPU/RAM) hay scale out (thêm node).

---

## 6. API Gateway

```
   INTERNET                      │        MẠNG NỘI BỘ (private network)
                                 │
  [Frontend] ──────────► [API Gateway] ──┬──► [Product SVC]
   (browser/app)                 │        ├──► [Order SVC]
                                 │        ├──► [Auth SVC]
                                 │        └──► [Shipment SVC]
```

**Luật:**
- Toàn bộ service bên trong nằm trong **network nội bộ**, từ Internet **không truy cập trực tiếp được**.
- Frontend **bắt buộc** đi qua Gateway. Không được gọi thẳng service.
- Gateway không "mở đường" tới service nào thì bên ngoài không gọi được service đó.

### Nhiệm vụ 1 — Routing (định tuyến)

Client gọi:

```
POST  http://abc.com/product/v1/create-product
POST  http://abc.com/order/v1/cancel-order
```

Gateway đọc **prefix** đầu path để biết đích đến:

| Prefix | Route tới | Path còn lại chuyển tiếp |
|---|---|---|
| `/product` | Product Service | `/v1/create-product` |
| `/order` | Order Service | `/v1/cancel-order` |
| `/auth` | Auth Service | `/v1/login` |

### Nhiệm vụ 2 — Xác thực & Phân quyền tập trung

Vì **mọi request đều đi qua Gateway**, đặt auth ở đây là hợp lý nhất:
- Gateway verify token **một lần**, service bên trong không phải verify lại.
- Không lặp code auth ở 10 service.

### Nhiệm vụ 3 — Load Balancing

Order có 4 instance → Gateway chọn 1 trong 4 (round-robin, least-connection...) để chuyển request.

📌 *Bổ sung — các nhiệm vụ khác của Gateway*:

| Nhiệm vụ | Mô tả |
|---|---|
| **Rate limiting** | Chặn spam, giới hạn N request/giây/user |
| **Request/Response transform** | Đổi format, thêm/bớt header |
| **SSL termination** | Giải mã HTTPS ở gateway, bên trong đi HTTP cho nhẹ |
| **Logging / Tracing** | Gắn `trace-id` cho request để lần dấu qua các service |
| **Circuit breaker** | Service chết → trả lỗi nhanh thay vì treo chờ |
| **Response aggregation** | Gộp kết quả từ nhiều service thành 1 response |
| **CORS** | Xử lý tập trung |
| **API versioning** | Điều hướng `/v1`, `/v2` |

📌 *Bổ sung — Gateway không nên làm gì*:
- ❌ Không chứa **business logic**. Gateway chỉ điều phối, không tính giá, không tạo đơn.
- ❌ Không truy cập database nghiệp vụ.
- Nếu nhồi logic vào Gateway → nó trở thành monolith mới, mọi thay đổi lại phải deploy Gateway.

📌 *Bổ sung — công nghệ thường dùng*: **Spring Cloud Gateway** (hợp với lớp mình vì đang học Spring), Kong, Nginx, AWS API Gateway, Traefik.

---

## 7. Auth Service

- Luôn có một service chuyên trách xác thực/phân quyền — thường tên **Auth Service** / **Identity Service**.
- Gateway kết nối tới Auth Service để verify token.
- Có 2 cách bố trí (đều chấp nhận được):
  1. Gateway **tự** sinh & verify token (Gateway kiêm luôn Auth).
  2. Có **Auth Service riêng**, Gateway gọi sang. ← phổ biến hơn

```
[Frontend] ──► [Gateway] ──(verify token)──► [Auth Service] ──► [Auth DB]
                   │
                   └──(request hợp lệ)──► [Product / Order SVC]
```

📌 *Bổ sung — luồng JWT thực tế* (chi tiết sẽ học buổi sau):

```
1. Client POST /auth/login  {username, password}
2. Auth Service kiểm tra credential → trả về:
      accessToken  (JWT, sống ngắn ~15 phút)
      refreshToken (sống dài ~7 ngày)
3. Client gọi API khác kèm header:  Authorization: Bearer <accessToken>
4. Gateway verify chữ ký JWT (bằng public key / secret)
       - hợp lệ  → forward request, kèm header X-User-Id, X-Roles
       - hết hạn → 401, client dùng refreshToken xin accessToken mới
5. Service bên trong TIN header do Gateway gắn (vì network nội bộ)
```

**Tại sao JWT hợp với Microservice?** Vì JWT **self-contained** — thông tin user nằm trong token, service không cần gọi DB để biết ai đang gọi → **stateless** → scale out thoải mái.

### Auth Service vs User Service — tách hay gộp?

| | Auth Service | User Service |
|---|---|---|
| Trách nhiệm | Login, logout, token, password, role/permission | Profile, địa chỉ, quan hệ (bạn bè, phòng ban), thông tin cá nhân |

- Hệ thống **lớn** → tách riêng 2 service.
- Hệ thống **nhỏ** → gộp làm một, vì cả hai đều xoay quanh User.
- **Không có đúng/sai tuyệt đối** — tùy quy mô. Lớp mình **gộp làm một**.

---

## 8. Các nguyên tắc cứng (RULES)

> Phần này là **luật**, không phải gợi ý. Thầy nhấn mạnh nhiều lần.

### RULE 1 — Một service chỉ chọc vào MỘT database

```
[Order SVC] ──► [Order DB]          ✅
[Order SVC] ──┬► [Order DB]
              └► [Product DB]       ❌
```

### RULE 2 — Một database chỉ được chọc bởi MỘT service

```
[Order SVC] ──► [Order DB] ◄── [Report SVC]   ❌
```

> Quan hệ Service ↔ DB là **1–1**. Không nhiều ở bất kỳ chiều nào.

### RULE 3 — Muốn lấy data của service khác → gọi API của nó

Service A cần data của B:

```
[A] ──HTTP/gRPC──► [B Service] ──► [B DB]      ✅
[A] ──────────────────────────────► [B DB]     ❌ TUYỆT ĐỐI KHÔNG
```

Ví dụ thầy sửa bài: **Search Service** muốn tìm bài post → **không được** query thẳng vào Post DB, phải gọi API của Post Service.

📌 *Bổ sung — vì sao rule này quan trọng đến thế*:
- Nếu A đọc thẳng DB của B → A phụ thuộc vào **cấu trúc bảng** của B. B đổi tên cột → A vỡ mà B không hề biết.
- Đọc thẳng DB **phá vỡ business rule** của B (validate, tính toán, side-effect nằm trong code của B, không nằm trong bảng).
- Mất khả năng deploy độc lập → quay lại monolith nhưng phức tạp hơn.
- **Encapsulation ở cấp kiến trúc**: API là hợp đồng công khai, DB schema là chi tiết nội bộ.

### RULE 4 — Không tạo "service trung gian" chỉ để chứa quan hệ

Ví dụ mạng xã hội: quan hệ bạn bè giữa 2 user.

```
[User SVC] ──► [Relationship SVC] ◄── [User SVC]    ❌ SAI
```

- Không có service nào tồn tại chỉ để làm cầu nối giữa 2 entity.
- Quan hệ đó phải **thuộc về một trong hai** service liên quan.
- Bạn bè giữa các user → nằm trong **User Service** (bảng `friendship(user_id, friend_id, status)`).

📌 *Bổ sung*: nguyên nhân sâu xa — service phải chia theo **business capability** (năng lực nghiệp vụ), không chia theo **bảng dữ liệu**. "Relationship" không phải một năng lực nghiệp vụ độc lập, nó là một phần của việc quản lý người dùng.

---

## 9. Chia service bao nhiêu là đủ

> Người mới học Microservice hay nghĩ **"chia càng nhỏ càng tốt"** — **SAI**.

Mỗi service là một **hệ thống độc lập**, kéo theo:
- Pipeline CI/CD riêng
- Deploy riêng, monitoring riêng, log riêng
- Giao tiếp qua network (chậm hơn, có thể lỗi)
- Xử lý transaction xuyên service (rất phức tạp)

**Lỗi thầy sửa cho các bạn trong buổi:**

| Thiết kế sai | Sửa thành |
|---|---|
| `Teacher Service` + `Student Service` tách riêng | Gộp → **User Service** (phân biệt bằng `role`) |
| Tách `Subject`, `Major`, `Faculty`, `Tuition`, `Schedule`, `Score`, `Facility` thành 7 service | Gộp lại còn ~3–4 service |
| `Relationship Service` riêng | Đưa vào **User Service** |
| `Search Service` query thẳng DB của Post | Gọi API của Post Service |

**Kết luận thầy đưa ra:** hệ thống quản lý trường học domain đơn giản → **3–4 service là đủ**. Thực tế quản lý trường học **không cần** làm Microservice, dùng để thực hành thôi.

📌 *Bổ sung — tiêu chí xác định ranh giới service*:

1. **Business capability** — chia theo năng lực nghiệp vụ, không theo bảng/entity.
2. **Bounded Context (DDD)** — mỗi service là một ngữ cảnh có ngôn ngữ riêng. Từ "Product" trong Catalog nghĩa khác "Product" trong Inventory.
3. **High cohesion, loose coupling** — thứ hay đổi cùng nhau thì ở cùng nhau.
4. **Đội sở hữu** — 1 team sở hữu 1 service (2-pizza team). 3 team mà 20 service → quá tải.
5. **Tần suất gọi chéo** — A gọi B ở gần như mọi request → dấu hiệu nên gộp.
6. **Nhu cầu scale khác nhau** — nếu 2 module luôn scale cùng nhịp, tách ra chẳng lợi gì.

> **Lời khuyên thực chiến:** bắt đầu bằng **Monolith có module rõ ràng** (modular monolith), khi thấy rõ đường nứt thì mới tách. Tách sai còn tệ hơn không tách — gọi là *distributed monolith*, dở nhất mọi thế giới.

---

## 10. Phân tích domain E-commerce

Các sub-domain lớp liệt kê được:

| Domain | Nghiệp vụ chính |
|---|---|
| **Product** | Tạo/sửa/xóa sản phẩm, thuộc tính, variant (màu, size), danh mục (category), duyệt sản phẩm từ seller, media/hình ảnh, thống kê bán chạy, rating |
| **Order** | Đặt hàng, xác nhận, cập nhật trạng thái, hủy, trả hàng, tính giá trị đơn |
| **Cart** | Thêm/xóa sản phẩm, kiểm tra số lượng, cảnh báo thời gian, tự động cập nhật |
| **Payment** | Tích hợp cổng thanh toán, hoàn tiền, ví/thẻ, lịch sử giao dịch, xác nhận thành công/thất bại |
| **User / Account** | Đăng ký, đăng nhập, hồ sơ, role, trạng thái tài khoản, quên mật khẩu, 2FA, địa chỉ |
| **Seller** | Đăng ký & phê duyệt người bán, hồ sơ gian hàng, tài khoản ngân hàng, theo dõi hiệu suất |
| **Inventory / Stock** | Theo dõi tồn kho, giảm trừ, giữ hàng (reserve), tự động giải phóng, cảnh báo tồn, kiểm kê |
| **Shipment** | Tính phí vận chuyển, tích hợp API đối tác vận chuyển, mã vận đơn, theo dõi trạng thái |
| **Promotion** | Tạo mã giảm giá, điều kiện áp dụng, giới hạn số lượng, chiến dịch & thời hạn, flash sale |
| **Search** | Tìm kiếm, gợi ý (suggestion), sửa lỗi chính tả, bộ lọc, sắp xếp |
| **Review / Comment** | Bình luận, phản hồi, kiểm duyệt nội dung, tính điểm đánh giá trung bình |
| **Notification** | Gửi thông báo qua email/SMS/push, quản lý template, theo dõi trạng thái gửi |

Ghi chú thầy chỉnh:
- **Email** không phải domain riêng → thuộc **Notification**.
- **Suggestion** không phải sub-domain riêng → thuộc **Search** (nó phủ lên toàn bộ domain).
- **History** không phải service riêng → mỗi service tự có history của mình (Cart có history của Cart, Order có history của Order).

📌 *Bổ sung — Notification nên bất đồng bộ*: gửi email/SMS chậm và có thể lỗi. Không được để việc gửi mail làm đơn hàng thất bại. → Order publish event `OrderCreated` lên **Kafka**, Notification Service subscribe và gửi mail. Order không cần chờ.

---

## 11. Scope project của lớp

Thu hẹp còn **4 thành phần** cho 14 buổi:

```
                    ┌──────────────────┐
   [Frontend] ─────►│   API Gateway    │
                    └────────┬─────────┘
           ┌─────────────────┼─────────────────┐
           ▼                 ▼                 ▼
    ┌────────────┐   ┌──────────────┐   ┌─────────────┐
    │Auth Service│   │Product Service│  │Order Service│
    └─────┬──────┘   └──────┬───────┘   └──────┬──────┘
          ▼                 ▼                  ▼
    ┌──────────┐     ┌─────────────┐    ┌───────────┐
    │ auth_db  │     │ product_db  │    │ order_db  │
    └──────────┘     └─────────────┘    └───────────┘
```

| Service | Phạm vi |
|---|---|
| **API Gateway** | Routing, xác thực tập trung, load balancing |
| **Auth Service** | Xác thực + phân quyền + **gộp luôn User**: account, role/permission, thông tin người dùng, **address**, thông tin **seller** |
| **Product Service** | Sản phẩm, **variant** (màu sắc, chủng loại), **category**, **gộp luôn Promotion & Flash Sale** |
| **Order Service** | Toàn bộ nghiệp vụ mua hàng |

Lý do gộp: số buổi có hạn, hệ thống nhỏ. Thực tế Promotion, User, Seller hoàn toàn có thể tách thành service riêng.

**Pattern áp dụng:** `Database per Service` — mỗi service một schema riêng.

**Dự kiến bổ sung:** **Kafka** (giao tiếp bất đồng bộ) — thầy sẽ cố đưa vào. Redis chưa chắc.

---

## 12. Bài tập về nhà

> **Thiết kế cơ sở dữ liệu cho 3 service: Auth, Product, Order.**
> Mỗi service một database riêng (`per service per schema`).
> Càng chi tiết, càng thực tế càng tốt — làm như một dự án thật đang đi làm.
> Sau này sẽ thành project cá nhân.

📌 *Gợi ý bảng cho từng service*:

**`auth_db`**
```
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

**`product_db`**
```
categories        (id, parent_id, name, slug, level)
products          (id, seller_id, category_id, name, description, status, created_at)
product_variants  (id, product_id, sku, price, stock, attributes_json)
product_images    (id, product_id, variant_id, url, sort_order)
attributes        (id, name)                 -- Màu sắc, Size
attribute_values  (id, attribute_id, value)  -- Đỏ, Xanh, M, L
promotions        (id, code, type, value, start_at, end_at, quota, used_count, status)
promotion_products(promotion_id, product_id)
flash_sales       (id, name, start_at, end_at, status)
flash_sale_items  (flash_sale_id, variant_id, sale_price, quota, sold)
```

**`order_db`**
```
orders          (id, order_code, buyer_id, seller_id, status, subtotal,
                 discount, shipping_fee, total, created_at)
order_items     (id, order_id, product_id, variant_id,
                 product_name_snapshot, price_snapshot, quantity)
order_status_logs (id, order_id, from_status, to_status, note, changed_by, created_at)
payments        (id, order_id, method, amount, status, transaction_ref, paid_at)
shipments       (id, order_id, carrier, tracking_code, status, shipped_at, delivered_at)
```

📌 **Lưu ý thiết kế cực quan trọng — Snapshot data**

Trong Monolithic, `order_items` chỉ cần `product_id` rồi JOIN sang bảng product lấy tên và giá.
Trong Microservice **KHÔNG LÀM ĐƯỢC** vì Product ở database khác → không JOIN được.

→ Giải pháp: **snapshot** — lúc tạo đơn, copy tên + giá sản phẩm vào `order_items`.

Lợi ích kép:
1. Không cần gọi Product Service khi hiển thị đơn hàng cũ.
2. **Đúng nghiệp vụ hơn** — sản phẩm sau này đổi giá/đổi tên, đơn hàng cũ vẫn giữ nguyên giá tại thời điểm mua. (Monolithic JOIN trực tiếp thực ra là *sai nghiệp vụ*.)

📌 **Lưu ý — không có Foreign Key xuyên service**

`orders.buyer_id` trỏ tới user ở `auth_db` → **không thể** khai báo `FOREIGN KEY`. Chỉ lưu ID trần, ràng buộc do **application** đảm bảo, không phải database.

---

# Buổi 2 — Thiết kế DB & dựng Product Service

> Trọng tâm: quy ước thiết kế DB thực chiến + dựng Product Service bằng Spring Boot.

## 1. Thu hẹp scope Product Service

Thầy chốt: **Product Service chỉ quản lý Sản phẩm + Danh mục (Category)**.

- Bỏ variant, media, shipment, flash sale khỏi scope lớp.
- Giá (`price`) và tồn kho (`stock`) nằm **thẳng trên bảng `products`**.
- Lý do: số buổi có hạn, focus chính là **microservice**, không phải thiết kế DB.

📌 *Bổ sung*: thực tế `stock` **không** nên nằm trong bảng product (thầy có nói) — nó thuộc Inventory Service, vì tồn kho ghi/đọc rất nóng và cần cơ chế reserve riêng. Ở đây đơn giản hóa.

## 2. Category lồng nhau — Adjacency List Pattern

Yêu cầu: danh mục nhiều tầng như Shopee.
`Đồ điện tử` → `Điện thoại` → `Điện thoại cũ` / `Điện thoại mới`

**Pattern phổ biến nhất: `parent_id` tự tham chiếu (Adjacency List).**

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

Dữ liệu minh họa:

| id | name | parent_id |
|---|---|---|
| `uuid-A` | Đồ điện tử | `NULL` ← root |
| `uuid-B` | Điện thoại | `uuid-A` |
| `uuid-C` | Điện thoại cũ | `uuid-B` |
| `uuid-D` | Điện thoại mới | `uuid-B` |

**Nhược điểm (thầy hỏi cả lớp):**
1. **Xóa cha phải đệ quy xóa con** — không tự động.
2. **Search/lấy toàn bộ cây phải đệ quy** — query mệt, có thể N+1.
3. **Một category chỉ có ĐÚNG MỘT cha.**

📌 *Bổ sung — cách query cây trong MySQL 8+* (Recursive CTE):

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

## 3. Nhiều cha — Bảng quan hệ trung gian

Yêu cầu mở rộng: `Điện thoại cũ` vừa là con của `Điện thoại`, vừa là con của `Đồ cũ`.

Giải pháp: **bỏ `parent_id`, tạo bảng quan hệ**.

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

- Cả `left_id` và `right_id` đều **reference về `categories.id`**.
- Dùng `left_id`/`right_id` + cột `relationship` thay vì `parent_id`/`child_id` cứng → **generic hơn**, lưu được cả quan hệ ông–cháu, cháu–chắt.
- Một category không thể là con của chính nó.

> Thầy nói rõ: **lớp mình DÙNG `parent_id`** (đơn giản, phổ biến nhất). Phần nhiều cha chỉ để mở rộng kiến thức — hay bị hỏi khi phỏng vấn.

📌 *Bổ sung — 4 pattern lưu cây, để đối chiếu*:

| Pattern | Cách lưu | Đọc cây | Ghi | Nhiều cha |
|---|---|---|---|---|
| **Adjacency List** ← lớp dùng | `parent_id` | Chậm (đệ quy) | Nhanh | ❌ |
| Path Enumeration | `path = "/A/B/C/"` | Nhanh (LIKE) | Vừa | ❌ |
| Nested Set | `left`, `right` | Rất nhanh | Rất chậm | ❌ |
| **Closure Table** | Bảng quan hệ riêng | Nhanh | Vừa | ✅ |

## 4. Quy ước thiết kế DB thực chiến

### 4.1 — ID dùng UUID, không dùng auto-increment

```sql
id VARCHAR(36) PRIMARY KEY
```

| | INT AUTO_INCREMENT | UUID (36 ký tự) |
|---|---|---|
| Đoán được? | ✅ Rất dễ (`/order/1`, `/order/2`) | ❌ Gần như không thể |
| Merge nhiều DB | Đụng ID | Không đụng |
| Sinh ở client trước khi insert | ❌ | ✅ |
| Dung lượng / tốc độ index | Nhẹ, nhanh | Nặng hơn |

Thầy: thực tế đi làm **ít khi dùng auto-increment**, chủ yếu vì dễ đoán. Xác suất trùng UUID gần như bằng 0.

- Java: dùng kiểu `String`, **không** dùng `java.util.UUID` (bản chất vẫn là chuỗi; thầy chưa từng dùng kiểu UUID).

### 4.2 — Không đặt prefix tên bảng vào tên cột

```
❌ categories.category_name     ❌ orders.order_code    ❌ orders.order_id
✅ categories.name              ✅ orders.code          ✅ orders.id
```

Lý do: truy vấn luôn viết `categories.name` — prefix là thừa.

### 4.3 — Soft delete (xóa mềm) — bắt buộc

```sql
is_deleted BIT DEFAULT 0
```

- Nhiều công ty **must-have**, cấm xóa cứng (`DELETE`).
- **Mọi query tìm kiếm phải lọc thêm `is_deleted = 0`.** Thầy nhấn mạnh lỗi hay gặp: quên điều kiện này khi validate.

### 4.4 — Auditing — 4 cột bắt buộc trên mọi bảng

```sql
created_at        DATETIME
created_by        VARCHAR(255)
last_modified_at  DATETIME
last_modified_by  VARCHAR(255)
```

Trả lời: *bản ghi này được ai tạo, lúc nào; sửa lần cuối bởi ai, lúc nào.*

📌 *Bổ sung*: kiểu thời gian trong Java dùng `Instant` (thay `Date`) — mốc tính từ **1970-01-01 UTC**, không mang timezone, hợp cho hệ thống phân tán.

## 5. Nhắc lại Pattern / Anti-pattern

```
✅ PATTERN:       per service — per schema (mỗi service 1 database riêng)

❌ ANTI-PATTERN:  nhiều service  →  cùng 1 database
❌ ANTI-PATTERN:  1 service      →  nhiều database
```

> Nhiều service cùng chọc 1 database là điều **cực kỳ không được phép vi phạm**.

## 6. Dựng Product Service (Spring Boot)

### 6.1 — Khởi tạo project

| Mục | Giá trị |
|---|---|
| Tên | `VTI-DTN-2506-product-service` |
| Build | Maven |
| Java | 21 |
| Spring Boot | 3.5.x |

**Dependencies:** Spring Web, Lombok, Spring Data JPA, MySQL Driver, Validation.
(Spring Security thêm **sau**, khi đã xong cả 3 service. DevTools không cần.)

### 6.2 — Cấu trúc package (số nhiều)

```
com.vti.product
├── entities/       (số nhiều)
├── controllers/
├── repositories/
├── services/
├── dtos/
│   ├── request/
│   └── response/
├── mappers/
├── configs/
└── exceptions/
```

### 6.3 — BaseEntity (dùng chung)

```java
@Getter
@Setter
@MappedSuperclass                                  // BẮT BUỘC, nếu thiếu kế thừa không work
@EntityListeners(AuditingEntityListener.class)     // lắng nghe sự kiện auditing
public class BaseEntity {

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedDate
    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;
}
```

> ⚠️ Thiếu `@MappedSuperclass` → các cột kế thừa **không** được map. Đây là lỗi thầy mất thời gian debug trên lớp.

### 6.4 — Entity

```java
@Entity
@Getter
@Setter
@Table(name = "categories")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private String id;

    private String name;

    @Column(name = "parent_id")
    private String parentId;
}
```

```java
@Entity
@Getter
@Setter
@Table(name = "products")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private String id;

    private String name;
    private Integer price;
    private Integer stock;

    @Column(name = "category_id")
    private String categoryId;
}
```

📌 *Lưu ý*: thầy **không** dùng `@OneToMany` / `@ManyToOne` — chỉ lưu ID trần.
Lý do: (1) trả entity có quan hệ 2 chiều dễ **StackOverflow** khi serialize JSON; (2) handle không kỹ thì **hiệu năng rất tệ** (N+1 query). Nhiều dự án thực tế bỏ hẳn.

### 6.5 — Controller (RESTful)

```java
@RestController
@RequestMapping("/v1/products")     // BẮT BUỘC có version — chuẩn RESTful
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<BaseResponse> create(@RequestBody @Valid CreateProductRequest request) {
        Product product = productService.create(request);
        return ResponseEntity.ok(new BaseResponse(product, "Create product successfully"));
    }
}
```

> **RESTful là bộ quy ước, không phải bắt buộc kỹ thuật** — không tuân theo code vẫn chạy, nhưng nên follow. Version (`/v1`) là một trong các quy ước đó.

### 6.6 — BaseResponse & DTO

```java
@Getter
@Setter
@AllArgsConstructor
public class BaseResponse {
    private Object data;
    private String message;
    // thực tế còn có: code, timestamp, errors...
}
```

```java
@Getter
@Setter
public class CreateProductRequest {
    @NotBlank  private String name;
    @NotNull @Positive private Integer price;
    @NotNull @PositiveOrZero private Integer stock;
    @NotBlank  private String categoryId;
    // KHÔNG có id — id tự sinh, client không được truyền
}
```

> **Nguyên tắc**: theo lý thuyết không được trả entity trực tiếp ra API, phải bọc vào DTO. Thầy bỏ qua trong lớp cho gọn, nhưng nhắc rõ đó là cách làm chuẩn.

### 6.7 — Service

```java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public Product create(CreateProductRequest request) {
        // 1. Validate category tồn tại VÀ chưa bị xóa mềm
        boolean exists = categoryRepository
                .existsByIdAndIsDeletedFalse(request.getCategoryId());
        if (!exists) {
            throw new ApplicationException("Category not found");
        }

        // 2. Map request -> entity
        Product product = productMapper.toProduct(request);

        // 3. Save
        return productRepository.save(product);
    }
}
```

### 6.8 — Repository

```java
public interface ProductRepository extends JpaRepository<Product, String> {}
// kiểu ID là String (UUID), không phải Long

public interface CategoryRepository extends JpaRepository<Category, String> {
    boolean existsByIdAndIsDeletedFalse(String id);
}
```

### 6.9 — MapStruct (thay ModelMapper)

Lớp chọn **MapStruct** vì phổ biến hơn, áp dụng được nhiều trường hợp hơn (ModelMapper cũng dùng được, tùy công ty).

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toProduct(CreateProductRequest request);
}
```

📌 *Vì sao MapStruct nhanh hơn*: MapStruct **sinh code Java lúc compile** (không reflection); ModelMapper map bằng **reflection lúc runtime** → chậm hơn và lỗi chỉ lộ khi chạy.

### 6.10 — pom.xml: annotation processor (rất hay lỗi)

Lombok + MapStruct dùng chung annotation processor → phải khai báo đúng thứ tự, **Lombok trước, MapStruct sau**, kèm `lombok-mapstruct-binding`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
          </path>
          <path>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <version>1.6.3</version>
          </path>
          <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok-mapstruct-binding</artifactId>
            <version>0.2.0</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

> ⚠️ Sau khi sửa `pom.xml` **bắt buộc `mvn clean`** để xóa build cũ, rồi mới chạy lại.

Lỗi hay gặp: `Parameter N of constructor ... required a bean of type '...Mapper'` → MapStruct chưa sinh implementation → sai config annotation processor.

### 6.11 — application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/product_db
    username: root
    password: 123456789
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
```

## 7. Ghi chú thêm buổi 2

- **Stored Procedure**: thầy đi làm 5+ năm **gần như chưa từng thấy** dự án Java mới dùng procedure. Chỉ dùng khi học JDBC thuần. Bỏ qua.
- Lỗi `406 Not Acceptable` khi test Postman: thường do header `Content-Type` / `Accept` bị set sai — kiểm tra tab Headers, bỏ các checkbox thừa.

---

# Buổi 3 — Order Service & pattern dữ liệu thay đổi theo thời gian

## 1. Bài toán cốt lõi: giá sản phẩm thay đổi theo thời gian

**Tình huống thầy đặt ra:**

> Hôm nay bạn mua áo giá 100.000đ, đơn hàng đã confirm nhưng chưa ship.
> Ngày mai shop tăng giá lên 150.000đ.
> Shipper mở app ra — thu bạn 100.000 hay 150.000?

→ **100.000đ.** Đơn hàng phải giữ giá **tại thời điểm mua**.

**Vậy nếu `order_items` chỉ lưu `product_id` rồi JOIN sang `products` lấy giá → SAI**, vì giá bên product đã đổi.

Đây là bài toán chung: *bảng A tham chiếu bảng B, mà dữ liệu B thay đổi theo thời gian, nhưng A phải giữ giá trị tại thời điểm giao dịch.*

## 2. Hai pattern giải quyết

### Pattern 1 — SNAPSHOT (lớp mình dùng)

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

- ✅ Đơn giản, đọc nhanh, không cần JOIN.
- ❌ **Càng ngày càng phình cột**: ngoài `price` còn cần snapshot `product_name`, `color`, `size`... vì tất cả đều có thể bị sửa.

📌 *Biến thể*: gom hết vào **1 cột JSON** (`product_snapshot JSON`) thay vì tách nhiều cột. Thầy xác nhận: JSON hay tách cột **đều vẫn là snapshot**, chỉ khác cách lưu trữ.

### Pattern 2 — VERSIONING (versioned records)

Mỗi lần sửa product → **tạo bản ghi MỚI**, tăng `version`, không update bản ghi cũ.

```
products:
| id       | name | price   | version | parent_id  |
|----------|------|---------|---------|------------|
| uuid-v1  | A    | 100.000 | 1       | NULL       |  ← root
| uuid-v2  | A2   | 100.000 | 2       | uuid-v1    |  ← đổi tên
| uuid-v3  | A2   | 150.000 | 3       | uuid-v2    |  ← đổi giá

order_items.product_id → trỏ CHÍNH XÁC tới uuid-v2 (version tại lúc mua)
```

Quy tắc quan trọng thầy nhấn mạnh:
- **Primary key vẫn CHỈ là `id`** (UUID mới cho mỗi version). **Không** dùng composite key `(id, version)`.
- Muốn ràng buộc thì thêm **UNIQUE KEY** trên `(root_id, version)`, không đưa vào primary key.
- Có thể thêm `parent_id` trỏ về version trước hoặc version gốc.

- ✅ Giữ toàn bộ lịch sử, không phình cột, audit tốt.
- ❌ Bảng product phình số dòng, mọi query "sản phẩm hiện tại" phải lọc version mới nhất.

### Chọn cái nào?

| Tình huống | Nên dùng |
|---|---|
| Cần giữ ít trường (2–3 cột) | **Snapshot** ← lớp mình chọn |
| Cần giữ nhiều trường / cần full lịch sử thay đổi | **Versioning** |

> Thầy: *"Không có đúng và sai. Cả hai đều rất phổ biến."* Với hệ thống lớp — snapshot vì chỉ snapshot `price`.

📌 *Bổ sung — pattern thứ 3*: **Event Sourcing** — không lưu trạng thái mà lưu chuỗi sự kiện (`PriceChanged`, `NameChanged`), dựng lại trạng thái bằng cách replay. Mạnh nhất nhưng phức tạp nhất; một số bạn trong lớp nói tới hướng này.

## 3. ID vs CODE — hai trường khác nhau

Thầy sửa nhầm lẫn phổ biến: `id` và `code` **không trùng lặp**, cả hai đều unique nhưng mục đích khác nhau.

| | `id` | `code` |
|---|---|---|
| Dạng | UUID 36 ký tự | Chuỗi ngắn 6–10 ký tự |
| Ví dụ | `9f8c...e21a` | `ORD250830A7` |
| Dành cho | **Hệ thống** giao tiếp với nhau | **Người dùng** đọc, nhớ, tra cứu |
| Thân thiện | ❌ | ✅ |

Cả hai đều unique trên bảng `orders`.

## 4. Nguyên tắc đặt tên & thiết kế được nhắc lại

| ❌ Sai | ✅ Đúng | Lý do |
|---|---|---|
| Bảng tên `item` | `order_item` | `item` không rõ item của cái gì |
| `orders.order_code` | `orders.code` | Prefix thừa |
| `orders.promotion_code` | Bảng riêng `order_promotions` | **1 đơn hàng có thể áp NHIỀU promotion** |
| PK = `(id, version)` | PK = `id`, UNIQUE `(root_id, version)` | Thực tế hiếm dùng composite PK |
| `order_item` tạo độc lập | Luôn tạo **kèm theo order** | Order item không có ý nghĩa đứng riêng |

## 5. Không có Foreign Key xuyên service

```sql
CREATE TABLE orders (
    id           VARCHAR(36) PRIMARY KEY,
    customer_id  VARCHAR(36) NOT NULL,  -- ❌ KHÔNG FK → auth_db.users
    status       VARCHAR(50) NOT NULL,
    total_amount INT NOT NULL,
    ...
);
```

- Trong **Monolithic**: `orders` và `users` cùng 1 DB → khai FK được.
- Trong **Microservice**: `order_db` và `auth_db` là **2 database khác nhau** → **không thể** khai FK.
- Chỉ lưu ID trần; ràng buộc do **application** đảm bảo.

Tương tự: `order_items.product_id` trỏ sang `product_db` → không FK.

## 6. Schema Order Service (CHỐT — minimum, 2 bảng)

```sql
CREATE TABLE orders (
    id                VARCHAR(36) PRIMARY KEY,
    customer_id       VARCHAR(36) NOT NULL,   -- không FK (khác DB)
    status            VARCHAR(50) NOT NULL,   -- VARCHAR để dễ mở rộng, không dùng ENUM
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

📌 `status` để `VARCHAR` chứ không `ENUM` → dễ mở rộng trạng thái sau này mà không phải `ALTER TABLE`.

## 7. Bàn về Cart (giỏ hàng)

Câu hỏi trong lớp: lưu cart ở đâu?

| Nơi lưu | Đánh giá của thầy |
|---|---|
| **Client** (localStorage) | ❌ Không đồng bộ giữa thiết bị — Shopee trên điện thoại và máy tính phải thấy cùng giỏ hàng |
| **Redis / Cache** | ❌ Dữ liệu cart không nhiều (1 người vài chục–vài trăm sản phẩm), không đủ "special" để cache; server cache lỗi → **mất sạch cart** |
| **Database** | ✅ **Chọn cái này** |
| **Firebase** | ❌ Thầy đi làm 5+ năm chưa gặp dự án nào dùng Firebase làm DB chính |

> Quy tắc rút ra: **dữ liệu có cần đồng bộ giữa các thiết bị không?** Có → phải lưu server.

Cart có thể là service riêng, hoặc coi như "order ở trạng thái draft" nằm luôn trong Order Service. Tùy quan niệm.

## 8. Quy tắc giao tiếp giữa service (nhắc lại)

> Order Service muốn verify `product_id` có tồn tại không → **BẮT BUỘC gọi API của Product Service**.
> **Tuyệt đối không** connect thẳng vào `product_db`.

## 9. Bài tập buổi 3

1. Code **Order Service** — source code **HOÀN TOÀN MỚI**, tách biệt Product Service.
2. **Hai repository Git khác nhau** (không phải 1 repo chung).
3. Push cả `product-service` và `order-service` lên GitLab, set **public**, gửi link vào group chat.

---

# Buổi 4 — JPA Auditing, luồng đặt hàng & Kafka

## 1. JPA Auditing — tự động điền 4 cột audit

Vấn đề: sau khi tạo bản ghi, `created_at`, `created_by`, `last_modified_at`, `last_modified_by` đều `NULL`.

Cách thủ công (không nên):
```java
product.setIsDeleted(false);
product.setCreatedAt(Instant.now());
product.setLastModifiedAt(Instant.now());
```

Cách chuẩn: **JPA Auditing** — 4 bước.

### Bước 1 — Annotate BaseEntity

```java
@Getter
@Setter
@MappedSuperclass                                  // ① bắt buộc cho kế thừa
@EntityListeners(AuditingEntityListener.class)     // ② lắng nghe sự kiện
public class BaseEntity {

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @CreatedDate       @Column(name = "created_at")        private Instant createdAt;
    @CreatedBy         @Column(name = "created_by")        private String  createdBy;
    @LastModifiedDate  @Column(name = "last_modified_at")  private Instant lastModifiedAt;
    @LastModifiedBy    @Column(name = "last_modified_by")  private String  lastModifiedBy;
}
```

### Bước 2 — Bật JPA Auditing

```java
@SpringBootApplication
@EnableJpaAuditing                       // ③
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
```

### Bước 3 — AuditorAware: nói cho JPA biết "ai" đang thao tác

```java
@Configuration
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {          // ④
        return new AuditorAwareImpl();
    }

    static class AuditorAwareImpl implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            // TODO: khi tích hợp Spring Security sẽ lấy từ SecurityContextHolder:
            // return Optional.ofNullable(SecurityContextHolder.getContext())
            //         .map(SecurityContext::getAuthentication)
            //         .filter(Authentication::isAuthenticated)
            //         .map(Authentication::getName);

            // Tạm thời hardcode vì chưa có Security:
            return Optional.of("huan.nguyen");
        }
    }
}
```

### Bước 4 — Bỏ code set thủ công

Kết quả: mỗi lần `save()`, 4 cột tự điền. `@CreatedDate`/`@CreatedBy` chỉ điền lần đầu; `@LastModified*` cập nhật mỗi lần update.

> ⚠️ Thiếu `@MappedSuperclass` → auditing **không work** khi entity kế thừa. Đây là lỗi thầy debug mất khá lâu trên lớp.

**Áp dụng cho cả `product-service` và `order-service`.**

## 2. Thiết kế API đặt hàng (Create Order)

### Request — CHỈ 2 thứ

```json
POST /v1/orders
{
  "customerId": "uuid-customer",
  "items": [
    { "productId": "uuid-product-1", "quantity": 2 },
    { "productId": "uuid-product-2", "quantity": 1 }
  ]
}
```

### ⚠️ TUYỆT ĐỐI KHÔNG cho client truyền `price`

Thầy nhấn mạnh đây là lỗi bảo mật nghiêm trọng. Lý do:

> **Một API không chỉ được gọi từ Frontend. Bất kỳ ai cũng có thể gọi nó bằng Postman.**

Nếu client truyền `price` → hacker gọi API với `price: 1` và mua iPhone giá 1đ.

**Hai nguyên tắc rút ra:**

1. **Mọi thứ liên quan đến TIỀN không bao giờ nhận từ client.**
2. **Cái gì suy ra được từ dữ liệu server thì không nhận từ client.**
   Có `productId` → server tự query ra `price`. Vậy không cần client gửi `price`.

📌 *Bổ sung — danh sách trường không bao giờ tin client*: giá, tổng tiền, số tiền giảm, `userId`/`role` của người gọi (lấy từ token), trạng thái đơn hàng, `isAdmin`, phí ship đã tính.

### `customerId` cũng sẽ bỏ khi có Security

Khi tích hợp Spring Security, server biết ai đang gọi qua token → tự suy ra `customerId`. Tạm thời chưa có Security nên vẫn truyền vào.

### Validate bắt buộc

| Trường | Validate |
|---|---|
| `items` | Không rỗng |
| `customerId` | Không null |
| `productId` | **Phải tồn tại** trong Product Service |
| `quantity` | `> 0` và **`<= stock`** của sản phẩm |

## 3. Luồng đặt hàng (Create Order Flow)

```
① Order Service: validate thông tin cơ bản của order
       (items khác rỗng, customerId khác null)

② Order Service: validate order items
       → validate productId tồn tại
       → validate quantity <= stock
       → lấy PRICE của sản phẩm
   ⚠️ BẮT BUỘC gọi sang PRODUCT SERVICE

③ Lock stock (trừ tồn kho):  stock = stock - quantity
   ⚠️ BẮT BUỘC gọi sang PRODUCT SERVICE

④ Save order + order_items xuống order_db
```

**Có ít nhất 2 lần Order Service phải gọi Product Service:**
1. Lấy thông tin sản phẩm (validate + lấy giá)
2. Lock/trừ số lượng tồn kho

> Order Service **không được** chọc thẳng vào `product_db`. Chỉ Product Service mới được. Mọi thứ đi qua **API của Product Service**.

## 4. Hai kiểu giao tiếp giữa Microservice

### 4.1 — SYNC (Đồng bộ)

> Order gọi Product và **ĐỢI** cho đến khi Product phản hồi thì mới đi tiếp.

```
[Order Service] ──── gọi ────► [Product Service]
       │                              │
       │◄──── đợi... phản hồi ────────┘
       │
       ▼ đi tiếp
```

**Công nghệ:**

| Cách | Ghi chú |
|---|---|
| **REST / HTTP** ← thầy dùng | Giống hệt gọi Postman từ service này sang service kia |
| **gRPC** | Nhanh hơn REST **rất nhiều** |

**Vì sao thầy chọn HTTP dù gRPC nhanh hơn?**
Vì API HTTP viết ra **còn tái sử dụng cho các luồng khác** (frontend gọi, tích hợp bên thứ 3...), không chỉ để service gọi nhau → giảm effort, không phải viết 2 lớp API.

### 4.2 — ASYNC (Bất đồng bộ)

> Order **KHÔNG ĐỢI** Product. Product **lắng nghe sự kiện** của Order và tự hành động.

```
① Order Service: lưu order xuống DB với status = NEW
② Order Service: publish event lên Kafka, topic "order-created"
③ Product Service: consume event "order-created"
④ Product Service: lock stock
⑤ Product Service: publish event lên topic "product-locked"
⑥ Order Service: consume event "product-locked"
       → update order status = CONFIRMED
       → gửi email cho khách hàng
```

```
┌──────────────┐  publish   ┌─────────────────┐  consume  ┌────────────────┐
│Order Service │ ─────────► │ topic:          │ ────────► │Product Service │
│              │            │ order-created   │           │  → lock stock  │
└──────────────┘            └─────────────────┘           └───────┬────────┘
       ▲                                                          │ publish
       │ consume            ┌─────────────────┐                   │
       └─────────────────── │ topic:          │ ◄─────────────────┘
                            │ product-locked  │
                            └─────────────────┘
```

### Ví dụ dễ hiểu — đặt vé concert

| | Trải nghiệm người dùng |
|---|---|
| **SYNC** | Màn hình quay vòng chờ → *"Đặt vé THÀNH CÔNG"* hoặc *"THẤT BẠI"*. Phải đợi kết quả chính thức. |
| **ASYNC** | *"Yêu cầu đặt vé của bạn đã được ghi nhận. Chúng tôi sẽ phản hồi qua email."* → vài phút/vài giờ sau nhận mail kết quả. |

### So sánh

| | Sync | Async |
|---|---|---|
| Chờ phản hồi | ✅ Có | ❌ Không |
| Coupling | Chặt | Lỏng |
| B chết thì A? | A cũng lỗi | A vẫn chạy, event nằm chờ |
| Chịu tải cao | Kém | Tốt (Kafka làm bộ đệm) |
| Debug | Dễ | Khó (cần tracing) |
| Consistency | Ngay lập tức | **Eventual** |

> **Lớp sẽ học và làm CẢ HAI.** Sync các bạn đã biết (gọi API), nên buổi này học nền tảng Kafka trước.

📌 *Bổ sung*: Async còn giúp **fan-out** — 1 event `order-created` được nhiều service cùng nghe (Product lock stock, Notification gửi mail, Analytics ghi số liệu) mà Order Service **không cần biết** có bao nhiêu service đang nghe.

## 5. Kafka — kiến thức nền

### 5.1 — Kafka là gì

> **Coi Kafka như một database.** Có thằng ghi dữ liệu vào, có thằng đọc dữ liệu ra.

Khác biệt cốt lõi so với MySQL:

| | MySQL | Kafka |
|---|---|---|
| Sinh ra để | **Searching / filtering** | **Hàng đợi (queue)** |
| Query | `WHERE`, `JOIN`, `ORDER BY` | ❌ Không hỗ trợ |
| Đọc | Chọn lọc bản ghi bất kỳ | **Đọc cả cục, tuần tự** |
| Lưu trữ | Vĩnh viễn | **Tạm thời** (retention time) |
| Tốc độ | Nhanh | **Cực nhanh** (dùng được cho real-time) |

> Kafka: *"Tao quăng cả cục vào, mày nhận cả cục. Tao không sinh ra để support searching. Muốn search thì lưu vào chỗ khác."*
> Chính vì không phải support searching nên nó **rất nhanh**.

### 5.2 — Ánh xạ khái niệm Kafka ↔ MySQL

| MySQL | Kafka | Ý nghĩa |
|---|---|---|
| **Table** | **Topic** | Nơi chứa dữ liệu cùng loại |
| **Record** (dòng) | **Message** | Một đơn vị dữ liệu |
| **Primary Key** | **(Partition, Offset)** ⚠️ | Định danh duy nhất một message |
| Server | **Broker** | Một node Kafka |
| Cụm server | **Cluster** | Nhiều broker gộp lại |

Ví dụ: topic `order-created` = bảng chỉ chứa dữ liệu về đơn hàng vừa tạo.

### 5.3 — Producer / Consumer

| Hành động | Bên ghi | Bên đọc |
|---|---|---|
| Tên gọi 1 | **Publisher** | **Subscriber** |
| Tên gọi 2 | **Producer** | **Consumer** |
| Động từ | **Produce** | **Consume** |

### 5.4 — Offset

**Offset = THỨ TỰ của message trong partition**, không phải ID.

> Giống xếp hàng — mỗi người có một số thứ tự.

```
Partition 0:  [offset 0] [offset 1] [offset 2] [offset 3] ...
                ORD-1      ORD-2      ORD-3
```

⚠️ **Offset MỘT MÌNH không tương đương primary key.** Vì mỗi partition đều có offset bắt đầu từ 0 — partition 1 có offset 1, partition 5 cũng có offset 1.

→ Primary key thật sự là cặp **`(partition, offset)`**.

### 5.5 — Partition — "đường ray"

**Vấn đề với 1 partition duy nhất:**

```
Partition 0:  [1] → [2] → [3] → [4] → [5]
```
Phải đọc xong `1` mới được đọc `2`, xong `2` mới được `3`... **Tuần tự, chậm.**
Thuê 3 người đọc cũng **vô nghĩa** — người 2 vẫn phải chờ người 1 xong.

**Giải pháp: chia thành nhiều "đường ray" (partition):**

```
Topic "order-created" (5 partitions)

Partition 0:  [msg] [msg] [msg]  ← consumer 1 đọc
Partition 1:  [msg] [msg]        ← consumer 2 đọc   ← ĐỌC SONG SONG
Partition 2:  [msg] [msg] [msg]  ← consumer 3 đọc
Partition 3:  [msg]              ← consumer 4 đọc
Partition 4:  [msg] [msg]        ← consumer 5 đọc
```

- **Ghi nhanh hơn**: không phải chờ message trước ghi xong.
- **Đọc nhanh hơn**: nhiều consumer đọc song song, mỗi consumer 1 partition.
- Trong **cùng 1 partition**, thứ tự vẫn được giữ tuyệt đối.
- Producer **không cần chỉ định** partition — để Kafka tự điều phối (rải đều). Càng rải đều càng nhanh.

### 5.6 — ⚠️ TRADE-OFF: Partition làm MẤT THỨ TỰ TOÀN CỤC

Đây là điểm thầy nhấn mạnh: *"Để ý không kỹ là sau này code rất mệt."*

Ví dụ topic `order` chứa mọi sự kiện của đơn hàng:

```
Sự kiện thứ tự đúng:  ORDER-1 CREATED → ORDER-1 UPDATED → ORDER-1 CANCELLED

Nhưng khi rải partition:
Partition 1:  [ORDER-1 CREATED]
Partition 3:  [ORDER-1 UPDATED]
Partition 4:  [ORDER-1 CANCELLED]

→ 3 consumer đọc song song
→ Consumer đọc partition 4 có thể xử lý CANCELLED TRƯỚC khi CREATED được xử lý
→ ❌ LỖI LOGIC
```

| | 1 partition | Nhiều partition |
|---|---|---|
| Thứ tự | ✅ Đảm bảo tuyệt đối | ❌ Chỉ trong từng partition |
| Tốc độ | Chậm | ✅ Nhanh |
| Song song | ❌ | ✅ |

**Đây là trade-off phải chấp nhận.** Cách xử lý sẽ học buổi sau.

📌 *Bổ sung — cách giải quyết thực tế*: dùng **message key**. Kafka đảm bảo cùng một `key` luôn vào cùng một partition (`partition = hash(key) % numPartitions`). Set `key = orderId` → mọi sự kiện của cùng một đơn hàng vào cùng 1 partition → **giữ đúng thứ tự cho đơn hàng đó**, vẫn song song được giữa các đơn hàng khác nhau. Đây là lý do trường `key` tồn tại trong Kafka message.

📌 Với các công việc **độc lập** (100 job không phụ thuộc nhau) thì không cần lo thứ tự — cứ rải partition và xử lý song song.

### 5.7 — Broker & Cluster

Giống MySQL production tối thiểu 3 server để backup cho nhau:

- **Broker** = một server Kafka.
- **Cluster** = nhiều broker gộp lại thành cụm.
- Production không bao giờ chạy 1 broker.

### 5.8 — Retention time — Kafka KHÔNG lưu vĩnh viễn

> Kafka **không phải nơi recommend để lưu dữ liệu vĩnh viễn.** Muốn lưu lâu dài và query → dùng MySQL.

Cấu hình `Time to retain data`, ví dụ `1 day` → message quá 1 ngày sẽ bị **xóa tự động**.

### 5.9 — Cấu trúc một Kafka message

| Thành phần | Ghi chú |
|---|---|
| **Key** | Quyết định partition (xem mục 5.6) |
| **Value** | Nội dung — thường là **JSON**, cũng có thể string/số |
| **Partition** | Có thể tự chọn, hoặc để Kafka điều phối |
| **Timestamp** | Thời điểm publish |
| **Offset** | Thứ tự trong partition |

Ví dụ value:
```json
{ "id": "ORD-1", "status": "NEW" }
```

## 6. Kafka vs RabbitMQ

| | Kafka | RabbitMQ / ActiveMQ |
|---|---|---|
| Cơ chế | **PULL** — consumer **chủ động kéo** dữ liệu về | **PUSH** — broker **chủ động đẩy** cho consumer đã đăng ký |
| Lưu trữ | Ghi xuống **disk**, giữ theo retention | Xóa sau khi consume |
| Quy mô | Rất lớn | Nhỏ hơn |

**Cơ chế PULL của Kafka:** dữ liệu được publish lên và **nằm im ở đó**. Các consumer định kỳ (vd 1 giây/lần) hỏi Kafka *"có dữ liệu mới không?"* rồi tự kéo về.

**Cơ chế PUSH của RabbitMQ:** consumer đăng ký với broker *"tao muốn nghe"*, có dữ liệu mới broker đẩy xuống ngay.

> **Best practice thầy chốt: LUÔN DÙNG KAFKA.**
> Kafka rất lớn, rất nổi tiếng; những gì RabbitMQ làm được thì Kafka cũng làm được.
> Hỏi ngân hàng / viễn thông — gần như 100% dùng Kafka. Xem JD tuyển dụng cũng thấy Kafka phổ biến hơn hẳn.
>
> Không cần so sánh làm gì — cứ Kafka mà học.

📌 *Bổ sung — khi nào RabbitMQ vẫn hợp lý*: cần routing phức tạp (topic/fanout/direct exchange), cần per-message TTL, priority queue, hoặc hệ thống nhỏ không cần replay/lưu lại event. Nhưng đúng như thầy nói — mặc định chọn Kafka.

## 7. Setup Kafka bằng Docker Compose

- Thầy gửi file `docker-compose.yml` trong chat lớp.
- Comment lại service **MySQL** trong file đó (chạy sẽ lỗi vì máy đã có MySQL riêng).

```bash
docker compose up -d
```

- Kafka UI chạy ở **cổng 8081** — vào để tạo topic, publish/consume message thủ công, xem partition & offset.

Trong UI, khi tạo topic cần điền:
| Field | Ý nghĩa |
|---|---|
| Topic name | vd `order-created` |
| Number of partitions | Số "đường ray" |
| Time to retain data | Giữ message bao lâu (vd 1 day) |

## 8. Bài tập buổi 4

1. Thêm **JPA Auditing** vào **cả 2 service** (`product-service`, `order-service`).
2. Cài **Docker Compose**, chạy Kafka lên, vào UI cổng 8081 **vọc thử**: tạo topic, publish message, xem partition/offset.
3. Về nhà tự tìm hiểu thêm về Kafka.

---

# Buổi 5 — Thực hành giao tiếp Đồng bộ bằng WebClient

> ⚠️ *Buổi này không có transcript chữ, nội dung được khôi phục bằng cách transcribe file video.
> Phần lớn thời lượng buổi học là **thực hành im lặng** (thầy cho lớp ~1 tiếng tự code) nên bản ghi âm chỉ bắt được các đoạn thầy giảng. Các đoạn code bên dưới được dựng lại theo đúng những gì thầy mô tả.*

> **Vị trí trong mạch bài:** buổi 4 học **lý thuyết** Sync vs Async → buổi 5 **code luồng Sync** → buổi 6 tối ưu luồng Sync + chuyển sang Kafka.

## 1. Cấu hình cổng cho từng service

Mỗi service chạy trên một cổng riêng:

| Service | Port |
|---|---|
| **Order Service** | `8080` (mặc định, không cần config) |
| **Product Service** | `8888` |

```yaml
# product-service/application.yml
server:
  port: 8888
```

## 2. Tách interface Client

Thầy tách phần gọi sang service khác thành **interface + implementation** riêng, không viết thẳng trong service.

```java
// Interface — khai báo năng lực cần có
public interface ProductClient {
    List<ProductDTO> getProductsByIds(List<String> productIds);
}
```

```java
// Implementation — chứa logic gọi HTTP
@Component
@RequiredArgsConstructor
public class ProductClientImpl implements ProductClient {
    // logic gọi sang Product Service ở đây
}
```

📌 *Vì sao tách*: tầng service chỉ cần biết *"lấy cho tôi danh sách product"*, không cần biết lấy bằng HTTP hay gRPC hay Kafka. Đổi công nghệ giao tiếp thì chỉ sửa Impl, service không đụng tới.

## 3. WebClient — công cụ gọi Sync

> *"Để gọi sang bên chỗ product, hay là từ service này gọi sang service kia thì bắt buộc mình phải sử dụng WebClient."*

### 3.1 — Config Bean

```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```

### 3.2 — Gọi sang Product Service

```java
@Component
@RequiredArgsConstructor
public class ProductClientImpl implements ProductClient {

    private final WebClient.Builder webClientBuilder;

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8888";

    @Override
    public List<ProductDTO> getProductsByIds(List<String> productIds) {

        BaseResponse<List<ProductDTO>> response = webClientBuilder.build()
                .post()                                              // ① method
                .uri(PRODUCT_SERVICE_URL + "/v1/products/get-by-ids") // ② đường dẫn
                .bodyValue(productIds)                               // ③ body truyền đi
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<ProductDTO>>>() {})
                .block();                                            // ④ CHỜ — đây là SYNC

        // ⑤ Validate dữ liệu trả về
        if (response == null || response.getData() == null) {
            throw new ApplicationException("Dữ liệu truyền sang product service bị sai");
        }
        return response.getData();
    }
}
```

**Năm thứ cần quan tâm khi gọi** (đúng thứ tự thầy liệt kê):

| # | Thành phần | Ghi chú |
|---|---|---|
| ① | **Phương thức** | `post()` — vì truyền một *list* ID, không nhét vừa query param |
| ② | **URI** | Đường dẫn API bên service kia |
| ③ | **Body** | Dữ liệu truyền vào — `List<String> productIds` |
| ④ | **Kiểu trả về** | `BaseResponse<List<ProductDTO>>` — phải khai đúng, dùng `ParameterizedTypeReference` vì có generic |
| ⑤ | **Validate response** | Nếu `null` hoặc `data == null` → dữ liệu truyền sai, throw |

📌 *Điểm mấu chốt của SYNC*: `.block()` — Order Service **dừng lại chờ** Product Service phản hồi rồi mới đi tiếp. Đây chính là "đồng bộ" đã học ở buổi 4.

📌 *Bổ sung*: `WebClient` là API mới (reactive, non-blocking core) thay cho `RestTemplate` đã deprecated. Ngoài ra còn `FeignClient` (khai báo bằng interface + annotation, không phải viết code gọi) — thầy có nhắc ở buổi 8 khi ôn tập.

## 4. Luồng Create Order (thầy giảng lại)

> *"Bây giờ mình bỏ qua câu chuyện lock sản phẩm đi, tí nữa mình làm sau."*

```
① Request → OrderController → OrderService

② OrderService BẮT BUỘC phải validate:
      - product ID có thực sự tồn tại không?
        ("hay là mày quăng cho người ta một cái ID vớ vẩn?")
      - số lượng có phù hợp không?
        ("DB của tao chỉ có 10 sản phẩm nhưng mày muốn mua tận 11")
   ⚠️ Muốn validate được → phải gọi sang PRODUCT SERVICE

③ Khởi tạo biến tổng:        totalAmount = 0
④ Khởi tạo mảng rỗng:        List<OrderItem> items = new ArrayList<>()

⑤ Duyệt từng order item trong request:
      - kiểm tra product ID có tồn tại
      - kiểm tra quantity <= stock
      - tạo OrderItem, add vào mảng
      - cộng dồn totalAmount

⑥ Lưu order + order items xuống DB
```

📌 Lưu ý: ở buổi 5 luồng này còn viết theo kiểu **duyệt từng item và gọi service trong vòng lặp** — chính chỗ này bị thầy sửa ở **buổi 6** (không được gọi I/O trong loop, phải gom nhóm gọi một lần + dùng `Map` tra cứu `O(1)`).

## 5. Demo trên lớp

```http
POST http://localhost:8080/v1/orders
Content-Type: application/json

{
  "customerId": "<giá trị bất kỳ>",
  "items": [
    { "productId": "<id sản phẩm 1>", "quantity": 2 },
    { "productId": "<id sản phẩm 2>", "quantity": 1 }
  ]
}
```

Kết quả:
- Order tạo thành công.
- Kiểm tra DB thấy **2 đơn hàng** (1 đơn cũ + 1 đơn vừa tạo).
- Đơn vừa tạo có `total_amount = 700.000` — đúng bằng tổng của 2 sản phẩm × số lượng tương ứng.

## 6. Lời thầy nhận xét

> *"Thấy khó đúng không? Nhưng thực ra đi làm những luồng này không phải là luồng khó. Chỉ là các bạn mới tiếp xúc nên thấy nó phức tạp thôi — chứ những luồng này vẫn thuộc loại dễ."*

## 7. Bài tập buổi 5

Hoàn thiện luồng **create order gọi sang Product Service qua WebClient** (thầy cho thêm thời gian tại lớp, ai chưa xong về nhà làm nốt).

---

# Buổi 6 — Tối ưu giao tiếp Sync & triển khai Kafka

> Trọng tâm: (1) review + tối ưu luồng đặt hàng đồng bộ, (2) code Producer/Consumer Kafka thật.

## 1. Review luồng Create Order (Sync)

Luồng chuẩn thầy chốt sau khi review bài cả lớp:

```
① Group order items theo productId
      → gộp trùng, CỘNG DỒN quantity
② Lấy List<productId>  →  gọi Product Service MỘT LẦN (batch)
③ Chuyển List<ProductDTO> nhận về  →  Map<productId, ProductDTO>
④ Tạo order (status = PENDING), save để lấy orderId
⑤ Loop từng order item:
      - Tra Map: productId tồn tại không? → không thì throw
      - quantity <= stock? → không thì throw "Số lượng trong kho không đủ"
      - Tạo OrderItem (snapshot price), add vào list
      - Cộng dồn totalAmount += price * quantity
⑥ saveAll order items
⑦ Update lại order.totalAmount, save
⑧ Gọi Product Service để LOCK (trừ) stock — batch một lần
```

### 1.1 — Vì sao phải group items theo productId

Client (hoặc kẻ tấn công qua Postman) có thể gửi:

```json
{ "items": [
    { "productId": "P1", "quantity": 1 },
    { "productId": "P1", "quantity": 1 }
]}
```

Nếu không gộp → trừ stock **2 lần riêng biệt**, logic validate `quantity <= stock` chạy trên từng dòng nên có thể lọt. Gộp lại thành `{P1: 2}` rồi mới validate.

```java
Map<String, Integer> grouped = request.getItems().stream()
        .collect(Collectors.groupingBy(
                OrderItemRequest::getProductId,
                Collectors.summingInt(OrderItemRequest::getQuantity)
        ));
```

## 2. ⚠️ BÀI HỌC LỚN — Không gọi I/O trong vòng lặp

Đây là phần thầy dành nhiều thời gian nhất buổi.

### I/O là gì

Thao tác **đọc/ghi giao tiếp ra ngoài process**:
- Gọi sang service khác (Order → Product)
- Query xuống database (DB cũng là một server khác)
- Đọc/ghi file, gọi API bên thứ 3

**I/O cực kỳ tốn chi phí** — chậm hơn tính toán trong RAM hàng nghìn lần.

### Ví dụ "bao cát" của thầy

> Server ở Việt Nam, server kia ở Mỹ.
> Chạy 10 lần từ VN sang Mỹ, mỗi lần ôm **1 bao cát**
> — hay chạy **1 lần** ôm **10 bao cát**?
>
> → 1 lần / 10 bao. Nếu là 1000 bao thì càng chênh lệch khủng khiếp.

### ❌ SAI — I/O trong loop

```java
for (OrderItemRequest item : request.getItems()) {
    // ❌ Mỗi vòng lặp = 1 lần gọi mạng sang Product Service
    ProductDTO product = productClient.getProduct(item.getProductId());
    ...
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
List<String> ids = request.getItems().stream()
        .map(LockItem::getProductId).toList();
List<Product> products = productRepository.findAllById(ids);
// ... xử lý trong RAM ...
productRepository.saveAll(products);
```

> **Quy tắc**: hạn chế thao tác I/O ít nhất có thể. Xử lý theo list thì gom nhóm lại xử lý một lần.

## 3. Map lookup O(1) thay vì scan List O(n)

Sau khi nhận `List<ProductDTO>`, khi loop order items lại phải **tìm** product tương ứng.

### ❌ SAI — O(n²)

```java
for (OrderItemRequest item : items) {          // n vòng
    ProductDTO product = products.stream()      // lại duyệt n phần tử
            .filter(p -> p.getId().equals(item.getProductId()))
            .findFirst().orElseThrow();
}
// → O(n²)
```

### ✅ ĐÚNG — O(n)

```java
// Chuyển List -> Map một lần: O(n)
Map<String, ProductDTO> productMap = products.stream()
        .collect(Collectors.toMap(ProductDTO::getId, p -> p));

for (OrderItemRequest item : items) {           // n vòng
    ProductDTO product = productMap.get(item.getProductId());  // O(1)
    if (product == null) throw new ApplicationException("Product not exist");
}
// → O(n)
```

| Cách tra cứu | Độ phức tạp |
|---|---|
| Duyệt `List` | `O(n)` mỗi lần → tổng `O(n²)` |
| Tra `Map` (hash) | `O(1)` mỗi lần → tổng `O(n)` |

> Thầy nói thẳng: *"Anh nghĩ nhiều bạn sẽ không hiểu đâu, nhưng về nhà nghe lại phần này."*

## 4. Đặt tên DTO cho request

```java
// ❌ SAI
public ResponseEntity<?> validate(@RequestBody List<ProductDTO> products)
```
→ Người đọc API tưởng **phải truyền đủ mọi trường** của Product (name, price, category...).

```java
// ✅ ĐÚNG
public ResponseEntity<?> validate(@RequestBody List<ProductValidateRequest> request)

@Getter @Setter
public class ProductValidateRequest {
    private String productId;
    private Integer quantity;
}
```

> Request DTO phải **phản ánh đúng cái client cần truyền**, không tái dùng DTO to.

## 5. API Lock Product

```java
// DTO
@Getter @Setter
public class LockProductRequest {
    @NotEmpty
    private List<LockProductItem> items;   // ✅ LIST, không phải từng cái
}

@Getter @Setter
public class LockProductItem {
    @NotBlank private String productId;
    @NotNull @Positive private Integer quantity;
}
```

```java
// Product Service
@Transactional
public void lockProduct(LockProductRequest request) {
    List<String> ids = request.getItems().stream()
            .map(LockProductItem::getProductId).toList();

    List<Product> products = productRepository.findAllById(ids);   // 1 query
    Map<String, Product> map = products.stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

    for (LockProductItem item : request.getItems()) {
        Product p = map.get(item.getProductId());
        if (p == null) throw new ApplicationException("Product not found");
        if (p.getStock() < item.getQuantity())
            throw new ApplicationException("Not enough stock");
        p.setStock(p.getStock() - item.getQuantity());   // stock mới = cũ - quantity
    }

    productRepository.saveAll(products);                            // 1 lần save
}
```

### Vì sao phải "lock" stock

> `stock = 10`. Đơn A mua 9, đơn B mua 2.
> Nếu không trừ stock ngay khi tạo đơn A → đơn B vẫn thấy `stock = 10` và tạo thành công.
> → **Oversell.** Phải trừ ngay để "giữ chỗ", không thằng khác cướp mất.

## 6. Câu hỏi mở: đơn hàng nhiều seller

Hỏi: nếu 1 order chứa sản phẩm của **nhiều seller** thì sao?

Thầy trả lời:
- Thực tế **phải tách thành nhiều order** — mỗi seller một order.
- Lý do: **shipping**. Mỗi seller có địa chỉ lấy hàng khác nhau, shipper phải đến từng nơi. Không thể gộp 1 order với 1 địa chỉ giao.
- Ngoại lệ: **sản phẩm số** (digital) — không cần vận chuyển, không cần tách.
- Không cần client truyền `sellerId` — từ `productId` server tự suy ra seller.
- **Ngoài scope lớp** (lớp không làm shipping/seller).

## 7. Chuyển sang luồng ASYNC

Luồng bất đồng bộ cho Create Order:

```
① Order Service: validate order
      ⚠️ VẪN PHẢI gọi Product Service (cần lấy giá + check tồn tại)
② Order Service: save order với status = PENDING/NEW
③ Order Service: PUBLISH message lên topic "order-created"
      → KHÔNG gọi Product Service để lock nữa
④ Product Service: CONSUME topic "order-created"  →  lock product
⑤ Product Service: PUBLISH message lên topic "product-locked"
⑥ Order Service: CONSUME topic "product-locked"  →  update order status
```

> ⚠️ Bước ① **vẫn là sync**. Không thể async hoàn toàn vì cần giá sản phẩm ngay để tính `totalAmount` và trả response cho khách.
> Chỉ bước **lock stock** được chuyển sang async.

### Chia topic nhỏ hay gộp?

| Cách | Ưu | Nhược |
|---|---|---|
| 1 topic `order` chung mọi sự kiện | Ai cần toàn bộ chỉ consume 1 topic | Consume cả những event **không cần** |
| **Nhiều topic** (`order-created`, `order-cancelled`, `order-updated`) | Dễ quản lý, chỉ nghe cái mình cần | Ai cần toàn bộ phải consume nhiều topic |

> **Thầy prefer cách 2** — chia nhỏ theo sự kiện. Chia càng nhỏ càng dễ quản lý.
> Đây là **trade-off**, không có đúng/sai tuyệt đối.

## 8. Kafka PRODUCER (Order Service)

### 8.1 — Dependency

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### 8.2 — Config

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

**Vì sao cần serializer?**

> Java làm việc với **Object**. Kafka chỉ nhận **byte[]**.
> → Producer phải **serialize**: Object → byte[]
> → Consumer phải **deserialize**: byte[] → Object

| | Producer | Consumer |
|---|---|---|
| Chiều | Object → byte[] | byte[] → Object |
| Tên | **Serializer** | **Deserializer** |
| Key (String) | `StringSerializer` | `StringDeserializer` |
| Value (JSON) | `JsonSerializer` | `JsonDeserializer` |

⚠️ `JsonSerializer` thuộc package `org.springframework.kafka.support.serializer` (của Spring Kafka), **khác** package của `StringSerializer` (`org.apache.kafka.common.serialization` — của Kafka gốc).

### 8.3 — Publish message

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final KafkaTemplate<String, Object> kafkaTemplate;   // Spring tự tạo bean

    private static final String TOPIC_ORDER_CREATED = "order-created";

    public Order create(CreateOrderRequest request) {
        // ... logic tạo order ...
        Order createdOrder = orderRepository.save(order);

        // Publish lên Kafka
        kafkaTemplate.send(TOPIC_ORDER_CREATED, createdOrder);
        log.info("Publish new order success");

        return createdOrder;
    }
}
```

`send()` có nhiều overload:
- `send(topic, value)` — bỏ qua key
- `send(topic, key, value)` — có key (key quyết định partition)

### 8.4 — Tạo topic

Trên Kafka UI (cổng 8081):

| Field | Giá trị |
|---|---|
| Topic name | `order-created` |
| Partitions | 3 |
| Replication factor | 1 |
| Retention | 2 days |

📌 Nếu Kafka **không bật xác thực** và account có quyền → publish lên topic chưa tồn tại sẽ **tự tạo topic**. Ở công ty thường bị cấm, phải xin đội vận hành khai báo topic.

## 9. Kafka CONSUMER (Product Service)

### 9.1 — Config

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

### 9.2 — Listener

```java
@Component
@Slf4j
public class OrderCreatedConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-created")
    public void handleOrderCreatedEvent(String message) throws JsonProcessingException {
        Order order = objectMapper.readValue(message, Order.class);
        log.info("Receive order message: {}", order);

        // TODO: lock product ở đây
    }
}
```

```java
@Getter @Setter @ToString
@JsonIgnoreProperties(ignoreUnknown = true)    // ⚠️ bắt buộc
public class Order {
    private String id;
    private String customerId;
    private String status;
    private Integer totalAmount;
}
```

## 10. ⚠️ CONSUMER GROUP — phần quan trọng nhất buổi

### 10.1 — Consumer không đọc độc lập, mà đọc theo GROUP

**Bối cảnh:** khi tải lớn, ta chạy **nhiều instance** của cùng một service.

> 100 request, 1 máy → dồn hết vào 1 máy.
> Thuê thêm 9 máy → mỗi máy 10 request. Cả 10 máy đều chạy `product-service`, đều nối cùng DB.

Vấn đề: cả 10 instance đều consume topic `order-created`. Nếu mỗi instance đều xử lý message #1 → **1 công việc bị làm 10 lần**.

**Giải pháp: Consumer Group.**

> Các instance cùng khai `group-id: product-service` → Kafka hiểu chúng **cùng một nhóm**.
> Message #1 đã được instance A consume → instance B..J **không consume nữa**.
> → Chia tải, không trùng lặp.

### 10.2 — Hai tầng quan hệ

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

**Hai quy tắc:**
1. **Một topic → nhiều consumer group.** Các group đọc **hoàn toàn độc lập**, offset riêng. Payment đọc tới message 200, Product mới đọc tới 102 — không liên quan gì nhau.
2. **Một consumer group → nhiều consumer.** Các consumer trong group **chia sẻ** công việc, message nào đã đọc thì thằng khác không đọc lại.

### 10.3 — Partition quyết định số consumer hữu ích

| Partition | Consumer trong group | Kết quả |
|---|---|---|
| 3 | 10 | **3 đọc, 7 ngồi chơi** — thừa consumer |
| 3 | 2 | 1 consumer đọc 1 partition, 1 consumer đọc 2 partition |
| 3 | 1 | 1 consumer đọc cả 3 partition |
| 3 | 0 | Không ai đọc |

> **Số consumer hoạt động tối đa = số partition.** Thêm consumer vượt số partition là lãng phí.

### 10.4 — Consumer group "chết" khi nào

> Group chỉ được coi là chết khi **không còn một consumer nào** trong group.
> 10 instance, 9 chết còn 1 → group **vẫn sống**, vẫn đọc bình thường.

## 11. ⚠️ `auto-offset-reset` — bẫy hay gặp

| Giá trị | Nghĩa |
|---|---|
| `earliest` | Đọc từ message **ĐẦU TIÊN** của topic |
| `latest` | Đọc từ message **MỚI NHẤT** (bỏ qua lịch sử) |

### Điểm mấu chốt: config này CHỈ có tác dụng LẦN ĐẦU group đăng ký

```
Lần đầu group "product-service" join Kafka
   → Kafka: "Mày mới, bắt đầu đọc từ đâu?" → đọc auto-offset-reset
   → Ghi nhận offset khởi điểm

Group đã đọc tới message 101, rồi service CHẾT 15 NGÀY
   → Topic tích lũy tới 200 message

Service SỐNG LẠI với cùng group-id
   → Kafka: "15 ngày trước mày đã đọc tới 101 rồi"
   → BẮT ĐẦU TỪ 102, không phải 200, cũng không phải 0
   → auto-offset-reset lúc này VÔ NGHĨA
```

> Sửa `earliest` → `latest` sau khi group đã đăng ký thành công → **không có tác dụng gì**.
>
> **Ngoại lệ**: nếu lần đầu group đăng ký mà consume **liên tục bị lỗi**, offset chưa được commit → group coi như chưa đăng ký thành công → đổi config **vẫn có tác dụng**. Đây chính là tình huống trên lớp.

## 12. Ba lỗi thực tế gặp trên lớp

### Lỗi 1 — `The class is not in the trusted packages`

Spring Kafka mặc định **không cho phép** deserialize về class bất kỳ (lý do bảo mật — chống deserialization attack). Phải khai báo package tin cậy:

```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.json.trusted.packages: "*"     # tin mọi package
```

### Lỗi 2 — `__TypeId__` header — LỖI KINH ĐIỂN CỦA MICROSERVICE

`JsonSerializer` của Spring Kafka **tự nhét header `__TypeId__`** = tên đầy đủ class bên Producer:

```
Headers:
  __TypeId__ : com.example.vti.order.entities.Order
```

Consumer đọc header này và cố convert về **đúng class đó**. Nhưng Product Service **không có** class `com.example.vti.order.entities.Order` (khác service, khác package) → lỗi.

**Giải pháp thầy dùng (đơn giản nhất):** nhận value về **`String`**, tự parse bằng `ObjectMapper`.

```yaml
value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

```java
@KafkaListener(topics = "order-created")
public void handle(String message) throws JsonProcessingException {
    Order order = objectMapper.readValue(message, Order.class);
}
```

📌 *Bổ sung — các cách khác*:
| Cách | Ghi chú |
|---|---|
| Nhận `String` + `ObjectMapper` ← lớp dùng | Đơn giản, kiểm soát hoàn toàn |
| `spring.json.use.type.headers: false` + `spring.json.value.default.type` | Bỏ qua header, ép về class chỉ định |
| Tách **shared module** chứa event class dùng chung | Chuẩn nhất, nhưng tạo coupling giữa các service |
| Dùng **Avro + Schema Registry** | Chuẩn công nghiệp cho hệ thống lớn |

### Lỗi 3 — `UnrecognizedPropertyException`

JSON có trường `isDeleted`, `createdAt`... nhưng class `Order` bên Consumer chỉ khai 4 trường → Jackson báo lỗi.

```java
@JsonIgnoreProperties(ignoreUnknown = true)   // ✅ bỏ qua trường thừa
public class Order { ... }
```

## 13. ⚠️ Cơ chế RETRY — hành vi khác nhau tùy chỗ lỗi

Đây là chi tiết rất quan trọng thầy chỉ ra khi debug:

| Lỗi xảy ra ở đâu | Hành vi |
|---|---|
| **TRƯỚC** khi vào hàm listener (deserialize thất bại) | **Retry VÔ HẠN** — log lỗi liên tục, kẹt vĩnh viễn ở message đó |
| **TRONG** thân hàm listener (business logic throw) | Spring Kafka retry **~10 lần**, hết thì **bỏ qua**, commit offset, đọc message tiếp |

```
byte[] ──deserialize──► Object ──► @KafkaListener method
         ▲                              ▲
         │                              │
    lỗi ở đây                      lỗi ở đây
    → retry VÔ HẠN                 → retry 10 lần rồi skip
```

**Và:** consume **thất bại** thì offset **không được commit** → restart service sẽ **đọc lại message đó**. Chỉ khi consume **thành công** Kafka mới ghi nhận và chuyển sang offset tiếp theo.

📌 *Bổ sung — production nên có*: **Dead Letter Topic (DLT)**. Message retry hết số lần vẫn fail → đẩy sang topic `order-created.DLT` để điều tra sau, thay vì mất im lặng. Spring Kafka hỗ trợ qua `DeadLetterPublishingRecoverer` + `DefaultErrorHandler`.

## 14. Kafka KHÔNG có transaction chung với Database

Câu hỏi trên lớp: *"Kafka có nằm trong transaction không?"*

> **KHÔNG.** Kafka là một **server riêng**, Database là một **server riêng** — hai thực thể độc lập.
> Không có cơ chế nào đảm bảo transaction xuyên hai thực thể này.
> (Kafka có transaction **nội tại** của nó, nhưng không liên quan tới DB transaction.)

Hệ quả nguy hiểm:
```java
@Transactional
public Order create(...) {
    Order order = orderRepository.save(order);   // ① ghi DB
    kafkaTemplate.send("order-created", order);  // ② publish Kafka
    // Nếu ① rollback sau ② → DB không có order, nhưng Kafka ĐÃ có event → SAI LỆCH
}
```

📌 *Bổ sung — giải pháp chuẩn*: **Transactional Outbox Pattern**.
Ghi event vào bảng `outbox` **trong cùng transaction** với order → một tiến trình riêng đọc bảng `outbox` và publish lên Kafka. Đảm bảo DB và event luôn nhất quán.

## 15. Bài tập buổi 6

1. Hoàn thiện **lock product** (API + gọi từ Order Service) — buổi sau thầy check **tất cả** các bạn.
2. Code **Producer** trong Order Service (publish `order-created`).
3. Code **Consumer** trong Product Service (consume `order-created`).

> Thầy nhấn mạnh: *"Phần này khó, nhiều logic. Chỉ nghe mà không thực hành thì như vịt nghe sấm."*

---

# Buổi 7 — Event class, Race Condition & Retry Non-Blocking

## 1. ⚠️ Câu hỏi thầy hỏi cả lớp mãi không ai trả lời được

> **Tại sao Product Service phải consume topic `order-created`?**

Cả lớp trả lời vòng vo: *"để lắng nghe"*, *"để nhận message"* — thầy bảo đó chỉ là lặp lại câu hỏi.

**Đáp án: ĐỂ LOCK SẢN PHẨM.**

### So sánh hai luồng để hiểu rõ

| | **SYNC** | **ASYNC** |
|---|---|---|
| Ai chủ động? | **Order** gọi Product, **ra lệnh** "mày lock đi" | **Product** tự lắng nghe rồi **chủ động** đi lock |
| Order có chờ không? | ✅ Chờ Product phản hồi xong mới trả về user | ❌ Trả về user ngay, status = `NEW` |
| Trả về user khi nào? | Sau khi lock xong → "Đặt hàng thành công" | Ngay lập tức → "Đơn hàng đã được ghi nhận" |

> Trong cả hai luồng, **bắt buộc phải lock sản phẩm**. Chỉ khác **ai khởi xướng** việc đó.

## 2. OrderCreatedEvent — tách class Event riêng

**Vấn đề:** buổi 6 publish thẳng entity `Order` — nhưng `Order` **không chứa** `order items`. Product Service cần items để biết lock sản phẩm nào, số lượng bao nhiêu.

**Giải pháp:** tạo package `event` riêng chứa các event class.

```java
// package com.vti.order.event
@Getter @Setter
public class OrderCreatedEvent extends Order {
    private List<OrderItem> orderItems;
}
```

```java
// Mapper
@Mapper(componentModel = "spring")
public interface OrderMapper {
    Order toOrder(CreateOrderRequest request);
    OrderCreatedEvent toEvent(Order order);       // ← thêm
}
```

```java
// Service
Order createdOrder = orderRepository.save(order);
List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

OrderCreatedEvent event = orderMapper.toEvent(createdOrder);
event.setOrderItems(savedItems);                  // ← nhét items vào

kafkaTemplate.send(TOPIC_ORDER_CREATED, event);   // publish EVENT, không phải entity
```

Message trên Kafka trước và sau:

```json
// TRƯỚC — thiếu items, Product không lock được
{ "id": "...", "customerId": "...", "status": "NEW", "totalAmount": 200000 }

// SAU — đủ thông tin để lock
{ "id": "...", "customerId": "...", "status": "NEW", "totalAmount": 200000,
  "orderItems": [ { "productId": "P1", "quantity": 2, "price": 100000 } ] }
```

📌 Bên **Product Service** cũng phải tạo class `OrderCreatedEvent` + `Order` + `OrderItem` tương ứng để `ObjectMapper` parse về.

## 3. ObjectMapper nên là Bean

```java
// ❌ SAI — mỗi lần consume lại khởi tạo mới
public class OrderCreatedConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();
}

// ✅ ĐÚNG — inject bean có sẵn của Spring
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {
    private final ObjectMapper objectMapper;
    private final ProductService productService;

    @KafkaListener(topics = "order-created")
    public void handle(String message) throws JsonProcessingException {
        OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

        List<LockProductItem> items = event.getOrderItems().stream()
                .map(i -> new LockProductItem(i.getProductId(), i.getQuantity()))
                .toList();

        productService.lockProduct(new LockProductRequest(items));   // gọi service
    }
}
```

## 4. Cập nhật trạng thái đơn hàng — hoàn tất vòng lặp

Sau khi Product lock xong, order **phải** đổi trạng thái, không thể mãi ở `NEW`.

### Vì sao? — Ví dụ Shopee của thầy

```
Chờ xác nhận → Đang chuẩn bị hàng → Shipper đã lấy hàng → Đang giao → Đã giao
```

> **Mỗi bước trong hành trình đều phải chuyển trạng thái** để người dùng biết đơn hàng đang ở đâu. Nếu lock thất bại (hết hàng), cũng phải báo cho họ biết.

### Luồng hoàn chỉnh

```
① Order Service → publish "order-created"       (status = NEW)
② Product Service consume → lock stock
③ Product Service → publish "product-locked"    (chỉ chứa orderId)
④ Order Service consume → status = PREPARING/CONFIRMED
   (nếu lock fail → publish "product-lock-failed" → status = CANCELLED/FAILED)
```

## 5. ⚠️ Quy tắc đặt tên Topic

Thầy sửa bài Hải Trương: bạn đặt topic là `order-confirm`.

```
❌ order-confirm      ← sai
✅ product-locked     ← đúng
```

**Hai lý do:**

1. **Topic đặt theo NGUỒN PHÁT + trạng thái**, không theo hệ quả.
   - Message này **xuất phát từ Product Service** → tên phải bắt đầu bằng `product`.
   - `order-created` cũng vậy: dữ liệu của order, publish bởi Order Service, trạng thái `created`.

2. **Không được ràng buộc vào một consumer cụ thể.**
   > Đặt `order-confirm` nghĩa là việc lock này chỉ phục vụ cho việc confirm order.
   > Nhưng sau khi lock xong, có thể có **nhiều nghiệp vụ khác** cũng cần biết (analytics, inventory report...).
   > `product-locked` chỉ nói **"tao đã lock xong"** — ai muốn làm gì với thông tin đó là việc của họ.

## 6. ⚠️ RACE CONDITION & SELECT FOR UPDATE

Phần cực kỳ quan trọng, thầy nói **hay hỏi phỏng vấn từ level fresher/junior trở lên**.

### Vấn đề

Đoạn code hầu hết cả lớp viết:
```java
List<Product> products = productRepository.findAllById(ids);
for (...) { product.setStock(product.getStock() - quantity); }
productRepository.saveAll(products);
```

**Hai request đồng thời:**

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

→ Đây là **Race Condition** (tranh chấp dữ liệu). Hai tiến trình cùng đọc và cùng ghi một record.

### Giải pháp: SELECT ... FOR UPDATE (Pessimistic Lock)

```sql
SELECT * FROM products WHERE id = 'A' FOR UPDATE;
```

> Câu select này nằm trong một **transaction** và báo cho DB biết transaction đó **sẽ update** record.
> DB **khóa record** lại. Transaction khác muốn đọc phải **CHỜ** đến khi transaction đầu **commit**.

```
Request A: SELECT FOR UPDATE → đọc 10 → trừ 2 → save 8 → COMMIT
Request B:                     ⏳ CHỜ ...                    → đọc 8 → trừ 3 → save 5 ✅
```

### Triển khai trong Spring Data JPA

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

> ⚠️ **Không** gắn `@Lock` lên hàm `findById` dùng chung. Nhiều chỗ chỉ **search** mà không update — gắn lock vào sẽ khóa oan, làm chậm toàn hệ thống. Tách thành hàm riêng.

Verify bằng cách bật `show-sql: true` → thấy `... for update` ở cuối câu SQL.

### Khi nào cần?

> Thực tế xác suất 2 request tranh chấp cùng lúc **không nhiều**, khá hiếm. Sản phẩm bình thường có thể bỏ qua.
> Nhưng **sản phẩm tài chính thì cực kỳ chặt chẽ**, bắt buộc phải có.
>
> **Đi phỏng vấn thì luôn bị hỏi** — dù công ty bạn có làm hay không. Phải biết, chỉ là có triển khai hay không.

📌 *Bổ sung — Optimistic Lock, giải pháp thay thế*:
```java
@Entity
public class Product {
    @Version
    private Long version;   // JPA tự tăng mỗi lần update
}
```
Không khóa record. Khi update, JPA thêm `WHERE version = ?`. Nếu ai đó đã sửa trước (version đã đổi) → `OptimisticLockException` → app tự retry.
- **Pessimistic** (`FOR UPDATE`): phù hợp khi **tranh chấp nhiều** (flash sale)
- **Optimistic** (`@Version`): phù hợp khi **tranh chấp ít** — không khóa nên nhanh hơn

## 7. ⚠️ RETRY: BLOCKING vs NON-BLOCKING

Phần thầy đánh giá là **kiến thức nâng cao, nhiều Junior không biết, phỏng vấn trả lời được sẽ ghi điểm rất lớn**.

### 7.1 — Hai loại lỗi

| Loại | Ví dụ | Retry có ý nghĩa? |
|---|---|---|
| **Có thể retry** | `TimeoutException`, DB đang lỗi, gọi service khác đang bận, mạng chập chờn | ✅ Có — lát nữa có thể ok |
| **KHÔNG thể retry** | `BusinessException` — "Product không tồn tại", `orderId` bị null | ❌ Vô nghĩa — retry 1 tỷ lần vẫn null |

> Nếu `id` bằng null thì em retry 1 tỷ lần nó vẫn null mà.

### 7.2 — Blocking Retry (MẶC ĐỊNH — có vấn đề)

Spring Kafka mặc định retry **ngay lập tức**, liên tiếp:

```
Message 1 lỗi → retry ngay → lỗi → retry ngay → ... (10 lần) → bỏ qua
                └──────────────── Message 2 PHẢI CHỜ ────────────────┘
```

> Kafka xử lý message **theo đúng thứ tự**. Message 1 chưa xong thì Message 2 chưa được xử lý.
> Retry 100 lần, mỗi lần 1 giây → **Message 2 phải chờ 100 giây**.

→ Việc retry đang **BLOCK** toàn bộ message phía sau. Đó là lý do gọi là **Blocking Retry**.

**Và:** retry ngay lập tức thường vô ích. Nếu DB chết 1 tiếng, retry lại sau 1 mili-giây thì vẫn chết.

### 7.3 — Non-Blocking Retry (GIẢI PHÁP)

Message lỗi **không retry tại chỗ**, mà **đẩy sang một topic khác**:

```
topic: order-created
   │ Message 1 lỗi
   ├──────────────► topic: order-created-retry-2000    (retry sau 2 giây)
   │                     │ vẫn lỗi
   │                     ├──────► topic: order-created-retry-4000   (sau 4 giây)
   │                     │             │ vẫn lỗi
   │                     │             ├──────► order-created-retry-8000  (sau 8 giây)
   │                     │             │             │ vẫn lỗi
   │                     │             │             └──────► order-created-DLT
   │
   └─ Message 2 được consume NGAY, không phải chờ ✅
```

Consumer **đồng thời** lắng nghe cả topic chính lẫn các topic retry.

**Kết quả:** message lỗi vẫn được retry, nhưng **không chặn** các message phía sau.

### 7.4 — Cấu hình `@RetryableTopic`

```java
@RetryableTopic(
    attempts = "4",                                  // retry 4 lần
    backoff = @Backoff(delay = 2000, multiplier = 2.0),  // 2s → 4s → 8s
    exclude = { BusinessException.class }             // lỗi này KHÔNG retry
)
@KafkaListener(topics = "order-created")
public void handle(String message) { ... }
```

| Tham số | Ý nghĩa |
|---|---|
| `attempts` | Số lần thử |
| `backoff.delay` | Thời gian chờ lần đầu (ms) |
| `backoff.multiplier` | Hệ số nhân: `2000 → 4000 → 8000` (exponential backoff) |
| `exclude` | Các exception **không** retry — dành cho lỗi nghiệp vụ |

> **Spring Kafka tự tạo topic, tự đẩy message qua lại.** Không phải tự code — nhưng **vẫn phải hiểu concept**.

### 7.5 — Dead Letter Topic (DLT)

Sau khi hết số lần retry → message vào `<topic>-DLT`.

> **Vào DLT rồi thì KHÔNG consume nữa.** Nó đã không thể xử lý đúng logic thì consume tiếp cũng vô nghĩa.
> Lúc đó chỉ có **xử lý bằng tay** — điều tra và fix thủ công.

## 8. Bài tập buổi 7

1. Hoàn thiện **lock sản phẩm** (nếu chưa làm).
2. Làm **cập nhật trạng thái đơn hàng** sau khi lock (publish `product-locked` → order consume → đổi status).
3. Thêm `SELECT FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`) cho hàm update stock.
4. Áp dụng **`@RetryableTopic`** — retry non-blocking.

---

# Buổi 8 — Ôn tập, Kafka nâng cao & Redis Distributed Lock

## 1. ⚠️ `@Transactional` KHÔNG hoạt động xuyên Microservice

Câu hỏi của Dương: *"Sao consume lỗi mà order vẫn được tạo? Em kỳ vọng nó rollback."*

**Thầy: SAI HOÀN TOÀN.**

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
> Trước thời điểm publish message, dữ liệu **đã persist, đã commit** xuống DB rồi. **Không có cách nào rollback.**

### Rollback trong Microservice = Compensating Transaction

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

### Saga Pattern

> Đây là pattern chuẩn cho transaction phân tán. **Thầy không dạy** vì quá phức tạp với level lớp.
> Thực tế **ít công ty triển khai** — kể cả team thầy cũng không, vì chi phí vận hành cao hơn lợi ích.
> **Nên biết keyword để phỏng vấn.** Ai có thời gian thì tự research.

📌 *Bổ sung — Saga có 2 kiểu*:
| Kiểu | Cách hoạt động |
|---|---|
| **Choreography** | Các service tự lắng nghe event của nhau, không có điều phối viên. ← luồng lớp đang làm gần giống kiểu này |
| **Orchestration** | Có một **Saga Orchestrator** trung tâm điều phối từng bước và gọi compensating khi lỗi |

## 2. Ôn tập — Quy tắc Database

| Câu hỏi | Đáp án |
|---|---|
| `per service per schema` là gì? | Mỗi service có một database riêng |
| 2 service cùng chọc 1 DB? | ❌ **Anti-pattern** |
| 1 service chọc nhiều DB? | ❌ **Anti-pattern** |
| Service A lấy data của service B? | Phải đi qua **API của B**, không được chọc thẳng DB của B |

## 3. Ôn tập — Giao tiếp giữa Service

```
Giao tiếp giữa 2 microservice
│
├── SYNC (đồng bộ) ─── Command-based: ra lệnh và CHỜ phản hồi
│     ├── REST/HTTP  → công cụ: WebClient, RestTemplate, FeignClient
│     └── gRPC
│
└── ASYNC (bất đồng bộ) ─── Event-based: phát sự kiện, KHÔNG chờ
      ├── Kafka  ← lớp dùng
      └── RabbitMQ / ActiveMQ
```

> Đi phỏng vấn nên trả lời **generic trước** (sync/async, HTTP/gRPC), rồi mới đi vào công cụ cụ thể (WebClient...). Đừng nhảy thẳng vào `WebClient`.

**Flash sale, 1 triệu người mua cùng lúc — sync hay async tốt hơn?**
> **Async.** Kafka đóng vai trò **message queue / buffer** — cứ publish lên rồi xử lý dần, đơn hàng ở trạng thái chờ. Sync thì mọi người phải đợi nhau, dễ sập.

## 4. Ôn tập — Kafka cơ bản

| Thuật ngữ | Nghĩa |
|---|---|
| **Broker** | Một server Kafka |
| **Cluster** | Cụm nhiều broker — để backup cho nhau, một con chết con khác thay thế (khái niệm này cũng có ở MySQL) |
| **Topic** | ≈ Table trong MySQL |
| **Message** | ≈ Record trong MySQL |
| **Event / Data / Message** | **Bản chất giống nhau** — "publish một event" = "publish một message" = "publish data" |
| **Message gồm gì?** | **Key** + **Value** |
| **Partition** | "Đường ray" — cho phép xử lý **song song**. Đánh đổi: **mất tính ordering** |
| **Offset** | Số thứ tự message **trong một partition** |
| **Định danh 1 message** | Cần **CẢ** `partition` **VÀ** `offset` |

📌 Số partition thường cấu hình **3 hoặc 6**, không nên quá nhiều.

## 5. ⚠️ Consumer Group — ví dụ "cái bánh"

Thầy dùng ẩn dụ rất rõ:

> **1 partition = 1 cái bánh. 1 consumer = 1 người ăn.**

| Tình huống | Kết quả |
|---|---|
| 3 bánh, 1 người | 1 người ăn **cả 3 cái**, ăn **song song**, không hề chậm |
| 3 bánh, 2 người | 1 người ăn 2 cái, 1 người ăn 1 cái |
| 3 bánh, 3 người | Mỗi người 1 cái — **tối ưu** |
| 3 bánh, 4 người | 1 người **ngồi đói**, không có gì ăn |
| Đang ăn thì 1 người chết | Người khác **tiếp quản** phần của họ (rebalance) |

**Hai luật bất di bất dịch:**
1. **Một partition CHỈ được đọc bởi MỘT consumer** trong cùng group. Không có chuyện 2 consumer cùng chúi mõm vào 1 partition.
2. **Một consumer CÓ THỂ đọc nhiều partition** — song song, không chậm.

**Vì sao cần consumer group?**
> Thay vì 1 mình Hải Trương ăn, giờ Hải Trương + Dương + Việt Hoàng cùng ăn → **nhanh hơn**.
> Và nếu Hải Trương bận, người khác **ăn hộ phần** → **chịu lỗi**.

## 6. Ôn tập — `auto-offset-reset`

| Giá trị | Nghĩa |
|---|---|
| `latest` | Đọc từ message **mới nhất** ← thường dùng |
| `earliest` | Đọc từ message **đầu tiên** |

> **CHỈ có ý nghĩa khi consumer group đó consume topic LẦN ĐẦU.**
>
> Group đã đọc tới offset 3 → chết 1 tháng → topic có 1000 message → sống lại
> → **bắt đầu từ offset 4**, không phải 1000, không phải 0.
> Kafka đã ghi nhớ: *"ông này lần trước đọc tới message thứ 3 rồi."*

## 7. ⚠️ REPLICATION FACTOR (Replica) — MỚI

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
| 1 | 2 | ❌ | Bản chính và bản sao cùng nằm 1 server → server chết là mất hết, **vô nghĩa** |
| 2 | 2 | ✅ | |
| 2 | 3 | ❌ | Không đủ server để đặt bản sao thứ 3 |
| 5 | 3 | ✅ | 1 chính + 2 sao, 2 server còn lại không chứa gì |

> Docker Compose của lớp chỉ có **1 broker** → chỉ config được `replica = 1`.

### Leader & Follower

```
Broker A: [Partition 0 — LEADER]     ← MỌI thao tác đọc/ghi đều ở đây
Broker B: [Partition 0 — FOLLOWER]   ← chỉ SAO CHÉP từ leader
Broker C: [Partition 0 — FOLLOWER]   ← chỉ SAO CHÉP từ leader
```

- **Leader chứa bản chính**, **Follower chứa bản sao**.
- Leader chết → một Follower được **bầu lên** làm Leader ngay lập tức.
- ⚠️ **Đọc VÀ ghi đều làm việc với LEADER.** Follower chỉ có nhiệm vụ sao chép, không phục vụ đọc/ghi.

> Hiểu về replica rồi sẽ thấy **Kafka rất mạnh** — gần như không bao giờ mất dữ liệu.

## 8. ⚠️ ACKS — MỚI (chỉ áp dụng cho PRODUCER)

> Thầy giải thích nhầm sang consumer giữa chừng rồi **tự đính chính**: `acks` **chỉ dành cho Producer** — trường hợp **đẩy** dữ liệu lên, không phải kéo về.

```yaml
spring:
  kafka:
    producer:
      acks: all       # 0 | 1 | all
```

**Câu hỏi `acks` trả lời:** *khi publish message, phải có bao nhiêu broker xác nhận đã nhận thì mới coi là thành công?*

| `acks` | Chờ ai xác nhận | Tốc độ | Rủi ro |
|---|---|---|---|
| **`0`** | **Không chờ ai** — quăng lên rồi kệ | Nhanh nhất | **Mất message** nếu server chết |
| **`1`** | Chỉ **Leader** xác nhận | Trung bình | Leader confirm xong rồi **chết trước khi follower sao chép** → mất message |
| **`all`** | **Leader + TẤT CẢ Follower** xác nhận | Chậm nhất | **An toàn nhất — không mất message** |

> **Mặc định là `all`** vì nó an toàn nhất.
>
> Spring Kafka còn có cơ chế **retry khi publish fail** (mạng chập chờn) — tự thử lại.

## 9. Outbox Pattern

> Thầy cho keyword về tự research, không dạy vì phức tạp.
> **Biết cái này đi phỏng vấn là điểm cộng rất lớn.**

📌 *Bổ sung — vì sao cần*: chính là để giải bài toán mục 1 (DB commit và Kafka publish không cùng transaction). Ghi event vào bảng `outbox` **trong cùng transaction** với order, rồi một tiến trình riêng đọc `outbox` và publish lên Kafka → DB và event luôn nhất quán.

## 10. ⚠️ REDIS & DISTRIBUTED LOCK — Nội dung mới chính

### 10.1 — Redis là gì

| Đặc điểm | Ý nghĩa |
|---|---|
| **Key-Value store** | Giống `Map` trong Java — có key và value |
| **Lưu trên RAM** | ⚡ Cực nhanh, không phải đọc disk |
| **Độ phức tạp `O(1)`** | Tra cứu theo key — như `HashMap` |
| **SINGLE-THREADED** | ⚠️ Chỉ xử lý **một** lệnh tại một thời điểm — không song song |

> Chính vì **single-threaded** mà Redis phù hợp làm **lock**: đảm bảo tại một thời điểm chỉ một request được xử lý.
> (Database thì **multi-threaded**.)

### 10.2 — Vì sao cần Distributed Lock (khi đã có SELECT FOR UPDATE)?

**Bối cảnh: nhiều instance.**

```
[Product instance 1] ┐
[Product instance 2] ├──► [ 1 Product Database ]
[...]                │
[Product instance 10]┘
```

**Câu hỏi thầy hỏi:** mỗi instance có DB riêng được không?
> ❌ **KHÔNG.** Product A lưu ở DB này, Product B lưu DB khác thì dữ liệu **không nhất quán** — "nó bị dở hơi, không phải kiến thức của lập trình". Tất cả instance **bắt buộc** cùng nối vào **một** DB.

**`SELECT FOR UPDATE` có còn work với 10 instance không?**
> ✅ **CÓ, vẫn work tốt.** Vì 10 instance = **10 transaction khác nhau** trên **cùng 1 DB**. Cơ chế lock của DB vẫn đảm bảo transaction này commit xong transaction khác mới đọc được.

**Vậy vấn đề là gì?**
> **CHẬM.** Lock trong database rất chậm khi có **hàng triệu / hàng chục nghìn request đồng thời** (flash sale). Lock trên DB lúc đó **rất rủi ro**.
>
> Nếu chỉ vài nghìn request thì `SELECT FOR UPDATE` **không phải vấn đề**.

### 10.3 — Ý tưởng Distributed Lock

> Thay vì để **database** tự lock, ta nhờ **một thằng thứ ba đứng ngoài** điều phối.
> Mọi instance trước khi vào DB **phải đi hỏi Redis**: *"tao có được phép vào không?"*
> Redis đảm bảo **một thời điểm chỉ một instance** được vào. Các thằng khác **xếp hàng chờ**.

```
[instance 1] ┐
[instance 2] ├──► [ REDIS ]  ──(chỉ 1 thằng được qua)──► [ DATABASE ]
[instance N] ┘     điều phối
```

**Gọi là "distributed" vì việc lock diễn ra trên NHIỀU instance phân tán.**

### 10.4 — Cơ chế: xí chỗ bằng key

```
instance nào đến trước → PUT một key vào Redis   (xí chỗ)
instance đến sau       → hỏi Redis "mày có key này không?"
                          ├── CÓ  → có thằng đang vào DB → CHỜ
                          └── KHÔNG → được phép vào DB
instance đầu xong      → XÓA key  → thằng tiếp theo được vào
```

### 10.5 — Docker Compose

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

### 10.6 — Dependency

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

- `spring-boot-starter-data-redis` — kết nối Redis, các tác vụ insert/update/delete cơ bản.
- `redisson` — hỗ trợ các cơ chế **lock** nâng cao (cái "cờ" mà thầy nói).

### 10.7 — ⚠️ PHẢI SORT ID KHI TẠO KEY

Đây là **bẫy** thầy nhấn mạnh:

```
KHÔNG sort:
  Instance 1 muốn update product [1, 2]  →  key = "product:1,2"
  Instance 2 muốn update product [2, 1]  →  key = "product:2,1"

  → Hai key KHÁC NHAU (chỉ là 2 chuỗi text khác nhau)
  → CẢ HAI đều được phép vào DB
  → ❌ SAI LOGIC — cùng cập nhật product 1 và 2!

CÓ sort:
  Instance 1: [1, 2] → sort → key = "product:1,2"
  Instance 2: [2, 1] → sort → key = "product:1,2"   ← GIỐNG NHAU
  → Chặn được nhau ✅
```

### 10.8 — Code

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

### 10.9 — Hai tham số của `tryLock(waitTime, leaseTime, unit)`

| Tham số | Giá trị | Ý nghĩa |
|---|---|---|
| `waitTime` = **10s** | Thời gian **CHỜ** để lấy lock | *"Tao chờ tối đa 10 giây. Không được thì tao bỏ đi, báo lỗi."* |
| `leaseTime` = **5s** | Thời gian **GIỮ** lock tối đa | *"Cho tao tối đa 5 giây. Xong sớm tao trả sớm. Nhưng sau 5 giây mà tao không trả lời, mày cứ xóa key đi."* |

**Vì sao cần `leaseTime`?**
> Phòng trường hợp instance **lấy được lock rồi CHẾT**. Nó không thể gọi `unlock()` được nữa.
> Nếu không có `leaseTime` → key nằm lại vĩnh viễn → **deadlock**, không ai vào được nữa.
> Có `leaseTime` → Redis tự xóa key sau 5 giây → *"thằng này chắc chết rồi, cho thằng khác vào."*

### 10.10 — Demo: tại sao không thấy key trên Redis Insight

> Đoạn code chạy chỉ vài mili-giây → key được tạo rồi xóa ngay, không kịp nhìn.
> Muốn thấy phải **giả lập chậm**: `Thread.sleep(4000)` bên trong vùng lock.

### 10.11 — Redis vs Select For Update — chọn cái nào?

| | `SELECT FOR UPDATE` | **Redis Distributed Lock** |
|---|---|---|
| Cần công cụ thứ 3 | ❌ Không | ✅ Cần Redis |
| Tốc độ | Chậm khi tải rất cao | ⚡ Nhanh |
| Độ phức tạp code | Thấp | Cao hơn |
| Phù hợp | Vài nghìn request | Hàng chục nghìn / triệu request |

> **Thực tế đi làm:** dùng `SELECT FOR UPDATE` là **đã ok rồi**. Hệ thống chưa chắc nhiều request đến mức phải dùng Redis, mà Redis làm **tăng độ phức tạp code**.
> **Nhưng đi phỏng vấn chắc chắn họ sẽ hỏi.** Phải hiểu và tự cân nhắc được khi nào cần.

### 10.12 — Công dụng khác của Redis

> Redis **rất phổ biến với caching** — "cái đấy ai cũng biết". Distributed lock mới là cái ít người biết.

📌 *Bổ sung — các use case phổ biến*: cache, session store, rate limiting, leaderboard (sorted set), pub/sub, counter.

## 11. Bài tập buổi 8

1. Hoàn thiện **Redis distributed lock** cho hàm lock product.
2. Thêm `redis` + `redis-insight` vào `docker-compose.yml`.
3. Tự research **Outbox Pattern** (và **Saga Pattern** nếu có thời gian).

---

# Buổi 9 — Service Discovery với Eureka

## 1. ⚠️ Lỗ hổng của Redis lock theo danh sách ID

Hải Trương phát hiện đúng vấn đề mà buổi 8 chưa xử lý hết:

```
Request A muốn lock [1, 2, 3]  →  key = "product:1,2,3"
Request B muốn lock [1, 2]     →  key = "product:1,2"

→ HAI KEY KHÁC NHAU
→ CẢ HAI cùng vào DB
→ ❌ Vẫn tranh chấp trên product 1 và 2!
```

> Buổi 8 mới giải quyết trường hợp **hai danh sách giống hệt nhau nhưng khác thứ tự** (`[1,2]` vs `[2,1]`).
> Trường hợp **danh sách lồng nhau / giao nhau một phần** thì sort không cứu được.

### Giải pháp: lock đến TỪNG product

```
Thay vì:  lock "product:1,2,3"           (1 key cho cả list)
Làm:      lock "product:1"
          lock "product:2"               (3 key riêng)
          lock "product:3"
```

> Cách này **giống hệt cơ chế của database** — DB cũng lock đến từng **record** một, không lock cả nhóm.

📌 Redisson có `RedissonMultiLock` gom nhiều lock thành một — thầy nói **chưa từng dùng**, cho cả lớp về research thêm.

## 2. ⚠️ DEADLOCK khi lock từng product

Lock từng cái sinh vấn đề mới. Có hai cách triển khai, **cả hai đều có trade-off**:

### Option A — Lock hết rồi mới save

```
lock P1 → lock P2 → lock P3 → save tất cả → unlock tất cả
```
- ✅ Atomic — hoặc tất cả thành công, hoặc không gì cả
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
> (Ngược với nguyên tắc "gom nhóm I/O" ở buổi 6 — đây là cái giá phải trả cho tính đúng đắn.)

📌 *Bổ sung — cách tránh deadlock chuẩn*: **luôn lock theo thứ tự đã sort**. Nếu mọi request đều lock theo thứ tự tăng dần của ID, sẽ không bao giờ có chuyện A giữ P1 chờ P2 còn B giữ P2 chờ P1 — vì B cũng phải lấy P1 trước. Đây là lý do sort ở buổi 8 quan trọng hơn ta tưởng.

> Thầy: *"Cái vấn đề này cả lớp về research thêm. Nó cũng là một vấn đề nâng cao, không phải dễ đâu."*

## 3. Ôn tập — API Gateway làm gì

| Vai trò | Mô tả |
|---|---|
| **Forwarder / Routing** | Nhận request từ frontend, forward xuống microservice tương ứng |
| **Load Balancer** | Có 10 instance Product → chọn 1 để đẩy request xuống |
| **Xác thực & phân quyền tập trung** | Vì mọi request đều đi qua |
| **Rate limiting** | Giới hạn: 1 user chỉ được gọi API này 5 lần/giây |

> **Root cause khiến Gateway làm được nhiều thứ:** nó là **thằng ở giữa**, mọi request đều đi qua nó.
> Đổi lại, nó cũng chính là **nút thắt cổ chai (bottleneck)** của hệ thống.

## 4. ⚠️ Vấn đề: nhiều instance thì hardcode URL vô nghĩa

### Sự thật nền tảng

> **Hai ứng dụng KHÔNG THỂ chạy trên cùng một port trên cùng một máy.**
> Đó là lý do các bạn hay gặp lỗi `Port 8080 is already in use`.

→ Scale 10 instance Product = 10 port khác nhau: `8888`, `8887`, `8889`, ...

### Vấn đề trong code hiện tại

```java
// Order Service — buổi 5 viết thế này
private static final String PRODUCT_SERVICE_URL = "http://localhost:8888";
```

```
Scale lên 10 instance Product:
   localhost:8888  ← code CHỈ gọi thằng này
   localhost:8887  ← không bao giờ được gọi
   localhost:8889  ← không bao giờ được gọi
   ... 7 thằng nữa ngồi chơi

→ Scale 9 instance kia là VÔ NGHĨA
```

### Và còn tệ hơn

Danh sách instance **thay đổi liên tục**:
- Flash sale → scale từ 10 lên 20 instance → cần gọi cả 10 thằng mới
- Hết flash sale → co xuống 2 → 8 thằng chết, **không được gọi vào thằng đã chết**

> *"Nếu em là order, em có muốn gọi vào một thằng đã chết không?"* — Không.
> → Danh sách phải **real-time**.

### Giải pháp thủ công đều không ổn

| Cách | Vì sao không ổn |
|---|---|
| Lưu mảng IP cứng trong code | Không real-time, instance chết vẫn gọi |
| Chỉ định 1 instance "main" luôn sống | **Không ai đảm bảo được** một instance không chết (OOM, cao tải). Xác suất chết của cả 10 instance là như nhau |
| Mỗi service tự lưu IP main của mọi service khác | Có 5 service thì mỗi thằng phải duy trì 5 danh sách + tự đi kiểm tra sống chết → **quá phiền** |

## 5. Service Registry & Service Discovery

### Ẩn dụ "cuốn sổ của Hải Trương"

> Thuê một thằng (Hải Trương) **không làm gì cả**, chỉ ngồi giữ một **cuốn sổ**.
>
> - Mỗi instance Product khi scale lên → **báo cáo**: *"Tao là Product, IP của tao là X, port Y, tao đang sống."*
> - Hải Trương ghi vào sổ.
> - Order muốn gọi Product → **hỏi Hải Trương**, không hỏi Product.
> - Hải Trương mở sổ: *"Tao đang có 10 thằng Product sống, đây 10 địa chỉ IP, mày chọn một."*

### Heartbeat — làm sao biết instance còn sống

Hai cơ chế ngược nhau:

| Cơ chế | Ai chủ động | Công nghệ |
|---|---|---|
| **PUSH** | Service **tự báo cáo** định kỳ (vd 5 giây/lần): *"tao còn sống"* | **Eureka** |
| **PULL** | Registry **đi hỏi thăm** định kỳ: *"mày còn sống không?"* | **Kubernetes** |

> Quá 5 giây không thấy báo cáo → Hải Trương coi như **thằng đó chết**, loại khỏi danh sách.
> Khi Order hỏi, chỉ đưa những thằng **còn sống**.

### Hai kiến trúc triển khai

```
┌─ KHÔNG dùng Kubernetes ──► cần EUREKA (Server + Client)   ← LỚP HỌC CÁI NÀY
│
└─ Dùng KUBERNETES ────────► K8s TỰ đóng vai trò registry + discovery
                              KHÔNG cần Eureka, không cần khai báo gì
```

> Thầy nói thẳng: *"Kiến trúc này (Eureka) bọn anh không triển khai. Anh chưa từng triển khai ứng dụng nào theo kiến trúc này trên thực tế, bọn anh dùng K8s hết."*
> Nhưng **Eureka phù hợp với level lớp hơn** — học K8s sẽ tốn quá nhiều kiến thức mới, không đủ thời gian.
> Eureka **vẫn có hệ thống thật dùng**, không phải không có.

📌 *Bonus — K8s auto-scaling (HPA)*: cấu hình *"nếu instance nào RAM > 75% hoặc CPU > 80% thì scale thêm 1 instance"*. K8s có một component chuyên đi đo và tự scale. Chỉ là **config**, không phải code.

## 6. Eureka Server vs Eureka Client

| | **Eureka Server** | **Eureka Client** |
|---|---|---|
| Là gì | Một **ứng dụng/hệ thống** riêng | Một **thư viện** |
| Vai trò | Giữ **cuốn sổ cái** — service nào có bao nhiêu instance, IP/port, sống hay chết | Tự động gửi heartbeat lên Server định kỳ |
| Cài ở đâu | Chạy độc lập | Nhúng vào **TẤT CẢ** microservice |

**Client cài ở những đâu?**
- ✅ Product Service
- ✅ Order Service
- ✅ **Cả API Gateway**

> Câu hỏi thầy hỏi Dương: *"Gateway có cần nhiều instance không?"* — Dương trả lời "nó chỉ là một cái cổng thôi".
> **SAI.** Ẩn dụ: xây thành thì xây **một cổng hay nhiều cổng**? → Nhiều cổng.
> Gateway **cũng là một service**, cũng là một ứng dụng đang chạy → **cũng cần scale nhiều instance**.

> Không tự code heartbeat được không? Được — nhưng *"không ai làm chuyện đấy cả, nó hơi bị dở"*. Thư viện đã viết sẵn.

## 7. Code — Eureka Server

### 7.1 — Tạo project

New Project → Spring Boot → Maven → Java 21 → tên `discovery-server`
**Dependency:** `Eureka Server`

### 7.2 — Enable

```java
@SpringBootApplication
@EnableEurekaServer                   // ← bắt buộc
public class DiscoveryServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
```

### 7.3 — Config

```yaml
server:
  port: 8761                          # port MẶC ĐỊNH của Eureka Server

spring:
  application:
    name: discovery-server

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false       # ① KHÔNG tự đăng ký chính nó vào sổ
    fetch-registry: false             # ② KHÔNG tải sổ cái từ chỗ khác
    service-url:
      defaultZone: http://localhost:8761/eureka
```

| Config | Ý nghĩa |
|---|---|
| ① `register-with-eureka: false` | Server không tự ghi tên mình vào cuốn sổ của chính nó |
| ② `fetch-registry: false` | Server không đi tải sổ cái từ nơi khác — **các thằng khác đăng ký VÀO nó** |
| `defaultZone` | Đường dẫn các service khác dùng để đăng ký. Bản chất là `http://localhost:8761/eureka` |

### 7.4 — Giao diện

Chạy lên → truy cập **`http://localhost:8761`** → thấy dashboard liệt kê các service đã đăng ký.

## 8. Code — Eureka Client (Product & Order Service)

### 8.1 — Dependency (⚠️ cần đủ 3 phần)

```xml
<!-- ① Property version -->
<properties>
    <spring-cloud.version>2024.0.0</spring-cloud.version>
</properties>

<!-- ② Dependency -->
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>

<!-- ③ BOM — BẮT BUỘC, thiếu là không ăn version -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

> ⚠️ Eureka Client thuộc bộ **Spring Cloud** (khác Spring Boot) → phải khai `dependencyManagement` thì nó mới tự ăn được version.

### 8.2 — Config

```yaml
spring:
  application:
    name: product-service           # ⚠️ TÊN NÀY chính là địa chỉ để gọi

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

Tương tự cho Order Service với `name: order-service`.

### 8.3 — Chạy nhiều instance trong IntelliJ

```
Run → Edit Configurations → chọn config hiện tại → Copy Configuration
→ Đặt tên mới → thêm VM/Program option: server.port=8887
→ Apply
```

Lặp lại để có `8887`, `8888`, `8889` — **cùng một ứng dụng, ba port khác nhau**.

Chạy cả 3 → vào `localhost:8761` sẽ thấy `PRODUCT-SERVICE` có **3 instance**, cả 3 trạng thái **UP**.

## 9. ⚠️ Đổi hardcode URL → tên service

Đây là bước quan trọng nhất buổi.

```java
// ❌ TRƯỚC
private static final String PRODUCT_SERVICE_URL = "http://localhost:8888";

// ✅ SAU — dùng TÊN SERVICE (spring.application.name)
private static final String PRODUCT_SERVICE_URL = "http://product-service";
```

**Chuyện gì xảy ra bên dưới:**
```
Order gọi "http://product-service"
  → hỏi Eureka: "product-service có những instance nào đang sống?"
  → Eureka trả về [8887, 8888, 8889]
  → LoadBalancer chọn 1 trong 3
  → gọi thật vào IP:port đó
```

### Bắt buộc: `@LoadBalanced`

```java
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced                     // ⚠️ THIẾU LÀ KHÔNG CHẠY
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```

`@LoadBalanced` là annotation của **Spring Cloud** — nó dạy cho `WebClient` biết cách hỏi Eureka và phân tải.

### ⚠️ LỖI PHỔ BIẾN NHẤT BUỔI HỌC

Nhiều bạn bị `Connect timeout`. Nguyên nhân: **tạo `WebClient` mới** thay vì dùng bean đã có `@LoadBalanced`.

```java
// ❌ SAI — tạo builder mới, KHÔNG có @LoadBalanced
WebClient client = WebClient.builder().build();

// ✅ ĐÚNG — inject bean đã được đánh dấu @LoadBalanced
private final WebClient.Builder webClientBuilder;
...
webClientBuilder.build().post()...
```

> Thầy: *"Code của em đang sử dụng một cái WebClient không liên quan."*

## 10. Round Robin vs Random

| | Round Robin | Random |
|---|---|---|
| Cách chọn | **Lần lượt**: 1 → 2 → 3 → 1 → 2 → 3 | Ngẫu nhiên mỗi lần |
| Gọi 3 lần | Chắc chắn mỗi instance 1 lần | Có thể trúng 1 thằng cả 3 lần |
| Đồng đều | Luôn đồng đều | Chỉ đều khi **số mẫu đủ lớn** (như tung xúc xắc) |

> **Mặc định của Spring Cloud LoadBalancer là Round Robin.**

Demo trên lớp: gọi 5 request từ Order → log cho thấy lần lượt `8889` → `8889` → `8888` → `8887` → `8888`, tức đã **luân phiên** qua các instance.

## 11. Lỗi thực tế: `UnknownHostException` / `Connect timeout`

Một bạn dùng Windows bị: Eureka đăng ký instance bằng **hostname máy** (vd `admin-pc.net`) thay vì IP → máy khác (và chính nó) không resolve được → timeout.

**Fix:** ép Eureka dùng địa chỉ IP:

```yaml
eureka:
  instance:
    prefer-ip-address: true       # ← đăng ký bằng IP thay vì hostname
```

📌 Sau khi sửa phải **stop toàn bộ instance rồi restart**. Eureka **không real-time tuyệt đối** — instance đã chết vẫn còn hiển thị trong sổ một lúc (chờ hết chu kỳ heartbeat) rồi mới chuyển sang `unavailable`.

## 12. Kiến trúc sau buổi 9

```
                    ┌──────────────────────┐
                    │  Discovery Server    │  ← Eureka Server (cuốn sổ cái)
                    │  localhost:8761      │
                    └──────────┬───────────┘
                          ▲    │ trả về danh sách instance sống
             đăng ký +    │    │
             heartbeat 5s │    ▼
        ┌────────────────┴─────────────────────┐
        │                                      │
┌───────┴────────┐                    ┌────────┴─────────┐
│ Order Service  │  ──gọi qua TÊN──►  │ Product Service  │
│  :8080, :8181  │  "product-service" │ :8887,:8888,:8889│
└────────────────┘   + @LoadBalanced  └──────────────────┘
```

## 13. Bài tập buổi 9

1. Dựng **Eureka Server** (`discovery-server`, port `8761`).
2. Thêm **Eureka Client** vào Product + Order Service.
3. Chạy **nhiều instance** mỗi service, verify trên dashboard `localhost:8761`.
4. Đổi hardcode URL → **tên service** + thêm `@LoadBalanced`.
5. **Tự tìm hiểu trước Keycloak** — thầy gửi keyword trong chat.

> **Buổi sau:** **API Gateway** + **Auth Service tích hợp Keycloak**.

---

# Buổi 10 — API Gateway & Keycloak

> ⚠️ *Buổi này khôi phục từ bản ghi video. Phần lớn thời lượng là thực hành im lặng nên bản ghi chỉ bắt được các đoạn thầy giảng.*

## 1. API Gateway — Spring Cloud Gateway

Nhắc lại vai trò (đã học buổi 1 và 9):
- **Cửa ngõ** — mọi request từ frontend đều đi qua Gateway
- **Forward** request vào các service bên trong
- **Xác thực & phân quyền** tập trung tại Gateway

### Ranh giới trong/ngoài

```
        NGOÀI (Internet)          │          TRONG (network nội bộ)
                                  │
  [Frontend] ──Access Token──► [API GATEWAY] ──► [Product] [Order] [Auth]
                                  │              ← các service chỉ nói chuyện nội bộ
```

> Từ Gateway **trở vào** là các service nội bộ. Từ Gateway **trở ra** mới là giao tiếp với bên ngoài.

### Dependency — chỗ dễ sai

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

> ⚠️ Một bạn thêm nhầm `spring-cloud-starter-gateway-server-webflux` → thầy sửa: *"xóa chữ server đi"*.

📌 Gateway cũng cần **Eureka Client** (buổi 9) để route theo tên service thay vì hardcode IP.

## 2. ⚠️ Basic Auth vs Access Token

Phần thầy phân tích kỹ nhất buổi.

### Basic Auth có vấn đề gì

```
MỌI request đều phải truyền username + password:
   tạo đơn hàng   → truyền username/password
   lấy sản phẩm   → truyền username/password
   làm bất cứ gì  → truyền username/password

→ Xác suất bị LỘ rất cao
```

Tệ hơn: username/password là thứ người dùng **đọc được, nhớ được**, và họ có xu hướng đặt **ngắn, dễ nhớ**.

### Access Token giải quyết thế nào

> Thay vì truyền username/password, sinh ra một **chuỗi rất dài** đại diện cho username/password đó.

Nhưng thầy nói thẳng — điều này **chưa đủ**:

> *"Nó vẫn có thể bị ăn cắp. Vì tất cả request đều truyền cái đấy mà. Chỉ là nó khó đọc hơn thôi."*

### 🎯 Điểm khác biệt QUYẾT ĐỊNH: HẾT HẠN

| | Basic Auth | Access Token |
|---|---|---|
| Truyền ở mọi request | ✅ | ✅ |
| Có thể bị đánh cắp | ✅ | ✅ |
| Dễ đọc / dễ nhớ | ✅ (ngắn) | ❌ (chuỗi dài) |
| **Thời hạn sử dụng** | **Vĩnh viễn** — không đổi password thì dùng **đến cuối đời** | **Hết hạn** (vd 5 phút) |

> *"Nếu Hải Trương đánh cắp được Access Token của anh thì Hải Trương chỉ xài được trong vòng 5 phút thôi. Sau 5 phút hết hạn."*
>
> Còn Basic Auth bị đánh cắp mà mình không biết → nó dùng **mãi mãi**.

> **Tất cả website hiện tại đều dùng Access Token** — vì nó giảm thiểu rất nhiều rủi ro so với Basic Auth.

## 3. ⚠️ Xác thực GIỮA các Microservice — 3 options

Câu hỏi: Gateway → service, và service → service, có cần xác thực không?

> *"Có 3 options. **Không có đúng với sai.** Mình chỉ phân tích pros and cons thôi."*

| Option | Ưu | Nhược |
|---|---|---|
| **① Không xác thực gì** | Nhanh nhất, đơn giản nhất | Không có bảo mật nội bộ; **không truy vết được ai làm gì** |
| **② Basic Auth** | Đơn giản | **Quá nhiều người biết** username/password → audit vô nghĩa |
| **③ Access Token** | Bảo mật nhất, truy vết được | **Tốn CPU** giải mã; **public key có thể thay đổi** → phải đi lấy lại, tốn thời gian |

### Lý lẽ cho option ① và ②

> Từ bên ngoài vào **bắt buộc phải qua API Gateway**, mà Gateway đã yêu cầu Access Token rồi.
> Các service bên trong nằm trong **network nội bộ** — từ ngoài không tấn công vào được.

### ⚠️ Nhưng vấn đề thật nằm ở AUDIT

> *"Nếu một tháng trước Hải Trương cập nhật trường dữ liệu đó qua API, bây giờ anh **không truy vết được**.
> Vì nó chỉ ghi lại là `anonymous` thì làm sao biết ai làm việc đấy?"*
>
> *"**Bọn anh đang gặp đúng tình trạng này** — vì quá nhiều người biết username/password."*
>
> *"Câu chuyện gì đến cũng sẽ đến. Không biết là ai tác động luôn."*

Nhưng đổi hết sang Access Token cũng có giá:
- Tốn **CPU** chạy thuật toán giải mã
- **Public key không cố định** — khi nó đổi, phải đi lấy lại → tốn thời gian

📌 *Bổ sung*: cặp khóa trong JWT — **private key** ký token (chỉ Auth Server giữ), **public key** verify chữ ký (ai cũng lấy được). Keycloak expose public key qua endpoint JWKS; client cache lại và refresh khi key rotate.

### 📌 Chốt cho lớp

```
[Frontend] ──── Access Token (BẮT BUỘC) ────► [API Gateway]
                                                    │
                                              KHÔNG cần gì
                                                    ▼
                                    [Product] [Order] [Auth]
```

> Giữa Frontend ↔ Gateway: **Access Token**.
> Từ Gateway trở vào trong: **thả** — không cần Basic Auth cũng không cần Access Token.

## 4. Truyền thông tin user qua Header

Câu hỏi hay: *nếu Gateway giải mã token rồi, khi gọi xuống service thì có gửi kèm gì không?*

**Vấn đề:** Order Service vẫn cần biết **ai tạo đơn hàng** để điền vào các trường **auditing** (`created_by` — xem buổi 4).
Nhưng Gateway → Order không truyền token, vậy Order làm sao biết?

**Giải pháp:**

```
① Gateway nhận request kèm Access Token
② Gateway GIẢI MÃ token → biết user là "hai.truong"
③ Gateway GẮN thông tin user vào HEADER
④ Gọi xuống Order Service
⑤ Order Service ĐỌC header → biết ai tạo đơn hàng → điền created_by
```

📌 Thường dùng các header như `X-User-Id`, `X-Username`, `X-Roles`. Service bên trong **tin** các header này vì chúng đến từ network nội bộ (khớp với ghi chú ở buổi 1). Điều kiện an toàn: Gateway phải **strip** các header đó nếu client cố tình tự gửi lên.

> Thầy nói đây là **kiến thức nâng cao**, chỉ giải thích vì có người hỏi, không nằm trong nội dung định dạy.

## 5. Keycloak

### Là gì

> Một **open source rất nổi tiếng** chuyên lo xác thực & phân quyền.
> Được dùng trong **hầu hết các hệ thống lớn ở Việt Nam và trên thế giới** — VNPT, Viettel, và cả công ty thầy.

### Vai trò — 3 việc

| # | Vai trò |
|---|---|
| ① | **Quản lý user** hộ mình |
| ② | **Cấp Access Token** khi user login |
| ③ | **Verify** token có hợp lệ không |

### Vì sao không tự code?

> Tự code thì phải tự làm hết: bảng quản lý user, bảng password, bảng role, bảng access token...
> *"Tóm lại tất cả những gì mình làm sẽ **không tốt bằng** cái open source này."*

*(So với học phần Java Advance — hồi đó phải tự code Basic Auth, tự encrypt password để lưu DB.)*

### Docker Compose

```yaml
services:
  keycloak:
    container_name: keycloak
    image: quay.io/keycloak/keycloak:latest
    command: start-dev
    environment:
      - KEYCLOAK_ADMIN=admin
      - KEYCLOAK_ADMIN_PASSWORD=password
    ports:
      - "8080:8080"
    networks:
      - optimize-network
```

| | |
|---|---|
| Tài khoản mặc định | `admin` |
| Password mặc định | `password` |
| Port | **`8080`** |
| Truy cập | `http://localhost:8080` |

⚠️ Port `8080` **trùng với Order Service** (buổi 5). Cần đổi port của một trong hai.

📌 Khi chạy lại: `docker compose down` rồi `docker compose up`.

## 6. 🎁 Bonus: Outbox Pattern → CDC (Change Data Capture)

Hải Trương hỏi tiếp về Outbox Pattern (bài về nhà buổi 8): *"phải có thêm một thằng thứ ba đọc được thay đổi dưới DB đúng không?"*

### CDC là gì

> Thầy đính chính giữa chừng (ban đầu nói nhầm là ETL):
> **CDC = Change Data Capture.** Công cụ đọc **binlog** của MySQL để bắt được record nào vừa **tạo / sửa / xóa**.

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
> CDC bắt sự kiện sản phẩm được tạo/sửa/xóa → đẩy lên Elasticsearch → search luôn ở đó.

**② Đẩy lên Kafka**
> Bắt được dữ liệu đơn hàng mới → publish lên topic cho service khác consume.
> → Thay thế cho việc code Outbox Pattern thủ công (không cần tự publish, không lo publish fail).

### ⚠️ Thực tế: rất khó áp dụng

> *"Bên anh đã từng **offer giải pháp này** vào ứng dụng rồi. Ở **Viettel** và cả **OneMount** — **cả hai đều bị REJECT**."*

**Hai lý do:**
1. **Không ai chịu maintain công cụ mới.** Nó là open source, đội dev không maintain nổi.
2. **Phải có quyền access vào tận binlog** của database. DBA làm rất chặt, không cho.

> → Cuối cùng chỉ apply được **Outbox Pattern**, vì đội dev **chủ động hoàn toàn**, không phải phụ thuộc DBA.

> 💬 *"Thực tế mình sẽ ít lựa chọn hơn là trên lý thuyết. Trên lý thuyết CDC rất hiệu quả và effort bỏ ra rất ít, nhưng thực tế bọn anh không thể triển khai được."*

---

# Buổi 11 — Keycloak, Mã hóa bất đối xứng & Cấu trúc JWT

> ⚠️ *Khôi phục từ bản ghi video.*
> 💬 Thầy mở đầu: *"Bài hôm nay khá hay — nhưng hơi ít người biết."*

## 1. Các khái niệm trong Keycloak

*(Bài về nhà buổi 10: tự tìm hiểu Keycloak.)*

| Khái niệm | Là gì |
|---|---|
| **Realm** | "Vương quốc" — nơi người dùng đến **đăng nhập**. Chứa endpoint cấp token và **public key** để verify |
| **Client** | Đại diện cho một **hệ thống/ứng dụng** đăng ký với Keycloak |
| **User** | Đại diện cho **người dùng** |
| **Grant types** | Các giao thức OAuth2 mà client hỗ trợ để lấy access token |

### ⚠️ User vs Client — phân biệt quan trọng

| | Đại diện cho | Định danh | Bí mật |
|---|---|---|---|
| **User** | **Người dùng** | `username` | `password` |
| **Client** | **Hệ thống** | `client_id` | `client_secret` |

> `client_id` / `client_secret` **tương đương** với `username` / `password` — chỉ khác là dành cho **hệ thống** thay vì con người.

Tìm ở tab **Credentials** của client trong Keycloak admin console.

## 2. Mã hóa đối xứng vs bất đối xứng

### Đối xứng (Symmetric)

> Mã hóa và giải mã dùng **CHUNG MỘT KEY** — cùng một quy tắc cho cả hai chiều.

Ví dụ thầy đưa: `ABC` → mã hóa thành mã số `65, 66, 67` → giải mã ngược lại bằng đúng quy tắc đó.

📌 *Đính chính*: thầy lấy **Base64** làm ví dụ đối xứng. Thực ra **Base64 không phải mã hóa** — nó là **encoding** (mã hóa dạng biểu diễn), ai cũng decode được, không có key nào cả. Đi phỏng vấn cần nói đúng: ví dụ mã hóa đối xứng thật là **AES**, **DES**, **3DES**. Ý thầy muốn minh họa là "cùng một quy tắc cho cả hai chiều" — điều đó thì đúng.

### Bất đối xứng (Asymmetric)

> Mã hóa dùng **một key**, giải mã dùng **key KHÁC**.

```
[private key] ──ký/mã hóa──►  dữ liệu  ◄──verify/giải mã── [public key]
   giữ BÍ MẬT                                                 CÔNG KHAI
```

| Key | Ai giữ | Dùng để |
|---|---|---|
| **Private key** | **Chỉ mình giữ** (Auth Server / Keycloak) | **Ký** token |
| **Public key** | **Công khai** cho các bên khác lấy về | **Verify** chữ ký |

> 💬 Thầy: *"Người nghĩ ra mã hóa bất đối xứng khá là thông minh. Nó thay đổi mọi thứ trong security."*

📌 *Bổ sung — vì sao đây là bước ngoặt*: với mã hóa đối xứng, muốn hai bên nói chuyện an toàn thì phải **truyền key cho nhau trước** — mà truyền qua mạng thì có thể bị chặn. Bất đối xứng giải quyết đúng vấn đề đó: public key **cứ để lộ thoải mái**, chỉ private key cần giữ kín. Đây là nền tảng của HTTPS/TLS, chữ ký số, và JWT. Thuật toán phổ biến: **RSA**, **ECDSA**.

## 3. ⚠️ Cấu trúc JWT (Access Token)

Access Token là một chuỗi gồm **3 phần** ngăn cách bởi **dấu chấm**:

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9  .  eyJzdWIiOiJhZG1pbiIsImV4cCI6MTcwNX0  .  SflKxwRJSMeKKF2QT4f...
└──────────── HEADER ────────────────┘   └──────────── PAYLOAD ────────────┘   └──── SIGNATURE ────┘
```

| Phần | Chứa gì |
|---|---|
| **① Header** | **Thuật toán** dùng để ký (vd `RS256`) và loại token |
| **② Payload** | **Nội dung** — thông tin user (`sub`, `exp`, roles...) |
| **③ Signature** | **Chữ ký** — phần "ăn tiền" |

### ⚠️ Payload KHÔNG bí mật

Thầy demo trên lớp: paste token vào công cụ decode → **đọc được ngay** thông tin user.

> Token **không phải** thông tin cần che giấu nội dung. Ai cũng decode được payload.
> Cái được bảo vệ là **tính toàn vẹn** — không ai **sửa** được nội dung mà không bị phát hiện.

📌 Hệ quả thực tế: **không bao giờ để dữ liệu nhạy cảm** (password, số thẻ, CCCD) vào payload của JWT.

### Vì sao Header phải chứa thuật toán

> Sinh token bằng thuật toán nào thì lúc verify **phải dùng đúng thuật toán đó**.
> Có rất nhiều thuật toán bất đối xứng khác nhau → phải đính thông tin này vào token để bên verify biết đường mà làm.

### 🎯 Signature được sinh ra thế nào — 4 thành phần

```
SIGNATURE = f( header , payload , thuật toán , PRIVATE KEY )
```

> *"Nó được cấu thành bởi **bốn** thứ. Một là header, hai là payload, ba là thuật toán, bốn là private key.
> **Thiếu một trong bốn thì không ra được** chuỗi signature."*

### Vì sao không giả mạo được

```
Hacker sửa payload (vd đổi role thành "admin")
   → header + payload đã KHÁC
   → signature tính lại sẽ KHÁC
   → nhưng hacker KHÔNG CÓ private key để ký lại
   → server verify bằng public key → PHÁT HIỆN NGAY ❌
```

> Nếu header **hoặc** payload bị thay đổi → signature **chắc chắn** thay đổi.
> Public key verify signature sẽ phát hiện ra.

> 💬 *"Đây là phần lý thuyết rất sâu. Bạn nào hiểu được càng tốt, bởi vì **đi phỏng vấn họ vẫn sẽ hỏi những câu như này** để xem kiến thức sâu của ứng viên tới đâu. Không trả lời được cũng không sao vì kiến thức này không dễ."*

## 4. ⚠️ Vì sao cần Auth Service đứng giữa

Vấn đề đặt ra cuối buổi:

```
❌ SAI:  [Frontend] ──client_id + client_secret──► [Keycloak]
```

> `client_secret` là **password của hệ thống**. Không thể để frontend giữ và truyền lên —
> code frontend ai cũng xem được, secret sẽ **lộ ngay**.

**Nhưng vẫn cần lấy access token.** Giải pháp:

```
✅ ĐÚNG:
[Frontend] ──username/password──► [Auth Service] ──client_id+secret──► [Keycloak]
                                   (giữ secret ở            │
                                    phía server)            ▼
[Frontend] ◄───── access token ──── [Auth Service] ◄─── access token
```

> **Người dùng / frontend KHÔNG được phép gọi trực tiếp vào Keycloak.**
> Phải qua Auth Service của mình — nơi `client_secret` được giữ an toàn ở phía server.

Đây chính là lý do tồn tại của **Auth Service** trong kiến trúc 4 thành phần.

## 5. Định hướng tiếp theo

> Sẽ tích hợp Keycloak vào kiến trúc: **xác thực & phân quyền tập trung tại tầng API Gateway** (đúng như đã chốt ở buổi 1 và buổi 10).

---

# Buổi 12 — JWKS & Đề bài Project cuối khóa

> ⚠️ *Khôi phục từ bản ghi video. Buổi này **phần lớn là thực hành im lặng** — cả lớp tự code tích hợp Keycloak. Bản ghi chỉ bắt được hai phần thầy giảng: JWKS và đề bài project.*

## 1. JWKS — JSON Web Key Set

Nối tiếp buổi 11: backend cần **public key** để verify token. Lấy ở đâu?

> **Key Set = một BỘ các public key.** Keycloak expose sẵn một endpoint chứa bộ key này.

```
http://localhost:8080/realms/<tên-realm>/protocol/openid-connect/certs
```

Mở URL đó ra sẽ thấy danh sách các key:

```json
{
  "keys": [
    { "kid": "abc...", "alg": "RS256",    "kty": "RSA", "use": "sig", "n": "...", "e": "AQAB" },
    { "kid": "xyz...", "alg": "RSA-OAEP", "kty": "RSA", "use": "enc", "n": "...", "e": "AQAB" }
  ]
}
```

**Điểm cần nhớ:**

| | |
|---|---|
| Nội dung | Chỉ là **public key** — công khai, không có gì bí mật |
| Mỗi thuật toán một bộ key | `RS256` (dùng để **ký/verify**) và `RSA-OAEP` (dùng để **mã hóa**) là hai bộ khác nhau |
| Mục đích | **Backend phải lấy được bộ key này thì mới verify được token có hợp lệ hay không** |

📌 Đây chính là thứ thầy nhắc ở buổi 10 khi nói *"public key không cố định, có thể bị thay đổi → phải đi lấy lại"*. Keycloak **rotate key** định kỳ; Spring Security tự fetch lại JWKS và cache theo `kid` trong header của token.

📌 Trong Spring chỉ cần một dòng config, không phải tự gọi:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://localhost:8080/realms/<realm>/protocol/openid-connect/certs
```

---

# 📋 Đề bài Project cuối khóa

> *(Thầy giao ở buổi 12, review dần ở buổi 13-14, bảo vệ ở buổi thi.)*

## 1. Technical Stack — công nghệ nên dùng

| Công nghệ | Ghi chú |
|---|---|
| **Kafka** | Message broker |
| ~~RabbitMQ~~ | ❌ **Không cần** — tương tự Kafka, đã có Kafka rồi |
| **Redis** | Distributed lock + cache |
| **Spring Cloud** | Gateway, Eureka, LoadBalancer |
| **Keycloak** | Xác thực & phân quyền |
| **ELK** | Logging tập trung |

> Tất cả đều đã học trong học phần — không có công nghệ mới.

## 2. ⚠️ HAI PAIN POINT bắt buộc phải xử lý

Đây là phần cốt lõi của đề bài. Hệ thống thương mại điện tử phải giải quyết được **2 bài toán**:

### ① Đáp ứng lượng REQUEST ĐỒNG THỜI lớn

> Ngày **Flash Sale** có **rất rất nhiều request đồng thời**. Hệ thống phải xử lý được.

📌 *Các kiến thức đã học áp dụng được*:
- **Scale out** nhiều instance + **Eureka** + `@LoadBalanced` (buổi 1, 9)
- **Async qua Kafka** thay vì sync — Kafka làm buffer (buổi 4, 6)
- **Race condition**: `SELECT FOR UPDATE` hoặc **Redis distributed lock** (buổi 7, 8)
- **Caching** giảm tải DB (buổi 14)
- Tránh **I/O trong vòng lặp**, gom batch (buổi 6)

### ② Gửi NOTIFICATION cho HÀNG TRIỆU USER

> Đúng 8h sáng Black Friday, gửi thông báo khuyến mãi cho **tất cả user** trong hệ thống.
> *"Bọn em dùng Shopee thì có nhận được thông báo từ Shopee không? Mình có **hàng triệu user** như thế, chứ không phải một user."*

📌 *Hướng giải quyết*:
- **Không thể** gửi tuần tự trong một vòng lặp — sẽ mất hàng giờ và timeout
- Đẩy job vào **Kafka**, chia **nhiều partition**, nhiều **consumer** trong cùng group xử lý **song song** (buổi 4, 6)
- Notification Service tách riêng (buổi 13 — thầy nhấn mạnh **không gộp vào User Service**)
- Thiết kế có **channel** (email / SMS / push) và **retry** cho các bản ghi gửi lỗi
- Xử lý theo **batch**, không phải từng user một

## 3. Sản phẩm phải nộp

| # | Yêu cầu |
|---|---|
| 1 | **Vẽ System Design** — chi tiết, thể hiện đủ **mọi công nghệ** đang dùng (Keycloak, Kafka, Redis, Eureka, ELK, Gateway) |
| 2 | **Vẽ kiến trúc TRIỂN KHAI** (deployment architecture) |
| 3 | **Thiết kế cơ sở dữ liệu cho TỪNG microservice** |
| 4 | **Liệt kê tất cả luồng nghiệp vụ** (đặt hàng, đăng ký, login...) |
| 5 | **Code** các luồng |
| 6 | **Push lên Git** — tạo group, để **public**, gửi link vào group chat lớp |

## 4. Yêu cầu về chất lượng

> - Vẽ bằng **draw.io**, dùng **icon** cho đẹp — *"draw.io nó có hết đấy, vẽ đẹp cho anh một tí"*
> - **Đừng làm sơ sài** — *"cả lớp đang làm sơ sài đến mức không thể sơ sài hơn"*
> - Coi như **dự án cá nhân mang đi phỏng vấn**
> - Thiết kế DB phải **professional** — tham khảo nguồn trên mạng
> - **Tự do sáng tạo**, không có khuôn khổ cố định

---

# Buổi 13 — Centralized Logging với ELK Stack

## 1. Vì sao Logging quan trọng

### Log là gì
> Log là các dòng thông tin ứng dụng in ra để mình **truy vết xem ứng dụng đã chạy như nào**. Không chỉ khi lỗi — chạy bình thường cũng có log.

### Tình huống thầy đặt ra

> Bạn làm hệ thống thương mại điện tử. Khách hàng gọi điện:
> *"Tối hôm qua lúc 8 giờ, tôi đang thanh toán thì hệ thống bị lỗi."*
>
> Bạn có phải xử lý không?

Dương trả lời *"nếu do lỗi logic mình tạo ra thì phải xử lý"* — thầy sửa:

> **Cho dù lỗi do em hay do ai thì em cũng phải xử lý hết.** Em là người phát triển sản phẩm này. Khách báo lỗi thì không thể để im.

**Vấn đề:** chuyện đã xảy ra hôm qua. Không debug lại được.
→ **Thứ duy nhất còn lại là LOG.**

### Hệ quả

| Câu hỏi | Trả lời |
|---|---|
| Ứng dụng có cần lưu trữ log không? | ✅ **Chắc chắn.** Log quan trọng như dữ liệu, **không thể bị mất** |
| Lưu bao lâu? | Thực tế: 30 ngày / 60 ngày / 6 tháng / 1 năm. Cũ hơn thì xóa vì **log rất nhiều** |
| Có cần lưu log của TẤT CẢ microservice? | ✅ Có |

→ Log của mọi service phải **đổ về một chỗ tập trung** để lưu trữ và tra cứu.

## 2. ELK Stack

**ELK** = viết tắt của 3 công cụ liên quan nhau. Đây là **từ khóa xuất hiện nhiều trong JD tuyển dụng**.

| Chữ | Công cụ | Vai trò | Tương đương |
|---|---|---|---|
| **E** | **Elasticsearch** | "Cơ sở dữ liệu" chứa log | ≈ **MySQL** |
| **L** | **Logstash** | Đứng giữa — nhận log, filter/biến đổi rồi đẩy vào Elasticsearch | — |
| **K** | **Kibana** | Giao diện đồ họa kết nối Elasticsearch để xem log trực quan | ≈ **MySQL Workbench** |

### Vì sao cần Logstash ở giữa?

> Đẩy log trực tiếp về Elasticsearch **đôi khi không ổn** — vì mình vẫn cần **filter** log hoặc **biến đổi** log sang dạng khác.
> Logstash xử lý nhanh hơn và cho phép transform dữ liệu.

```
[Order Service]   ─┐
[Product Service] ─┼──► [ Logstash ] ──► [ Elasticsearch ] ◄── [ Kibana ]
[Auth Service]    ─┤      (nhận,           (lưu trữ)            (xem)
[API Gateway]     ─┘       transform)
```

### Ánh xạ khái niệm Elasticsearch ↔ MySQL

| MySQL | Elasticsearch |
|---|---|
| **Table** | **Index** |
| **Record** | **Document** (doc) |

📌 *Bổ sung*: Elasticsearch không chỉ để chứa log — nó là **search engine** (full-text search, inverted index). Trong e-commerce nó thường được dùng làm Search Service. Ở đây ta chỉ dùng khía cạnh lưu trữ + truy vấn log.

## 3. ⚠️ Ai dựng ELK?

> Khi đi làm, **Elasticsearch, Logstash và Kibana do đội hạ tầng (DevOps) dựng**, không phải nhiệm vụ của dev.
> Dù họ dựng bằng container hay máy vật lý thì cũng là việc của họ.
>
> **Nhiệm vụ của mình:** làm sao **đẩy được log từ ứng dụng vào Logstash**, rồi vào Kibana xem.

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

**Các port cần nhớ:**

| Service | Port | Ghi chú |
|---|---|---|
| Elasticsearch | `9200`, `9300` | |
| Logstash | `5044` (mặc định), **`5000` TCP/UDP** | ứng dụng đẩy log vào 5000 |
| **Kibana** | **`5601`** | vào bằng trình duyệt |

> Ba service này cần chung một **network** để giao tiếp với nhau.
> Logstash chạy mặc định ở `5044` nhưng hoàn toàn có thể chìa thêm port khác (TCP/UDP) tùy nhu cầu.

⚠️ **ELK ngốn RAM.** Máy yếu thì Elasticsearch **không start lên được**, kéo theo Kibana không kết nối được. Start cũng khá lâu, cần kiên nhẫn.

## 5. File `logstash.conf`

Được mount từ máy host vào `/usr/share/logstash/pipeline/logstash.conf` trong container. Logstash đọc file này lúc khởi động.

Gồm **2 phần**:

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
    # Không có app_name → đẩy vào index chung
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
| **input** | Log được nhận vào **qua đâu** — ở đây là TCP port `5000`, dạng JSON |
| **output** | Log được đẩy đi **đâu** — Elasticsearch, vào index nào |

**Quy tắc đặt tên index:**
```
order-service-log-2026.01.15
product-service-log-2026.01.15
```
→ Mỗi service một index, **mỗi ngày một index riêng**.

📌 *Vì sao tách index theo ngày*: dễ xóa log cũ (chỉ cần drop index của ngày cũ), dễ áp policy retention, query nhanh hơn vì không phải quét toàn bộ lịch sử.

## 6. Code Spring Boot

### 6.1 — Dependency

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### 6.2 — File `logback-spring.xml`

Đặt trong `src/main/resources/`.

```xml
<configuration>

    <!-- ① Appender CONSOLE — hiển thị ra màn hình khi chạy local -->
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

> Không config gì thì console có màu sắc mặc định. Config pattern thì nó theo đúng định dạng mình đặt.

**LOGSTASH appender:**
- `destination` — nơi đẩy log tới: `localhost:5000`
- `customFields` — **thêm trường `app_name` vào JSON** → chính là trường mà `logstash.conf` dùng để quyết định index

### 6.3 — Log đẩy lên trông như thế nào

Hiển thị ra console chỉ là **một dòng**, nhưng bản chất đẩy sang Logstash là một **JSON**:

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

## 7. Cách viết log tốt

### Vì sao phải log?

> Ở local em **debug được**. Nhưng triển khai lên **production thì KHÔNG debug được**.
> Đóng vai trò là người phải truy vết *"tại sao nó bị lỗi"* — em cần log ở những điểm nào?

### Ví dụ thầy làm trên luồng Create Order

```java
@Service
@RequiredArgsConstructor
@Slf4j                                    // ← Lombok
public class OrderServiceImpl implements OrderService {

    public Order create(CreateOrderRequest request) {

        log.info("Receive new request creating order: {}", request.toString());

        // ...
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

**Nguyên tắc rút ra:**

| Điểm log | Mục đích |
|---|---|
| Đầu API — log **input** | Biết client gửi gì lên |
| Trước/sau khi gọi service khác | Biết đã gọi được chưa, nhận về gì |
| Sau khi save DB | Xác nhận đã lưu, ID là gì |
| Sau khi publish Kafka | Xác nhận event đã đi |

> ⚠️ Khi log entity vừa save, thầy **chỉ log `getId()`**, không log full object.

📌 *Bổ sung — vì sao chỉ log ID*: object đầy đủ rất dài làm log phình to; và quan trọng hơn — có thể chứa **dữ liệu nhạy cảm** (thông tin khách hàng, địa chỉ). Log là nơi nhiều người đọc được, đừng đưa PII/mật khẩu/token vào.

📌 *Bổ sung — các level log*: `ERROR` (lỗi cần xử lý) > `WARN` (bất thường nhưng chưa lỗi) > `INFO` (mốc nghiệp vụ quan trọng — mức đang dùng) > `DEBUG` > `TRACE`. Production thường để `INFO`, tạm bật `DEBUG` khi cần điều tra.

### Áp dụng cho toàn bộ service

> Apply cả **4 service**: Order Service, Product Service, Auth Service, API Gateway.

## 8. Xem log trên Kibana

### 8.1 — Truy cập
`http://localhost:5601` → **Explore on my own**

### 8.2 — Xem index đã có
```
☰ (ba chấm) → Stack Management → Index Management
```
Thấy các index như `order-service-log-2026.01.15` với số lượng document (vd 55 docs).

### 8.3 — Tạo Index Pattern
```
☰ → Stack Management → Index Patterns → Create index pattern
```

| Field | Giá trị |
|---|---|
| Pattern | `*-service-*` |
| Time field | `@timestamp` |

> Pattern `*-service-*` khớp **mọi index chứa chữ `service`** → gom log của tất cả microservice vào một chỗ xem.
> `order-service-log-...` ✅ `product-service-log-...` ✅

### 8.4 — Xem log
```
☰ → Discover
```

- Chọn index pattern vừa tạo.
- **Chọn field để hiển thị**, nếu không thì rất khó đọc. Thầy chọn: **`level`**, **`app_name`**, **`message`**.
- Phía trên có **dashboard thống kê** số lượng log theo từng mốc thời gian.
- Mặc định hiển thị **15 phút gần nhất** — có thể nới ra 7 ngày, xem log của quá khứ.

> Đây chính là thứ giải quyết bài toán ban đầu: khách báo lỗi hôm qua lúc 8h tối → vào Kibana, lọc theo thời gian, tìm nguyên nhân.

## 9. Review project cuối khóa

### 9.1 — Auth Service vs User Service

Việt Hoàng tách 2 service: Auth (username/email/password) và User (địa chỉ, họ tên).

> Thầy: *"Nó bị cồng kềnh quá, em nên gộp nó thành một thôi. Tách ra maintain nó mệt."*

*(Nhất quán với buổi 1 — hệ thống nhỏ thì gộp Auth + User.)*

### 9.2 — ⚠️ Notification KHÔNG thuộc User Service

Việt Hoàng để Notification trong User Service. Thầy phản ứng mạnh:

> *"Trời ơi, sao Notification lại cho vào User Service? Thiết kế microservice kiểu gì vậy?
> **Hai thằng chẳng liên quan gì với nhau lại gộp thành một. Còn hai thằng liên quan nhau thì lại tách ra.**"*

| | |
|---|---|
| Notification | **Gửi thông báo** |
| User | **Thông tin người dùng** |

→ Hai domain khác nhau hoàn toàn, **không được gộp**.

### 9.3 — System Design phải thể hiện công nghệ

Việt Hoàng vẽ Auth Service nhưng không vẽ Keycloak.

> *"Đừng vẽ chỉ mỗi như này — người ta sẽ không biết em có sử dụng Keycloak hay không.
> **Nhìn vào kiến trúc, người ta phải biết em đang sử dụng những công nghệ gì.**"*

→ Phải vẽ đủ: **Keycloak, Kafka, Redis, Eureka, ELK**, API Gateway...

### 9.4 — Thiết kế Notification cần generic hơn

Thiết kế của bạn chỉ có `title`, `content` — quá đơn giản.

> Một hệ thống Notification thực tế cần thêm **`channel`** — các kênh gửi: **email, SMS, push notification trên app**.
> Về research trên Google/GitHub để có thiết kế generic hơn.

📌 *Bổ sung — bảng gợi ý*:
```sql
notifications        (id, user_id, type, title, content, status, created_at)
notification_channels(id, notification_id, channel, recipient, status, sent_at, retry_count)
notification_templates(id, code, channel, subject_template, body_template)
```

### 9.5 — Yêu cầu cho bài cuối khóa

> *"Cả lớp về vẽ thật chi tiết. Nó giống như một dự án cá nhân của cả lớp mang đi phỏng vấn.
> **Đừng làm sơ sài** — cả lớp đang làm sơ sài đến mức không thể sơ sài hơn."*

**Checklist:**
1. **Vẽ system design chi tiết** — thể hiện đủ mọi công nghệ đang dùng.
2. **Liệt kê tất cả luồng nghiệp vụ** — đặt đơn hàng, tạo user, sign up, login...
3. **Thiết kế DB professional** — tham khảo nguồn trên mạng, không tự bịa cho xong.
4. **Code các luồng.**
5. **Push lên Git** — tạo group, để **public**, gửi link vào group chat lớp.

> Bài này **tự do sáng tạo**, không có khuôn khổ cố định.

## 10. Lịch còn lại

| | |
|---|---|
| Thứ 7 tuần này | **Nghỉ** — để cả lớp tập trung làm project |
| Còn lại | **2 buổi**: 1 buổi học kiến thức + 1 buổi ôn tập |
| Mục tiêu | Buổi sau hoàn thiện **~2/3 project** (hoặc xong hết) để thầy review và góp ý |

## 11. Bài tập buổi 13

1. Dựng **ELK** bằng Docker Compose, vào được Kibana ở `localhost:5601`.
2. Config `logback-spring.xml` + dependency cho **cả 4 service**.
3. Thêm **log** vào ít nhất một luồng nghiệp vụ hoàn chỉnh.
4. Tạo index pattern trên Kibana, xem được log.
5. **Vẽ lại system design chi tiết** (thêm cả ELK vào).
6. Hoàn thiện project cuối khóa.

---

# Buổi 14 — Caching (Caffeine & Redis)

> Buổi kiến thức **cuối cùng** của học phần. Buổi 15 là ôn tập + review project.

## 1. Vì sao cần Caching

### Bài toán

```
API lấy chi tiết sản phẩm:
   JOIN products
      + product_variants
      + categories
      + ... nhiều bảng khác

→ Câu query đã CHẬM sẵn
→ Hàng TRIỆU người dùng cùng xem một sản phẩm
→ Cả triệu request cùng hit database, lặp đi lặp lại CÙNG MỘT phép tính
→ Server và database QUÁ TẢI
```

> Nếu sản phẩm **chưa bị thay đổi** thì kết quả người đầu tiên nhận được **giống hệt** người cuối cùng. Vậy tính lại cả triệu lần để làm gì?

### Cache là gì

> Cache lưu lại một **bản snapshot** của dữ liệu. Thay vì phải vào cơ sở dữ liệu lấy, lần sau lấy thẳng từ chỗ đã lưu.

```
User 1: GET product A1  →  query DB (chậm)  →  LƯU LẠI  →  trả về A1
User 2: GET product A1  →  lấy từ CACHE (nhanh)         →  trả về A1
User N: GET product A1  →  lấy từ CACHE (nhanh)         →  trả về A1
```

### "Chi phí" ở đây là gì

> Chi phí = **thời gian tính toán** hoặc **bộ nhớ**.
> Cache đáng dùng khi việc lấy dữ liệu **tốn chi phí lớn** và **lặp lại nhiều lần**.

📌 Không chỉ cache query DB. Có thể cache:
- Kết quả **gọi sang service khác** (mất ~1 giây mỗi lần)
- **Cả một hàm** — query 300ms + vòng lặp/tính toán 300ms = 600ms → cache cả 600ms đó
- Cache được ở **tầng service, repository hoặc controller**

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

**Kết quả đo trên lớp:**

| Lần gọi | Thời gian |
|---|---|
| Request 1 (query DB) | **305 ms** |
| Request 2 (từ cache) | **7 ms** |

→ Nhanh hơn **hàng chục lần**.

### ⚠️ Nhưng KHÔNG dùng HashMap trong thực tế

| Vấn đề | Giải thích |
|---|---|
| **Giới hạn dung lượng** | Dùng trực tiếp RAM của tiến trình, không kiểm soát được |
| **Coupling chặt** | Logic cache trộn thẳng vào business code |
| **Không share giữa instance** | 10 instance = 10 cache riêng, mỗi thằng tự tính lại |
| **Mất khi restart** | Instance chết là mất sạch cache |
| **Không có TTL, không có eviction** | Dữ liệu outdated vĩnh viễn |

## 3. 💬 Hai thứ khó nhất trong khoa học máy tính

> *"Trong khoa học máy tính có hai thứ khó nhất:*
> 1. **Naming** *— đặt tên sao cho meaningful*
> 2. **Cache invalidation** *— làm mới cache khi dữ liệu thay đổi"*

Đây là câu nói kinh điển của ngành. Vấn đề: nếu product bị update giá từ 300k → 400k thì cache vẫn trả 300k → **dữ liệu không đồng bộ với DB**.

> ⚠️ Vấn đề này là **chung cho MỌI công cụ cache** — HashMap, Caffeine, Redis đều gặp.

## 4. Local Cache vs Global Cache

```
┌── LOCAL CACHE ────────────────────┐   ┌── GLOBAL CACHE (Distributed) ───┐
│                                    │   │                                  │
│  [instance 1] có cache riêng       │   │  [instance 1] ─┐                 │
│  [instance 2] có cache riêng       │   │  [instance 2] ─┼──► [ REDIS ]    │
│  [instance 3] có cache riêng       │   │  [instance 3] ─┘   (dùng chung)  │
│                                    │   │                                  │
│  Công cụ: CAFFEINE / EhCache       │   │  Công cụ: REDIS                  │
└────────────────────────────────────┘   └──────────────────────────────────┘
```

| | **Local Cache** | **Global Cache** |
|---|---|---|
| Lưu ở đâu | Ngay trên máy chạy instance (heap) | Server riêng (Redis) |
| Tốc độ | ⚡ **Rất nhanh** — không cần mở TCP, không qua mạng | Chậm hơn — phải mở kết nối, truyền gói tin qua mạng |
| Share giữa instance | ❌ Không | ✅ Có |
| Mất khi restart | ✅ Mất hết | ❌ Không mất |
| Dung lượng | Bị giới hạn bởi RAM của máy đó | Nhiều hơn — server chuyên dụng |
| Tính lại nhiều lần | ✅ Mỗi instance tự tính | ❌ Chỉ instance đầu tiên tính |
| Chi phí vận hành | Không tốn gì | Phải **maintain thêm một hệ thống**, tốn tiền server |

### Ví dụ minh họa

> 10 instance, 10 request đi vào 10 instance khác nhau:
> - **Global cache**: instance 1 tính toán và cache lên Redis → instance 2→10 chỉ việc lấy về. **Tính 1 lần.**
> - **Local cache**: máy 2 không lấy được cache của máy 1 → **phải tự tính lại**. Tính **10 lần**.

### ❓ Redis cũng qua mạng, khác gì query MySQL?

> - **Redis**: lưu trên **RAM**, dạng **key-value**, tra cứu **O(1)** — sinh ra chỉ để làm việc đó
> - **MySQL**: lưu trên **disk**, dạng **bảng quan hệ**, phải **JOIN**
>
> → Redis nhanh hơn rất nhiều dù cả hai đều phải truyền gói tin qua mạng.

⚠️ Lưu ý địa lý: nếu Redis đặt ở Mỹ mà server ở Việt Nam thì độ trễ mạng rất lớn. Local cache không có vấn đề này.

## 5. Công cụ: Caffeine vs EhCache vs Redis

| Loại | Công cụ | Ghi chú |
|---|---|---|
| **Local** | **Caffeine** ← lớp dùng | Phổ biến nhất, nhanh nhất cho ứng dụng Java |
| **Local** | **EhCache** | Cũng nổi tiếng; có thể tích hợp bên thứ 3 để làm global cache. Ít được prefer hơn Caffeine |
| **Global** | **Redis** ← lớp dùng | Được prefer hơn hẳn EhCache cho global cache |

> Sau này đi làm nghe ai nhắc **"EhCache"** thì hiểu là họ đang dùng nó cho **local cache**.

## 6. Spring Cache Abstraction

> Spring **đã có sẵn cache**, nhưng **chỉ ở tầng interface** — chưa có implementation cụ thể.
> Implementation do thư viện bên dưới (Caffeine / Redis) đảm nhiệm.

```
        Spring Cache (interface)
        @Cacheable  @CacheEvict  CacheManager
                    ▲
        ┌───────────┴───────────┐
   [Caffeine]                [Redis]
   implement                 implement
```

**Hệ quả cực kỳ tiện:** đổi từ Caffeine sang Redis chỉ cần **đổi config** — code nghiệp vụ (`@Cacheable`, `@CacheEvict`) **giữ nguyên hoàn toàn**.

## 7. Triển khai — Caffeine (Local Cache)

### 7.1 — Dependency

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 7.2 — Bật cache

```java
@SpringBootApplication
@EnableCaching                       // ← bắt buộc
public class ProductServiceApplication { ... }
```

### 7.3 — Config

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

| Config | Ý nghĩa |
|---|---|
| ① **`expireAfterWrite`** | Sau khi **ghi** vào cache, N giây sau tự **hết hạn và xóa** |
| ② **`maximumSize`** | Chỉ giữ tối đa N entry; cũ nhất bị đẩy ra khi đầy |

### 7.4 — ⚠️ TTL (Time To Live) — giải pháp cho dữ liệu outdated

> Thay vì cache **vĩnh viễn** (dữ liệu outdated mãi mãi), ta cho cache **sống 5 phút**.
> Sau 5 phút cache tự xóa → request tiếp theo tính lại và cache mới.
>
> → Dữ liệu **chỉ có thể outdated tối đa 5 phút**, không phải mãi mãi.

### 7.5 — Vì sao cần `maximumSize`

> DB có 1 triệu record — **không thể** cache cả 1 triệu vì quá nhiều bộ nhớ.
> Giới hạn 100 cái. Cái nào cũ thì bị xóa bớt.

📌 *Bổ sung — thuật toán eviction*: Caffeine dùng **W-TinyLFU** — kết hợp tần suất truy cập (LFU) và thời gian gần đây (LRU), giữ lại những entry thực sự "nóng". Đây là lý do Caffeine hit-rate cao hơn EhCache.

### 7.6 — `@Cacheable`

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
| `key` | Key trong map đó (dùng SpEL: `#tênThamSố`) |
| `condition` | Chỉ cache khi điều kiện đúng |

**Kết quả demo:**

| Lần gọi | Thời gian |
|---|---|
| 1 | **335 ms** |
| 2 | **14 ms** |
| 3 | **12 ms** |

Đọc log thấy rõ: request 1 báo *"no entry for key"* → xuống DB. Request 2 lấy thẳng từ cache. Sau khi quá 10 giây TTL → lại phải xuống DB.

## 8. ⚠️ `@CacheEvict` — Cache Invalidation

Cache chỉ đọc thì chưa đủ — khi dữ liệu **thay đổi** phải xóa cache cũ.

> Hàm `get*` → **lấy** dữ liệu → dùng `@Cacheable`
> Hàm `update*`, `delete*` → **làm thay đổi** dữ liệu → dùng `@CacheEvict`

```java
@CacheEvict(value = "product", allEntries = true)
public Product update(String id, UpdateProductRequest request) {
    // ... update ...
}
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

**Demo trên lớp:**
```
① search product  → 284 ms  (query DB)
② search product  → 10 ms   (cache)
③ search product  → 6 ms    (cache)
④ update product  → log: "invalidate toàn bộ cache"
⑤ search product  → log: "no entry for key" → QUERY DB LẠI ✅
⑥ search product  → nhanh trở lại (cache mới)
```

### ⚠️ Anti-pattern: cache trên API listing

> Cache ở API **listing/search** **không phải best practice — nó là anti-pattern**, nên tránh.
> Nên cache ở API **`getById`**.

📌 *Vì sao*: list có vô số tổ hợp filter/sort/paging → mỗi tổ hợp một cache entry → hit rate thấp, tốn bộ nhớ, và chỉ cần một bản ghi đổi là phải xóa sạch.

📌 *Bổ sung — các annotation còn lại*:
| Annotation | Dùng khi |
|---|---|
| `@Cacheable` | Đọc — có cache thì trả cache, không thì chạy hàm rồi cache lại |
| `@CacheEvict` | Xóa cache |
| `@CachePut` | **Luôn chạy hàm** rồi **ghi đè** cache — hợp cho `update` khi muốn cập nhật cache thay vì xóa |
| `@Caching` | Gộp nhiều annotation trên cùng một hàm |

## 9. Chuyển Caffeine → Redis (Global Cache)

Đây là chỗ thể hiện sức mạnh của Spring Cache abstraction.

### 9.1 — Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

*(Đã thêm từ buổi 8 khi làm distributed lock.)*

Giờ project có **cả hai** thư viện — Caffeine và Redis. Chỉ cần config chọn dùng cái nào.

### 9.2 — Config

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
```

### 9.3 — Bean với `@Primary`

```java
@Configuration
public class CacheConfig {

    // Bean cũ của Caffeine vẫn giữ nguyên
    @Bean
    public CacheManager cacheManager() { ... }

    // Bean mới của Redis, đánh dấu ƯU TIÊN
    @Bean
    @Primary                                        // ⚠️ điểm mấu chốt
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60));   // TTL, tương đương expireAfterWrite
        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}
```

**`@Primary` là gì?** — câu hỏi thầy hỏi Hải Trương:
> Khi có **hai bean cùng kiểu dữ liệu**, bean nào được đánh `@Primary` thì Spring **ưu tiên chọn** bean đó.

→ Có 2 `CacheManager` (Caffeine + Redis) → Spring lấy cái có `@Primary` = Redis.

### 9.4 — ⚠️ Code nghiệp vụ KHÔNG ĐỔI

> `@Cacheable`, `@CacheEvict` là **interface của Spring**. Chỉ khác implementation bên dưới.
> Đổi từ cache trên RAM sang cache trên Redis — **không sửa một dòng business code nào**.

### 9.5 — Xem cache trên RedisInsight

`http://localhost:8001` → Browser → thấy các key cache.

### 9.6 — 🎯 Demo quyết định: cache share giữa instance

```
Chạy 2 instance Product Service: port 8888 và 8887

① Gọi instance 8888  →  query DB, cache lên Redis
② Gọi instance 8887  →  KHÔNG query DB ✅
```

> *"Hai instance khác nhau hoàn toàn. Thằng 8887 chưa được gọi lần nào hết mà vẫn không phải xuống DB."*

Đây chính là điều **local cache không làm được**.

## 10. 💬 Trade-off khi đi làm

> *"Đi làm không có đúng và sai. Nó là trade-off.
> Đi học thì mình rất bay bổng, nhưng đi làm phải bám với thực tế."*

### Ba câu hỏi phải trả lời theo thứ tự

**① Có CẦN cache không?**
- Chỉ 100–1000 người dùng → API chậm cũng không phải vấn đề
- Dữ liệu ít, query không chậm → **không cần cache**
- Cache thêm vào là thêm **độ phức tạp**

**② Nếu cần — Local hay Global?**
- Có đủ tiền/tài nguyên dựng Redis không?
- Mua server vật lý rất tốn kém. **Nhiều công ty không có Redis** vì không dùng cloud.
- Cloud thì dễ hơn — dùng bao nhiêu trả bấy nhiêu.

**③ Team có sẵn sàng không?**
- **Cache invalidation rất phức tạp**, không hề dễ
- Không phải ai cũng hiểu rõ; team nhiều fresher, dự án gấp → cân nhắc

> *"Có cái gì thì mình xài cái đấy. Phải trade-off: dự án tôi có từng này tiền, từng này tài nguyên thì tôi chấp nhận dùng giải pháp nào."*

## 11. Thi & Project cuối khóa

| | |
|---|---|
| **Buổi 15** | Ôn tập + thầy review project, góp ý cần sửa gì |
| **Yêu cầu** | Hoàn thiện **~100%** project trước buổi 15 |
| **Lịch thi** | **Thứ 7 tuần này** hoặc **Thứ 3 tuần sau** (2 đợt, chọn 1) |
| **Hình thức** | Mang chính project đi **bảo vệ** |

> Làm càng đầy đủ càng tốt — từ đó thầy hướng dẫn nên bổ sung gì để thành một **mini project** mang đi phỏng vấn.

## 12. Bài tập buổi 14

1. Config **Caffeine** cache cho Product Service (`@EnableCaching`, `CacheConfig`, TTL, maximumSize).
2. Thêm `@Cacheable` vào API `getById`, đo thời gian trước/sau.
3. Thêm `@CacheEvict` vào API `update`, verify cache bị xóa.
4. Chuyển sang **Redis** bằng `@Primary` — verify code nghiệp vụ không đổi.
5. Chạy **2 instance**, verify cache được share qua Redis.
6. **Hoàn thiện project cuối khóa 100%.**

---

# Thuật ngữ

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

# Lộ trình dự kiến các buổi sau

> Phần này sẽ được cập nhật sau mỗi buổi.

- [x] **Buổi 1** — Tổng quan Monolithic vs Microservice, Scale, Database per Service, API Gateway, phân tích domain
- [x] **Buổi 2** — Thiết kế DB (category lồng nhau, UUID, soft delete, auditing), dựng **Product Service** bằng Spring Boot
- [x] **Buổi 3** — Thiết kế **Order Service**, pattern Snapshot vs Versioning, không FK xuyên service, code Order Service
- [x] **Buổi 4** — JPA Auditing, thiết kế API đặt hàng (bảo mật giá), Sync vs Async, **kiến thức nền Kafka**
- [x] **Buổi 5** — Thực hành **giao tiếp Sync** bằng **WebClient**: `ProductClient` interface + impl, config port, luồng create order, demo
- [x] **Buổi 6** — Tối ưu Sync (tránh I/O trong loop, Map O(1)), luồng Async, code **Kafka Producer/Consumer**, Consumer Group
- [x] **Buổi 7** — `OrderCreatedEvent`, cập nhật trạng thái đơn, đặt tên topic, **Race Condition + SELECT FOR UPDATE**, **Retry Non-Blocking + DLT**
- [x] **Buổi 8** — Ôn tập, `@Transactional` không xuyên service, **Replication/Leader-Follower**, **acks**, **Redis Distributed Lock**
- [x] **Buổi 9** — Lỗ hổng lock theo list ID, deadlock, **Service Registry/Discovery**, dựng **Eureka Server + Client**, `@LoadBalanced`
- [x] **Buổi 10** — **API Gateway** (Spring Cloud Gateway), Basic Auth vs Access Token, xác thực nội bộ, **Keycloak**, bonus CDC
- [x] **Buổi 11** — Khái niệm Keycloak, **mã hóa đối xứng vs bất đối xứng**, **cấu trúc JWT**, vì sao cần Auth Service
- [x] **Buổi 12** — **JWKS**, và **đề bài Project cuối khóa** (2 pain point: request đồng thời + notify triệu user)
- [x] **Buổi 13** — **ELK Stack**: centralized logging, `logback-spring.xml`, Kibana
- [x] **Buổi 14** — **Caching**: Caffeine (local) + Redis (global), `@Cacheable`/`@CacheEvict`, trade-off
- [ ] **Buổi 15** — Ôn tập + review project cuối khóa *(buổi cuối)*

### Trạng thái kiến trúc project

| Thành phần | Trạng thái |
|---|---|
| **Product Service** | ✅ CRUD, lock stock, Kafka, distributed lock, Eureka, cache, ELK |
| **Order Service** | ✅ Create order, Kafka, cập nhật status, Eureka, ELK |
| **Discovery Server** | ✅ Eureka Server, port 8761 |
| **Auth Service** | ✅ Có (dùng **Keycloak**) — chi tiết ở buổi 10-12 |
| **API Gateway** | ✅ Có — chi tiết ở buổi 10-12 |
| Kafka | ✅ Producer, Consumer, Retry non-blocking, DLT |
| Redis | ✅ Distributed lock **+ Global cache** |
| Service Discovery | ✅ Eureka + `@LoadBalanced` |
| Logging | ✅ ELK Stack (Elasticsearch + Logstash + Kibana) |
| Caching | ✅ Caffeine (local) / Redis (global) |

### Lịch thi

| | |
|---|---|
| Trước buổi 15 | Hoàn thiện **~100%** project |
| Buổi 15 | Ôn tập + thầy review, góp ý |
| Thi | **Thứ 7** hoặc **Thứ 3 tuần sau** (2 đợt, chọn 1) — mang project đi **bảo vệ** |

---

## Checklist tự kiểm tra

### Sau buổi 1 — Kiến trúc
- [ ] Cài được draw.io, vẽ lại được sơ đồ Monolithic và Microservice
- [ ] Giải thích được vì sao Monolithic không scale lệch được
- [ ] Phân biệt scale up và scale out
- [ ] Nêu được ít nhất 3 nhiệm vụ của API Gateway
- [ ] Thuộc 4 RULE ở [mục 8](#8-các-nguyên-tắc-cứng-rules)

### Sau buổi 2 — DB & Product Service
- [ ] Thiết kế được category lồng nhau bằng `parent_id`, nêu được 3 nhược điểm
- [ ] Biết cách xử lý category có nhiều cha (bảng quan hệ `left_id`/`right_id`/`relationship`)
- [ ] Giải thích được vì sao dùng UUID thay auto-increment
- [ ] Nhớ 4 cột auditing + cột `is_deleted`, và luôn lọc `is_deleted = 0` khi query
- [ ] Chạy được Product Service, gọi được API tạo product qua Postman

### Sau buổi 3 — Order Service
- [ ] Giải thích được tình huống "giá đổi sau khi đặt hàng" và 2 pattern xử lý
- [ ] Phân biệt Snapshot vs Versioning, biết khi nào dùng cái nào
- [ ] Hiểu vì sao `orders.customer_id` không có Foreign Key
- [ ] Phân biệt `id` và `code`
- [ ] Có 2 repository Git riêng, đã push public

### Sau buổi 4 — Auditing & Kafka
- [ ] JPA Auditing chạy được trên cả 2 service (4 cột không còn `NULL`)
- [ ] Giải thích được vì sao **không bao giờ** cho client truyền `price`
- [ ] Vẽ lại được luồng đặt hàng 4 bước và chỉ ra 2 chỗ phải gọi Product Service
- [ ] Phân biệt Sync vs Async, cho được ví dụ đặt vé concert
- [ ] Ánh xạ được: Topic↔Table, Message↔Record, (Partition,Offset)↔Primary Key
- [ ] Giải thích được partition tăng tốc nhưng **mất thứ tự toàn cục**
- [ ] Chạy được Kafka bằng `docker compose up -d`, vào UI cổng 8081 tạo topic và publish message

### Sau buổi 5 — WebClient
- [ ] Config được port riêng cho từng service (Product `8888`, Order `8080`)
- [ ] Tách được `ProductClient` interface + implementation
- [ ] Gọi được sang service khác bằng `WebClient` — nhớ đủ 5 thành phần: method, uri, body, kiểu trả về, validate response
- [ ] Hiểu `.block()` chính là điểm làm cho lời gọi trở thành **đồng bộ**

### Sau buổi 6 — Kafka & tối ưu
- [ ] Giải thích được vì sao **không được gọi I/O trong vòng lặp** (ví dụ bao cát)
- [ ] Biết chuyển `List` → `Map` để tra cứu `O(1)` thay vì `O(n²)`
- [ ] Publish được message lên Kafka từ Order Service
- [ ] Consume được message ở Product Service
- [ ] Giải thích được **Consumer Group** và vì sao cần nó
- [ ] Hiểu `auto-offset-reset` chỉ có tác dụng lần đầu group đăng ký
- [ ] Biết 3 lỗi kinh điển: `trusted.packages`, `__TypeId__`, `UnrecognizedProperty`

### Sau buổi 7 — Race Condition & Retry
- [ ] Vẽ được kịch bản race condition (stock 10, 2 request cùng trừ)
- [ ] Viết được `@Lock(LockModeType.PESSIMISTIC_WRITE)` và verify thấy `for update` trong log SQL
- [ ] Biết vì sao **không** gắn `@Lock` lên hàm `findById` dùng chung
- [ ] Phân biệt lỗi **retryable** và **non-retryable**
- [ ] Giải thích được Blocking Retry **chặn** message sau, Non-Blocking thì không
- [ ] Cấu hình được `@RetryableTopic`, quan sát được các topic `-retry-2000/4000/8000` và `-DLT`
- [ ] Đặt tên topic theo **nguồn phát**, không theo hệ quả

### Sau buổi 8 — Kafka nâng cao & Redis
- [ ] Giải thích được vì sao `@Transactional` **không** rollback xuyên service
- [ ] Biết "rollback" trong microservice = đổi status (compensating), không phải xóa
- [ ] Nêu được keyword **Saga Pattern** và **Outbox Pattern**
- [ ] Trả lời được: 3 partition + 4 consumer thì sao? 3 partition + 1 consumer thì sao?
- [ ] Biết `replica` không được lớn hơn số `broker`, và vì sao
- [ ] Biết mọi đọc/ghi Kafka đều với **Leader**
- [ ] Phân biệt `acks = 0 / 1 / all` và rủi ro từng mức
- [ ] Giải thích được vì sao Redis (single-threaded, RAM) hợp làm distributed lock
- [ ] **Biết vì sao phải SORT product IDs khi tạo lock key**
- [ ] Hiểu `tryLock(waitTime, leaseTime)` và vì sao cần `leaseTime`
- [ ] Cân nhắc được: khi nào `SELECT FOR UPDATE` là đủ, khi nào cần Redis

### Sau buổi 9 — Eureka & Service Discovery
- [ ] Giải thích được vì sao lock key `"1,2,3"` và `"1,2"` vẫn tranh chấp nhau
- [ ] Biết giải pháp là lock **từng product**, và biết nó sinh ra rủi ro **deadlock**
- [ ] Giải thích được vì sao hardcode `localhost:8888` làm việc scale trở nên vô nghĩa
- [ ] Nêu được: 2 ứng dụng **không thể** chạy cùng port trên cùng máy
- [ ] Kể lại được ẩn dụ "cuốn sổ của Hải Trương" (Service Registry)
- [ ] Phân biệt Eureka (**push** — service tự báo cáo) vs K8s (**pull** — registry đi hỏi)
- [ ] Phân biệt Eureka **Server** (ứng dụng) và Eureka **Client** (thư viện)
- [ ] Biết Gateway **cũng phải** cài Eureka Client (vì Gateway cũng cần nhiều instance)
- [ ] Dựng được Eureka Server, thấy service đăng ký trên `localhost:8761`
- [ ] Chạy được nhiều instance qua Copy Configuration trong IntelliJ
- [ ] Đổi URL sang tên service + nhớ `@LoadBalanced`
- [ ] Biết lỗi kinh điển: dùng `WebClient.builder()` mới thay vì bean có `@LoadBalanced`
- [ ] Phân biệt Round Robin và Random

### Sau buổi 10 — Gateway & Keycloak
- [ ] Dựng được API Gateway với `spring-cloud-starter-gateway` (không nhầm sang bản `-server-webflux`)
- [ ] Giải thích được vì sao Access Token an toàn hơn Basic Auth — **mấu chốt là HẾT HẠN**, không phải khó đọc
- [ ] Nêu được 3 option xác thực nội bộ và pros/cons từng cái
- [ ] Hiểu vấn đề thật của "không xác thực nội bộ" là **không audit được ai làm gì**
- [ ] Biết chi phí của Access Token nội bộ: tốn CPU + public key có thể rotate
- [ ] Giải thích được cách Gateway truyền danh tính user xuống service qua **header**
- [ ] Nêu được 3 vai trò của Keycloak
- [ ] Chạy được Keycloak (`admin`/`password`, port 8080), biết nó đụng port với Order Service
- [ ] Biết **CDC / Debezium** là gì và vì sao thực tế **bị reject** (maintain + quyền binlog)

### Sau buổi 11 — JWT & mã hóa
- [ ] Phân biệt **User** (`username`/`password`) và **Client** (`client_id`/`client_secret`) trong Keycloak
- [ ] Phân biệt mã hóa **đối xứng** và **bất đối xứng**; biết Base64 **không phải** mã hóa
- [ ] Biết private key **ký**, public key **verify**
- [ ] Kể được 3 phần của JWT và mỗi phần chứa gì
- [ ] Biết **payload decode được** → không để dữ liệu nhạy cảm vào
- [ ] Nêu đủ **4 thành phần** sinh ra signature
- [ ] Giải thích được vì sao hacker sửa payload thì bị phát hiện
- [ ] Giải thích được vì sao header phải chứa tên thuật toán
- [ ] Giải thích được vì sao **frontend không được gọi thẳng Keycloak** → cần Auth Service

### Sau buổi 12 — JWKS & Project
- [ ] Biết **JWKS** là gì, lấy ở endpoint nào của Keycloak
- [ ] Hiểu vì sao mỗi thuật toán có một bộ key riêng (RS256 ký, RSA-OAEP mã hóa)
- [ ] Biết backend cần JWKS mới verify được token
- [ ] Nắm rõ **2 pain point** của đề bài và biết dùng kiến thức nào để giải

### Sau buổi 13 — ELK & Logging
- [ ] Giải thích được vì sao log là thứ **duy nhất còn lại** khi khách báo lỗi hôm qua
- [ ] Nêu đúng vai trò E / L / K và vì sao cần Logstash ở giữa
- [ ] Ánh xạ: Index ≈ Table, Document ≈ Record
- [ ] Dựng được ELK, vào Kibana `localhost:5601`
- [ ] Hiểu `logstash.conf`: `input` (tcp 5000, json) và `output` (index theo `app_name` + ngày)
- [ ] Config `logback-spring.xml` với 2 appender CONSOLE + LOGSTASH
- [ ] Biết `customFields` `app_name` chính là thứ quyết định index
- [ ] Tạo index pattern `*-service-*`, chọn field `level`/`app_name`/`message`
- [ ] Biết chỉ log ID chứ không log full object (dài + lộ dữ liệu nhạy cảm)
- [ ] Nhớ: **Notification không thuộc User Service**

### Sau buổi 14 — Caching
- [ ] Giải thích được bài toán: query JOIN chậm × triệu request lặp lại
- [ ] Nêu 2 việc khó nhất ngành: **naming** và **cache invalidation**
- [ ] Kể được 4 lý do không dùng HashMap làm cache
- [ ] So sánh Local vs Global cache đủ 5 tiêu chí
- [ ] Giải thích được vì sao Redis nhanh hơn query MySQL dù đều qua mạng
- [ ] Biết Spring Cache chỉ là **interface**, implementation do Caffeine/Redis
- [ ] Config được Caffeine: `@EnableCaching`, `expireAfterWrite`, `maximumSize`
- [ ] Dùng đúng `@Cacheable` (value/key/condition) và `@CacheEvict` (`allEntries`)
- [ ] Biết cache trên API **listing là anti-pattern**, nên cache `getById`
- [ ] Đổi sang Redis bằng `@Primary` mà **không sửa business code**
- [ ] Verify được cache share giữa 2 instance qua Redis
- [ ] Trả lời được 3 câu hỏi trade-off: có cần cache? local hay global? team sẵn sàng?
