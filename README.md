# RideLink

RideLink is a backend-only ride-sharing platform for the IT3130 Application Development group assignment. This repository currently contains four independent Spring Boot service skeletons. Business features and MongoDB Atlas connections will be added later.

## Microservices

| Service | Owner | Port | Health endpoint |
| --- | --- | ---: | --- |
| Account Service | Nelushi Balasuriya | 8081 | http://localhost:8081/api/health |
| Driver & Vehicle Service | Lithali | 8082 | http://localhost:8082/api/health |
| Ride Management Service | Amasha | 8083 | http://localhost:8083/api/health |
| Fare & Payment Service | Hansi Hansi | 8084 | http://localhost:8084/api/health |

## Technology stack

Java 21, Spring Boot 3, Maven, MongoDB Atlas (to be configured later), REST, Swagger/OpenAPI, Postman, and GitHub Actions (workflow to be added later).

Each service has its own Maven project and can be built or run independently. Install Java 21. The Maven Wrapper downloads Maven on first use if needed.

## Build on Windows (PowerShell)

From the repository root:

```powershell
cd account-service
.\mvnw.cmd clean verify
cd ..\driver-vehicle-service
.\mvnw.cmd clean verify
cd ..\ride-management-service
.\mvnw.cmd clean verify
cd ..\fare-payment-service
.\mvnw.cmd clean verify
```

## Run on Windows (PowerShell)

Open a separate terminal for each service from the repository root:

```powershell
cd account-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd driver-vehicle-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd ride-management-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd fare-payment-service
.\mvnw.cmd spring-boot:run
```

Check each health endpoint in the table above. Swagger UI is available at `http://localhost:<port>/swagger-ui.html` for each running service.

MongoDB Atlas credentials are intentionally absent. MongoDB auto-configuration is disabled in each service until its own Atlas connection is configured. Remove the MongoDB exclusions when adding those connections. The Account Service includes Spring Security as a dependency, but its auto-configuration is disabled for this skeleton so the health endpoint remains accessible; authentication will be configured later.
