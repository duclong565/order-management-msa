# 08 — API Gateway & Bảo mật

> Gateway, Basic Auth vs Access Token, Keycloak, mã hóa bất đối xứng, cấu trúc JWT, JWKS.
> ⭐ **Chủ đề hay hỏi phỏng vấn** — đặc biệt phần JWT.
> 📎 Nguồn: buổi 1, 9, 10, 11, 12

---

## 1. API Gateway làm gì

```
        NGOÀI (Internet)          │          TRONG (network nội bộ)
                                  │
  [Frontend] ──Access Token──► [API GATEWAY] ──► [Product] [Order] [Auth]
```

| Vai trò | Mô tả |
|---|---|
| **Routing / Forwarder** | Nhận request từ frontend, forward xuống service tương ứng |
| **Load Balancer** | Có 10 instance Product → chọn 1 để đẩy request |
| **Xác thực & phân quyền tập trung** | Vì mọi request đều đi qua |
| **Rate limiting** | Giới hạn: 1 user chỉ được gọi API này 5 lần/giây |

**Các nhiệm vụ khác:** request/response transform, SSL termination, logging/tracing (gắn `trace-id`), circuit breaker, response aggregation, CORS, API versioning.

> **Root cause khiến Gateway làm được nhiều thứ:** nó là **thằng ở giữa**, mọi request đều đi qua.
> Đổi lại, nó cũng chính là **nút thắt cổ chai (bottleneck)** của hệ thống.

### Gateway KHÔNG nên làm gì

- ❌ Không chứa **business logic**. Gateway chỉ điều phối
- ❌ Không truy cập database nghiệp vụ

> Nhồi logic vào Gateway → nó trở thành monolith mới, mọi thay đổi lại phải deploy Gateway.

### Routing hoạt động thế nào

Client gọi:
```
POST  http://abc.com/product/v1/create-product
POST  http://abc.com/order/v1/cancel-order
```

Gateway đọc **prefix** đầu path:

| Prefix | Route tới | Path chuyển tiếp |
|---|---|---|
| `/product` | Product Service | `/v1/create-product` |
| `/order` | Order Service | `/v1/cancel-order` |

### Dependency — chỗ dễ sai

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

> ⚠️ Thêm nhầm `spring-cloud-starter-gateway-server-webflux` → thầy sửa: *"xóa chữ server đi"*.

📌 Gateway cũng cần **Eureka Client** ([file 07](07-service-discovery.md)) để route theo tên service.

---

## 2. ⚠️ Basic Auth vs Access Token

### Basic Auth có vấn đề gì

```
MỌI request đều phải truyền username + password:
   tạo đơn hàng   → truyền username/password
   lấy sản phẩm   → truyền username/password
   làm bất cứ gì  → truyền username/password

→ Xác suất bị LỘ rất cao
```

Tệ hơn: username/password là thứ người dùng **đọc được, nhớ được**, và họ đặt **ngắn, dễ nhớ**.

### Access Token giải quyết thế nào

> Thay vì truyền username/password, sinh ra một **chuỗi rất dài** đại diện cho credential đó.

Nhưng điều này **chưa đủ**:
> *"Nó vẫn có thể bị ăn cắp. Vì tất cả request đều truyền cái đấy mà. Chỉ là nó khó đọc hơn thôi."*

### 🎯 Điểm khác biệt QUYẾT ĐỊNH: HẾT HẠN

| | Basic Auth | Access Token |
|---|---|---|
| Truyền ở mọi request | ✅ | ✅ |
| Có thể bị đánh cắp | ✅ | ✅ |
| Dễ đọc / dễ nhớ | ✅ (ngắn) | ❌ (chuỗi dài) |
| **Thời hạn sử dụng** | **Vĩnh viễn** — dùng **đến cuối đời** | **Hết hạn** (vd 5 phút) |

> *"Nếu ai đó đánh cắp được Access Token của anh thì chỉ xài được trong vòng 5 phút thôi."*
> Còn Basic Auth bị đánh cắp mà mình không biết → nó dùng **mãi mãi**.

> **Tất cả website hiện tại đều dùng Access Token.**

---

## 3. ⚠️ Xác thực GIỮA các Microservice — 3 options

> *"Có 3 options. **Không có đúng với sai.** Mình chỉ phân tích pros and cons thôi."*

| Option | Ưu | Nhược |
|---|---|---|
| **① Không xác thực gì** | Nhanh nhất, đơn giản nhất | Không bảo mật nội bộ; **không truy vết được ai làm gì** |
| **② Basic Auth** | Đơn giản | **Quá nhiều người biết** username/password → audit vô nghĩa |
| **③ Access Token** | Bảo mật nhất, truy vết được | **Tốn CPU** giải mã; **public key có thể thay đổi** → phải lấy lại |

### Lý lẽ cho option ① và ②

> Từ bên ngoài vào **bắt buộc phải qua API Gateway**, mà Gateway đã yêu cầu Access Token rồi.
> Các service bên trong nằm trong **network nội bộ** — từ ngoài không tấn công vào được.

### ⚠️ Nhưng vấn đề thật nằm ở AUDIT

> *"Nếu một tháng trước ai đó cập nhật trường dữ liệu qua API, bây giờ anh **không truy vết được**.
> Vì nó chỉ ghi lại là `anonymous` thì làm sao biết ai làm việc đấy?"*
>
> *"**Bọn anh đang gặp đúng tình trạng này** — vì quá nhiều người biết username/password."*
>
> *"Câu chuyện gì đến cũng sẽ đến. Không biết là ai tác động luôn."*

### 📌 Chốt cho lớp

```
[Frontend] ──── Access Token (BẮT BUỘC) ────► [API Gateway]
                                                    │
                                              KHÔNG cần gì
                                                    ▼
                                    [Product] [Order] [Auth]
```

---

## 4. Truyền thông tin user qua Header

**Vấn đề:** Order Service vẫn cần biết **ai tạo đơn hàng** để điền `created_by` (auditing).
Nhưng Gateway → Order không truyền token, vậy Order làm sao biết?

```
① Gateway nhận request kèm Access Token
② Gateway GIẢI MÃ token → biết user là "hai.truong"
③ Gateway GẮN thông tin user vào HEADER
④ Gọi xuống Order Service
⑤ Order Service ĐỌC header → biết ai tạo đơn → điền created_by
```

📌 Thường dùng header `X-User-Id`, `X-Username`, `X-Roles`.
Service bên trong **tin** các header này vì chúng đến từ network nội bộ.
⚠️ Điều kiện an toàn: Gateway phải **strip** các header đó nếu client cố tình tự gửi lên.

---

## 5. Keycloak

### Là gì

> Một **open source rất nổi tiếng** chuyên lo xác thực & phân quyền.
> Được dùng trong **hầu hết các hệ thống lớn ở Việt Nam và trên thế giới** — VNPT, Viettel...

### Vai trò — 3 việc

| # | Vai trò |
|---|---|
| ① | **Quản lý user** hộ mình |
| ② | **Cấp Access Token** khi user login |
| ③ | **Verify** token có hợp lệ không |

### Vì sao không tự code?

> Tự code phải tự làm hết: bảng user, bảng password, bảng role, bảng access token...
> *"Tóm lại tất cả những gì mình làm sẽ **không tốt bằng** cái open source này."*

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
| Port | **`8080`** ⚠️ *(đụng Order Service — cần đổi một trong hai)* |

### Các khái niệm

| Khái niệm | Là gì |
|---|---|
| **Realm** | "Vương quốc" — nơi người dùng **đăng nhập**. Chứa endpoint cấp token và **public key** |
| **Client** | Đại diện cho một **hệ thống/ứng dụng** |
| **User** | Đại diện cho **người dùng** |
| **Grant types** | Các giao thức OAuth2 client hỗ trợ để lấy token |

### ⚠️ User vs Client

| | Đại diện cho | Định danh | Bí mật |
|---|---|---|---|
| **User** | **Người dùng** | `username` | `password` |
| **Client** | **Hệ thống** | `client_id` | `client_secret` |

> `client_id` / `client_secret` **tương đương** `username` / `password` — chỉ khác là dành cho **hệ thống**.
> Tìm ở tab **Credentials** của client.

---

## 6. Mã hóa đối xứng vs bất đối xứng

### Đối xứng (Symmetric)

> Mã hóa và giải mã dùng **CHUNG MỘT KEY**.

Ví dụ thật: **AES**, **DES**, **3DES**.

📌 *Lưu ý*: thầy lấy **Base64** làm ví dụ. Thực ra **Base64 không phải mã hóa** — nó là **encoding**, ai cũng decode được, không có key nào cả. **Đi phỏng vấn phải nói đúng.** Ý thầy minh họa là "cùng một quy tắc cho cả hai chiều" — điều đó thì đúng.

### Bất đối xứng (Asymmetric)

> Mã hóa dùng **một key**, giải mã dùng **key KHÁC**.

```
[private key] ──ký/mã hóa──►  dữ liệu  ◄──verify/giải mã── [public key]
   giữ BÍ MẬT                                                 CÔNG KHAI
```

| Key | Ai giữ | Dùng để |
|---|---|---|
| **Private key** | **Chỉ mình giữ** (Keycloak) | **Ký** token |
| **Public key** | **Công khai** | **Verify** chữ ký |

📌 *Vì sao đây là bước ngoặt*: với mã hóa đối xứng, hai bên phải **truyền key cho nhau trước** — mà truyền qua mạng thì có thể bị chặn. Bất đối xứng giải quyết đúng vấn đề đó: public key **cứ để lộ thoải mái**, chỉ private key cần giữ kín. Đây là nền tảng của HTTPS/TLS, chữ ký số, và JWT. Thuật toán: **RSA**, **ECDSA**.

---

## 7. ⭐ Cấu trúc JWT (Access Token)

Chuỗi gồm **3 phần** ngăn cách bởi **dấu chấm**:

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

Paste token vào công cụ decode → **đọc được ngay** thông tin user.

> Token **không phải** thông tin cần che giấu nội dung. Ai cũng decode được payload.
> Cái được bảo vệ là **tính toàn vẹn** — không ai **sửa** được nội dung mà không bị phát hiện.

📌 **Không bao giờ để dữ liệu nhạy cảm** (password, số thẻ, CCCD) vào payload.

### Vì sao Header phải chứa thuật toán

> Sinh token bằng thuật toán nào thì lúc verify **phải dùng đúng thuật toán đó**.
> Có nhiều thuật toán bất đối xứng → phải đính thông tin này vào token.

### 🎯 Signature — 4 thành phần

```
SIGNATURE = f( header , payload , thuật toán , PRIVATE KEY )
```

> *"Nó được cấu thành bởi **bốn** thứ. **Thiếu một trong bốn thì không ra được** chuỗi signature."*

### Vì sao không giả mạo được

```
Hacker sửa payload (vd đổi role thành "admin")
   → header + payload đã KHÁC
   → signature tính lại sẽ KHÁC
   → nhưng hacker KHÔNG CÓ private key để ký lại
   → server verify bằng public key → PHÁT HIỆN NGAY ❌
```

> 💬 *"Đây là phần lý thuyết rất sâu. **Đi phỏng vấn họ vẫn sẽ hỏi những câu như này** để xem kiến thức sâu của ứng viên tới đâu."*

---

## 8. JWKS — JSON Web Key Set

Backend cần **public key** để verify token. Lấy ở đâu?

> **Key Set = một BỘ các public key.** Keycloak expose sẵn endpoint chứa bộ key này.

```
http://localhost:8080/realms/<tên-realm>/protocol/openid-connect/certs
```

```json
{
  "keys": [
    { "kid": "abc...", "alg": "RS256",    "kty": "RSA", "use": "sig", "n": "...", "e": "AQAB" },
    { "kid": "xyz...", "alg": "RSA-OAEP", "kty": "RSA", "use": "enc", "n": "...", "e": "AQAB" }
  ]
}
```

| | |
|---|---|
| Nội dung | Chỉ là **public key** — công khai, không bí mật |
| Mỗi thuật toán một bộ key | `RS256` (ký/verify) và `RSA-OAEP` (mã hóa) là hai bộ khác nhau |
| Mục đích | **Backend phải lấy được bộ key này mới verify được token** |

📌 Đây chính là thứ nói ở mục 3 — *"public key không cố định, có thể bị thay đổi → phải đi lấy lại"*.
Keycloak **rotate key** định kỳ; Spring Security tự fetch lại JWKS và cache theo `kid` trong header token.

### Config trong Spring
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://localhost:8080/realms/<realm>/protocol/openid-connect/certs
```

---

## 9. ⚠️ Vì sao cần Auth Service đứng giữa

```
❌ SAI:  [Frontend] ──client_id + client_secret──► [Keycloak]
```

> `client_secret` là **password của hệ thống**. Không thể để frontend giữ —
> code frontend ai cũng xem được, secret sẽ **lộ ngay**.

```
✅ ĐÚNG:
[Frontend] ──username/password──► [Auth Service] ──client_id+secret──► [Keycloak]
                                   (giữ secret ở            │
                                    phía server)            ▼
[Frontend] ◄───── access token ──── [Auth Service] ◄─── access token
```

> **Người dùng / frontend KHÔNG được phép gọi trực tiếp vào Keycloak.**

Đây chính là **lý do tồn tại của Auth Service** trong kiến trúc 4 thành phần.

---

## 10. Luồng JWT hoàn chỉnh

```
1. Client POST /auth/login  {username, password}
2. Auth Service → Keycloak → trả về:
      accessToken  (JWT, sống ngắn ~15 phút)
      refreshToken (sống dài ~7 ngày)
3. Client gọi API khác kèm header:  Authorization: Bearer <accessToken>
4. Gateway verify chữ ký JWT (bằng public key từ JWKS)
       - hợp lệ  → forward request, kèm header X-User-Id, X-Roles
       - hết hạn → 401, client dùng refreshToken xin accessToken mới
5. Service bên trong TIN header do Gateway gắn (vì network nội bộ)
```

**Tại sao JWT hợp với Microservice?**
> JWT **self-contained** — thông tin user nằm trong token, service không cần gọi DB để biết ai đang gọi
> → **stateless** → scale out thoải mái.

### Auth Service vs User Service — tách hay gộp?

| | Auth Service | User Service |
|---|---|---|
| Trách nhiệm | Login, logout, token, password, role | Profile, địa chỉ, quan hệ, thông tin cá nhân |

- Hệ thống **lớn** → tách riêng
- Hệ thống **nhỏ** → gộp làm một ← **lớp dùng**
- **Không có đúng/sai tuyệt đối**

---

## 📚 Đọc tiếp

- [07 — Service Discovery](07-service-discovery.md)
- [09 — Logging & ELK](09-logging-elk.md)
