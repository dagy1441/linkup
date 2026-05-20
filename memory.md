# LinkUp — Implementation Memory

Living record of what is built, what is pending, and the design decisions taken.
Updated at the end of every phase that meets the Definition of Done (CLAUDE.md).

---

## Phase A — Cross-cutting foundations ✅

**Status:** DONE — DoD met.

### Delivered

| Layer | Components |
|------|------------|
| `core/security` | `JwtProperties` (`@ConfigurationProperties`), `JwtAuthConverter` (maps `realm_access.roles` → `ROLE_*`), `SecurityConfig` (stateless, OAuth2 RS, public reads on `/api/v1/activities/**` + Swagger + Actuator health) |
| `core/exception` | `DomainException` hierarchy — `ResourceNotFoundException` (404), `ConflictException` (409), `BusinessRuleViolationException` (422), `ForbiddenOperationException` (403) — plus `ApiError` record and `GlobalExceptionHandler` (`@RestControllerAdvice`) covering also `OptimisticLockingFailureException`, `MethodArgumentNotValidException`, `AccessDeniedException`, `AuthenticationException`, fallback to 500 |
| `core/audit` | `Auditable` `@MappedSuperclass` (`createdAt/updatedAt/createdBy/updatedBy/@Version`), `AuditorAwareImpl` (reads JWT `sub`) |
| `core/configuration` | `JpaAuditingConfig` (`@EnableJpaAuditing`), `OpenApiConfig` (Bearer JWT scheme) |
| `shared/dto` | `PageResponse<T>` envelope |
| `shared/event` | `DomainEvent` marker interface |

### Tests

- `JwtAuthConverterTest` — 4 cases (claim path, missing claim, blank, custom path)
- `GlobalExceptionHandlerTest` — 7 cases (each HTTP status path)
- `ModularityTest` — `ApplicationModules.verify()` + PlantUML doc generation

**13/13 tests green.**

---

## Phase B — Auth module ✅

**Status:** DONE — DoD met.

### Delivered

| Layer | Components |
|------|------------|
| `feature/auth` | `@ApplicationModule(displayName = "Auth")` |
| `domain/model` | `User` (aggregate root, JPA entity extending `Auditable`, role collection via `@ElementCollection`), `UserStatus` enum (ACTIVE, DISABLED) |
| `domain` | `UserRepository` (port: `findByKeycloakId`, `findById`, `existsByKeycloakId`, `save`) |
| `domain/event` | `UserProvisionedEvent` (record implementing `DomainEvent`) |
| `domain/exception` | `UserNotFoundException extends ResourceNotFoundException` |
| `application` | `UserProvisioningService` (idempotent `ensureLocalUser(Jwt)` — creates+publishes event OR refreshes), `CurrentUserService` (resolves SecurityContext → User, lazy provisioning) |
| `infrastructure/persistence` | `JpaUserRepository extends JpaRepository<User, UUID>, UserRepository` |
| `infrastructure/web` | `JwtUserProvisioningFilter` — `OncePerRequestFilter` at `Ordered.LOWEST_PRECEDENCE` so it runs **after** Spring Security populates the context |
| `controller` | `MeController` (`GET /api/v1/auth/me`), `UserResponse` DTO |
| Flyway | `V1__create_users_table.sql` — `users` + `user_roles` tables with `uk_users_keycloak_id`, `ix_users_email`, `ix_user_roles_user_id` |

### Tests (22 green, 3 IT skipped without Docker)

- **Domain** — `UserTest` (6 cases): invariants, factory, sync, status toggle, defensive copy
- **Application** — `UserProvisioningServiceTest` (4 cases, Mockito): create + event, refresh-no-event, missing email rejection, displayName fallback chain
- **Controller** — `MeControllerTest` (`@WebMvcTest`, security autoconfig excluded): JSON payload assertions
- **Infrastructure IT** — `JpaUserRepositoryIT` (`@DataJpaTest` + `jdbc:tc:postgresql`, gated by `dockerAvailable()`): persist+find, exists, unique constraint

### Architecture verification

- Spring Modulith: GREEN. `feature.auth` depends only on `core::audit`, `core::exception`, `shared::event` (all exposed via `@NamedInterface`).
- No direct cross-module access to internal packages.

### Design decisions

1. **JPA annotations directly on aggregates** (KISS) — no separate persistence model. Revisit if domain purity becomes a hard requirement.
2. **Filter-based provisioning** (`JwtUserProvisioningFilter` as `@Component` outside the Security filter chain) so `core/security` keeps zero knowledge of feature modules — clean boundary.
3. **Roles as `Set<String>` snapshot** on `User` — fresh data comes from JWT each request via `syncFromIdentity`. We don't try to be the source of truth for roles; Keycloak is.
4. **Keycloak owns signup** — users register via Keycloak's built-in page; backend only mirrors. No Admin API call from this codebase.
5. **`UserProvisionedEvent`** is the only cross-module signal emitted by auth. Consumers (notification, recommendation) can subscribe later without coupling.

### DoD checklist

- ✅ Functional: provisioning + `/me` work end-to-end
- ✅ Tests: domain / application / controller / IT all written, 22 green
- ✅ Documentation: OpenAPI annotations on `MeController`, JavaDoc on services
- ✅ Zero tech debt: no TODO, no commented code, no hacks
- ✅ Deployable: stateless, env-driven config
- ✅ Observability: SLF4J logs at INFO/DEBUG in services + filter + controller, Actuator/Prometheus inherited from Phase A, OTel ready
- ✅ Security: JWT validation, role mapping, no secrets, principle of least privilege
- ✅ Performance: unique index on `keycloak_id`, secondary index on `email`, no N+1 (roles eager but bounded)
- ✅ Inter-module: `UserProvisionedEvent` via `ApplicationEventPublisher`, no direct deps

---

## Phase C — Activity module ✅

**Status:** DONE — DoD met.

### Delivered

| Layer | Components |
|------|------------|
| `feature/activity` | `@ApplicationModule(displayName = "Activity")` |
| `domain/model` | `Activity` (aggregate with capacity + bookedCount accounting), `Location` `@Embeddable` VO, `ActivityStatus` enum (PUBLISHED, CANCELLED) |
| `domain` | `ActivityRepository` port (queries + atomic seat ops) |
| `domain/event` | `ActivityCreatedEvent`, `ActivityCancelledEvent` |
| `domain/exception` | `ActivityNotFoundException` (404), `ActivityAccessDeniedException` (403), `ActivityInvalidStateException` (422), `ActivityFullException` (409) |
| `application` | `ActivityProperties` (`linkup.activity.{default,max}-page-size`), `ActivityCommandService` (create/update/cancel + organizer authorization), `ActivityQueryService` (paginated list + city filter, page-size cap), `ActivitySeatServiceImpl` |
| `api` (named interface) | `ActivitySeatService` — the only cross-module contract; booking will inject this |
| `infrastructure/persistence` | `JpaActivityRepository` — JPQL queries + `@Modifying` atomic UPDATEs for seat reserve/release |
| `controller` | `ActivityController` — GET list (public), GET id (public), POST create, PUT update, DELETE cancel |
| `controller/dto` | `ActivityRequest` (Bean Validation: `@NotBlank`, `@Future`, `@Positive`, `@Size`), `ActivityResponse` |
| Flyway | `V2__create_activities_table.sql` — CHECK constraints (capacity > 0, booked_count in [0,capacity], lat/lng ranges, status enum), FK to users, 3 indexes |
| `core/configuration` | `ClockConfig` — `Clock.systemUTC()` bean for deterministic testability |
| `LinkupApplication` | `@ConfigurationPropertiesScan` added |

### Cross-module wiring

- New `feature.auth.api.CurrentUserAccessor` interface (named interface `api`); `CurrentUserService` implements it.
- `ActivityController` depends on `CurrentUserAccessor` instead of the internal `CurrentUserService` → clean Modulith boundary.

### Tests (38 new, all green; full suite **61/61**)

- **Domain** — `ActivityTest` (12), `LocationTest` (6): factory invariants, status transitions, seat reserve/release, organizer check, capacity vs booked guards
- **Application** — `ActivityCommandServiceTest` (6, Mockito): create+event, update by organizer, forbidden update, not-found, cancel+event, cancel by non-organizer
- **Application** — `ActivityQueryServiceTest` (4): default page size, lowercased city filter, max-size cap, not-found
- **Application** — `ActivitySeatServiceImplTest` (3): reserve true/false, release no-op
- **Controller** — `ActivityControllerTest` (`@WebMvcTest`, security excluded): list envelope, get-by-id, create 201, validation 400, cancel 200

### Architecture verification

- Spring Modulith: GREEN. `feature.activity` reaches only into `core::audit`, `core::exception`, `shared::event`, `shared::dto`, `feature.auth::api`.
- `LinkupApplicationTests` (full context load + Testcontainers Postgres) passes — V1 + V2 migrations apply cleanly.

### Design decisions

1. **Atomic seat counter on `Activity`** (`reserveSeatAtomic`/`releaseSeatAtomic` via `@Modifying` JPQL) — race-free without row locks. `booked_count` denormalized; CHECK constraint guards integrity.
2. **No DRAFT status for MVP** (YAGNI) — activities are immediately PUBLISHED on create. Add DRAFT later when scheduling/preview is needed.
3. **`Clock` bean** injected into time-sensitive services — tests use `Clock.fixed(...)` for determinism.
4. **Page-size cap configurable** via `linkup.activity.max-page-size` (default 100). Hard-coded fallback in `ActivityProperties` if not set.
5. **City filter normalised to lower-case** on both write (validation untouched, normalisation at query) and query side — index on `LOWER(city)` matches.
6. **`feature.auth.api.CurrentUserAccessor`** — minimal facade returning just the internal `userId`. Avoids leaking the `User` aggregate across modules.
7. **`@ConfigurationPropertiesScan`** on `LinkupApplication` — all `@ConfigurationProperties` records discovered automatically (no per-class `@EnableConfigurationProperties`).

### DoD checklist

- ✅ Functional: CRUD + pagination + city filter + organizer authorization work end-to-end
- ✅ Tests: domain / application / controller / full context-load all written, 61 green total
- ✅ Documentation: OpenAPI annotations on `ActivityController`, JavaDoc on domain + services
- ✅ Zero tech debt: no TODO, no commented code
- ✅ Deployable: Flyway V2 idempotent, no Hibernate auto-DDL
- ✅ Observability: SLF4J INFO on writes, DEBUG on seat ops
- ✅ Security: public GETs explicit in `SecurityConfig`, writes require JWT, organizer check in service
- ✅ Performance: indexes on `(status, starts_at)`, `(organizer_id)`, `LOWER(city)`; no N+1; atomic UPDATE for capacity
- ✅ Inter-module: `ActivityCreatedEvent`/`ActivityCancelledEvent` published; `ActivitySeatService` exposed via `@NamedInterface("api")`

---

## Phase D — Booking module 🚧 PENDING

### Scope (planned)
- `Booking` aggregate, partial unique index `(user_id, activity_id) WHERE status='CONFIRMED'`
- `BookingService.create` calling `ActivitySeatService.tryReserveSeat` (atomic, race-free)
- Enforce `linkup.booking.max-items-per-user` cap
- `BookingCreatedEvent` / `BookingCancelledEvent`
- Concurrency test (N parallel bookings on capacity-1 activity → exactly 1 success)

---

## Architecture audit + critical fixes ✅ (post-Phase C)

A senior-architect audit was run against `CLAUDE.md` rules. Verdict: **PASS WITH MINOR ISSUES**.
The 5 critical issues were fixed in this pass; the 17 minor suggestions remain logged in the audit report.

### Fixes applied

1. **Dual provisioning eliminated** — deleted `JwtUserProvisioningFilter` (and empty `infrastructure/web/` dir). Lazy provisioning via `CurrentUserService.getCurrent()` is now the **single** path. Removes per-request DB writes on every authenticated call, fixes the brittle filter ordering, and prevents writes when a JWT is sent on a public GET.
2. **Role-based authorization** — `@PreAuthorize("hasRole('ORGANIZER')")` added on `POST/PUT/DELETE /api/v1/activities/**`. `@EnableMethodSecurity` was already on; now it has work to do.
3. **`Clock` threaded through `Activity.update`** — `setStartsAt(startsAt, now)` and `update(..., now)` now require the caller's clock. `ActivityCommandService` injects `Clock` and passes `Instant.now(clock)`. Domain remains framework-free; tests are fully deterministic.
4. **`organizerId` removed from public response** — replaced with `organizerDisplayName`. New cross-module API `feature.auth.api.UserDirectory` (single + batch lookup) implemented by `UserDirectoryService`. `ActivityController` enriches responses via `findDisplayNames(...)` for list (batch) and `findDisplayName(...)` for single — no N+1.
5. **Test gaps filled** — added missing `update` controller test, fallback-name test, and a domain test verifying `update` rejects past `startsAt` relative to the caller's clock.

### Updated signatures (breaking the Activity factory)

- `Activity.create(title, desc, location, startsAt, capacity, organizerId, now)`
- `Activity.update(title, desc, location, startsAt, capacity, now)`

### Tests after fixes: **64/64 green** (+ Modulith verify green + full context load via `LinkupApplicationTests` + Testcontainers)

### Structural refactor — option C (alignment with updated CLAUDE.md)

Following an audit gap analysis, the **module layout was tightened** for both `feature/auth` and `feature/activity`:

| Before | After |
|--------|-------|
| `feature/<m>/controller/...` | `feature/<m>/infrastructure/rest/controller/...` |
| `feature/<m>/controller/dto/...` | `feature/<m>/infrastructure/rest/dto/...` |
| `feature/<m>/infrastructure/persistence/Jpa<M>Repository.java` | `feature/<m>/infrastructure/persistence/jpa/Jpa<M>Repository.java` |

Why: REST is one driving adapter among possibly many (GraphQL, gRPC later) — placing it under `infrastructure/rest/` matches hexagonal intent. The `persistence/jpa/` subfolder leaves room for non-JPA adapters (cache, search) without touching the JPA one.

**Not adopted**: the `application/{command,query,usecases}/` subdivision proposed by CLAUDE.md. For a 2-3-service module it adds churn without organisational value (KISS / YAGNI). CLAUDE.md was relaxed: subdivide only when a module reaches ≥ 4 services.

Tests follow the same package layout. `excludeFilters` regex in `@WebMvcTest` was simplified to just `core.security.*` (the deleted `JwtUserProvisioningFilter` no longer needs an exclusion).

**All 64 tests still pass · Spring Modulith verify GREEN.**

### Outstanding minor items (deferred)

Tracked from audit report, not blocking Phase D:

- Empty scaffold packages (`infrastructure/{docker,gitlab,...}/`, `feature/activity/presentation/`) — pure noise, can be deleted
- `spring-boot-starter-mail` and `spring.cache.type: redis` declared but unused — remove until needed
- DRY: JWT claim-path traversal duplicated between `JwtAuthConverter` and `UserProvisioningService` — extract helper in `core.security`
- `@SuppressWarnings("unchecked")` on `UserProvisioningService.extractRealmRoles` is misplaced (no unchecked cast in body)
- `PageResponse` could carry `hasNext`/`hasPrevious`
- Redundant `@EnableConfigurationProperties(JwtProperties.class)` in `SecurityConfig` (covered by `@ConfigurationPropertiesScan`)

---

## Phase D — Booking module 🚧 PENDING

### Scope (planned)
- `Booking` aggregate, partial unique index `(user_id, activity_id) WHERE status='CONFIRMED'`
- `BookingService.create` with atomic seat reservation against activity's `booked_count`
- Enforce `linkup.booking.max-items-per-user` cap
- `BookingCreatedEvent` / `BookingCancelledEvent`
- Concurrency test (N parallel bookings on capacity-1 activity → exactly 1 success)
