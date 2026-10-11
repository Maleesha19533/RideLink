# RideLink – Fare & Payment Service

Owns fare estimation, final fare calculation, simulated payment processing and receipt
generation/retrieval. Part of the RideLink microservices system (IT3130 Application
Development group assignment). This service owns its own PostgreSQL database - no other
service may read or write this service's tables directly.

## Fare rule

- Base Fare = Rs.100
- Per KM = Rs.80
- Formula = Base Fare + (Distance × Per KM)

This is our implementation choice for the Fare & Payment Service and is not an
assignment-mandated fare value; it was chosen to keep the service simple, consistent and
testable.

## Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 14+ (a local instance, or one per teammate/environment)

## Configuration

All configuration is via environment variables (see `src/main/resources/application.properties`
for defaults; `.env.example` is a template with fake values). Nothing sensitive is committed to
the repository.

| Variable | Purpose | Example |
|---|---|---|
| `DB_URL` | JDBC URL for this service's own database | `jdbc:postgresql://localhost:5432/farepaymentdb` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | **Required, no default** - database password. Set it in the terminal you run the service from (see below); never put it in a file that is committed. | your PostgreSQL password |
| `SERVER_PORT` | HTTP port | `8084` |
| `RIDELINK_JWT_SECRET` | **Required** - HMAC signing secret. Must be **identical** across all four RideLink services (Account Service issues tokens; the other three, including this one, only verify them). No default - the app refuses to start without it. | a long random string, 32+ bytes |
| `RIDELINK_INTERNAL_API_KEY` | **Required, no default.** Shared secret sent as the `X-Internal-Api-Key` header when this service calls Ride Management's internal endpoints. Must be **identical** on Ride Management Service, and different from the JWT secret. | a long random string |
| `RIDE_MANAGEMENT_BASE_URL` | Base URL of Ride Management Service | `http://localhost:8083` |
| `RIDE_MANAGEMENT_INTEGRATION_ENABLED` | Feature toggle for the Ride Management calls below. Keep `true` for the integrated demo; can be set `false` for isolated local testing before Ride Management Service exists/is reachable. | `true` |

Create the database first:

```sql
CREATE DATABASE farepaymentdb;
```

## Running locally

Set the environment variables **in the same terminal window** you start the service from.
The password is only ever read from `DB_PASSWORD`; it is not stored anywhere in the code.

Git Bash / Linux / Mac:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/farepaymentdb
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export RIDELINK_JWT_SECRET=replace-with-the-same-secret-account-service-uses
export RIDELINK_INTERNAL_API_KEY=replace-with-the-same-internal-key-ride-management-uses
export RIDE_MANAGEMENT_BASE_URL=http://localhost:8083

mvn spring-boot:run
```

Windows Command Prompt:

```
set DB_URL=jdbc:postgresql://localhost:5432/farepaymentdb
set DB_USERNAME=postgres
set DB_PASSWORD=postgres
set RIDELINK_JWT_SECRET=replace-with-the-same-secret-account-service-uses
set RIDELINK_INTERNAL_API_KEY=replace-with-the-same-internal-key-ride-management-uses
set RIDE_MANAGEMENT_BASE_URL=http://localhost:8083

mvn spring-boot:run
```

The service starts on `http://localhost:8084` (override with `SERVER_PORT`).

**If you see `password authentication failed for user "postgres"`:** `DB_PASSWORD` was not set
in that terminal window (a new window forgets it) or does not match the password PostgreSQL
has for that user. Set it again and re-run. Also make sure the `farepaymentdb` database exists.

## Security

Every endpoint except Swagger/OpenAPI and `/actuator/health` requires a valid bearer JWT.
This service **never issues tokens itself** - only Account Service does that (`POST
/api/accounts/login`). A `JwtAuthFilter` verifies the token's signature and expiry locally
using the shared `RIDELINK_JWT_SECRET`, then trusts its `sub` (user id) and `role` claims -
no callback to Account Service is needed on every request.

| Endpoint | Required role | Ownership check |
|---|---|---|
| `POST /api/fare/estimate` | Any authenticated user | - |
| `POST /api/fare/final` | `PASSENGER` or `ADMIN` | Must be the ride's own passenger |
| `POST /api/payments/process` | `PASSENGER` or `ADMIN` | Must be the ride's own passenger |
| `GET /api/payments/{paymentId}/status` | Any authenticated user | Must be the ride's own passenger |
| `POST /api/receipts/generate` | `PASSENGER` or `ADMIN` | Must be the ride's own passenger |
| `GET /api/receipts/{receiptId}` | Any authenticated user | Must be the ride's own passenger |
| `GET /api/receipts/ride/{rideId}` | Any authenticated user | Must be the ride's own passenger |

**Passenger-ownership check:** `RideOwnershipGuard` confirms the caller's own user id (the
JWT's `sub` claim) matches the ride's `passengerId` as reported by Ride Management Service.
It runs on every payment and receipt endpoint - not just the two that create something, but
also the three read endpoints above - since a payment/receipt record is one passenger's
private billing information, not something any logged-in driver or other passenger should be
able to pull up by guessing a `paymentId`/`receiptId`/`rideId`. Admins bypass this check.
`/api/fare/estimate` is deliberately left without an ownership check: a fare estimate isn't
sensitive per-passenger data the way a payment/receipt is. `/api/fare/final` is checked in
`FareController` (before the fare is calculated) so only the ride's passenger or an admin can
trigger it. Like the other Ride
Management-dependent checks in this service, ownership is skipped (not enforced) when
`RIDE_MANAGEMENT_INTEGRATION_ENABLED=false`, since it can't be verified without asking Ride
Management Service who the passenger actually is.

**Known trade-off:** unlike Account Service's own filter, this service does not re-check the
caller's live account status in a database on every request - it doesn't own the `users`
table, and querying Account Service's database directly would violate the assignment's
data-ownership rule. A token therefore remains valid here for its full lifetime even if the
account is suspended in Account Service afterwards. Worth naming as a limitation in the
report's security section (a shorter token lifetime, or a central API gateway that checks
status, are the usual mitigations).

## Service-to-service integration

This service makes two outbound calls to **Ride Management Service**, both synchronous REST
via Spring's `RestClient` (`RideManagementClient`, with connect/read timeouts). Both go to
Ride Management's **internal** endpoints and are authenticated with the shared
`X-Internal-Api-Key` header (`RIDELINK_INTERNAL_API_KEY`) - **not** the end user's bearer
token. (Forwarding a passenger's own token would mean Ride Management had to let passengers
call "mark this ride paid", so a passenger could mark their own ride as paid.)

1. **`GET {ride-management}/api/internal/rides/{rideId}`** - used to confirm the ride exists,
   is `COMPLETED`, who its passenger is (feeds `RideOwnershipGuard`), and how far it
   travelled. **The final fare is calculated from the `distanceKm` Ride Management returns
   here - the `distanceKm` in the caller's request is ignored** (otherwise a passenger could
   send a tiny distance for a long ride and pay less). Deliberately synchronous: the endpoint
   cannot answer its own caller before it knows whether the ride actually happened.
   (`/api/fare/estimate` does **not** call this - an estimate works before a ride exists.)
2. **`PATCH {ride-management}/api/internal/rides/{rideId}/payment-status`** - sent after a
   payment succeeds **and its database transaction has committed**, to tell Ride Management
   the ride is paid. **Best-effort** (logged, not thrown, on failure) since this service's own
   `PaymentRecord` is already the durable source of truth for payment status.

If Ride Management Service is down, times out, or rejects the internal key, calls that need
it return **`503 Service Unavailable`** (not a misleading 400).

**Report discussion point (LO2):** interaction #2 is a reasonable candidate for
**asynchronous messaging** instead (e.g. a `PaymentCompleted` event on a queue that Ride
Management subscribes to), since this service doesn't need Ride Management's response to
answer its own caller. Synchronous REST was used to keep the demo simple; a queue would
decouple the two services' uptime and be more resilient if Ride Management is temporarily
down - a good comparison to write up.

**Contract Ride Management must provide** (see `RideSummaryResponse` /
`PaymentCompletedNotification`): `GET /api/internal/rides/{rideId}` returning
`{ rideId, passengerId, driverId, status, distanceKm }`, and
`PATCH /api/internal/rides/{rideId}/payment-status` accepting `{ paymentId, amount, status }`,
both requiring the `X-Internal-Api-Key` header. `passengerId` must be the same stable user id
Account Service puts in the JWT `sub` claim.

Set `RIDE_MANAGEMENT_INTEGRATION_ENABLED=false` to develop/test this service in isolation
before Ride Management exists (this is also how the plain `FareServiceTest`/`PaymentServiceTest`
run). In that mode the request's `distanceKm` is used and ownership is not enforced.

## Payment safety rules

- Payment is only accepted against a `FINAL` fare, for exactly the final fare amount.
- A ride can only be paid once and only ever gets one `FINAL` fare (calling `/api/fare/final`
  again returns the existing one). Payment runs in one transaction with a database write lock
  on the fare row, so two simultaneous payment requests cannot both succeed.
- "One `FINAL` fare per ride" is enforced **by the database**, not only by an application check
  (see *Database integrity rules* below). This matters because the payment lock is taken on a
  single fare row: if two simultaneous `/api/fare/final` calls could each insert a `FINAL` row,
  the same ride would have two separately-lockable fares and could be paid twice.
- A wrong amount is rejected with `400` **and recorded as a `FAILED` payment** (with a failure
  reason), so the failed attempt is on file. A correct payment can still follow it.
- A ride can only have one receipt; this is also enforced by a database unique constraint
  (a simultaneous duplicate returns `409`).

## Database integrity rules

| Rule | How it is enforced |
|---|---|
| One `FINAL` fare per ride | `fare_records.final_ride_id` has a `UNIQUE` constraint (`uk_fare_records_final_ride_id`). The column holds the `rideId` **only for `FINAL` rows** and is `NULL` for `ESTIMATED` rows (filled automatically by `FareRecord`, never set by hand). |
| Many `ESTIMATED` fares per ride | Allowed - a plain unique constraint on `(rideId, fareType)` would have blocked these, which is why the nullable marker column is used instead. `NULL` values never collide in PostgreSQL, MySQL or H2. |
| One receipt per ride | `receipt_records.ride_id` is `UNIQUE`. |
| Unique payment id / receipt id | `payment_records.payment_id`, `receipt_records.receipt_id` are `UNIQUE`. |

When two `/api/fare/final` requests race, the loser hits the constraint, and `FareService`
catches it and returns the winner's `FINAL` fare - so both callers get the same fare and no
error. `calculateFinalFare` must therefore **not** be made `@Transactional` (see the comment in
`FareService`).

**Existing local database?** A fresh database needs nothing - Hibernate adds the column and
constraint on first start. If your `farepaymentdb` already holds fare rows from before this
change, either drop and recreate it, or run `docs/db-migration-final-fare-unique.sql` to
backfill `final_ride_id` for old `FINAL` rows.

## API documentation

- Swagger UI: `http://localhost:8084/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8084/v3/api-docs`

## Running tests

```bash
mvn test
```

Tests run against an in-memory H2 database - no PostgreSQL instance required. Covers:

- `FareServiceTest` / `PaymentServiceTest` - original business-rule unit tests (fare
  calculation, payment amount matching, negative cases), run with the Ride Management
  integration switched off.
- `PaymentGuardrailsTest` - fixes made ahead of Ride Management Service being ready (plus: a
  wrong-amount payment is recorded as `FAILED`, and a correct retry still works):
  payment is rejected against an `ESTIMATED` fare (must be `FINAL`); a ride cannot be paid or
  receipted twice; `getFareByRideId()`/`getPaymentByRideId()` no longer crash when a ride has
  more than one `FareRecord`/`PaymentRecord` row.
- `FinalFareUniquenessTest` - the database enforces one `FINAL` fare per ride: a second `FINAL`
  insert is rejected, many `ESTIMATED` rows per ride are still allowed, an estimate and a final
  can coexist, different rides each get their own final, repeat calls return the same fare, and
  8 simultaneous `/api/fare/final` calls all get the same single `FINAL` fare.
- `FareServiceRideIntegrationTest` - the Ride Management validation logic (final fare uses the
  ride's own distance, not the requested one; a ride gets only one FINAL fare), with
  `RideManagementClient` mocked: final fare succeeds when the ride is `COMPLETED`, is
  rejected for any other status, and propagates a not-found error when Ride Management
  Service doesn't know the ride.
- `client/RideManagementClientTest` - the real HTTP behaviour of the client against a tiny fake
  Ride Management (JDK `HttpServer`): sends the internal API key to the internal paths and
  never forwards a user's bearer token; 404 → not found; rejected key / service down → 503-type
  errors; the payment notification never throws.
- `security/JwtServiceTest` - token signature/expiry verification (valid, expired, wrong
  secret, garbage token).
- `controller/SecurityIntegrationTest` - proves the filter chain end-to-end: no token → 401,
  wrong role → 403, expired token → 401, correct role → 200.
- `controller/PaymentOwnershipIntegrationTest` - `RideOwnershipGuard` end-to-end across all
  five payment/receipt endpoints (process, status, generate, get-by-id, get-by-ride): a
  passenger who isn't the ride's owner is rejected on every one, the actual owning passenger
  is allowed through all of them, and an admin bypasses the check entirely. Also covers
  `/api/fare/final` ownership, drivers being refused, the payment notification being sent
  exactly once (and not at all for a failed payment), and malformed JSON returning `400`.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/fare/estimate` | Estimate fare for a ride |
| POST | `/api/fare/final` | Calculate final fare for a completed ride |
| POST | `/api/payments/process` | Simulate processing a payment |
| GET | `/api/payments/{paymentId}/status` | Check payment status |
| POST | `/api/receipts/generate` | Generate a receipt for a successful payment |
| GET | `/api/receipts/{receiptId}` | Retrieve a receipt by receipt ID |
| GET | `/api/receipts/ride/{rideId}` | Retrieve a receipt by ride ID |

### Request/response shapes

#### A. Fare estimate
- `POST /api/fare/estimate`
```json
{ "rideId": "RIDE-001", "distanceKm": 10 }
```
Response: `200` with `{ rideId, distanceKm, baseFare, perKmRate, fareAmount, status: "ESTIMATED" }`.
Errors: `400` invalid ride ID or distance; `401` missing/invalid token.

#### B. Final fare
- `POST /api/fare/final`
```json
{ "rideId": "RIDE-001" }
```
`distanceKm` may be included but is **ignored** when the Ride Management integration is on -
the distance Ride Management recorded for the ride is used. Passenger (owner) or admin only.
Response: `200` with `status: "FINAL"` (calling it again for the same ride returns the same fare).
Errors: `400` invalid ride ID, or ride not yet `COMPLETED` / no distance recorded; `404` ride
unknown to Ride Management Service; `401`/`403` auth failures; `503` Ride Management unavailable.

#### C. Process payment
- `POST /api/payments/process`
```json
{ "rideId": "RIDE-001", "fareId": 1, "amount": 900, "paymentMethod": "CARD" }
```
Response: `200` with `{ paymentId, rideId, amount, status: "SUCCESS", message, failureReason }`.
Errors: `400` invalid amount/missing fields/amount mismatch (a mismatch is also recorded as a
`FAILED` payment); `404` fare not found; `401`/`403` auth failures (passenger/admin only);
`503` Ride Management unavailable.

#### D. Payment status
- `GET /api/payments/{paymentId}/status`
Response: `200` with `{ paymentId, rideId, fareId, amount, paymentMethod, status, failureReason, createdAt, updatedAt }`.
Errors: `404` payment ID unknown.

#### E. Receipt generation and retrieval
- `POST /api/receipts/generate`
```json
{ "rideId": "RIDE-001", "paymentId": "PAY-ABCD1234" }
```
Response: `201` with `{ receiptId, rideId, paymentId, amount, finalFare, issuedAt, status: "ISSUED" }`.
- `GET /api/receipts/{receiptId}` / `GET /api/receipts/ride/{rideId}`
Errors: `400` payment doesn't belong to the ride or wasn't successful; `404` receipt/payment/ride
data missing; `401`/`403` auth failures (generation is passenger/admin only).

## Trying it end-to-end (Swagger/Postman)

1. Register and log in against **Account Service** to get a JWT: `POST /api/accounts/register`
   then `POST /api/accounts/login` (see Account Service's own README for sample credentials).
2. Call this service's endpoints with `Authorization: Bearer <token>` from step 1.
3. For `/api/fare/final` and `/api/payments/process` to succeed against a real Ride Management
   Service, the ride referenced by `rideId` needs to actually exist there with status
   `COMPLETED` and a recorded distance, and the caller must be that ride's passenger. Until that service is ready, set `RIDE_MANAGEMENT_INTEGRATION_ENABLED=false`
   to exercise this service on its own.
