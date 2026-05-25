package com.siide.linkup.feature.profile.infrastructure.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.siide.linkup.feature.auth.api.CurrentUserAccessor;
import com.siide.linkup.feature.profile.application.ProfileCommandService;
import com.siide.linkup.feature.profile.application.dto.UpdateProfileCommand;
import com.siide.linkup.feature.profile.domain.model.Gender;
import com.siide.linkup.feature.profile.domain.model.Profile;
import com.siide.linkup.feature.profile.infrastructure.rest.dto.ProfileRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProfileController.class,
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
class ProfileControllerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired MockMvc mockMvc;

    @MockitoBean ProfileCommandService commandService;
    @MockitoBean CurrentUserAccessor currentUserAccessor;

    private final UUID userId = UUID.randomUUID();

    @Test
    void get_me_auto_provisions_and_returns_empty_profile() throws Exception {
        when(currentUserAccessor.requireCurrentUserId()).thenReturn(userId);
        Profile profile = Profile.empty(userId);
        when(commandService.ensureProfile(userId)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.complete").value(false));
    }

    @Test
    void put_me_updates_and_returns_response_still_incomplete_without_interests() throws Exception {
        when(currentUserAccessor.requireCurrentUserId()).thenReturn(userId);

        Profile updated = Profile.empty(userId);
        updated.update("Hi", "Abidjan", LocalDate.of(1995, 1, 1), Gender.UNDISCLOSED,
                java.time.Instant.parse("2026-05-25T10:00:00Z"));
        when(commandService.update(eq(userId), any(UpdateProfileCommand.class))).thenReturn(updated);

        ProfileRequest body = new ProfileRequest("Hi", "Abidjan",
                LocalDate.of(1995, 1, 1), Gender.UNDISCLOSED);

        mockMvc.perform(put("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Hi"))
                .andExpect(jsonPath("$.city").value("Abidjan"))
                // bio+city+DOB alone are NOT enough — needs ≥1 interest (US-005).
                .andExpect(jsonPath("$.complete").value(false));
    }

    @Test
    void put_me_interests_replaces_picks() throws Exception {
        when(currentUserAccessor.requireCurrentUserId()).thenReturn(userId);

        Profile updated = Profile.empty(userId);
        updated.update("Hi", "Abidjan", LocalDate.of(1995, 1, 1), Gender.MALE,
                java.time.Instant.parse("2026-05-25T10:00:00Z"));
        updated.replaceInterests(java.util.Set.of("yoga", "foot"));
        when(commandService.updateInterests(eq(userId), any())).thenReturn(updated);

        com.siide.linkup.feature.profile.infrastructure.rest.dto.InterestsRequest body =
                new com.siide.linkup.feature.profile.infrastructure.rest.dto.InterestsRequest(
                        java.util.Set.of("yoga", "foot"));

        mockMvc.perform(put("/api/v1/profile/me/interests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(true))
                .andExpect(jsonPath("$.interests.length()").value(2));
    }

    @Test
    void put_me_rejects_oversized_bio() throws Exception {
        when(currentUserAccessor.requireCurrentUserId()).thenReturn(userId);
        ProfileRequest body = new ProfileRequest("x".repeat(200), null, null, null);

        mockMvc.perform(put("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void put_me_rejects_future_dob() throws Exception {
        when(currentUserAccessor.requireCurrentUserId()).thenReturn(userId);
        ProfileRequest body = new ProfileRequest(null, null, LocalDate.of(2099, 1, 1), null);

        mockMvc.perform(put("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }
}
