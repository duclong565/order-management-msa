# 07 — Service Discovery (Eureka)

> Vì sao hardcode URL làm việc scale trở nên vô nghĩa, và cách giải bằng Service Registry.
> 📎 Nguồn: buổi 9

---

## 1. ⚠️ Vấn đề: nhiều instance thì hardcode URL vô nghĩa

### Sự thật nền tảng

> **Hai ứng dụng KHÔNG THỂ chạy trên cùng một port trên cùng một máy.**
> Đó là lý do hay gặp lỗi `Port 8080 is already in use`.

→ Scale 10 instance Product = 10 port khác nhau: `8888`, `8887`, `8889`, ...

### Vấn đề trong code

```java
// Cách viết ban đầu
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

### Các giải pháp thủ công đều không ổn

| Cách | Vì sao không ổn |
|---|---|
| Lưu mảng IP cứng trong code | Không real-time, instance chết vẫn gọi |
| Chỉ định 1 instance "main" luôn sống | **Không ai đảm bảo được** một instance không chết (OOM, cao tải). Xác suất chết của cả 10 instance là như nhau |
| Mỗi service tự lưu IP main của mọi service khác | 5 service thì mỗi thằng phải duy trì 5 danh sách + tự kiểm tra sống chết → **quá phiền** |

---

## 2. Service Registry & Service Discovery

### 📓 Ẩn dụ "cuốn sổ"

> Thuê một người **không làm gì cả**, chỉ ngồi giữ một **cuốn sổ**.
>
> - Mỗi instance Product khi scale lên → **báo cáo**: *"Tao là Product, IP của tao là X, port Y, tao đang sống."*
> - Người đó ghi vào sổ.
> - Order muốn gọi Product → **hỏi người giữ sổ**, không hỏi Product.
> - Trả lời: *"Tao đang có 10 thằng Product sống, đây 10 địa chỉ IP, mày chọn một."*

### Heartbeat — làm sao biết instance còn sống

| Cơ chế | Ai chủ động | Công nghệ |
|---|---|---|
| **PUSH** | Service **tự báo cáo** định kỳ (~5 giây): *"tao còn sống"* | **Eureka** |
| **PULL** | Registry **đi hỏi thăm** định kỳ: *"mày còn sống không?"* | **Kubernetes** |

> Quá 5 giây không thấy báo cáo → coi như **thằng đó chết**, loại khỏi danh sách.
> Khi Order hỏi, chỉ đưa những thằng **còn sống**.

### Hai kiến trúc triển khai

```
┌─ KHÔNG dùng Kubernetes ──► cần EUREKA (Server + Client)   ← LỚP HỌC CÁI NÀY
│
└─ Dùng KUBERNETES ────────► K8s TỰ đóng vai trò registry + discovery
                              KHÔNG cần Eureka, không cần khai báo gì
```

> Thầy nói thẳng: *"Kiến trúc Eureka này bọn anh không triển khai. Anh chưa từng triển khai ứng dụng nào theo kiến trúc này trên thực tế, bọn anh dùng K8s hết."*
> Nhưng **Eureka phù hợp với level lớp hơn** — học K8s tốn quá nhiều kiến thức mới.
> Eureka **vẫn có hệ thống thật dùng**.

📌 *K8s auto-scaling (HPA)*: cấu hình *"instance nào RAM > 75% hoặc CPU > 80% thì scale thêm 1 instance"*. K8s có component chuyên đo và tự scale. Chỉ là **config**, không phải code.

---

## 3. Eureka Server vs Eureka Client

| | **Eureka Server** | **Eureka Client** |
|---|---|---|
| Là gì | Một **ứng dụng/hệ thống** riêng | Một **thư viện** |
| Vai trò | Giữ **cuốn sổ cái** | Tự động gửi heartbeat lên Server |
| Cài ở đâu | Chạy độc lập | Nhúng vào **TẤT CẢ** microservice |

**Client cài ở những đâu?**
- ✅ Product Service
- ✅ Order Service
- ✅ **Cả API Gateway**

> ⚠️ Câu hỏi bẫy: *"Gateway có cần nhiều instance không?"*
> Nhiều người trả lời "nó chỉ là một cái cổng thôi" — **SAI**.
>
> Ẩn dụ: xây thành thì xây **một cổng hay nhiều cổng**? → Nhiều cổng.
> Gateway **cũng là một service**, cũng là ứng dụng đang chạy → **cũng cần scale nhiều instance**.

> Không tự code heartbeat được không? Được — nhưng *"không ai làm chuyện đấy cả, nó hơi bị dở"*.

---

## 4. Code — Eureka Server

### Tạo project
New Project → Spring Boot → Maven → Java 21 → tên `discovery-server`
**Dependency:** `Eureka Server`

### Enable
```java
@SpringBootApplication
@EnableEurekaServer                   // ← bắt buộc
public class DiscoveryServerApplication { ... }
```

### Config
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
| ① `register-with-eureka: false` | Server không tự ghi tên mình vào sổ của chính nó |
| ② `fetch-registry: false` | Server không tải sổ từ nơi khác — **các thằng khác đăng ký VÀO nó** |
| `defaultZone` | Đường dẫn các service khác dùng để đăng ký |

### Giao diện
Chạy lên → **`http://localhost:8761`** → dashboard liệt kê service đã đăng ký.

---

## 5. Code — Eureka Client

### ⚠️ Dependency — cần đủ 3 phần

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

> ⚠️ Eureka Client thuộc bộ **Spring Cloud** (khác Spring Boot) → phải khai `dependencyManagement`.

### Config
```yaml
spring:
  application:
    name: product-service           # ⚠️ TÊN NÀY chính là địa chỉ để gọi

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
  instance:
    prefer-ip-address: true         # đăng ký bằng IP thay vì hostname
```

### Chạy nhiều instance trong IntelliJ

```
Run → Edit Configurations → chọn config hiện tại → Copy Configuration
→ Đặt tên mới → thêm option: server.port=8887
→ Apply
```

Chạy cả 3 → vào `localhost:8761` sẽ thấy `PRODUCT-SERVICE` có **3 instance**, cả 3 **UP**.

---

## 6. ⚠️ Đổi hardcode URL → tên service

Bước quan trọng nhất.

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

`@LoadBalanced` là annotation của **Spring Cloud** — dạy `WebClient` biết cách hỏi Eureka và phân tải.

### ⚠️ LỖI PHỔ BIẾN NHẤT

Nhiều người bị `Connect timeout`. Nguyên nhân: **tạo `WebClient` mới** thay vì dùng bean có `@LoadBalanced`.

```java
// ❌ SAI — tạo builder mới, KHÔNG có @LoadBalanced
WebClient client = WebClient.builder().build();

// ✅ ĐÚNG — inject bean đã được đánh dấu @LoadBalanced
private final WebClient.Builder webClientBuilder;
...
webClientBuilder.build().post()...
```

---

## 7. Round Robin vs Random

| | Round Robin | Random |
|---|---|---|
| Cách chọn | **Lần lượt**: 1 → 2 → 3 → 1 → 2 → 3 | Ngẫu nhiên mỗi lần |
| Gọi 3 lần | Chắc chắn mỗi instance 1 lần | Có thể trúng 1 thằng cả 3 lần |
| Đồng đều | Luôn đồng đều | Chỉ đều khi **số mẫu đủ lớn** |

> **Mặc định của Spring Cloud LoadBalancer là Round Robin.**

Demo: gọi 5 request → log cho thấy lần lượt `8889` → `8889` → `8888` → `8887` → `8888`.

---

## 8. Lỗi thực tế: `UnknownHostException` / `Connect timeout`

Máy Windows: Eureka đăng ký instance bằng **hostname máy** (vd `admin-pc.net`) thay vì IP → không resolve được → timeout.

**Fix:**
```yaml
eureka:
  instance:
    prefer-ip-address: true       # ← đăng ký bằng IP
```

📌 Sau khi sửa phải **stop toàn bộ instance rồi restart**.
Eureka **không real-time tuyệt đối** — instance đã chết vẫn hiển thị trong sổ một lúc (chờ hết chu kỳ heartbeat) rồi mới chuyển `unavailable`.

---

## 9. Kiến trúc hoàn chỉnh

```
                    ┌──────────────────────┐
                    │  Discovery Server    │  ← Eureka Server (cuốn sổ cái)
                    │  localhost:8761      │
                    └──────────┬───────────┘
                          ▲    │ trả về danh sách instance sống
             đăng ký +    │    │
             heartbeat 5s │    ▼
        ┌────────────────┴─────────────────────┐
┌───────┴────────┐                    ┌────────┴─────────┐
│ Order Service  │  ──gọi qua TÊN──►  │ Product Service  │
│  :8080, :8181  │  "product-service" │ :8887,:8888,:8889│
└────────────────┘   + @LoadBalanced  └──────────────────┘
```

---

## 📚 Đọc tiếp

- [03 — Giao tiếp giữa Service](03-giao-tiep-giua-service.md) — WebClient
- [08 — Gateway & Bảo mật](08-gateway-va-bao-mat.md)
