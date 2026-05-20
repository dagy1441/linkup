package com.siide.linkup.feature.activity.api;

import java.util.UUID;

/**
 * Cross-module API for seat capacity management. Booking module is the only intended
 * consumer. Both operations are race-free atomic SQL updates.
 */
public interface ActivitySeatService {

    /**
     * Try to reserve one seat. Returns {@code true} when a seat was secured (booked count
     * incremented), {@code false} otherwise. Reasons for failure: activity not found,
     * cancelled, already started, or full.
     */
    boolean tryReserveSeat(UUID activityId);

    /** Release a previously reserved seat. No-op if booked count is already 0. */
    void releaseSeat(UUID activityId);
}
