# 📘 Microservice — Sổ tay học tập

> Toàn bộ kiến thức học phần Microservice (14 buổi), được sắp xếp lại **theo chủ đề** để dễ học.

---

## 🗂️ Cách dùng bộ tài liệu này

Có **hai cách đọc**, chọn cách hợp với bạn:

| Cách đọc | File | Khi nào dùng |
|---|---|---|
| 📚 **Theo CHỦ ĐỀ** ← khuyến nghị | [`hoc-theo-chu-de/`](hoc-theo-chu-de/) | Học, ôn tập, tra cứu |
| 📅 **Theo BUỔI HỌC** | [`MICROSERVICE.md`](MICROSERVICE.md) | Xem lại một buổi cụ thể, nhớ mạch bài giảng |

Hai bản **cùng nội dung**, chỉ khác cách sắp xếp.

---

## 📚 Học theo chủ đề

### Phần 1 — Nền tảng *(đọc trước)*

| # | File | Nội dung chính |
|---|---|---|
| **01** | [Kiến trúc & Nền tảng](hoc-theo-chu-de/01-kien-truc-nen-tang.md) | Monolithic vs Microservice, Scale up/out, Database per Service, **4 nguyên tắc cứng** |
| **02** | [Thiết kế Database](hoc-theo-chu-de/02-thiet-ke-database.md) | UUID, soft delete, auditing, category cây, **Snapshot vs Versioning** |

### Phần 2 — Giao tiếp giữa Service

| # | File | Nội dung chính |
|---|---|---|
| **03** | [Giao tiếp giữa Service](hoc-theo-chu-de/03-giao-tiep-giua-service.md) | Sync vs Async, WebClient, **không gọi I/O trong loop**, Map O(1) |
| **04** | [Kafka](hoc-theo-chu-de/04-kafka.md) | Topic/Partition/Offset, Producer/Consumer, **Consumer Group**, Retry, DLT, acks |
| **07** | [Service Discovery](hoc-theo-chu-de/07-service-discovery.md) | Eureka, `@LoadBalanced`, vì sao hardcode URL vô nghĩa |

### Phần 3 — Xử lý vấn đề khó

| # | File | Nội dung chính |
|---|---|---|
| **05** | [Concurrency & Locking](hoc-theo-chu-de/05-concurrency-va-locking.md) ⭐ | **Race condition**, `SELECT FOR UPDATE`, Redis lock, Deadlock |
| **06** | [Caching](hoc-theo-chu-de/06-caching.md) | Caffeine vs Redis, TTL, **Cache Invalidation** |
| **10** | [Transaction phân tán](hoc-theo-chu-de/10-transaction-phan-tan.md) ⭐ | `@Transactional` không xuyên service, **Saga**, **Outbox**, CDC |

### Phần 4 — Bảo mật & Vận hành

| # | File | Nội dung chính |
|---|---|---|
| **08** | [Gateway & Bảo mật](hoc-theo-chu-de/08-gateway-va-bao-mat.md) ⭐ | API Gateway, Basic Auth vs Token, Keycloak, **cấu trúc JWT**, JWKS |
| **09** | [Logging & ELK](hoc-theo-chu-de/09-logging-elk.md) | Elasticsearch + Logstash + Kibana, cách viết log tốt |

### Phần 5 — Thực hành

| # | File | Nội dung chính |
|---|---|---|
| **11** | [Spring Boot Cheatsheet](hoc-theo-chu-de/11-spring-boot-cheatsheet.md) | Setup, entity, DTO, MapStruct, **tổng hợp annotation**, **danh sách lỗi hay gặp** |
| **12** | [Project cuối khóa](hoc-theo-chu-de/12-project-cuoi-khoa.md) | Đề bài, 2 pain point, **checklist trước khi nộp** |

---

## 🔖 Tài liệu tra cứu

| File | Dùng để |
|---|---|
| ⭐ [**CÂU HỎI PHỎNG VẤN**](CAU-HOI-PHONG-VAN.md) | **35 câu** tổng hợp mọi chỗ thầy nói *"đi phỏng vấn họ sẽ hỏi"* + gợi ý trả lời |
| [THUẬT NGỮ](THUAT-NGU.md) | Tra cứu nhanh mọi thuật ngữ |
| [MICROSERVICE.md](MICROSERVICE.md) | Bản đầy đủ theo từng buổi học |

---

## 🎯 Lộ trình học gợi ý

```
① Nền tảng          01 → 02
                      ↓
② Giao tiếp         03 → 04 → 07
                      ↓
③ Vấn đề khó        05 → 06 → 10
                      ↓
④ Bảo mật/Vận hành  08 → 09
                      ↓
⑤ Làm project       11 (tra cứu) → 12 (checklist)
                      ↓
⑥ Ôn thi/phỏng vấn  CAU-HOI-PHONG-VAN.md
```

**Nếu gấp (ôn thi/phỏng vấn):** đọc thẳng [CÂU HỎI PHỎNG VẤN](CAU-HOI-PHONG-VAN.md), chỗ nào chưa chắc thì lần theo link về file gốc.

---

## 🏗️ Kiến trúc project

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

   Hạ tầng: Eureka (8761) · Kafka (9092) · Redis (6379)
            Keycloak (8080) · Kibana (5601) · Elasticsearch (9200)
```

### Bảng port

| Thành phần | Port |
|---|---|
| Order Service | `8080`, `8181` |
| Product Service | `8887`, `8888`, `8889` |
| Eureka | `8761` |
| Kafka | `9092` · UI `8081` |
| Redis | `6379` · Insight `8001` |
| Keycloak | `8080` ⚠️ *(đụng Order — cần đổi)* |
| Elasticsearch | `9200`, `9300` |
| Logstash | `5044`, `5000` |
| Kibana | `5601` |

---

## 🧰 Công cụ

| Công cụ | Dùng để |
|---|---|
| **draw.io** | Vẽ kiến trúc — bắt buộc |
| IntelliJ IDEA | Code |
| Docker Compose | Dựng Kafka, Redis, Keycloak, ELK |
| Postman | Test API |

---

## ⚠️ Ghi chú về nguồn

| Buổi | Nguồn |
|---|---|
| 1–4, 6–9, 13–14 | Transcript chữ |
| **5, 10, 11, 12** | **Khôi phục từ bản ghi video** — đã đánh dấu rõ trong file |

Transcript thô của 4 buổi khôi phục lưu tại `buoi*-transcript-raw.txt` để đối chiếu.

**Vài chỗ đã đính chính so với lời giảng** (ghi rõ tại chỗ trong tài liệu):
- **Base64 không phải mã hóa** — nó là encoding, không có key
- **Cách tránh deadlock** bằng sort thứ tự lock
- **Message key trong Kafka** để giữ ordering khi có nhiều partition
# order-management-msa
