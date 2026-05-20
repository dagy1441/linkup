package com.siide.linkup.feature.booking.infrastructure.rest.controller;

import com.siide.linkup.feature.auth.api.CurrentUserAccessor;
import com.siide.linkup.feature.booking.application.BookingCommandService;
import com.siide.linkup.feature.booking.application.BookingQueryService;
import com.siide.linkup.feature.booking.application.dto.CreateBookingCommand;
import com.siide.linkup.feature.booking.domain.model.Booking;
import com.siide.linkup.feature.booking.domain.model.BookingStatus;
import com.siide.linkup.feature.booking.infrastructure.rest.dto.BookingRequest;
import com.siide.linkup.feature.booking.infrastructure.rest.dto.BookingResponse;
import com.siide.linkup.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "User reservations on activities. Every endpoint requires authentication.")
@PreAuthorize("isAuthenticated()")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingCommandService commandService;
    private final BookingQueryService queryService;
    private final CurrentUserAccessor currentUserAccessor;

    public BookingController(BookingCommandService commandService,
                             BookingQueryService queryService,
                             CurrentUserAccessor currentUserAccessor) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @PostMapping
    @Operation(summary = "Create a new booking for the authenticated user.")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest body) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Booking created = commandService.create(new CreateBookingCommand(body.activityId(), body.seats()), userId);
        log.info("POST /bookings → created id={} userId={}", created.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(created));
    }

    @GetMapping("/me")
    @Operation(summary = "List the authenticated user's bookings (paginated, optional status filter).")
    public ResponseEntity<PageResponse<BookingResponse>> listMine(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Page<Booking> result = queryService.listMine(userId, Optional.ofNullable(status), page, size);
        return ResponseEntity.ok(PageResponse.from(result, BookingResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single booking by id (must belong to the authenticated user).")
    public ResponseEntity<BookingResponse> getById(@PathVariable UUID id) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Booking booking = queryService.getOwnedById(id, userId);
        return ResponseEntity.ok(BookingResponse.from(booking));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a booking (must belong to the authenticated user).")
    public ResponseEntity<BookingResponse> cancel(@PathVariable UUID id) {
        UUID userId = currentUserAccessor.requireCurrentUserId();
        Booking cancelled = commandService.cancel(id, userId);
        return ResponseEntity.ok(BookingResponse.from(cancelled));
    }
}
