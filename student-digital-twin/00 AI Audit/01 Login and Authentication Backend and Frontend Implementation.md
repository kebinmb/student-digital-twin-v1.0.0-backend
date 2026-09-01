# Login, Authentication & Session Implementation Audit

**Target Repositories**: `student-digital-twin-v1.0.0-backend` & `student-digital-twin-v1.0.0-frontend`  
**Reference Standards**: Spring Boot 3.5+ (OAuth2 Resource Server, Nimbus JWT, Argon2id, Virtual Threads) & Angular 21 (Zoneless, Signals, PrimeNG 21 Standalone)  
**Audit Date**: September 1, 2026  
**Audit Status**: **VERIFIED & COMPREHENSIVELY MAPPED**

---

## 1. High-Level Architectural Overview

The authentication and session management architecture employs a **stateless, dual-token security model** combining short-lived, in-memory **JSON Web Tokens (JWT)** for API authorization with long-lived, stateful, cryptographically secure **Refresh Tokens** stored in MySQL and delivered via partitioned, HttpOnly cookies.

```
+---------------------------------------------------------------------------------------------------------+
|                                           FRONTEND (Angular 21)                                         |
|                                                                                                         |
|  +---------------------+      Credentials        +--------------------+    Token Signal (In-Memory)     |
|  |   LoginComponent    | ----------------------> |    AuthService     | <-------------------------+ |
|  +---------------------+  {userOrEmail, pass}    +--------------------+                           | |
|                                                             |                                     | |
|                                             withCredentials: true                                 | |
|                                                             v                                     | |
|  +---------------------+                       +---------------------------+                      | |
|  |  Protected Feature  | --------------------> |      authInterceptor      |                      | |
|  |     (Dashboard)     |   Outbound Request    +---------------------------+                      | |
|  +---------------------+                       | - Appends Authorization   |                      | |
|                                                | - Catches 401s            |                      | |
|                                                | - Concurrency Refresh Lock|                      | |
|                                                +---------------------------+                      | |
+--------------------------------------------------------------|------------------------------------+-----+
                                                               | HTTP / TLS
                                                               v
+---------------------------------------------------------------------------------------------------------+
|                                          BACKEND (Spring Boot 3.5)                                      |
|                                                                                                         |
|  +---------------------------------------------------------------------------------------------------+  |
|  |                                      WebSecurityConfig (Stateless)                                |  |
|  |  - /api/public/**  -> permitAll()                                                                 |  |
|  |  - /api/admin/**   -> hasRole('ADMIN')                                                            |  |
|  |  - /api/**         -> authenticated() via localJwtDecoder + JwtRoleConverter                      |  |
|  +---------------------------------------------------------------------------------------------------+  |
|                                              |                                                          |
|                        +---------------------+---------------------+                                    |
|                        |                                           |                                    |
|                        v                                           v                                    |
|       +---------------------------------+        +-----------------------------------+                  |
|       |    AuthenticationController     |        |      OAuth2 Resource Server       |                  |
|       |  - POST /api/public/auth/login  |        |  - Nimbus RSA256 Local Validation |                  |
|       |  - POST /api/public/auth/refresh|        |  - Audience & Skew Verification   |                  |
|       |  - POST /api/public/auth/logout |        +-----------------------------------+                  |
|       +---------------------------------+                          |                                    |
|                        |                                           |                                    |
|                        v                                           v                                    |
|       +---------------------------------+        +-----------------------------------+                  |
|       |           AuthService           |        |        JwtRoleConverter           |                  |
|       |  - Argon2id Password Hashing    |        |  - roles -> ROLE_ADMIN, etc.      |                  |
|       |  - Timing Attack Mitigation     |        +-----------------------------------+                  |
|       |  - Refresh Token Rotation       |                                                       |
|       |  - Replay / Theft Detection     |                                                       |
|       +---------------------------------+                                                       |
|                        |                                                                        |
|         +--------------+--------------+                                                         |
|         |                             |                                                         |
|         v                             v                                                         |
|  +--------------+            +-------------------+                                              |
|  | TokenService |            | JPA Repositories  |                                              |
|  | (RSA-256 JWT)|            | User & RefreshTok |                                              |
|  +--------------+            +-------------------+                                              |
+---------------------------------------|-----------------------------------------------------------------+
                                        v
                               +-----------------+
                               |   MySQL 8.0     |
                               | - users         |
                               | - user_roles    |
                               | - refresh_tokens|
                               +-----------------+
```

### End-to-End Authentication Data Flow

1. **Submission**: User submits `usernameOrEmail` and `password` on `LoginComponent`.
2. **Dispatch**: `AuthService.login()` dispatches `POST /api/public/auth/login` with `withCredentials: true`.
3. **Filter Chain**: `WebSecurityConfig` permits `/api/public/**` unauthenticated without initializing HTTP sessions (`SessionCreationPolicy.STATELESS`).
4. **Verification & Constant-Time Fallback**: `AuthService.authenticate()` queries `UserRepository` by username or email. If the account does not exist, verification is executed against a hardcoded `DUMMY_HASH` to guarantee constant-time Argon2 computation, preventing username enumeration via side-channel latency profiling.
5. **Token Generation**:
   - **Access Token**: `TokenService` mints an RSA256-signed JWT containing claims: `sub` (User ID), `preferred_username`, `email`, `roles`, `iss`, `aud`, and a 15-minute expiration (`900` seconds).
   - **Refresh Token**: 64 cryptographically secure random bytes are generated, base64url-encoded, and saved to the `refresh_tokens` table with a 7-day expiration.
6. **Cookie Delivery**: The backend writes the refresh token into a `Set-Cookie` header: `REFRESH_TOKEN=<token>; Path=/api/public/auth; HttpOnly; Secure; SameSite=None; Partitioned; Max-Age=604800`. The access token is returned in the response JSON payload.
7. **Client Ingestion**: The frontend stores the access token strictly in an in-memory signal (`accessTokenSignal`), isolating it from XSS attack vectors. The browser stores the partitioned, HttpOnly cookie.
8. **Resource Requests**: `authInterceptor` injects `Authorization: Bearer <accessToken>` into outbound protected requests. Spring's resource server decodes the token with `localJwtDecoder`, while `JwtRoleConverter` maps roles to `ROLE_<ROLE>` granted authorities.
9. **Automatic Token Rotation & Theft Detection**: When an access token expires (401), `authInterceptor` locks concurrent requests, calls `/api/public/auth/refresh`, and receives a renewed access token and new cookie. If a revoked refresh token is presented, `AuthService.rotateRefreshToken()` purges **all** active refresh tokens for the user (`deleteAllByUserId`), terminating all active sessions across all devices.

---

## 2. Backend Audit (`student-digital-twin-v1.0.0-backend`)

### 2.1 Entities & Database Schema

#### `User` Entity
* **Path:** `com.sdt.web_app.entities.authentication.User`
* **Table:** `users` (defined in `V1__init_auth_schema.sql`)
* **Fields:**
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `username`: `VARCHAR(50) NOT NULL UNIQUE` (immutable: `updatable = false`)
  - `email`: `VARCHAR(100) NOT NULL UNIQUE`
  - `password`: `VARCHAR(255) NOT NULL` (excluded from Lombok `toString` via `@ToString(exclude = "password")` and `@Getter(AccessLevel.NONE)`)
  - `enabled`: `BOOLEAN NOT NULL DEFAULT TRUE`
  - `roles`: Stored via `@ElementCollection(fetch = FetchType.EAGER)` in table `user_roles` with composite primary key `(user_id, role)`, mapped as `EnumType.STRING`
  - `createdAt`: `TIMESTAMP DEFAULT CURRENT_TIMESTAMP` (immutable)
* **Security Domain Methods:** `getPasswordHash()`, `updatePassword(hash)`, `disableAccount()`, `enableAccount()`, `addRole(role)`, `removeRole(role)`, `hasRole(role)`.

#### `RefreshToken` Entity
* **Path:** `com.sdt.web_app.entities.authentication.RefreshToken`
* **Table:** `refresh_tokens` (indexed by `idx_refresh_tokens_token`)
* **Fields:**
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `token`: `VARCHAR(255) NOT NULL UNIQUE` (cryptographic random base64url string)
  - `user`: `@ManyToOne(fetch = FetchType.LAZY) JOIN COLUMN user_id` (foreign key with `ON DELETE CASCADE`)
  - `expiryDate`: `TIMESTAMP NOT NULL`
  - `revoked`: `BOOLEAN NOT NULL DEFAULT FALSE`
  - `createdAt`: `TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
* **Validation Methods:** `isExpired()`, `isValid()`, `revoke()`.

#### `Roles` Enum
* **Path:** `com.sdt.web_app.entities.authentication.Roles`
* **Values:** `ADMIN`, `REGISTRAR`, `FACULTY`, `DEAN`, `CHAIRPERSON`, `STUDENT`.

---

### 2.2 Repositories & Data Access

#### `UserRepository`
* `findByUsername(String username)`: Case-sensitive lookup.
* `findByEmail(String email)`: Lookup by email.
* `existsByUsername(String username)` / `existsByEmail(String email)`: Uniqueness checks for registration.

#### `RefreshTokenRepository`
* `findByToken(String token)`: Fetch token entity.
* `revokeIfActive(@Param("token") String token)`: Atomic conditional query (`UPDATE RefreshToken r SET r.revoked = true WHERE r.token = :token AND r.revoked = false`) returning modified row count. Used to prevent race conditions during token rotation.
* `deleteAllByUserId(@Param("userId") Long userId)`: Batch purge of all sessions for a compromised user.

---

### 2.3 Services & Security Architecture

#### `AuthService`
* **Password Hashing:** Uses `PasswordEncoder` (Argon2id configured via `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`).
* **Side-Channel Mitigation:** Line 27 declares `DUMMY_HASH`. When a non-existent account attempts authentication, the password is encrypted against `DUMMY_HASH` to neutralize timing difference attacks.
* **Registration:** Assigns default role `Set.of(Roles.STUDENT)` and catches `DataIntegrityViolationException` to throw `UserAlreadyExistsException`.
* **Refresh Token Rotation:**
  - `@Transactional(noRollbackFor = InvalidTokenException.class)`
  - Replay of revoked/expired tokens triggers `refreshTokenRepository.deleteAllByUserId(user.getId())`.
  - Atomic revocation via `revokeIfActive()`. If 0 rows updated, concurrent replay is flagged and all sessions are wiped.
* **Logout:** Queries token by value and flags `revoked = true`.

#### `TokenService`
* Uses `JwtEncoder` with RSA256.
* Generates signed JWTs valid for 15 minutes (`plus(15, ChronoUnit.MINUTES)`).
* Claims: `iss` (`spring.application.name`), `sub` (User ID), `aud` (`api://sdt-webapp`), `preferred_username`, `email`, `roles` (`List<String>`).

#### `AuthCryptoConfig`
* Generates an in-memory 2048-bit RSA Key Pair on startup.
* Provides `NimbusJwtEncoder` and `NimbusJwtDecoder` (`localJwtDecoder`) beans.

#### `JwtRoleConverter`
* Converts JWT claims into Spring `SimpleGrantedAuthority` objects (`ROLE_<ROLE>`).
* Supports direct `"roles"` string array or Keycloak-style `"realm_access.roles"`. Sets `preferred_username` as the authentication principal.

#### `WebSecurityConfig`
* **Security Filter Chain (`@Order(2)`):** Targets `/api/**` and `/ws/**`.
* **CORS:** Allows origins `http://localhost:3000`, `5173`, `8080`, and `4200` with `allowCredentials(true)` and exposed `Set-Cookie` header.
* **CSRF:** Explicitly disabled (`csrf.disable()`) due to stateless Bearer tokens and SameSite cookie protection.
* **Headers:** Enforces Strict Content Security Policy (`default-src 'self'`) and Permissions-Policy.
* **URL Authorization:**
  - `/api/public/**` -> `permitAll()`
  - `/api/admin/**` -> `hasRole("ADMIN")`
  - `/api/orders/**` -> `hasAnyRole("STUDENT", "ADMIN", "FACULTY")`
  - `anyRequest()` -> `authenticated()`

#### `ActuatorSecurityConfig`
* **Security Filter Chain (`@Order(1)`):** Binds to `EndpointRequest.toAnyEndpoint()`.
* `/health` and `/info` are public. `/prometheus` and `/metrics` require `ROLE_MONITORING`. Other endpoints require `ROLE_INFRA_ADMIN`.

#### `GrpcAuthInterceptor` & `AsyncSecurityConfig`
* Extends JWT validation to gRPC calls (listening on port 9090).
* Propagates `SecurityContextHolder` across Virtual Threads using Micrometer `ContextSnapshotFactory` and `TaskDecorator`.

---

### 2.4 Auth Endpoints Summary Table

| HTTP Method | Path | Auth Required | Request Body / Cookies | Success Response | Error Responses |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/public/auth/register` | Anonymous | `RegisterRequest` JSON (`username`, `email`, `password`) | `201 Created` (empty body) | `400 Bad Request` (validation)<br>`409 Conflict` (username/email in use) |
| `POST` | `/api/public/auth/login` | Anonymous | `LoginRequest` JSON (`usernameOrEmail`, `password`) | `200 OK` + `AuthResponse` JSON + `Set-Cookie: REFRESH_TOKEN` | `400 Bad Request`<br>`401 Unauthorized` (bad credentials/inactive) |
| `POST` | `/api/public/auth/refresh` | Cookie (`REFRESH_TOKEN`) | Cookie: `REFRESH_TOKEN` | `200 OK` + `AuthResponse` JSON + updated `Set-Cookie: REFRESH_TOKEN` | `401 Unauthorized` (expired/revoked/compromised) |
| `POST` | `/api/public/auth/logout` | Cookie (`REFRESH_TOKEN`) | Cookie: `REFRESH_TOKEN` | `204 No Content` + `Set-Cookie: REFRESH_TOKEN=; Max-Age=0` | `204 No Content` |
| `GET` | `/api/public/auth/me` | — | — | **Not Implemented** | `404 Not Found` |

---

### 2.5 Backend Test Coverage

`AuthenticationIntegrationTest.java` provides extensive coverage (546 lines):
* **Registration Scenarios:** Successful registration with Argon2 hash and default `STUDENT` role; duplicate username rejection (`409 Conflict`); payload validation errors (`400 Bad Request`).
* **Login Scenarios:** Username and email interchangeable login; password mismatch rejection (`401 Unauthorized`); disabled user rejection (`401 Unauthorized`); non-existent user rejection (`401 Unauthorized`).
* **Protected Endpoint Verification:** End-to-end access to protected resources with minted JWT; anonymous rejection (`401 Unauthorized`); role authorization (`ROLE_FACULTY` permitted on `/orders`, forbidden on `/admin/dashboard`; `ROLE_ADMIN` permitted on `/admin/dashboard`).
* **Refresh Token Rotation & Theft:** Rotation emits new token and revokes previous; **theft detection test confirms reuse of a revoked token immediately wipes all active user sessions from the DB and returns 401**.
* **Concurrency & Race Conditions:** Multi-threaded tests executing simultaneous refresh requests verify that at most one succeeds; concurrent duplicate registrations verify exactly one succeeds while others yield `409 Conflict`.

---

## 3. Frontend Audit (`student-digital-twin-v1.0.0-frontend`)

### 3.1 State Management & Context

#### `AuthService`
* **In-Memory Token Storage:** Maintains `accessTokenSignal = signal<string | null>(null)`. The JWT token is never persisted in `localStorage` or `sessionStorage`, making it completely inaccessible to XSS script injection.
* **Reactivity:**
  - `accessToken = computed(() => this.accessTokenSignal())`
  - `isAuthenticated = computed(() => !!this.accessTokenSignal())`
  - `currentUser = computed(...)`: Safely extracts and decodes user payload claims (`preferred_username`, `email`, `role`) directly from the JWT.
* **Local Storage Usage:** Only stores `chmsu_remembered_user` (plain username string) when the user checks "Stay signed in" on login, for form pre-population.

---

### 3.2 Routes & Route Guards

#### `authGuard`
* Checks `authService.isAuthenticated()`. If `true`, navigation proceeds synchronously.
* If `false`, initiates a silent token refresh via `authService.refreshToken()`, leveraging the browser's HttpOnly cookie.
* If refresh succeeds, navigation is permitted.
* If refresh fails (e.g., no cookie or expired session), redirects to `/login` with `returnUrl` query parameter: `router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })`.

#### `guestGuard`
* Prevents already-authenticated users from navigating back to the `/login` screen, redirecting them to `/dashboard`.

#### Route Configuration (`app.routes.ts`)
* `/login` guarded by `canActivate: [guestGuard]`.
* `/dashboard` guarded by `canActivate: [authGuard]`, serving `DashboardLayoutComponent` with child routes.

---

### 3.3 API Clients & Interceptors

#### `authInterceptor`
* **Header Injection:** For all endpoints excluding `/api/public/auth`, clones the request and attaches `Authorization: Bearer <accessToken>`.
* **401 Catch & Concurrency Lock:**
  - Catches `401 Unauthorized` responses on protected requests.
  - Employs a mutex flag `isRefreshing` and a `refreshTokenSubject = new BehaviorSubject<string | null>(null)`.
  - When the first 401 arrives, triggers `authService.refreshToken()`.
  - Subsequent concurrent 401s are blocked and queued via `refreshTokenSubject.pipe(filter(token => token !== null), take(1), switchMap(...))`.
  - When the new token arrives, all queued requests are re-sent with the new `Bearer` header.
  - If refresh fails, `authService.clearAuth()` is called and errors are propagated.

---

### 3.4 UI Components

#### `LoginComponent`
* Standalone component using reactive forms: `usernameOrEmail` (min length 3), `password` (required), `rememberMe` (boolean).
* Uses PrimeNG standalone components: `InputText`, `Password`, `Button`, `Checkbox`.
* UI states controlled by signals: `isLoading = signal(false)`, `errorMessage = signal<string | null>(null)`.
* Automatically dismisses error alert as soon as the user modifies form fields (`loginForm.valueChanges`).
* Seamlessly handles RFC 7807 `ProblemDetail` backend error responses.

#### `DashboardLayoutComponent`
* Renders user identity (`userName()`, `userRole()`, `userInitials()`) in the topbar and profile menu.
* "Sign Out" action triggers `authService.logout()`, revoking the backend session cookie and navigating to `/login`.

---

### 3.5 Frontend Test Coverage

* `login-component.spec.ts`: 4 unit tests covering component instantiation, form validation rules, invalid submit marking, and error message auto-clear.
* `dashboard-layout.component.spec.ts`: 3 unit tests covering layout creation, sidebar collapse state toggling, and user initials formatting.
* **Gaps:** `auth-service.spec.ts`, `auth-guard.spec.ts`, and `auth-interceptor.spec.ts` only contain boilerplate `should be created` tests.

---

## 4. Discrepancies, Gaps & Security Risks

### 1. [HIGH] Frontend `APP_INITIALIZER` Misalignment
* **Location:** `app.config.ts:17-28`
* **Issue:** `initializeApp()` checks `localStorage.getItem('token') || localStorage.getItem('refreshToken')`. Since the application intentionally uses in-memory access tokens and HttpOnly cookies, `localStorage` never holds any token.
* **Impact:** On a hard page refresh (F5), `initializeApp` immediately returns `of(null)` without attempting a silent refresh. The application only attempts silent refresh when navigating to an `authGuard`-protected route. For ungarded routes or components, the app temporarily treats the user as unauthenticated until a navigation event occurs.
* **Recommendation:** Remove the `localStorage` check and allow `authService.refreshToken()` to run silently via HttpOnly cookie.

### 2. [HIGH] Ephemeral In-Memory RSA Key Pair
* **Location:** `AuthCryptoConfig.java:32-40`
* **Issue:** The RSA keypair is generated dynamically at application startup (`KeyPairGenerator.getInstance("RSA")`).
* **Impact:** Every time the backend restarts or deploys, previously minted access tokens become immediately un-verifiable, causing widespread 401s. In multi-instance / clustered deployments, an access token generated by Instance A cannot be decoded by Instance B without a shared JWKS/Keystore.
* **Recommendation:** Persist RSA keys in a secure keystore (PKCS#12 / PEM), environment variables, or an external OIDC/Keycloak service.

### 3. [MEDIUM] Missing User Profile (`/me`) Endpoint
* **Location:** `AuthenticationController.java`
* **Issue:** No endpoint exists to fetch fresh account data, status, or roles for the authenticated user.
* **Impact:** The frontend is forced to decode user identity from the static JWT claims. If an administrator disables a user or alters their roles mid-session, the frontend will not reflect these changes until the access token expires and refresh is triggered.
* **Recommendation:** Implement `GET /api/public/auth/me` or `GET /api/user/profile` returning current user entity state.

### 4. [MEDIUM] No Automated Cleanup for Expired Refresh Tokens
* **Location:** `refresh_tokens` table / `RefreshTokenRepository.java`
* **Issue:** Expired and revoked tokens remain in the database indefinitely.
* **Impact:** Database bloat over time on the `refresh_tokens` table.
* **Recommendation:** Add a scheduled task (`@Scheduled(cron = "0 0 3 * * ?")`) executing `DELETE FROM refresh_tokens WHERE expiry_date < NOW() OR revoked = true`.

### 5. [MEDIUM] Incomplete Production Environment Config
* **Location:** `src/environments/environment.production.ts`
* **Issue:** `export const environment = {};` is empty.
* **Impact:** If `angular.json` is configured to replace `environment.ts` with `environment.production.ts`, `environment.apiUrl` becomes `undefined`, causing all frontend auth requests to fail (`undefined/public/auth/login`).
* **Recommendation:** Populate `environment.production.ts` with valid production API URL configurations.

### 6. [LOW] Leading Slash Inconsistency on Controller Route
* **Location:** `AuthenticationController.java:22`
* **Issue:** Uses `@PostMapping("register")` without leading slash, while `@PostMapping("/login")`, `/refresh`, and `/logout` use leading slashes. While Spring Web normalizes this, consistency prevents misconfiguration.

### 7. [LOW] Frontend Unit Test Coverage Gaps for Auth Services
* **Location:** `auth-service.spec.ts`, `auth-guard.spec.ts`, `auth-interceptor.spec.ts`
* **Issue:** Missing assertions for HTTP requests, 401 interception, token queuing, and error propagation.

---

## 5. Executive Summary

- **Architecture:** Dual-token model with 15-minute in-memory RSA256 JWT access tokens and 7-day stateful, rotating refresh tokens stored in MySQL.
- **Cookie Security:** Refresh tokens are delivered via `HttpOnly`, `Secure`, `SameSite=None`, and `Partitioned` (CHIPS) cookies scoped to `/api/public/auth`.
- **Credential Storage:** User passwords hashed using **Argon2id** (`defaultsForSpringSecurity_v5_8`). Side-channel timing attacks on login are mitigated using a dummy hash calculation.
- **Theft Detection:** Any replay of revoked or expired refresh tokens triggers an immediate wipe of **all** user sessions across all devices (`deleteAllByUserId`).
- **Frontend Security:** Access tokens reside exclusively in Angular Signals (in-memory); zero storage in `localStorage` or `sessionStorage` (immune to XSS).
- **Concurrency Control:** `authInterceptor` locks and queues concurrent 401 requests using RxJS `BehaviorSubject` while a single refresh operation is in flight.
- **Silent Session Restoration:** `authGuard` initiates a silent refresh via the HttpOnly cookie on direct navigation to protected routes.
- **Action Items:**
  1. Fix `app.config.ts` `APP_INITIALIZER` which redundantly checks `localStorage` for tokens.
  2. Persist RSA keys for JWT signing instead of generating ephemeral keys in memory on startup.
  3. Implement `GET /api/public/auth/me` for runtime profile synchronization.
  4. Implement `@Scheduled` cleanup for expired refresh tokens in MySQL.

---

## 6. PasswordReset Component Implementation Details

### 6.1 Component Summary

The `PasswordReset` implementation provides self-service credential recovery using temporary single-use reset tokens and email dispatch:

- **Entity (`PasswordResetToken.java`)**: Mapped to `password_reset_tokens` table. Features unique indexed token column, `@OneToOne(fetch = FetchType.LAZY)` user mapping with foreign key `fk_password_reset_user`, `Instant` expiration timestamp (`expiryDate`), `createdAt` metadata, domain factory `createTokenForUser()`, and expiry check `isExpired()`.
- **Repository (`PasswordResetTokenRepository.java`)**: Extends `JpaRepository<PasswordResetToken, Long>`, providing `findByToken(String token)` and `deleteByUser(User user)` query methods.
- **Service (`PasswordResetService.java`)**: Handles reset initiation and completion workflows. Injects `UserRepository`, `PasswordResetTokenRepository`, `JavaMailSender`, and `PasswordEncoder`.
- **Controller (`PasswordResetController.java`)**: RestController mapped under `/api/auth`, exposing `@PostMapping("/forgot-password")` and `@PostMapping("/reset-password")`.
- **DTOs (`AuthDtos.java`)**: Uses records `ForgotPasswordRequest` (`@NotBlank`, `@Email`) and `ResetPasswordRequest` (`@NotBlank` token, `@NotBlank`, `@Size(min = 8)` newPassword).

### 6.2 Integration Status & Domain Alignment

- **Domain Method Alignment**: Integrates with `User.java` domain encapsulation method `updatePassword(hashedPassword)` which validates non-null/non-blank values prior to state update.
- **Database Mappings**: Lazy-loaded `@OneToOne` association prevents unnecessary user fetching during token lookups. Database schema constraints (`unique = true`, `nullable = false`, foreign key naming) align with JPA model definitions.
- **Transactional Boundaries**: Both `initiatePasswordReset` and `completePasswordReset` execute within `@Transactional` boundaries, ensuring atomic cleanup of obsolete tokens, persistence of new credentials, and database synchronization.

### 6.3 Behavioral Flow & Operational Notes

```
[User Request] -> POST /api/auth/forgot-password { email }
  -> PasswordResetController.forgotPassword()
  -> PasswordResetService.initiatePasswordReset()
       -> Find User by Email
       -> Delete Existing Tokens (deleteByUser)
       -> Generate Raw UUID Token
       -> Save PasswordResetToken (30-min expiry)
       -> Dispatch Reset Link Email via JavaMailSender
  -> Controller returns 200 OK generic message

[User Action] -> POST /api/auth/reset-password { token, newPassword }
  -> PasswordResetController.resetPassword()
  -> PasswordResetService.completePasswordReset()
       -> Lookup Token (findByToken)
       -> Validate Token Expiry (isExpired)
       -> Encode New Password via PasswordEncoder
       -> Update User Password (user.updatePassword)
       -> Save Updated User Entity
       -> Invalidate / Delete Token (delete)
  -> Controller returns 200 OK success message
```

- **Token Expiration**: Expiration window set to 30 minutes (`EXPIRATION_MINUTES = 30`). Verification checks `Instant.now().isAfter(expiryDate)`. Expired tokens are purged upon validation failure.
- **Single-Use Invalidation**: Existing reset tokens for a user are purged prior to issuing a new token, and used tokens are deleted immediately upon successful password modification.
- **Response Status Handling**: The `/forgot-password` endpoint returns a generic response message ("If the email is registered, a password reset link has been dispatched.") to ensure predictable UI feedback. The `/reset-password` endpoint catches `IllegalArgumentException` and maps it to HTTP 400 Bad Request.

---

## 7. Full-Stack Password Reset Technical Audit & Best Practices Review

### 7.1 Architecture & Component Integration Status

| Layer / Component | Implementation Technology | Pattern & Conventions | Status | Alignment & Verification Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Backend Schema** | Flyway (`V1__init_auth_schema.sql`) | MySQL DDL, Foreign Key Cascades, Indexes | **VERIFIED** | `password_reset_tokens` table explicitly defined with `user_id` FK (`ON DELETE CASCADE`), `token` unique constraint, and composite indexes on `token` and `user_id`. |
| **Backend Entity** | JPA / Hibernate (`PasswordResetToken.java`) | `@Entity`, `@OneToOne(fetch = LAZY)`, Immutable ID | **VERIFIED** | Lazy-loaded user association prevents N+1 fetching; `@ToString(exclude = "user")` avoids circular logs; equality based on business key `token`. |
| **Backend Repository** | Spring Data JPA (`PasswordResetTokenRepository`) | Declarative Query Methods | **VERIFIED** | Provides `findByToken(String token)` and `deleteByUser(User user)` query derivation. |
| **Backend Service** | Spring `@Service` (`PasswordResetService.java`) | `@Transactional`, Constructor Injection | **VERIFIED** | Manages token lifecycle, purges prior user tokens on re-issue, encodes passwords via `PasswordEncoder`, dispatches email via `JavaMailSender`. |
| **Backend Controller** | Spring `@RestController` (`PasswordResetController`) | REST Endpoint Mapping, Request Validation | **VERIFIED** | `/api/auth/forgot-password` and `/api/auth/reset-password` endpoints validate DTOs with `@Valid`; returns uniform status messages. |
| **Backend DTOs** | Java Records (`AuthDtos.java`) | Record DTOs, Bean Validation | **VERIFIED** | `ForgotPasswordRequest` and `ResetPasswordRequest` define validation constraints (`@NotBlank`, `@Email`, `@Size(min = 8)`). |
| **Frontend Service** | Angular 17+ Service (`password-reset-service.ts`) | `inject()`, Angular Signals, RxJS | **VERIFIED** | Manages reactive state (`isLoading`, `statusMessage`, `errorMessage`) using `signal` and `computed`; uses `HttpClient` with text response type. |
| **Frontend Forgot View** | Standalone Component (`forgot-password-component`) | Signal state, NonNullable FormBuilder | **VERIFIED** | Uses `fb.nonNullable.group()`, reactive validation error feedback, PrimeNG UI components (`InputText`, `Button`), dismissible alert banners. |
| **Frontend Reset View** | Standalone Component (`reset-password-component`) | Custom Group Validator, Query Param Signals | **VERIFIED** | Extracts token from `ActivatedRoute` snapshot; enforces `passwordMatchValidator` group validation; PrimeNG `p-password` inputs with mask toggle. |
| **Frontend Routing** | Angular Routes (`app.routes.ts`) | Lazy Loading (`loadComponent`), Route Guards | **VERIFIED** | Routes `/forgot-password` and `/reset-password` lazy-load components and enforce `canActivate: [guestGuard]`. |

### 7.2 Quality & Edge Case Verification

1. **Token Lifecycle & Entropy**:
   - **Generation**: Uses `UUID.randomUUID().toString()`, yielding 122 bits of randomness.
   - **Expiration**: Enforces 30-minute expiration (`EXPIRATION_MINUTES = 30`) verified via `Instant.now().isAfter(expiryDate)`.
   - **Single-Use Enforcement**: Existing user tokens are purged prior to issuing a new token (`deleteByUser`), and used tokens are explicitly deleted (`delete(resetToken)`) after successful password update.

2. **Email Enumeration Defense**:
   - The `/forgot-password` endpoint returns `200 OK` with a generic message `"If the email is registered, a password reset link has been dispatched."` whether the target email exists in `UserRepository` or not, mitigating username/email enumeration.

3. **Front-End Validation & UX Integrity**:
   - **Touched / Dirty Validation**: Errors trigger only after control interaction (`control.dirty || control.touched`), preventing premature error messages on initial load.
   - **Group Password Matching**: `passwordMatchValidator` verifies `newPassword === confirmPassword` at form group level and displays feedback under `confirmPassword` input.
   - **Accessibility & ARIA Attributes**: Alert banners specify `role="alert"`, `aria-live="assertive"`/`polite"`, dismiss buttons specify `aria-label`, and inputs bind correctly to `<label for="...">`.
   - **Loading & State Controls**: Submit buttons toggle `[disabled]` and `[loading]` during HTTP transactions, preventing double-submission.

### 7.3 Actionable Recommendations

- **[Backend Performance / Resilience] Async Email Dispatch**:
  - *Current State*: `sendResetEmail()` is executed synchronously within the `@Transactional` method `initiatePasswordReset()`.
  - *Recommendation*: Annotate email dispatch or event listener with `@Async` or decouple mail dispatch from the database transaction boundary. This prevents database connection holding during SMTP network latency and isolates mail delivery failures from rolling back the token entity.
- **[Backend Error Handling] ProblemDetail Exception Handling**:
  - *Current State*: `PasswordResetController` catches `IllegalArgumentException` and manually returns `ResponseEntity.badRequest().body(ex.getMessage())`.
  - *Recommendation*: Delegate exception handling to `GlobalExceptionHandler` returning RFC 7807 `ProblemDetail` responses for unified error reporting across all auth endpoints.
- **[Frontend Cleanup] Artifact & Debug Template Clean-Up**:
  - *Current State*: `reset-password-component.html` contains an unremoved starter paragraph `<p>reset-password-component works!</p>` above `<div class="login-page">`.
  - *Recommendation*: Remove the leftover starter `<p>` element to ensure clean layout rendering.
- **[Frontend Code Clean-Up] Unused Imports**:
  - *Current State*: `password-reset-service.ts` imports unused `Service` symbol from `@angular/core`.
  - *Recommendation*: Remove `Service` from `@angular/core` import declaration.


