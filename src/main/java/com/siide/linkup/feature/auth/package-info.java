/**
 * Auth bounded context.
 * <p>
 * Owns the local {@code users} table (mirror of Keycloak identities) and the
 * provisioning flow that creates a local user record on the first authenticated
 * request. Other modules consume the local user via the {@code CurrentUserService}
 * facade exposed at the root of this package.
 * <p>
 * Sub-packages are internal and must not be referenced from other feature modules.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Auth")
package com.siide.linkup.feature.auth;
