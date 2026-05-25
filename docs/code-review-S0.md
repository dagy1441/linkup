# Code review — Sprint Hardening S0

**Périmètre** : `feature/auth`, `feature/activity`, `feature/booking`, `core/idempotency`, `core/security`.
**Date** : 2026-05-25 · **Reviewer** : Claude (sécurité + perf ciblée).
**Verdict global** : **PASS avec 2 HIGH à corriger avant prod**.

Légende : **HIGH** = bug ou faille à traiter avant la prochaine PR · **MEDIUM** = à planifier dans S0/S1 · **LOW** = backlog tech, à grouper.

---

## HIGH

### H-1 — Inconsistance silencieuse si `completeInNewTx` échoue après succès du handler

**Fichier** : `core/idempotency/IdempotencyService.java:106-114`

```java
try {
    ResponseEntity<T> response = handler.get();
    completeInNewTx(pending.getId(), response);   // ⬅ si crash ici…
    return response;
} catch (RuntimeException businessError) {
    deleteInNewTx(pending.getId());               // ⬅ on supprime le pending
    throw businessError;
}
```

**Scénario** :
1. `handler.get()` réussit → booking créé + seats réservés + TX committée.
2. `completeInNewTx` plante (DB indisponible 200ms, réseau, etc.).
3. On entre dans le `catch` → on **supprime la ligne pending** + on rethrow.
4. Le client voit une 500.
5. Le client retry avec la **même Idempotency-Key**.
6. `findByKeyAndUserIdAndEndpoint` → vide (on a supprimé). On crée un **second** booking. ⚠️

**Impact** : double booking, double prélèvement de seats, perte d'intégrité.

**Fix proposé** :
- Catcher séparément la branche "completion failure" : si le handler a réussi, ne **PAS** delete la pending row. Marker à la place comme "completed-but-uncached" (status = 200 + body = null), et au prochain replay renvoyer un statut OK générique avec un header `X-Idempotency-Replay: completed-without-body`.
- Ou : retry interne 3x sur `completeInNewTx` avec backoff exponentiel.
- Ou plus simple : **fail loudly** (laisser propager la 500 sans delete) → le pending row protège contre le retry (409 IN_PROGRESS jusqu'à TTL).

Test à ajouter : mock `repository.save` pour throw dans la branche complete + vérifier qu'un second `execute` même clé ne ré-exécute pas le handler.

---

### H-2 — Aucun `CorsConfigurationSource` mais `.cors(cors -> {})` activé

**Fichier** : `core/security/SecurityConfig.java:28`

```java
.cors(cors -> {})
```

Active le filtre CORS sans définir de source. Conséquence Spring Security 7 : aucun header `Access-Control-Allow-Origin` n'est émis. Le SPA / mobile web qui s'attaquera au backend en prod **sera bloqué par le navigateur**. En dev (Bruno = client HTTP brut) c'est invisible.

**Fix proposé** : créer un bean `CorsConfigurationSource` paramétré par `linkup.security.cors.allowed-origins` (env vars).

```java
@Bean
CorsConfigurationSource corsConfigurationSource(CorsProperties props) {
    var cfg = new CorsConfiguration();
    cfg.setAllowedOriginPatterns(props.allowedOrigins()); // env-driven
    cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setExposedHeaders(List.of("Location","Idempotency-Key"));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(3600L);
    var src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/api/**", cfg);
    return src;
}
```

Default dev : `["http://localhost:*"]`. Prod : domaine app + domaine mobile.

---

## MEDIUM

### M-1 — `Activity.cancel()` laisse les bookings orphelins

**Fichier** : `feature/activity/domain/model/Activity.java:102` + `ActivityCommandService.cancel`

Quand un organizer annule une activité :
- `Activity.status = CANCELLED`
- `ActivityCancelledEvent` publié
- **MAIS** : `bookedCount` reste positif, les bookings restent `CONFIRMED`, aucun listener ne les annule

**Impact** : les users voient leur booking comme actif alors que l'activité est morte. Pas de remboursement déclenché (S4).

**Fix proposé** (à faire avec S4 payment) :
- `feature/booking` écoute `ActivityCancelledEvent` via `@ApplicationModuleListener`
- → annule tous les bookings CONFIRMED de cette activité
- → émet `BookingCancelledEvent(reason="ACTIVITY_CANCELLED")`
- → `feature/payment` écoute et trigger le refund

Trace dans memory.md comme dette explicite si non fait maintenant.

---

### M-2 — Composite index manquant sur `bookings (user_id, status, created_at DESC)`

**Fichier** : `db/migration/V3__create_bookings_table.sql:30-32`

Index actuels : 3 monocolonnes (`user_id`, `activity_id`, `status`). La requête `findByUserIdAndStatus` ordonnée par `createdAt DESC` (BookingQueryService.listMine) fait :

```sql
SELECT ... FROM bookings WHERE user_id = ? AND status = ? ORDER BY created_at DESC LIMIT 20;
```

Postgres choisira `ix_bookings_user_id` puis filtrera/triera en mémoire. Sur 10k+ bookings/user (V2 power-users), ça scan beaucoup.

**Fix proposé** (V6 migration ou inclus dans V3 si rejouable) :
```sql
CREATE INDEX ix_bookings_user_status_created ON bookings (user_id, status, created_at DESC);
-- Drop ix_bookings_user_id et ix_bookings_status (redondants couverts par le composite)
DROP INDEX ix_bookings_user_id;
DROP INDEX ix_bookings_status;
```

---

### M-3 — Index `LOWER(city)` manquant sur `activities`

**Fichier** : `db/migration/V2__create_activities_table.sql` + `JpaActivityRepository.findPublishedUpcomingByCity`

Query : `WHERE LOWER(a.location.city) = :city` — l'index actuel est sur `city` direct, donc inutilisé pour cette query. Toute filter par ville fait un seq scan.

**Fix proposé** (V6) :
```sql
CREATE INDEX ix_activities_city_lower ON activities (LOWER(city));
DROP INDEX ix_activities_city;
```

---

### M-4 — `BookingCommandService.cancel` peut laisser des seats fantômes

**Fichier** : `feature/booking/application/BookingCommandService.java:97-100`

```java
booking.cancel(Instant.now(clock));
seatService.releaseSeats(booking.getActivityId(), booking.getSeats());
```

`releaseSeatsAtomic` retourne 0 silencieusement si `booked_count < qty` (drift de données, double cancel). Logge un WARN mais ne throw pas → le booking est CANCELLED en domaine, mais l'invariant capacité est cassé.

**Fix proposé** : si `updated == 0`, lever une exception dans `ActivitySeatServiceImpl.releaseSeats` (current : juste log.warn). La TX outer rollback → le booking reste CONFIRMED → on retourne 409 au client.

---

### M-5 — Pas de limite globale sur la taille des payloads JSON

**Fichier** : `application.yaml` (manquant)

Tomcat défault = 2MB. Suffit pour 99% des requêtes, mais un attaquant peut envoyer 2MB de JSON → SHA-256 hash + serialize idempotency = CPU burn.

**Fix proposé** :
```yaml
server.tomcat.max-http-request-header-size: 8KB
spring.servlet.multipart.max-request-size: 1MB
spring.servlet.multipart.max-file-size: 1MB
# Pour le JSON :
spring.http.max-request-body-size: 256KB   # MVP — étendre quand besoin (US-030 photos)
```

Plus un cap explicite sur la longueur du body sérialisé dans `IdempotencyService.serialize` : if > 64KB → reject.

---

## LOW

### L-1 — Validation regex sur Idempotency-Key
`IdempotencyService.validateKey` check seulement length + blank. Ajouter `^[A-Za-z0-9_-]{1,128}$` pour bloquer espaces / unicode / control chars (évite logs corrompus).

### L-2 — `JpaActivityRepository.releaseSeatsAtomic` ignore le status
Release fonctionne même sur CANCELLED. Probablement voulu (compensation), mais peut surprendre. À documenter ou ajouter `WHERE status = PUBLISHED`.

### L-3 — `BookingController.cancel` utilise `Map.of("id", id)` comme body idempotent
Workaround pour avoir un body sur DELETE. L'endpoint key contient déjà l'id → passer `null` simplifie.

### L-4 — `PageResponse` n'a pas `hasNext`/`hasPrevious`
Déjà flaggé dans audit. Ajout 5 lignes, gain UX immédiat pour les clients mobile (Phase B nouveau audit).

### L-5 — `IdempotencyCleanupScheduler` non-distribué
Si backend scale horizontalement, tous les nodes scan en parallèle. Postgres encaisse, mais log noise + travail dupliqué. ShedLock plus tard.

### L-6 — `@SuppressWarnings("unchecked")` mal placé dans `UserProvisioningService.extractRealmRoles`
Aucun cast unchecked dans le body. Supprimer l'annotation.

### L-7 — Dead code / config inutilisée
- `spring-boot-starter-mail` déclaré dans `pom.xml` mais aucun `JavaMailSender` injecté nulle part → à supprimer jusqu'à Sprint Notification
- `spring.cache.type: redis` déclaré mais `@Cacheable` jamais utilisé → à supprimer
- Empty scaffold dirs `infrastructure/{docker,gitlab,...}/` → à supprimer

### L-8 — DRY : extraction JWT claim path traversal
Logique dupliquée entre `JwtAuthConverter.extractRoles` et `UserProvisioningService.extractRealmRoles`. Extraire un `JwtClaimPath.traverse(jwt, "a.b.c")` dans `core.security`.

### L-9 — Warning Prometheus metrics Modulith
`spring.security.authorizations` registered avec et sans tag `module.key` selon le code path. Ajouter un `MeterFilter` qui drop le tag `module.key` sur cette série pour homogénéiser.

---

## Perf — récap

| # | Item | Impact | Fix |
|---|------|--------|-----|
| P1 | `ActivityController.list` N+1 organizer name | ✅ déjà batch via `UserDirectory.findDisplayNames` | — |
| P2 | `bookings` composite index missing | seq scan sur >10k rows/user | M-2 |
| P3 | `activities.city` LOWER index missing | seq scan sur filter ville | M-3 |
| P4 | `show-sql: true` dev seulement | hot path en prod ? | vérifier `application-prod.yml` |

---

## Sécurité — checklist OWASP API Top 10

| # | Risk | Statut |
|---|------|--------|
| API1 — Broken Object Level Auth | ✅ Owner check service-side (`booking.isOwnedBy`, `activity.isOrganizedBy`) |
| API2 — Broken Authentication | ⚠️ Bug Keycloak `basic` scope corrigé. Vérifier rate limit Keycloak prod |
| API3 — Excessive Data Exposure | ✅ `BookingResponse` n'expose pas `userId`, `ActivityResponse` n'expose pas `organizerId` |
| API4 — Lack of Resources & Rate Limiting | ❌ aucun rate-limit. Ajouter Bucket4j sur `/api/v1/bookings` (S0 ou S1) |
| API5 — Broken Function Level Auth | ✅ `@PreAuthorize("hasRole('ORGANIZER')")` sur writes Activity |
| API6 — Mass Assignment | ✅ Records DTO, pas de `@RequestBody Activity` direct |
| API7 — Security Misconfig | ⚠️ CORS H-2 + max-body-size M-5 |
| API8 — Injection | ✅ JPA params + Postgres prepared statements |
| API9 — Improper Asset Mgmt | ⚠️ Swagger UI exposé même en prod (toggle `SWAGGER_ENABLED` mais default `true`) |
| API10 — Insufficient Logging | ✅ INFO sur writes, DEBUG sur reads, MDC sur traceId/userId |

---

## Plan d'action S0 (priorisé)

| Ordre | Item | Effort | Branche cible |
|-------|------|--------|---------------|
| 1 | **H-1** idempotency completion-failure | 2h + 1 test | `chore/hardening-s0` |
| 2 | **H-2** CORS configuration | 1h | `chore/hardening-s0` |
| 3 | **M-4** seat release strictness | 30min + 1 test | `chore/hardening-s0` |
| 4 | **M-5** payload size caps | 30min | `chore/hardening-s0` |
| 5 | **M-2 + M-3** index migrations (V6) | 1h | `chore/hardening-s0` |
| 6 | **L-1, L-3, L-4, L-6, L-7, L-8, L-9** cleanup batch | 2h | `chore/hardening-s0` |
| 7 | API4 Rate limiting (Bucket4j) | 3h | report sur PR séparée |
| 8 | API9 Swagger off en prod | déjà OK via env var, juste doc | — |
| 9 | **M-1** activity cancel → bookings compensation | report Sprint S2 (notification) | — |
| 10 | Tests Bruno restants (Replay, 422 overbook, cancel) | 1h | `chore/hardening-s0` |

**Total S0 chiffré : ~10h dev + 4h tests + revue PR.**

---

## DoD du Sprint Hardening

- ✅ Tous HIGH fixés + tests
- ✅ Tous MEDIUM fixés ou explicitement reportés (M-1) avec ticket
- ✅ Au moins 80% des LOW fixés
- ✅ GitHub Actions CI vert (build + test + ModularityTest + Jacoco coverage)
- ✅ `memory.md` mis à jour avec section "Sprint Hardening S0"
- ✅ PR mergée sur main
