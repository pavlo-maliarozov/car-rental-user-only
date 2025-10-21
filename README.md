# 🚗 Car Rental System — User-Only (Java 21, Spring Boot 3)

A simplified **Car Rental** backend focused on **user flows**:
- Sign up & log in (JWT)
- Check availability by car type, date, and duration
- Create, edit, cancel reservations
- View “my” reservations

Built with **Java 21**, **Spring Boot 3.3**, **Hibernate (JPA)**, **PostgreSQL**, **Redis** (cache), **Flyway**, and **JUnit/Mockito/MockMvc**.

---

## 🧩 Tech Stack

- **Language:** Java 21  
- **Framework:** Spring Boot 3.3  
- **Build:** Maven  
- **DB:** PostgreSQL (prod), H2 (tests)  
- **ORM:** Hibernate (JPA)  
- **Migrations:** Flyway  
- **Cache:** Redis (availability caching)  
- **Auth:** JWT (Spring Security)  
- **Tests:** JUnit 5, Mockito, MockMvc

---

## 🧠 Domain Model

- **User** — registered user (email, password hash)  
- **Reservation** — booking with `carType`, `startAt`, `endAt`, `days`, `status` (`CONFIRMED`, `CANCELLED`)  
- **Capacity** — available quantity per `CarType`  
- **CarType** — enum: `SEDAN`, `SUV`, `VAN`
  - Robust parsing: accepts `"sedan"`, `"SEDAN"`, etc. via `CarType.from(String)`
  - JSON serializes to lower-case code (`"sedan"`, `"suv"`, `"van"`)

---

## 🌐 REST API (User-Only)

> All endpoints under `/api/**` (except `/api/auth/**`) require a JWT:  
> `Authorization: Bearer <token>`

### Auth
- **POST** `/api/auth/signup` — Register a new user. Returns JWT token.  
  Body: `{ "email": "user@example.com", "password": "pw" }`
- **POST** `/api/auth/login` — Log in and receive JWT token.  
  Body: `{ "email": "user@example.com", "password": "pw" }`

### Availability
- **GET** `/api/availability?carType=sedan&startAt=2025-10-22T10:00:00Z&days=2`  
  Returns `{ carType, startAt, days, available }`  
  - `carType`: case-insensitive string (`sedan|suv|van`)  
  - `startAt`: ISO-8601 instant (`Instant`)  
  - `days`: integer ≥ 1  
  - Capacity = seeded values − overlapping **CONFIRMED** reservations

### Reservations
- **POST** `/api/reservations` — Create a new reservation.  
  Body: `{ "carType": "SEDAN", "startAt": "2025-10-22T10:00:00Z", "days": 2 }`
- **PUT** `/api/reservations/{id}` — Edit an existing reservation.  
  Body: `{ "carType": "SEDAN", "startAt": "2025-10-25T10:00:00Z", "days": 1 }`
- **DELETE** `/api/reservations/{id}` — Cancel a reservation (idempotent).
- **GET** `/api/reservations/my` — List current user’s reservations.

**Validation & rules:**
- `startAt` must be in the future; `days ≥ 1`
- Cannot edit a `CANCELLED` reservation
- Overlap conflict → **409 Conflict**
- Availability cached in Redis; cache disabled in tests

---

## ⚙️ Setup & Run

### Requirements
- Java 21, Maven 3.9+
- (Optional) Docker for Postgres/Redis

### 1️⃣ Clone repository
```bash
git clone <your-repo-url>.git
cd <project-folder>
```

### 2️⃣ Start dependencies (optional)
```bash
docker compose -f docker/docker-compose.yml up -d
```
Runs:
- PostgreSQL → `localhost:5432`
- Redis → `localhost:6379`

### 3️⃣ Configure (optional)
Adjust credentials in `src/main/resources/application.yml` if needed.

### 4️⃣ Build & run
```bash
mvn clean package
mvn spring-boot:run
```
App runs at: `http://localhost:8080`

---

## 🧪 Tests

### Run all tests
```bash
mvn -Dspring.profiles.active=test test
```
Test profile:
- H2 in-memory DB (`create-drop`)
- Flyway & Redis disabled
- `@Sql` seeds capacities before tests

Integration tests cover:
- Signup → login → availability (valid/invalid types)
- Reservation creation (user1) → conflict (user2)
- Edit & cancel flow

Run a specific test:
```bash
mvn -Dtest=IntegrationFlowTest test
```

---

## 🧰 Project Structure

```
src/
 ├─ main/
 │   ├─ java/com/example/rental/
 │   │   ├─ controller/        # REST controllers
 │   │   ├─ service/           # Business logic
 │   │   ├─ model/             # Entities & enums
 │   │   ├─ repository/        # JPA Repositories
 │   │   ├─ dto/               # Request/Response DTOs
 │   │   └─ config/            # Security, JWT, Cache
 │   └─ resources/
 │       ├─ application.yml
 │       └─ db/migration/      # Flyway migrations
 └─ test/
     ├─ java/com/example/rental/
     │   ├─ IntegrationFlowTest.java
     │   └─ ReservationServiceTest.java
     └─ resources/
         └─ application.yml
```

---

## 🧪 Example cURL Usage

```bash
# Signup
curl -sX POST http://localhost:8080/api/auth/signup   -H "Content-Type: application/json"   -d '{"email":"u1@example.com","password":"pw"}'

# Login (get token)
TOKEN=$(curl -sX POST http://localhost:8080/api/auth/login   -H "Content-Type: application/json"   -d '{"email":"u1@example.com","password":"pw"}' | jq -r .token)

# Check availability
START=$(date -u -d "+1 hour" +"%Y-%m-%dT%H:%M:%SZ")
curl -sG http://localhost:8080/api/availability   -H "Authorization: Bearer $TOKEN"   --data-urlencode "carType=sedan"   --data-urlencode "startAt=$START"   --data-urlencode "days=2"

# Create reservation
curl -sX POST http://localhost:8080/api/reservations   -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json"   -d "{"carType":"SEDAN","startAt":"$START","days":2}"

# List my reservations
curl -s http://localhost:8080/api/reservations/my   -H "Authorization: Bearer $TOKEN"
```

---

## 📝 Notes

- `CarType` is **case-insensitive** (`sedan`, `SUV`, `van`).
- If you see parameter name errors, ensure:
  - `@RequestParam("startAt")` is used, or
  - Maven compiler uses `-parameters` flag.
- Cache key SpEL is null-safe.
- Flyway seeds initial capacities in production.
- Integration tests self-seed with `@Sql`.

---

## 📄 License

MIT © 2025 — Your Team Name
