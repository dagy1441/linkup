# CLAUDE.md

Guide pour Claude Code et l'équipe travaillant sur LinkUp.
Ce fichier contient **uniquement** les règles techniques actionnables.
Pour la vision produit, les acteurs, les features et le backlog : voir [`docs/PRODUCT.md`](docs/PRODUCT.md).
État courant d'implémentation par phase : voir [`memory.md`](memory.md).

---

## 1. Vision (résumé)

LinkUp connecte les personnes via des activités réelles et locales en Afrique de l'Ouest (lancement Abidjan).
Acteurs : **participants**, **organisateurs**, **partenaires**.
Stack produit : Web & Mobile, paiement Mobile Money (Orange Money / MTN / Wave).

## 2. Roadmap modules

| Phase | Modules | Statut |
|-------|---------|--------|
| MVP | `auth`, `activity`, `booking`, `payment`, `notification`, `profile` | auth ✅ · activity ✅ · booking 🚧 |
| V2 | `social`, `recommendation`, `organizer`, `moderation` | — |
| V3 | `analytics`, `catalog` | — |

---

## 3. Architecture

**Stack** : Clean Architecture + DDD + Spring Modulith (modular monolith) +
Event-driven + Security-first (Keycloak / OAuth2).

### 3.1 Layout top-level

```
com.siide.linkup/
├── core/            # cross-cutting : audit, configuration, exception, security
├── feature/         # un sous-package par bounded context
├── infrastructure/  # artefacts déploiement : docker, helm, k3s, gitlab-ci, monitoring
└── shared/          # DTOs + events partagés (kernel)
```

### 3.2 Layout par module

```
feature/<module>/
├── package-info.java        @ApplicationModule(displayName = "<Module>")
├── api/                     @NamedInterface("api") — contrats exposés cross-module
├── domain/
│   ├── model/               aggregates, value objects, enums
│   ├── event/               domain events (records implementing DomainEvent)
│   ├── exception/           exceptions typées (extends core.exception.*)
│   └── <Module>Repository   port (interface)
├── application/             @Service use-cases, frontières transactionnelles
│   └── dto/                 command + view records
├── infrastructure/
│   ├── persistence/jpa/     adapters JPA
│   └── rest/
│       ├── controller/      controllers fins
│       └── dto/             request/response records
└── internal/                (optionnel) détails d'implémentation privés
```

**Subdivision `application/` (command/query/usecase)** : seulement à partir de
**≥ 4 services** dans le module. Avant : plat (KISS).

**`internal/`** : helpers, strategies, schedulers, listeners privés.
Code appartenant au module mais non destiné à être consulté de l'extérieur.

### 3.3 Règles de couches

- **Domain** : framework-light. Spring **interdit**. JPA annotations **tolérées**
  sur les aggregates tant que la persistence reste relationnelle (décision KISS,
  cf. memory.md Phase B). Pas de dépendance vers `application/`, `infrastructure/`.
- **Application** : orchestration use-cases, transactions. Pas de JPA direct,
  passe par les ports `domain`.
- **Infrastructure** : adapters (JPA, Redis, REST externes, messaging).
- **Controllers** : très fins, zéro logique métier.

### 3.4 Cross-module : Spring Modulith named interfaces

Sub-packages **internal par défaut**. Pour exposer : `package-info.java`
annoté `@NamedInterface("<nom>")`. Exposés aujourd'hui :

| Module | Sous-packages exposés |
|--------|----------------------|
| `core` | `audit`, `exception` |
| `shared` | `event`, `dto` |
| `feature.auth` | `api` (`CurrentUserAccessor`, `UserDirectory`) |
| `feature.activity` | `api` (`ActivitySeatService`) |

**Règle absolue** : un module ne référence un autre module **que via** son
`api/`. Aucun accès à `domain/`, `application/`, `infrastructure/` d'un autre module.

Vérification : `./mvnw test -Dtest=ModularityTest`.

### 3.5 Communication asynchrone

Domain events publiés via `ApplicationEventPublisher`.
Exemples : `BookingCreatedEvent → notification`,
`PaymentCompletedEvent → activity`.

**Contrat de stabilité** : les events `shared/event/*` sont des **contrats**.
Évolution = additive (nouveaux champs nullable) ou nouvelle version (`*V2`).
Préparation à un schema registry / extraction microservices.

---

## 4. Principes

**SOLID** obligatoire · **DRY** · **KISS** (simple > clever) · **YAGNI** (jamais
de feature spéculative).

**Naming** : verbes pour méthodes (`create`, `reserveSeat`), noms pour classes
(`Activity`, `BookingService`), constantes en `SCREAMING_SNAKE_CASE`.

---

## 5. Git workflow

### 5.1 Branches

| Branche | Rôle |
|---------|------|
| `main` | production, toujours déployable |
| `develop` | intégration des features prêtes pour la prochaine release |
| `feature/<scope>-<slug>` | nouvelle feature ; merge dans `develop` via PR |
| `fix/<scope>-<slug>` | bugfix non urgent ; merge dans `develop` |
| `hotfix/<scope>-<slug>` | bug critique en prod ; merge dans `main` **et** `develop` |
| `chore/<slug>` | tâches techniques (deps, CI, refacto isolé) |
| `docs/<slug>` | doc only |

**Conventions de nommage** :

- `scope` = nom du module (`auth`, `activity`, `booking`, ...) ou `core`, `infra`.
- `slug` = description courte en `kebab-case`.
- Exemples : `feature/booking-create-endpoint`, `fix/activity-pagination-cap`,
  `hotfix/payment-orange-callback-500`, `chore/bump-spring-boot-4-0-1`.

**Règles** :

1. Une feature = **une** branche dédiée, partant de `develop`.
2. La branche reste **courte** (idéalement < 1 semaine). Si elle grossit, découper.
3. Rebase sur `develop` avant la PR (pas de merge commit de `develop` dans la feature).
4. PR obligatoire — pas de push direct sur `main` ni `develop`. Minimum 1 review approuvée.
5. CI verte obligatoire (`./mvnw clean test` + `ModularityTest`).
6. Branche supprimée après merge.

### 5.2 Conventional Commits

Tous les commits suivent [Conventional Commits 1.0](https://www.conventionalcommits.org/).

**Format** :

```
<type>(<scope>): <description>

[body optionnel]

[footer optionnel]
```

**Types autorisés** :

| Type | Usage |
|------|-------|
| `feat` | nouvelle fonctionnalité |
| `fix` | correction de bug |
| `docs` | documentation uniquement |
| `style` | formatage, point-virgule, espaces (pas de changement de logique) |
| `refactor` | refacto sans changement de comportement ni de fix |
| `perf` | amélioration de performance |
| `test` | ajout / correction de tests |
| `build` | changements affectant le build ou les deps externes (Maven, Docker) |
| `ci` | configuration CI (Jenkins, GitLab CI, ArgoCD) |
| `chore` | tâche de maintenance n'entrant pas dans les autres catégories |
| `revert` | annulation d'un commit précédent |

**Scope** : nom du module ou couche (`auth`, `activity`, `booking`, `core`, `infra`, `docs`).
Optionnel mais **fortement recommandé**.

**Description** : impératif présent, minuscule, **pas de point final**, ≤ 72 chars.

**Breaking change** : suffixer le type/scope par `!` **et** ajouter un footer
`BREAKING CHANGE: <explication>`.

**Exemples** :

```
feat(booking): add POST /api/v1/bookings endpoint
fix(activity): cap page size to configured max
refactor(auth): extract JWT claim helper to core.security
test(activity): add concurrency test for seat reservation
docs(product): add backlog mapping to modules
build(deps): bump spring-boot to 4.0.1
chore(ci): enable ModularityTest in GitLab pipeline
feat(payment)!: switch to async confirmation flow

BREAKING CHANGE: POST /api/v1/payments now returns 202 + callback
instead of 200. Clients must subscribe to payment.completed event.
```

**Référence ticket** dans le footer si applicable : `Refs: US-043` ou `Closes: US-043`.

### 5.3 Pull Request

- **Titre** : même format qu'un commit Conventional (`feat(booking): ...`).
- **Description** : contexte + US référencée + check-list DoD.
- **Squash & merge** par défaut. Le commit final doit respecter Conventional Commits.

---

## 6. Build & Run

```bash
./mvnw spring-boot:run            # dev profile par défaut
./mvnw clean package              # build + package
./mvnw clean test                 # full suite
./mvnw test -Dtest=ClassName      # une classe
./mvnw test -Dtest=ClassName#m    # une méthode
```

## 7. Local dev setup

1. `cp .env.exampl .env` et ajuster.
2. Démarrer les dépendances via profils Compose :
   ```bash
   docker compose up -d                          # postgres + redis + keycloak
   docker compose --profile tools up -d          # + pgadmin + mailhog
   docker compose --profile app up -d --build    # stack complète + backend
   ```
3. Services :
   - PostgreSQL → `localhost:5432` (`postgres/postgres`, db `linkup`)
   - Redis → `localhost:6379`
   - Keycloak → `http://localhost:8081` (admin/admin), realm `linkup`, management `:9001`
   - PgAdmin (profil `tools`) → `http://localhost:5050`
   - Mailhog (profil `tools`) → SMTP `:1025`, UI `http://localhost:8025`
4. Swagger UI (dev only) : `http://localhost:8080/swagger-ui.html`.

### Docker image

Multi-stage, non-root (`linkup`), `tini` en PID 1, healthcheck
`/actuator/health/liveness`, expose `8080`.

```bash
docker build -t linkup-backend:local .
docker compose --profile app build
```

---

## 8. Security

**OAuth2 JWT via Keycloak.** Backend = Resource Server. Roles lus depuis
`realm_access.roles` (configurable : `linkup.security.jwt.role-claim`).
Beans de sécurité dans `core/security/`.
Issuer dev : `http://localhost:8081/realms/linkup` (`KEYCLOAK_ISSUER_URI`).

**Authorization** : `@PreAuthorize` sur les writes (`hasRole('ORGANIZER')`, etc.).
**Provisioning utilisateur** : lazy via `CurrentUserService.getCurrent()`.
**Aucun secret en dur** : variables d'environnement uniquement.

### Realm bootstrap

Auto-import au boot depuis `infrastructure/keycloak/realms/linkup-realm.json` :
- Rôles : `user` (défaut), `organizer`, `admin`
- Client : `linkup-web` (public, PKCE, direct access grants pour tests)
- Users seed : `alice@linkup.io / alice123` (organizer+user), `bob@linkup.io / bob123` (user)

Token de test :
```bash
curl -s -X POST http://localhost:8081/realms/linkup/protocol/openid-connect/token \
  -d 'grant_type=password' -d 'client_id=linkup-web' \
  -d 'username=alice@linkup.io' -d 'password=alice123' | jq -r .access_token
```

Reset realm : stop Keycloak → `docker volume rm linkup_postgres_data` → restart.

---

## 9. Database

**Flyway** dans `src/main/resources/db/migration` : `V{n}__{description}.sql`.
DDL auto Hibernate **jamais** en prod.
Indexes obligatoires sur clés critiques + index secondaires sur filtres fréquents.

**Pas de FK cross-module.** Si `booking` référence `activity.id`, c'est un UUID sans
contrainte SQL. Prépare l'extraction microservices.

### Profiles

| Profile | DB DDL | Usage |
|---------|--------|-------|
| `dev` (défaut) | `update` | local |
| `prod` | `validate` | prod |
| `test` | `create-drop` | Testcontainers |

---

## 10. Observabilité (obligatoire par module)

- **Logs** structurés SLF4J JSON, MDC enrichi (`module`, `traceId`, `userId`).
  INFO sur les writes, DEBUG sur les détails.
- **Metrics** : `/actuator/health|metrics|prometheus`. Naming `linkup.<module>.*`.
- **Tracing** : OpenTelemetry auto-instrumentation, propagation `traceparent`.
- **Erreurs** : passer par `core.exception.*` + `GlobalExceptionHandler`.

---

## 11. Tests

- **Unit** : domain pur (JUnit + AssertJ), application avec Mockito.
- **Controller** : `@WebMvcTest` avec exclusion `core.security.*`,
  `excludeAutoConfiguration = {SecurityAutoConfiguration, ServletWebSecurityAutoConfiguration, OAuth2ResourceServerAutoConfiguration}`,
  `addFilters = false`.
- **IT JPA** : `@DataJpaTest` + Testcontainers Postgres, gardé par `dockerAvailable()`.
- **Context-load** : `LinkupApplicationTests` (Testcontainers).
- **Modularité** : `ModularityTest` (`ApplicationModules.verify()`) — **bloquant en CI**.

**Clock** : injecter `Clock` dans les services time-sensitive ; tests avec
`Clock.fixed(...)`.

---

## 12. Configuration

| Property | Default | Env var |
|----------|---------|---------|
| `linkup.security.jwt.role-claim` | `realm_access.roles` | — |
| `linkup.booking.max-items-per-user` | `5` | `BOOKING_MAX_ITEMS_PER_USER` |
| `linkup.activity.default-page-size` | `20` | `ACTIVITY_DEFAULT_PAGE_SIZE` |
| `linkup.activity.max-page-size` | `100` | `ACTIVITY_MAX_PAGE_SIZE` |

Tous les `@ConfigurationProperties` sont scannés via `@ConfigurationPropertiesScan`
sur `LinkupApplication`.

---

## 13. Definition of Done

Une feature est terminée **uniquement** si :

- ✅ **Fonctionnel** : besoin du ticket entièrement respecté.
- ✅ **Tests** : unit + controller + IT pertinents écrits et verts ; aucun test cassé.
- ✅ **Documentation** : OpenAPI sur endpoints, JavaDoc sur services/domain ;
  `memory.md` mis à jour si la phase atteint la DoD.
- ✅ **Zero dette** : pas de TODO, code commenté, hack non documenté.
- ✅ **Déployable** : context-load vert ; migrations Flyway idempotentes.
- ✅ **Observabilité** : logs structurés, metrics, gestion d'erreurs propre.
- ✅ **Sécurité** : pas de secret en dur, JWT validé, principe du moindre privilège.
- ✅ **Performance** : pagination sur toutes les listes, indexes DB, pas de N+1,
  cache Redis si lecture fréquente.
- ✅ **Inter-module** : events via `ApplicationEventPublisher`, aucune dépendance
  directe ; contrats `api/`.
- ✅ **Git** : commits Conventional Commits, branche `feature/*` rebasée sur
  `develop`, PR approuvée, branche supprimée après merge.

---

## 14. Règles IA / Team

Toute IA ou dev travaillant sur LinkUp **doit** :

1. Respecter l'architecture sans exception.
2. Ne jamais contourner SOLID / DDD / les frontières Modulith.
3. Ne jamais introduire de logique métier dans un controller ou une couche infra.
4. Ne jamais bypass les tests (`--no-verify`, skip, ignore).
5. Ne jamais livrer du code non testable.
6. Respecter le git workflow (branches + Conventional Commits) sans exception.
7. Mettre à jour [`memory.md`](memory.md) à la fin de chaque phase respectant la DoD.
