package com.siide.linkup.feature.activity.api;

import java.util.UUID;

/**
 * Cross-module API for seat capacity management. Booking module is the only intended
 * consumer. Both operations are race-free atomic SQL updates.
 */
public interface ActivitySeatService {

    /**
     * Try to reserve {@code qty} seats in a single atomic operation. Returns
     * {@code true} only when all requested seats were secured. Reasons for failure:
     * activity not found, cancelled, already started, or insufficient remaining capacity.
     *
     * @param qty number of seats to reserve, must be {@code > 0}
     */
    boolean tryReserveSeats(UUID activityId, int qty);

    /**
     * Release {@code qty} previously reserved seats. Throws if the activity is
     * missing or has fewer booked seats than {@code qty} — the caller's transaction
     * must roll back so the matching booking is not marked cancelled while seats
     * stay stuck.
     *
     * @param qty number of seats to release, must be {@code > 0}
     * @throws com.siide.linkup.feature.activity.domain.exception.SeatReleaseFailedException
     *         when the UPDATE affects zero rows
     */
    void releaseSeats(UUID activityId, int qty);
}
