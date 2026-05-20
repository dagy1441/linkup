package com.siide.linkup.feature.booking.infrastructure.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.siide.linkup.core.idempotency.IdempotencyService;
import com.siide.linkup.feature.auth.api.CurrentUserAccessor;
import com.siide.linkup.feature.booking.application.BookingCommandService;
import com.siide.linkup.feature.booking.application.BookingQueryService;
import com.siide.linkup.feature.booking.domain.model.Booking;
import com.siide.linkup.feature.booking.infrastructure.rest.dto.BookingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BookingController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                ServletWebSecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.siide\\.linkup\\.core\\.security\\..*"
        )
)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired MockMvc mockMvc;
    @MockitoBean BookingCommandService commandService;
    @MockitoBean BookingQueryService queryService;
    @MockitoBean CurrentUserAccessor currentUserAccessor;
    @MockitoBean IdempotencyService idempotencyService;

    @BeforeEach
    void wireIdempotencyPassthrough() {
        // Default: pass through to the supplier (no caching). Individual tests can override.
        when(idempotencyService.execute(any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(5)).get());
    }

    @Test
    void create_returns_201_with_payload() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Booking created = Booking.confirm(userId, activityId, 2);

        when(currentUserAccessor.requireCurrentUserId()).thenReturn(userId);
        when(commandService.create(any(), eq(userId))).thenReturn(created);

        BookingRequest body = new BookingRequest(activityId, 2);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityId").value(activityId.toString()))
                .andExpect(jsonPath("$.seats").value(2))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void create_returns_400_when_idempotency_key_header_missing() throws Exception {
        BookingRequest body = new BookingRequest(UUID.randomUUID(), 1);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"));
    }

    @Test
    void create_returns_400_on_validation_failure() throws Exception {
        BookingRequest invalid = new BookingRequest(null, 0);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void list_mine_returns_paginated_envelope() throws Exception {
        UUID userId = UUID.randomUUID();
        Booking b = Booking.confirm(userId, UUID.randomUUID(), 1);
        Page<Booking> page = new PageImpl<>(List.of(b));

        when(currentUserAccessor.requireCurrentUserId()).thenReturn(userId);
        when(queryService.listMine(eq(userId), eq(Optional.empty()), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/bookings/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].seats").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void get_by_id_returns_booking() throws Exception {
        UUID userId = UUID.randomUUID();
        Booking b = Booking.confirm(userId, UUID.randomUUID(), 3);

        when(currentUserAccessor.requireCurrentUserId()).thenReturn(userId);
        when(queryService.getOwnedById(b.getId(), userId)).thenReturn(b);

        mockMvc.perform(get("/api/v1/bookings/{id}", b.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(b.getId().toString()))
                .andExpect(jsonPath("$.seats").value(3));
    }

    @Test
    void delete_returns_cancelled_booking() throws Exception {
        UUID userId = UUID.randomUUID();
        Booking b = Booking.confirm(userId, UUID.randomUUID(), 1);
        b.cancel(java.time.Instant.now());

        when(currentUserAccessor.requireCurrentUserId()).thenReturn(userId);
        when(commandService.cancel(b.getId(), userId)).thenReturn(b);

        mockMvc.perform(delete("/api/v1/bookings/{id}", b.getId())
                        .header("Idempotency-Key", "del-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
