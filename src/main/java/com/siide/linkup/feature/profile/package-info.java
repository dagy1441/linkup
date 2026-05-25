/**
 * Profile bounded context.
 * <p>
 * Owns user-facing profile data (bio, city, date of birth, gender, photo,
 * interests) and the account lifecycle (soft delete + scheduled hard purge).
 * Identity itself (keycloakId, email, roles) stays in {@code feature.auth} —
 * this module references {@code userId} as a bare UUID, no FK across modules.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Profile")
package com.siide.linkup.feature.profile;
