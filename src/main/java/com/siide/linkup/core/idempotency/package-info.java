/**
 * Cross-cutting replay cache for mutating endpoints. Exposed to feature modules so
 * controllers can wrap their POST/DELETE handlers with {@code IdempotencyService.execute(...)}.
 */
@org.springframework.modulith.NamedInterface("idempotency")
package com.siide.linkup.core.idempotency;
