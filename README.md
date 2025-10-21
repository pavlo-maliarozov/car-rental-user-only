# Car Rental — User-Only Simplified (IntelliJ-ready Maven project)

Focuses on user flows:
- Signup / Login (JWT)
- Check availability (case-insensitive `carType` with `CarType.from`)
- Create / Edit / Cancel reservations
- List "my" reservations

Capacity per type lives in `capacities` (seeded via Flyway: SEDAN=1, SUV=1, VAN=1).

## Run locally
```bash
docker compose -f docker/docker-compose.yml up -d  # optional
mvn spring-boot:run
```

## Run tests
```bash
mvn -Dspring.profiles.active=test test
```

Notes:
- Tests use H2 and disable Flyway/caching.
- Availability endpoint accepts `carType` string; invalid types return HTTP 400 with message.
