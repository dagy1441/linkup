/**
 * Activity bounded context.
 * <p>
 * Owns the {@code activities} table, the lifecycle of activities (PUBLISHED → CANCELLED)
 * and seat capacity accounting. Other modules consume the published API via the
 * {@code api} named interface (currently: {@code ActivitySeatService} used by booking).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Activity")
package com.siide.linkup.feature.activity;
