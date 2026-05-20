/**
 * Booking bounded context.
 * <p>
 * Owns the {@code bookings} table and the lifecycle of a reservation
 * (CONFIRMED → CANCELLED). Delegates seat capacity accounting to the activity
 * module via its {@code ActivitySeatService} API. Never accesses another
 * module's internal packages.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Booking")
package com.siide.linkup.feature.booking;
