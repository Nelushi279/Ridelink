# Driver profile management

This feature owns driver profiles only. Account identity is stored as `accountId`; there are no account-service calls, credentials, vehicle data, availability, location, or ride integrations.

## API

| Method | Path | Success | Purpose |
| --- | --- | --- | --- |
| POST | `/api/drivers` | 201 + Location header | Create a PENDING profile |
| GET | `/api/drivers/{driverId}` | 200 | Retrieve by driver ID |
| GET | `/api/drivers/account/{accountId}` | 200 | Retrieve by account identity |
| PATCH | `/api/drivers/{driverId}` | 200 | Update supplied profile fields |

POST example:

```json
{
  "accountId": "account-123",
  "fullName": "Test Driver",
  "phoneNumber": "0771234567",
  "licenseNumber": "B1234567",
  "licenseExpiryDate": "2028-12-31",
  "serviceArea": "Colombo"
}
```

Example successful response (POST, either GET, and PATCH use the same schema):

```json
{
  "id": "driver-123",
  "accountId": "account-123",
  "fullName": "Test Driver",
  "phoneNumber": "0771234567",
  "licenseNumber": "B1234567",
  "licenseExpiryDate": "2028-12-31",
  "serviceArea": "Colombo",
  "status": "PENDING",
  "createdAt": "2026-10-03T10:00:00Z",
  "updatedAt": "2026-10-03T10:00:00Z"
}
```

IDs and timestamps are illustrative; MongoDB generates real IDs. Creation sets both timestamps to the same current instant.

PATCH example:

```json
{"fullName":"Updated Driver","phoneNumber":"0779876543","licenseNumber":"B7654321","licenseExpiryDate":"2029-12-31","serviceArea":"Kandy"}
```

The response contains those updated fields and a refreshed `updatedAt`; `id`, `accountId`, `status`, and `createdAt` are preserved. Each supplied field is validated. Omitted or null PATCH fields are left unchanged; a request with no non-null editable fields returns 400. Blank strings are rejected. Unknown and protected JSON fields are rejected, including alongside legitimate updates. Create requests also reject server-owned fields.

Validation error example:

```json
{"timestamp":"2026-10-03T10:00:00Z","status":400,"message":"Invalid request data","fieldErrors":{"fullName":"must not be blank"}}
```

Expired-license error example:

```json
{"timestamp":"2026-10-03T10:00:00Z","status":400,"message":"License expiry date must be today or later","fieldErrors":{}}
```

Not-found example:

```json
{"timestamp":"2026-10-03T10:00:00Z","status":404,"message":"Driver not found","fieldErrors":{}}
```

Duplicate example:

```json
{"timestamp":"2026-10-03T10:00:00Z","status":409,"message":"License number is already in use","fieldErrors":{}}
```

Duplicate account errors use `A driver profile already exists for this accountId`. Concurrent duplicate writes use the safe generic message `Account ID or license number is already in use`. Unexpected errors return 500 with `Internal server error`; stack traces and database details are not sent to clients.

## MongoDB and running locally

In PowerShell, set a connection URI for this service's own database, then run:

```powershell
$env:DRIVER_VEHICLE_MONGODB_URI = '<your driver-vehicle database URI>'
.\mvnw.cmd spring-boot:run
```

The credential-free fallback is `mongodb://localhost:27017/ridelink_driver_vehicle`. No Atlas credentials are stored in source. `spring.data.mongodb.auto-index-creation=true` creates unique `accountId` and `licenseNumber` indexes on the `drivers` collection. The database user needs index-creation permission. Existing duplicate data must be resolved before these unique indexes can be created. Application checks provide clear errors; unique indexes enforce the rules during concurrent writes.

Profile string values are trimmed before persistence and duplicate checks; matching is case-sensitive. License dates use `LocalDate`, with today's date evaluated in `Asia/Colombo`; a license expiring today is valid. Timestamps use UTC `Instant` values.

Health: `http://localhost:8082/api/health`

Swagger UI: `http://localhost:8082/swagger-ui/index.html`

OpenAPI JSON: `http://localhost:8082/v3/api-docs`

## Files and responsibilities

All paths below are relative to `driver-vehicle-service/`.

Created under `src/main/java/com/ridelink/drivervehicle/`:

| File | Purpose |
| --- | --- |
| `controller/DriverController.java` | Four REST endpoints, validation entry points, HTTP status codes, OpenAPI documentation |
| `dto/CreateDriverRequest.java` | Required create fields and maximum lengths |
| `dto/UpdateDriverRequest.java` | Optional editable fields, blank/length validation, empty PATCH detection |
| `dto/DriverResponse.java` | Explicit safe response schema |
| `model/Driver.java` | MongoDB document, unique indexes, profile fields and timestamps |
| `model/DriverStatus.java` | PENDING, ACTIVE, INACTIVE, SUSPENDED |
| `repository/DriverRepository.java` | Persistence and account/license lookup operations |
| `service/DriverService.java` | Duplicate/expiry rules, timestamps, mapping and profile updates |
| `exception/DriverNotFoundException.java` | Missing-profile failure |
| `exception/DuplicateDriverException.java` | Account/license conflict failure |
| `exception/InvalidDriverProfileException.java` | Expired license or empty PATCH failure |
| `exception/ApiError.java` | Consistent JSON error schema |
| `exception/GlobalExceptionHandler.java` | Safe 400/404/409/500 responses |

Other created files:

- `src/test/java/com/ridelink/drivervehicle/DriverProfileTest.java`: HTTP tests through the real controller, service, validation, advice and OpenAPI configuration, using a mocked repository.
- `DRIVER_PROFILE.md`: API examples, configuration, file inventory and verification notes.

Modified:

- `src/main/resources/application.properties`: enables MongoDB, service-specific URI, unique index creation and strict unknown-property rejection.

The POM, application class, health controller and original health test are unchanged.

## Verification

- `.\mvnw.cmd test`: 46 tests, zero failures/errors/skips, BUILD SUCCESS.
- `.\mvnw.cmd clean verify`: 46 tests, zero failures/errors/skips, BUILD SUCCESS; executable JAR packaged.
- Tests exercise creation, PENDING/timestamps, duplicate account/license, required-field validation, license expiry including today, both lookups, each editable field, empty PATCH, protected fields, malformed JSON, database duplicate race handling, safe unexpected errors, health, Swagger UI and all four OpenAPI operations.
- MongoDB is mocked in these tests. Live Atlas persistence and physical index enforcement require a reachable configured database and were not verified against Atlas.
- Maven can emit Mockito dynamic-agent warnings on Java 21; these do not fail the build.
- No branch switch, commit, push, or changes to other microservices.
- `git diff --name-status dev` shows tracked differences only. New feature files remain untracked and are listed by `git status --short --untracked-files=all`.
Final tracked diff against `dev`:

```text
M driver-vehicle-service/src/main/resources/application.properties
```

The 15 new files in the inventory above are untracked and therefore absent from this command's output. All changed and new files are within `driver-vehicle-service/`; other microservices have no changes.