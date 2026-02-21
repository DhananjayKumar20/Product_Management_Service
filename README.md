# 📦 Product Module API

A robust, production-ready RESTful backend service for managing **Products and Items** with JWT-based authentication, role-based access control, pagination support, and full Swagger documentation — built with Spring Boot and PostgreSQL.

---

## 📑 Table of Contents

- [Project Overview](#-project-overview)
- [Tech Stack](#️-tech-stack)
- [Features](#-features)
- [Architecture](#️-architecture)
- [Setup Instructions](#-setup-instructions)
- [Docker Setup](#-docker-setup)
- [API Documentation](#-api-documentation)
- [Sample Request & Response Payloads](#-sample-request--response-payloads)
- [Database Schema](#️-database-schema)
- [Security](#-security)
- [Error Handling](#️-error-handling)
- [Contributing](#-contributing)

---

## 🎯 Project Overview

The **Product Module API** is a Spring Boot application designed to manage products and their associated items with enterprise-grade security and clean RESTful design. The system provides:

- 🔐 **JWT Authentication** with secure token-based login
- 🛡️ **Role-Based Access Control** (ADMIN / USER)
- 📦 **Product & Item Management** — full CRUD operations
- 📄 **Pagination & Sorting** on all collection endpoints
- 📖 **Swagger/OpenAPI** documentation
- 🐳 **Docker & Docker Compose** support for containerized deployment

### 🔑 Key Capabilities

- Secure user registration and login with JWT
- Create, read, update, and delete products
- Each product has one associated item with quantity
- Paginated product listing with newest-first sorting
- Standardized JSON error responses
- Input validation using Jakarta Validation
- Database migration support via Flyway

---

## 🛠️ Tech Stack

### Language & Runtime
- **Java 17+**

### Frameworks & Libraries
| Technology | Purpose |
|---|---|
| Spring Boot 3.x | Core application framework |
| Spring Data JPA (Hibernate) | ORM & database operations |
| Spring Security | Authentication & authorization |
| Spring Web (MVC) | RESTful API development |
| Spring Validation | Input validation (Jakarta) |
| Lombok | Boilerplate reduction |
| JJWT (0.11.5) | JWT token generation & validation |
| Flyway | Database migration management |
| Springdoc OpenAPI 2.x | Swagger UI & API docs |

### Database
- **PostgreSQL** (Primary)
- **MySQL** (Alternative)

### DevOps & Tools
- **Docker & Docker Compose** — Containerization
- **Maven** — Build tool
- **Postman** — API testing
- **IntelliJ IDEA** — IDE
- **Git / GitHub** — Version control

---

## ✨ Features

### 🔑 Authentication
- User registration and login
- JWT access token generation
- Stateless session management
- Role-based endpoint protection (ADMIN / USER)

### 📦 Product Management
- Create a new product with item quantity
- Retrieve a single product by ID
- Update product name and item quantity (preserves existing item ID)
- Delete a product
- Paginated list of all products (sorted by newest first)

### 🔩 Item Management
- Each product has one item (single quantity object)
- Retrieve item by product ID
- Item ID preserved on update (no new IDs created)

### 📄 Pagination & Sorting
- `page` parameter (default: 0)
- `size` parameter (default: 10)
- Auto-sorted by `id DESC` (newest first)

### 🛡️ Security
- All endpoints protected by JWT (except `/api/auth/**`)
- Role-based access: ADMIN and USER roles
- Stateless architecture (no sessions)

---

## 🏗️ Architecture

### System Architecture Diagram

```
┌──────────────────────────────────────────────────┐
│               REST Client (Postman / UI)          │
└───────────────────────┬──────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────┐
│            🔐 JWT Authentication Filter           │
│         (Validates Bearer Token on each request)  │
└───────────────────────┬──────────────────────────┘
                        │
           ┌────────────┴────────────┐
           ▼                         ▼
┌─────────────────────┐   ┌─────────────────────────┐
│   AuthController    │   │    ProductController     │
│  - /api/auth/login  │   │  - Request validation    │
│  - /api/auth/signup │   │  - Response formatting   │
│  - JWT generation   │   │  - Pagination params     │
└────────┬────────────┘   └────────────┬────────────┘
         │                             │
         ▼                             ▼
┌─────────────────────┐   ┌─────────────────────────┐
│   AuthService       │   │   ProductServiceImpl     │
│  - User validation  │   │  - Business logic        │
│  - Token creation   │   │  - Pagination & sorting  │
│  - Role assignment  │   │  - DTO conversions       │
└────────┬────────────┘   └────────────┬────────────┘
         │                             │
         ▼                      ┌──────┴──────┐
┌─────────────────────┐         ▼             ▼
│   UserRepository    │  ┌────────────┐ ┌────────────┐
│   - JPA CRUD        │  │ProductRepo │ │  ItemRepo  │
└────────┬────────────┘  │ - JPA CRUD │ │ - JPA CRUD │
         │               └────────────┘ └────────────┘
         │                      │             │
         └──────────────────────┼─────────────┘
                                ▼
                    ┌───────────────────────┐
                    │  PostgreSQL / MySQL   │
                    │     Database          │
                    └───────────────────────┘
```

### Layer Responsibilities

**Controller Layer**
- HTTP request/response handling
- Input validation trigger
- Pagination parameter binding
- API endpoint mapping (`/api/v1/`)

**Service Layer**
- Core business logic
- Pageable creation with Sort
- Entity ↔ DTO conversion via Mapper
- Transaction management

**Repository Layer**
- JPA entity CRUD operations
- Spring Data `PagingAndSortingRepository`
- Query execution

**Security Layer**
- JWT filter on every request
- Role extraction from token
- User loading from DB on each request

---

## 🚀 Setup Instructions

### Prerequisites

- JDK 17 or higher
- Maven 3.6+
- PostgreSQL 14+
- Git
- Docker (optional but recommended)

### 1. Clone the Repository

```bash
git clone https://github.com/DhananjayKumar20/Product_Management_Service.git -b master .
cd product-module-api
```

### 2. Configure Database

Update `src/main/resources/application.properties`:

```properties
# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/product
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Flyway
spring.flyway.enabled=false

# JWT
jwt.secret=your_jwt_secret_key_here
jwt.expiration-ms=28800000

# Swagger
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
```

### 3. Create Database

```sql
CREATE DATABASE product;
```

### 4. Build & Run

```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

### 5. Verify

```
http://localhost:8080/swagger-ui/index.html
```

---

## 🐳 Docker Setup

### Docker Compose (Recommended)

```bash
# Start all services
cd path/to/project
docker compose up --build

# Stop all services
docker compose down -v
```

### Services Started

| Service | Port | Description |
|---|---|---|
| `springboot` | `8080` | Spring Boot Application |
| `postgres_db` | `5432` | PostgreSQL Database |

### Environment Variables (docker-compose.yml)

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/product
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: your_password
SPRING_PROFILES_ACTIVE: docker
```

---

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api/v1
```

### Swagger UI
```
http://localhost:8080/swagger-ui/index.html
```

### Authentication Header
```
Authorization: Bearer <your_jwt_token>
```

---

### 📋 Endpoints Overview

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/signup` | ❌ Public | Register new user |
| `POST` | `/api/auth/login` | ❌ Public | Login & get JWT token |
| `GET` | `/api/v1/products` | ✅ JWT | Get all products (paginated) |
| `GET` | `/api/v1/products/{id}` | ✅ JWT | Get product by ID |
| `POST` | `/api/v1/products` | ✅ JWT | Create new product |
| `PUT` | `/api/v1/products/{id}` | ✅ JWT | Update product |
| `DELETE` | `/api/v1/products/{id}` | ✅ JWT | Delete product |
| `GET` | `/api/v1/products/{id}/items` | ✅ JWT | Get item by product ID |

---

## 📝 Sample Request & Response Payloads

### 🔐 1. Register User (Signup)

**Endpoint:** `POST /api/auth/signup`

**Request Body:**
```json
{
  "name": "viksssddsa",
  "email": "dhsadssdsns@example.com",
  "password": "123",
  "userType": "ADMIN"
}
```

**Response:** `200 OK`
```json
{
  "id": 3,
  "email": "dhsadssdsns@example.com",
  "name": "viksssddsa",
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkaHNhZHNzZHNuc0BleGFtcGxlLmNvbSIsInVzZXJJZCI6MywiaWF0IjoxNzcxNjQxODcxLCJleHAiOjE3NzE2NzA2NzF9.fO2tNEvJw7Gfq6R9GWiNpgAanuLNiTALQoBn7C_pA1QTAToUnMdK1tYvX5lFioxz4PiAje6tw2VjyHGXyvNBIg",
  "message": "Signup successful"
}
```

> ✅ Signup successful.

---

### 🔐 2. Login

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "dhsadssdsns@example.com",
  "password": "123"
}
```

**Response:** `200 OK`
```json
{
  "id": 3,
  "email": "dhsadssdsns@example.com",
  "name": "viksssddsa",
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkaHNhZHNzZHNuc0BleGFtcGxlLmNvbSIsInVzZXJJZCI6MywiaWF0IjoxNzcxNjQyMjEzLCJleHAiOjE3NzE2NzEwMTN9._sxhVR0LmY-GiuoyUMVZq5cFMydCIG5kwHwycYvz19JvRqiumnty3gs1nTqO3nBHKSTF5O5NFqnuD5fQllIAsQ",
  "message": "Login successful"
}
```

> ⚠️ Copy the `token` value and use it as `Bearer <token>` in the `Authorization` header for all protected endpoints.

---

### 📦 3. Create Product

**Endpoint:** `POST /api/v1/products`

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "productName": "Pad",
  "items": {
    "quantity": 50
  }
}
```

**Response:** `200 OK`
```json
{
  "id": 7,
  "productName": "Pad",
  "createdBy": 1,
  "createdOn": "2026-02-21T01:47:22.6518124",
  "modifiedBy": null,
  "modifiedOn": null,
  "items": {
    "id": 9,
    "quantity": 50
  }
}
```

---

### 📋 4. Get All Products (Paginated)

**Endpoint:** `GET /api/v1/products?page=0&size=10`

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-indexed) |
| `size` | `10` | Records per page |

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 7,
      "productName": "Pad",
      "createdBy": 1,
      "createdOn": "2026-02-21T01:47:22.651812",
      "modifiedBy": null,
      "modifiedOn": null,
      "items": {
        "id": 9,
        "quantity": 50
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "empty": false
}
```

---

### 🔍 5. Get Product by ID

**Endpoint:** `GET /api/v1/products/{id}`

**Example:** `GET /api/v1/products/7`

**Response:** `200 OK`
```json
{
  "id": 7,
  "productName": "Pad",
  "createdBy": 1,
  "createdOn": "2026-02-21T01:47:22.651812",
  "modifiedBy": null,
  "modifiedOn": null,
  "items": {
    "id": 9,
    "quantity": 50
  }
}
```

**Error Response:** `404 Not Found`
```json
{
  "status": 404,
  "message": "Product not found with id: 7",
  "timestamp": "2026-02-21T10:00:00.000Z"
}
```

---

### ✏️ 6. Update Product

**Endpoint:** `PUT /api/v1/products/{id}`

**Example:** `PUT /api/v1/products/7`

**Request Body:**
```json
{
  "productName": "Laptop Pad",
  "items": {
    "quantity": 10
  }
}
```

**Response:** `200 OK`
```json
{
  "id": 7,
  "productName": "Laptop Pad",
  "createdBy": 1,
  "createdOn": "2026-02-21T01:47:22.651812",
  "modifiedBy": 1,
  "modifiedOn": "2026-02-21T02:15:30.123456",
  "items": {
    "id": 9,
    "quantity": 10
  }
}
```

> ✅ **Note:** Item `id: 9` remains the same — only `quantity` is updated. No new item ID is created.

---

### 🗑️ 7. Delete Product

**Endpoint:** `DELETE /api/v1/products/{id}`

**Example:** `DELETE /api/v1/products/7`

**Response:** `200 OK`
```json
{
  "message": "Product deleted successfully"
}
```

---

### 🔩 8. Get Item by Product ID

**Endpoint:** `GET /api/v1/products/{id}/items`

**Example:** `GET /api/v1/products/7/items`

**Response:** `200 OK`
```json
{
  "id": 9,
  "quantity": 50
}
```

---

## 🗄️ Database Schema

### product Table

```sql
CREATE TABLE product (
  id          SERIAL PRIMARY KEY,
  product_name VARCHAR(255) NOT NULL,
  created_by  INT          NOT NULL,
  created_on  TIMESTAMP    NOT NULL,
  modified_by INT,
  modified_on TIMESTAMP
);
```

| Column | Type | Description |
|---|---|---|
| `id` | SERIAL | Auto-generated primary key |
| `product_name` | VARCHAR(255) | Name of the product |
| `created_by` | INT | User ID who created the product |
| `created_on` | TIMESTAMP | Creation timestamp |
| `modified_by` | INT | User ID who last updated (nullable) |
| `modified_on` | TIMESTAMP | Last update timestamp (nullable) |

---

### item Table

```sql
CREATE TABLE item (
  id         SERIAL PRIMARY KEY,
  product_id INT NOT NULL,
  quantity   INT NOT NULL,
  FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);
```

| Column | Type | Description |
|---|---|---|
| `id` | SERIAL | Auto-generated primary key |
| `product_id` | INT | Foreign key to product table |
| `quantity` | INT | Quantity of the item |

**Relationship:** One `Product` → One `Item` (OneToMany mapped, single item used)

---

### users Table

```sql
CREATE TABLE users (
  id       SERIAL PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  email    VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role     VARCHAR(50)  NOT NULL
);
```

---

## 🔒 Security

### JWT Authentication Flow

```
1. Client sends POST /api/auth/login with credentials
2. Server validates credentials from DB
3. Server generates JWT token (HS256)
4. Client stores token and sends it in every request
   → Authorization: Bearer <token>
5. JwtAuthenticationFilter validates token on each request
6. If valid → request proceeds; if invalid → 401 Unauthorized
```

### JWT Configuration

```properties
jwt.secret=your_256bit_secret_key
jwt.expiration-ms=28800000   # 8 hours
```

### Role-Based Access

| Role | Access |
|---|---|
| `ADMIN` | Full access to all endpoints |
| `USER` | Read-only or limited access |
| `Public` | `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**` |

---

## ⚠️ Error Handling

### Standard Error Response Format

```json
{
  "status": 404,
  "message": "Product not found with id: 7",
  "timestamp": "2026-02-21T10:00:00.000Z"
}
```

### HTTP Status Codes

| Status | Description | When Used |
|---|---|---|
| `200 OK` | Success | GET, PUT, DELETE successful |
| `201 Created` | Resource created | POST successful |
| `400 Bad Request` | Validation failure | Invalid request body |
| `401 Unauthorized` | Auth failed | Missing/invalid JWT token |
| `403 Forbidden` | Access denied | Insufficient role |
| `404 Not Found` | Not found | Product/Item ID doesn't exist |
| `500 Internal Server Error` | Server error | Unexpected exceptions |

### Common Error Scenarios

**Missing or expired JWT:**
```json
{
  "status": 401,
  "message": "JWT token is expired or invalid",
  "timestamp": "2026-02-21T10:00:00.000Z"
}
```

**Product not found:**
```json
{
  "status": 404,
  "message": "Product not found with id: 99",
  "timestamp": "2026-02-21T10:00:00.000Z"
}
```

**Validation error:**
```json
{
  "status": 400,
  "message": "productName: must not be blank",
  "timestamp": "2026-02-21T10:00:00.000Z"
}
```

---

## 🤝 Contributing

Contributions are welcome!

### Steps to Contribute

1. **Fork the Repository**
```bash
git clone https://github.com/DhananjayKumar20/Product_Management_Service.git -b master .
cd product-module-api
```

2. **Create a Feature Branch**
```bash
git checkout -b feature/your-feature-name
```

3. **Make Changes & Commit**
```bash
git add .
git commit -m "feat: add your feature description"
```

4. **Push & Create Pull Request**
```bash
git push origin feature/your-feature-name
```

### Code Guidelines
- Follow Spring Boot best practices
- Use meaningful variable/method names
- Add validation for all input fields
- Handle all exceptions with proper HTTP status codes
- Write clean, readable code with comments where needed

---

## 📄 License

This project is licensed under the **MIT License** — see the LICENSE file for details.

---

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- PostgreSQL community for robust database support
- Zest India IT Pvt Ltd for the assignment opportunity

---

**📅 Last Updated:** February 21, 2026
**🔖 Version:** 1.0.0
**👨‍💻 Author:** Dhananjay Kumar
