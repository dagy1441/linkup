package com.siide.linkup.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(request.getMethod()).thenReturn("GET");
    }

    @Test
    void resource_not_found_returns_404_with_error_code() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new ResourceNotFoundException("USER_NOT_FOUND", "User not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("USER_NOT_FOUND");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void conflict_returns_409() {
        ResponseEntity<ApiError> response = handler.handleConflict(
                new ConflictException("DUPLICATE_BOOKING", "Already booked"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_BOOKING");
    }

    @Test
    void business_rule_violation_returns_422() {
        ResponseEntity<ApiError> response = handler.handleBusinessRule(
                new BusinessRuleViolationException("ACTIVITY_FULL", "No seats left"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().code()).isEqualTo("ACTIVITY_FULL");
    }

    @Test
    void forbidden_operation_returns_403() {
        ResponseEntity<ApiError> response = handler.handleForbiddenDomain(
                new ForbiddenOperationException("NOT_ORGANIZER", "Not your activity"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("NOT_ORGANIZER");
    }

    @Test
    void optimistic_locking_returns_409_with_stable_code() {
        ResponseEntity<ApiError> response = handler.handleOptimisticLocking(
                new OptimisticLockingFailureException("stale"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");
    }

    @Test
    void access_denied_returns_403() {
        ResponseEntity<ApiError> response = handler.handleAccessDenied(
                new AccessDeniedException("nope"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void unexpected_exception_returns_500() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
