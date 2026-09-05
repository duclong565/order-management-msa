# 11 — Spring Boot Cheatsheet

> Tổng hợp mọi thứ liên quan đến code: setup project, entity, DTO, mapper, config, các lỗi hay gặp.
> 📎 Nguồn: buổi 2, 4, 6, 7

---

## 1. Khởi tạo project

| Mục | Giá trị |
|---|---|
| Build | Maven |
| Java | 21 |
| Spring Boot | **3.5.x** — ⚠️ không dùng bản mới nhất, chưa stable |

**Dependencies cơ bản:** Spring Web, Lombok, Spring Data JPA, MySQL Driver, Validation.

> ⚠️ *"Hạn chế dùng version mới nhất, bởi vì nó sẽ không stable."*

### Cấu trúc package (số nhiều)

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
├── consumers/
├── event/
└── exceptions/
```

---

## 2. BaseEntity & JPA Auditing

### BaseEntity

```java
@Getter
@Setter
@MappedSuperclass                                  // ① BẮT BUỘC cho kế thừa
@EntityListeners(AuditingEntityListener.class)     // ② lắng nghe sự kiện auditing
public class BaseEntity {

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @CreatedDate       @Column(name = "created_at")        private Instant createdAt;
    @CreatedBy         @Column(name = "created_by")        private String  createdBy;
    @LastModifiedDate  @Column(name = "last_modified_at")  private Instant lastModifiedAt;
    @LastModifiedBy    @Column(name = "last_modified_by")  private String  lastModifiedBy;
}
```

> ⚠️ Thiếu `@MappedSuperclass` → các cột kế thừa **không** được map. Đây là lỗi mất nhiều thời gian debug nhất.

### Bật JPA Auditing

```java
@SpringBootApplication
@EnableJpaAuditing                       // ③
public class ProductServiceApplication { ... }
```

### AuditorAware — nói cho JPA biết "ai" đang thao tác

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
            // Khi tích hợp Spring Security:
            // return Optional.ofNullable(SecurityContextHolder.getContext())
            //         .map(SecurityContext::getAuthentication)
            //         .filter(Authentication::isAuthenticated)
            //         .map(Authentication::getName);

            return Optional.of("huan.nguyen");   // tạm hardcode
        }
    }
}
```

> 4 bước: `@MappedSuperclass` + `@EntityListeners` → annotation trên field → `@EnableJpaAuditing` → `AuditorAware` bean.
> `@CreatedDate`/`@CreatedBy` chỉ điền lần đầu; `@LastModified*` cập nhật mỗi lần update.

---

## 3. Entity

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
    private String categoryId;        // ⚠️ chỉ lưu ID, KHÔNG dùng @ManyToOne
}
```

### ⚠️ Vì sao KHÔNG dùng `@OneToMany` / `@ManyToOne`

1. Trả entity có quan hệ 2 chiều dễ **StackOverflow** khi serialize JSON
2. Handle không kỹ thì **hiệu năng rất tệ** (N+1 query)

> Nhiều dự án thực tế bỏ hẳn. Chỉ lưu ID trần.

---

## 4. Controller

```java
@RestController
@RequestMapping("/v1/products")     // ⚠️ BẮT BUỘC có version — chuẩn RESTful
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

> **RESTful là bộ quy ước, không phải bắt buộc kỹ thuật** — không tuân theo code vẫn chạy, nhưng nên follow.
> Version (`/v1`) là một trong các quy ước đó.

---

## 5. DTO

### BaseResponse
```java
@Getter @Setter @AllArgsConstructor
public class BaseResponse {
    private Object data;
    private String message;
    // thực tế còn có: code, timestamp, errors...
}
```

### Request DTO
```java
@Getter @Setter
public class CreateProductRequest {
    @NotBlank  private String name;
    @NotNull @Positive private Integer price;
    @NotNull @PositiveOrZero private Integer stock;
    @NotBlank  private String categoryId;
    // ⚠️ KHÔNG có id — id tự sinh, client không được truyền
}
```

**Nguyên tắc đặt tên:** Request DTO phải **phản ánh đúng cái client cần truyền**, không tái dùng DTO to.

```java
// ❌ SAI — người đọc tưởng phải truyền đủ mọi trường của Product
public ResponseEntity<?> validate(@RequestBody List<ProductDTO> products)

// ✅ ĐÚNG
public ResponseEntity<?> validate(@RequestBody List<ProductValidateRequest> request)
```

> **Nguyên tắc:** theo lý thuyết không được trả entity trực tiếp ra API, phải bọc vào DTO.
> Thầy bỏ qua trong lớp cho gọn, nhưng đó là cách làm chuẩn.

---

## 6. Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public Product create(CreateProductRequest request) {
        // ⚠️ Validate phải lọc CẢ is_deleted
        boolean exists = categoryRepository
                .existsByIdAndIsDeletedFalse(request.getCategoryId());
        if (!exists) {
            throw new ApplicationException("Category not found");
        }

        Product product = productMapper.toProduct(request);
        return productRepository.save(product);
    }
}
```

> ⚠️ **Toàn bộ business logic tập trung ở tầng SERVICE.**
> Controller và Consumer chỉ gọi xuống service.

---

## 7. Repository

```java
public interface ProductRepository extends JpaRepository<Product, String> {
    // ⚠️ kiểu ID là String (UUID), không phải Long

    List<Product> findAllByIdIn(List<String> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)          // SELECT ... FOR UPDATE
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIdInForUpdate(@Param("ids") List<String> ids);
}
```

---

## 8. MapStruct

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toProduct(CreateProductRequest request);
    OrderCreatedEvent toEvent(Order order);
}
```

### MapStruct vs ModelMapper

| | MapStruct ← lớp dùng | ModelMapper |
|---|---|---|
| Cách hoạt động | **Sinh code Java lúc compile** | **Reflection lúc runtime** |
| Tốc độ | Nhanh | Chậm hơn |
| Phát hiện lỗi | Lúc **compile** | Chỉ khi **chạy** |

### ⚠️ pom.xml — annotation processor (rất hay lỗi)

Lombok + MapStruct dùng chung annotation processor → phải khai đúng thứ tự, **Lombok trước, MapStruct sau**, kèm `lombok-mapstruct-binding`:

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

**Lỗi hay gặp:** `Parameter N of constructor ... required a bean of type '...Mapper'`
→ MapStruct chưa sinh implementation → sai config annotation processor.

---

## 9. application.yml — tổng hợp

```yaml
server:
  port: 8888                                  # mỗi service một port

spring:
  application:
    name: product-service                     # tên để Eureka nhận diện

  datasource:
    url: jdbc:mysql://localhost:3306/product_db
    username: root
    password: 123456789

  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true                            # bật để verify SELECT ... FOR UPDATE

  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
    consumer:
      group-id: product-service
      auto-offset-reset: latest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      properties:
        spring.json.trusted.packages: "*"

  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
  instance:
    prefer-ip-address: true

logging:
  level:
    org.springframework.cache: TRACE           # xem log cache hit/miss
```

---

## 10. Tổng hợp Annotation

### Core
| Annotation | Dùng để |
|---|---|
| `@SpringBootApplication` | Điểm khởi động |
| `@RestController` + `@RequestMapping("/v1/...")` | Controller |
| `@Service` / `@Component` / `@Configuration` | Đăng ký bean |
| `@RequiredArgsConstructor` | Lombok — inject qua constructor |
| `@Slf4j` | Lombok — tạo `log` |
| `@Primary` | Khi có 2 bean cùng kiểu, ưu tiên bean này |

### JPA
| Annotation | Dùng để |
|---|---|
| `@Entity` + `@Table(name="...")` | Entity |
| `@MappedSuperclass` | Class cha cho entity kế thừa |
| `@EntityListeners(AuditingEntityListener.class)` | Lắng nghe auditing |
| `@EnableJpaAuditing` | Bật auditing |
| `@CreatedDate` `@CreatedBy` `@LastModifiedDate` `@LastModifiedBy` | 4 cột audit |
| `@Lock(LockModeType.PESSIMISTIC_WRITE)` | `SELECT ... FOR UPDATE` |
| `@Version` | Optimistic lock |

### Kafka
| Annotation | Dùng để |
|---|---|
| `@KafkaListener(topics = "...")` | Consumer |
| `@RetryableTopic(...)` | Retry non-blocking + DLT |

### Cache
| Annotation | Dùng để |
|---|---|
| `@EnableCaching` | Bật cache |
| `@Cacheable(value, key, condition)` | Đọc từ cache |
| `@CacheEvict(value, allEntries)` | Xóa cache |
| `@CachePut` | Chạy hàm rồi ghi đè cache |

### Spring Cloud
| Annotation | Dùng để |
|---|---|
| `@EnableEurekaServer` | Eureka Server |
| `@LoadBalanced` | Cho `WebClient.Builder` biết hỏi Eureka và phân tải |

### Jackson
| Annotation | Dùng để |
|---|---|
| `@JsonIgnoreProperties(ignoreUnknown = true)` | Bỏ qua trường thừa khi deserialize |

---

## 11. Danh sách LỖI hay gặp

| Lỗi | Nguyên nhân | Cách sửa |
|---|---|---|
| Cột auditing toàn `NULL` | Thiếu `@MappedSuperclass` | Thêm vào BaseEntity |
| `required a bean of type '...Mapper'` | Sai annotation processor | Config `pom.xml` + `mvn clean` |
| `Port 8080 is already in use` | Đụng port (Keycloak cũng 8080) | Đổi port |
| `The class is not in the trusted packages` | Spring Kafka chặn deserialize | `spring.json.trusted.packages: "*"` |
| Kafka consumer báo lỗi class không tồn tại | Header `__TypeId__` trỏ class của Producer | Nhận `String` + `ObjectMapper` |
| `UnrecognizedPropertyException` | JSON thừa trường | `@JsonIgnoreProperties(ignoreUnknown = true)` |
| `Connect timeout` khi gọi service | Dùng `WebClient.builder()` mới thay vì bean có `@LoadBalanced` | Inject bean |
| `UnknownHostException` (Windows) | Eureka đăng ký bằng hostname | `eureka.instance.prefer-ip-address: true` |
| Query trả về bản ghi đã xóa | Quên lọc `is_deleted = 0` | Thêm điều kiện |
| `406 Not Acceptable` (Postman) | Sai header `Content-Type`/`Accept` | Kiểm tra tab Headers |
| Elasticsearch không start | Máy thiếu RAM | Tăng RAM cho Docker |

---

## 12. Ghi chú khác

### Stored Procedure
> Thầy đi làm 5+ năm **gần như chưa từng thấy** dự án Java mới dùng procedure.
> Chỉ dùng khi học JDBC thuần. **Bỏ qua.**

### Chạy nhiều instance trong IntelliJ
```
Run → Edit Configurations → Copy Configuration
→ Đặt tên mới → thêm option: server.port=8887 → Apply
```

### Docker Compose
```bash
docker compose up -d      # khởi động
docker compose down       # dừng và xóa container
docker ps                 # xem container ĐANG CHẠY
docker ps -a              # xem TẤT CẢ container (kể cả đã chết)
```

---

## 📚 Đọc tiếp

- [02 — Thiết kế Database](02-thiet-ke-database.md)
- [04 — Kafka](04-kafka.md)
- [06 — Caching](06-caching.md)
