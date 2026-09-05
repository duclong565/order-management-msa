# 01 — Kiến trúc & Nền tảng

> Nền móng của toàn bộ học phần. Đọc file này trước tất cả các file khác.
> 📎 Nguồn: buổi 1, 8, 9

---

## 1. Monolithic là gì

```
[Frontend] ──────► [ Backend (1 source code) ] ──────► [ 1 Database ]
```

- **Một** source code backend duy nhất
- Mọi domain nằm chung: Product, Order, Cart, Payment, User, Shipment...
- Mỗi domain chỉ là một `Controller` + `Service` + `Repository` trong cùng project
- **Một** database, các domain chỉ khác nhau **tên bảng**

### Monolithic KHÔNG phải là xấu

| Ưu điểm | Nhược điểm |
|---|---|
| Dev nhanh, đơn giản | Scale phải scale toàn bộ |
| Debug dễ (1 process, 1 stack trace) | Sửa 1 dòng → build lại cả hệ thống |
| Transaction ACID gọn (1 DB) | 1 module crash → sập cả app |
| Không tốn chi phí network nội bộ | Codebase phình → build/test chậm dần |
| Chi phí hạ tầng thấp | Nhiều team dẫm chân nhau trên 1 repo |

> **Nguyên tắc thực tế:** dự án nhỏ / startup giai đoạn đầu → **cứ làm Monolithic**.
> Chỉ tách Microservice khi thực sự có nhu cầu (tải lớn, nhiều team, cần scale lệch).

---

## 2. Scale — khái niệm nền tảng

### Ví dụ quán xôi

Bình thường bán 100 gói/ngày. Hôm nay có sự kiện → 1000 gói/ngày. Một mình không kham nổi:

1. **Thuê thêm người** → nhiều người cùng làm
2. **Tự cố gắng hơn** → bỏ nghỉ trưa, uống nước tăng lực

### Ánh xạ sang hệ thống

| Cách | Tên kỹ thuật | Nghĩa |
|---|---|---|
| Thuê thêm người | **Scale out** (Horizontal) | Thêm **số lượng server/instance** |
| Tự cố gắng hơn | **Scale up** (Vertical) | Tăng **CPU / RAM** của chính server đó |

| | Scale up | Scale out |
|---|---|---|
| Giới hạn | Có trần cứng (phần cứng max) | Gần như vô hạn |
| Downtime | Thường phải restart | Không cần |
| Chi phí | Tăng phi tuyến | Tăng tuyến tính |
| Điều kiện | Không cần gì | App phải **stateless** |
| Điểm chết | Single point of failure | Có redundancy |

> **Microservice ưu tiên scale out.** Muốn scale out được thì service phải **stateless** —
> không giữ session/state trong RAM của instance, vì request lần sau có thể rơi vào instance khác.
> State đẩy hết ra DB / Redis / JWT token.

---

## 3. Điểm yếu của Monolithic — bài toán Flash Sale

Lúc flash sale, tải phân bố **rất lệch**:

| Module | Tải |
|---|---|
| Order, Payment | 🔥🔥🔥 Cực cao |
| Cart, Product | 🔥🔥 Cao |
| Shipment | 🙂 Thấp — mai mốt mới ship |
| Blog, Review | 🙂 Rất thấp |

**Vấn đề:** Monolithic chỉ có **1 cục source code** → scale phải nhân bản **toàn bộ**:

```
Trước:  [Backend]
Sau:    [Backend]  [Backend]  [Backend]   ← nhân bản CẢ CỤC
```

→ Chỉ cần Order mạnh hơn nhưng phải trả tiền cho cả Blog, Review nhân bản theo. **Lãng phí.**

**Ba điểm yếu khác:**
1. **Deploy toàn phần** — sửa 1 dòng ở Blog → deploy lại cả hệ thống, rủi ro downtime cho Order
2. **Lỗi lan** — memory leak ở Review sập process → mất luôn Order, Payment
3. **Khóa công nghệ** — cả hệ thống dùng chung 1 ngôn ngữ/framework/version

---

## 4. Microservice — giải pháp

```
                    ┌──────────────────────┐
                    │    API Gateway       │
                    └──────────┬───────────┘
        ┌──────────┬───────────┼───────────┬──────────┐
        ▼          ▼           ▼           ▼          ▼
   ┌────────┐ ┌────────┐ ┌─────────┐ ┌────────┐ ┌──────────┐
   │Product │ │  Cart  │ │  Order  │ │Payment │ │ Shipment │
   └───┬────┘ └───┬────┘ └────┬────┘ └───┬────┘ └────┬─────┘
       ▼          ▼           ▼          ▼           ▼
   ┌────────┐ ┌────────┐ ┌─────────┐ ┌────────┐ ┌──────────┐
   │Prod DB │ │Cart DB │ │Order DB │ │ Pay DB │ │ Ship DB  │
   └────────┘ └────────┘ └─────────┘ └────────┘ └──────────┘
```

Mỗi service: **source code riêng**, **deploy độc lập**, **scale độc lập**, **database riêng**.

### Scale lệch — cái Microservice giải quyết

Flash sale chỉ Order quá tải → chỉ scale Order lên 4 instance, các service khác giữ nguyên.

### ⚠️ Cái giá phải trả

| Vấn đề | Mô tả |
|---|---|
| Network latency | Gọi in-process (nano-giây) → gọi HTTP/gRPC (mili-giây) |
| Distributed transaction | Không còn `@Transactional` xuyên service → xem [file 10](10-transaction-phan-tan.md) |
| Data consistency | Chấp nhận **eventual consistency** |
| Debug khó | 1 request qua 5 service → cần distributed tracing |
| Vận hành phức tạp | N service = N pipeline CI/CD, N bộ log, N bộ monitoring |
| Partial failure | Service B chết → A xử lý sao? → Circuit Breaker, retry, timeout |

> *"Microservices buy you options at the cost of complexity."*

---

## 5. Database per Service

### Vì sao phải tách DB

Nếu tách service nhưng vẫn chung 1 DB:
- Scale service được nhưng **DB thành bottleneck**
- Ví dụ nhà hàng: khách đông → thuê thêm **phục vụ** (scale service) nhưng vẫn **1 đầu bếp** (1 DB) → vẫn tắc
- Các service **coupling** qua schema: Order đổi bảng → Product vỡ

### ⚠️ Thực tế triển khai

Production **không bao giờ dùng 1 server cho DB** — tối thiểu **3 server** để backup/failover.
Nhưng 10 service × 3 server = **30 server vật lý** → **không công ty nào chịu nổi chi phí**.

**Cách làm thật:**

```
     ┌──────── Cụm DB vật lý (3 node, replication) ─────────┐
     │  schema: product_db   schema: order_db   auth_db     │
     └──────────────────────────────────────────────────────┘
           ▲                ▲                ▲
      [Product SVC]    [Order SVC]      [Auth SVC]
```

- Chung cụm server vật lý (~3 node để HA)
- Mỗi service một **schema/database logic riêng** (`CREATE DATABASE product_db;`)
- Về mặt logic vẫn là **Database per Service**

### 3 mức tách DB

| Mức | Mô tả | Chi phí | Độ cô lập |
|---|---|---|---|
| 1. Private tables | Chung schema, quy ước không đụng bảng nhau | Rẻ nhất | Yếu |
| 2. **Schema per service** ← lớp dùng | Chung DB server, khác schema | Vừa | Tốt |
| 3. DB server per service | Mỗi service 1 cụm DB riêng | Đắt | Mạnh nhất |

📌 Việc chọn SSD/HDD, thêm node là **scope của DevOps**, không phải dev.

---

## 6. ⚠️ BỐN NGUYÊN TẮC CỨNG

> Đây là **luật**, không phải gợi ý. Thầy nhấn mạnh nhiều lần.

### RULE 1 — Một service chỉ chọc vào MỘT database
```
[Order SVC] ──► [Order DB]                       ✅
[Order SVC] ──┬► [Order DB] └► [Product DB]      ❌
```

### RULE 2 — Một database chỉ được chọc bởi MỘT service
```
[Order SVC] ──► [Order DB] ◄── [Report SVC]      ❌
```
> Quan hệ Service ↔ DB là **1–1**. Không nhiều ở bất kỳ chiều nào.
> Nhiều service cùng chọc 1 DB là điều **cực kỳ không được phép vi phạm**.

### RULE 3 — Muốn lấy data của service khác → gọi API của nó
```
[A] ──HTTP/gRPC──► [B Service] ──► [B DB]      ✅
[A] ──────────────────────────────► [B DB]     ❌ TUYỆT ĐỐI KHÔNG
```

**Vì sao quan trọng đến thế:**
- A đọc thẳng DB của B → A phụ thuộc **cấu trúc bảng** của B. B đổi tên cột → A vỡ mà B không biết
- **Phá vỡ business rule** của B (validate, tính toán nằm trong code của B, không nằm trong bảng)
- Mất khả năng deploy độc lập → quay lại monolith nhưng phức tạp hơn
- **Encapsulation ở cấp kiến trúc**: API là hợp đồng công khai, DB schema là chi tiết nội bộ

### RULE 4 — Không tạo "service trung gian" chỉ để chứa quan hệ
```
[User SVC] ──► [Relationship SVC] ◄── [User SVC]    ❌
```
Quan hệ bạn bè phải nằm **trong User Service** (bảng `friendship`).

> Nguyên nhân sâu xa: service chia theo **business capability**, không chia theo **bảng dữ liệu**.

---

## 7. Chia service bao nhiêu là đủ

> Người mới học hay nghĩ **"chia càng nhỏ càng tốt"** — **SAI**.

Mỗi service là một **hệ thống độc lập**, kéo theo: CI/CD riêng, deploy riêng, monitoring riêng, giao tiếp qua network, transaction xuyên service phức tạp.

**Lỗi thầy sửa trên lớp:**

| Thiết kế sai | Sửa thành |
|---|---|
| `Teacher Service` + `Student Service` tách riêng | Gộp → **User Service** (phân biệt bằng `role`) |
| Tách `Subject`, `Major`, `Faculty`, `Tuition`... thành 7 service | Gộp còn ~3–4 service |
| `Relationship Service` riêng | Đưa vào **User Service** |
| `Notification` gộp vào `User Service` | **Tách ra** — hai domain khác nhau hoàn toàn |

> 💬 *"Hai thằng chẳng liên quan gì với nhau lại gộp thành một. Còn hai thằng liên quan nhau thì lại tách ra."*

### Tiêu chí xác định ranh giới service

1. **Business capability** — chia theo năng lực nghiệp vụ, không theo bảng
2. **Bounded Context (DDD)** — "Product" trong Catalog khác "Product" trong Inventory
3. **High cohesion, loose coupling** — thứ hay đổi cùng nhau thì ở cùng nhau
4. **Đội sở hữu** — 1 team sở hữu 1 service
5. **Tần suất gọi chéo** — A gọi B ở gần như mọi request → nên gộp
6. **Nhu cầu scale khác nhau** — 2 module luôn scale cùng nhịp thì tách ra vô ích

> **Lời khuyên:** bắt đầu bằng **Modular Monolith**, khi thấy rõ đường nứt mới tách.
> Tách sai còn tệ hơn không tách — gọi là **distributed monolith**, dở nhất mọi thế giới.

---

## 8. Kiến trúc project của lớp

```
                    ┌──────────────────┐
   [Frontend] ─────►│   API Gateway    │
                    └────────┬─────────┘
           ┌─────────────────┼─────────────────┐
           ▼                 ▼                 ▼
    ┌────────────┐   ┌───────────────┐  ┌─────────────┐
    │Auth Service│   │Product Service│  │Order Service│
    └─────┬──────┘   └──────┬────────┘  └──────┬──────┘
          ▼                 ▼                  ▼
     ┌──────────┐    ┌─────────────┐    ┌───────────┐
     │ auth_db  │    │ product_db  │    │ order_db  │
     └──────────┘    └─────────────┘    └───────────┘

   + Discovery Server (Eureka)  + Kafka  + Redis  + Keycloak  + ELK
```

| Service | Phạm vi |
|---|---|
| **API Gateway** | Routing, xác thực tập trung, load balancing |
| **Auth Service** | Xác thực + phân quyền + **gộp luôn User** (account, role, address, seller) |
| **Product Service** | Sản phẩm, variant, category, **gộp luôn Promotion & Flash Sale** |
| **Order Service** | Toàn bộ nghiệp vụ mua hàng |

---

## 📚 Đọc tiếp

- [02 — Thiết kế Database](02-thiet-ke-database.md)
- [03 — Giao tiếp giữa Service](03-giao-tiep-giua-service.md)
- [08 — Gateway & Bảo mật](08-gateway-va-bao-mat.md)
