# TripBook

![CI](https://github.com/JustAnyoone/tripbook-api/actions/workflows/ci.yml/badge.svg?branch=portfolio)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen)
![License](https://img.shields.io/badge/License-MIT-yellow)

REST API for managing trip bookings: passengers, destinations and the trips that connect them. Built with Spring Boot, Spring Data JPA and MySQL.

The project started as a university assignment (Object-Oriented Programming, CEUB, tag `v1.0-entrega`) and is being evolved as a portfolio project: DTOs, pagination, automated tests and continuous integration are already in place, and a React front-end is included, and authentication is next.

## Features

- Full CRUD for passengers, destinations and trips
- Each trip links one passenger to one destination
- Business rules: the return date cannot be before the departure date, and the passenger and destination must exist
- A destination that still has trips cannot be deleted; deleting a passenger also deletes their trips
- Pagination, sorting and filtering on every list endpoint
- Input validation with clear error messages and a single, consistent error format
- Interactive API documentation with Swagger UI
- React + TypeScript front-end with paginated, filterable and sortable tables and forms for every resource
- Automated tests on both sides (back-end: unit, web layer, repository and end-to-end; front-end: components and API client) running on every push through GitHub Actions

## Tech stack

- Java 17
- Spring Boot 4 (Web MVC, Data JPA, Validation)
- Hibernate / JPA, MySQL 8
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ, MockMvc, H2 (tests only)
- React 19, TypeScript, Vite, React Router
- Vitest and Testing Library
- Maven, npm, GitHub Actions

## Architecture

Layered structure under the `com.tt.tripbook` package:

| Package      | Responsibility                                                    |
|--------------|-------------------------------------------------------------------|
| `controller` | HTTP endpoints and API documentation                              |
| `dto`        | Request and response records, so entities are never exposed       |
| `service`    | Business rules and transactions                                   |
| `repository` | Data access with Spring Data JPA                                  |
| `model`      | JPA entities and relationships                                    |
| `exception`  | Custom exceptions and the global error handler                    |

```
Client -> Controller -> Service -> Repository -> Hibernate/JPA -> MySQL
              |
             DTOs (validated on the way in, mapped on the way out)
```

## Data model

| Entity        | Main attributes                                              | Relationships                                |
|---------------|--------------------------------------------------------------|----------------------------------------------|
| `Passenger`   | name, documentNumber (unique), email, birthDate              | One passenger has many trips                 |
| `Destination` | city, country, description                                   | One destination is used by many trips        |
| `Trip`        | departureDate, returnDate, price, status                     | Belongs to one passenger and one destination |

Modeling notes:

- `Trip` owns the foreign keys `passenger_id` and `destination_id` (`@ManyToOne` + `@JoinColumn`).
- Removing a passenger removes their trips (`cascade` and `orphanRemoval`), because a trip cannot exist without its passenger.
- A destination with trips cannot be removed (HTTP 409). The foreign key enforces the same rule at the database level.
- `status` accepts only `PENDING`, `CONFIRMED` or `CANCELLED`.
- `price` is a `BigDecimal`, which is the right type for money.

## Getting started

### Prerequisites

- Java 17 or later
- MySQL 8 running on port 3306 (not needed to run the tests)

### Run the application

1. Clone the repository:

```bash
git clone https://github.com/JustAnyoone/tripbook-api.git
cd tripbook-api
```

2. Set the MySQL password. The application reads the `DB_PASSWORD` environment variable and falls back to `root` when it is not set.

```bash
# Linux / macOS
export DB_PASSWORD="your_password"
```

```powershell
# Windows (PowerShell); reopen the terminal afterwards
setx DB_PASSWORD "your_password"
```

3. Start the application:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell use `.\mvnw spring-boot:run`.

The `tripbook` database and its tables are created automatically on the first run (`createDatabaseIfNotExist=true` and `ddl-auto=update`).

### Configuration

Settings live in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tripbook?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:root}
spring.jpa.hibernate.ddl-auto=update

# Pagination: never return more than 100 items per page
spring.data.web.pageable.max-page-size=100

# Origins allowed to call the API from a browser (default: the Vite dev server)
app.cors.allowed-origins=http://localhost:5173
```

### Run the front-end

The front-end lives in `frontend/` and needs Node.js 22 or later. With the API running on port 8080:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. In development Vite forwards `/api` to `http://localhost:8080`, so nothing else needs configuring.

To serve the front-end from another origin, set `VITE_API_URL` when building it, and list that origin in `app.cors.allowed-origins` (comma separated) on the API. The default is `http://localhost:5173`.

Other scripts: `npm test` runs the tests and `npm run build` type-checks and builds the production bundle.

## API documentation

With the application running, open Swagger UI:

http://localhost:8080/swagger-ui/index.html

It lists every endpoint with its request and response formats and lets you try them in the browser. The OpenAPI document is at `http://localhost:8080/v3/api-docs`.

## Endpoints

| Resource     | Base route         | Operations                          |
|--------------|--------------------|-------------------------------------|
| Passengers   | `/api/passengers`  | GET (page), GET by id, POST, PUT, DELETE |
| Destinations | `/api/destinations`| GET (page), GET by id, POST, PUT, DELETE |
| Trips        | `/api/trips`       | GET (page), GET by id, POST, PUT, DELETE |

Additional queries:

| Route                                | Description                          |
|--------------------------------------|--------------------------------------|
| `GET /api/trips/passenger/{id}`      | Trips of one passenger (paginated)   |
| `GET /api/trips/destination/{id}`    | Trips to one destination (paginated) |

### Pagination, sorting and filtering

Every list endpoint accepts `page` (starting at 0), `size` (default 10, maximum 100) and `sort` (for example `sort=city,desc`). Each one also has a filter:

| Endpoint                | Filter                                                    | Default order                         |
|-------------------------|-----------------------------------------------------------|---------------------------------------|
| `GET /api/destinations` | `?country=` (partial match, case-insensitive)             | `city` ascending                      |
| `GET /api/passengers`   | `?name=` (partial match, case-insensitive)                | `name` ascending                      |
| `GET /api/trips`        | `?status=` (`PENDING`, `CONFIRMED` or `CANCELLED`)        | `departureDate`, newest first         |

```
GET /api/destinations?country=port&page=0&size=2&sort=city,desc
```

```json
{
  "content": [
    { "id": 2, "city": "Porto", "country": "Portugal", "description": null },
    { "id": 1, "city": "Lisbon", "country": "Portugal", "description": "Portuguese capital" }
  ],
  "page": 0,
  "size": 2,
  "totalElements": 2,
  "totalPages": 1,
  "last": true
}
```

An unknown `sort` property or an unknown trip `status` returns `400`.

### Examples

Create a destination (`POST /api/destinations`):

```json
{
  "city": "Lisbon",
  "country": "Portugal",
  "description": "Portuguese capital"
}
```

Create a passenger (`POST /api/passengers`):

```json
{
  "name": "Ana Souza",
  "documentNumber": "AB123456",
  "email": "ana@email.com",
  "birthDate": "1998-05-20"
}
```

Create a trip (`POST /api/trips`). Send only the ids of the passenger and the destination:

```json
{
  "departureDate": "2026-12-20",
  "returnDate": "2026-12-30",
  "price": 3500.50,
  "status": "CONFIRMED",
  "passengerId": 1,
  "destinationId": 1
}
```

The response embeds the full passenger and destination.

## Error handling

Every error uses the same format:

```json
{
  "status": 404,
  "message": "Destination not found: id 999",
  "timestamp": "2026-10-09T21:00:00"
}
```

| Status | When it happens                                                                          |
|--------|------------------------------------------------------------------------------------------|
| 201    | Record created                                                                           |
| 204    | Record deleted                                                                           |
| 400    | Invalid data, unknown status or sort property, or return date before departure date      |
| 404    | Trip, passenger or destination not found                                                 |
| 409    | Document number already registered, or deleting a destination that still has trips       |

## Testing

### Back-end

```bash
./mvnw test
```

The tests do not need MySQL: they run against an in-memory H2 database (`src/test/resources/application.properties` replaces the main configuration while tests run).

| Layer                  | Approach                                                                    |
|------------------------|-----------------------------------------------------------------------------|
| Services               | Mockito unit tests, no Spring context                                       |
| Controllers            | Standalone MockMvc with the real `GlobalExceptionHandler`                   |
| Repositories           | `@SpringBootTest` + `@Transactional` on H2: derived queries, cascade, orphan removal, constraints |
| End to end             | MockMvc against the full context: controller, service, repository and database |

### Front-end

```bash
cd frontend
npm test
```

Vitest and Testing Library cover the API client (error mapping, query building), the date helpers, the pagination component and the destinations page end to end against a mocked `fetch`: listing, filtering, sorting, creating, and the message shown when the API refuses a delete.

GitHub Actions runs both suites, and builds the front-end, on every push and pull request (`.github/workflows/ci.yml`).

## Project structure

```
src/main/java/com/tt/tripbook
  controller/   DestinationController, PassengerController, TripController
  dto/          *Request, *Response records and PageResponse
  service/      DestinationService, PassengerService, TripService
  repository/   DestinationRepository, PassengerRepository, TripRepository
  model/        Destination, Passenger, Trip
  exception/    ApiError, ConflictException, ResourceNotFoundException,
                BusinessRuleException, GlobalExceptionHandler
src/main/resources
  application.properties
src/test/java/com/tt/tripbook
  controller/ service/ repository/ integration/
frontend/
  src/api/          typed API client
  src/components/   ListPage, Dialog, Pagination, forms
  src/pages/        TripsPage, PassengersPage, DestinationsPage
```

## Roadmap

- [x] DTOs for input and output
- [x] Automated tests and continuous integration
- [x] Pagination, sorting and filtering
- [x] React front-end
- [ ] Authentication and authorization (Spring Security with JWT)
- [ ] Docker setup and deployment
- [ ] `enum` for the trip status

## License

Released under the MIT License. See [LICENSE](LICENSE).

## Author

Rhyan Gabriel Portilho Duarte, student at CEUB.
