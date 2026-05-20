package com.siide.linkup.feature.activity.infrastructure.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.siide.linkup.feature.activity.application.ActivityCommandService;
import com.siide.linkup.feature.activity.application.ActivityQueryService;
import com.siide.linkup.feature.activity.domain.model.Activity;
import com.siide.linkup.feature.activity.domain.model.Location;
import com.siide.linkup.feature.activity.infrastructure.rest.dto.ActivityRequest;
import com.siide.linkup.feature.auth.api.CurrentUserAccessor;
import com.siide.linkup.feature.auth.api.UserDirectory;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ActivityController.class,
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
class ActivityControllerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired MockMvc mockMvc;
    @MockitoBean ActivityCommandService commandService;
    @MockitoBean ActivityQueryService queryService;
    @MockitoBean CurrentUserAccessor currentUserAccessor;
    @MockitoBean UserDirectory userDirectory;

    @Test
    void list_returns_paginated_envelope_with_organizer_name() throws Exception {
        Activity a = sampleActivity();
        Page<Activity> page = new PageImpl<>(List.of(a));
        when(queryService.listPublishedUpcoming(eq(Optional.empty()), any(), any())).thenReturn(page);
        when(userDirectory.findDisplayNames(anyCollection()))
                .thenReturn(Map.of(a.getOrganizerId(), "Alice"));

        mockMvc.perform(get("/api/v1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Brunch"))
                .andExpect(jsonPath("$.content[0].organizerDisplayName").value("Alice"))
                .andExpect(jsonPath("$.content[0].organizerId").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_falls_back_when_organizer_name_unresolved() throws Exception {
        Activity a = sampleActivity();
        when(queryService.listPublishedUpcoming(eq(Optional.empty()), any(), any()))
                .thenReturn(new PageImpl<>(List.of(a)));
        when(userDirectory.findDisplayNames(anyCollection())).thenReturn(Map.of());

        mockMvc.perform(get("/api/v1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].organizerDisplayName").value("Unknown organizer"));
    }

    @Test
    void get_by_id_returns_payload() throws Exception {
        Activity a = sampleActivity();
        when(queryService.getById(a.getId())).thenReturn(a);
        when(userDirectory.findDisplayName(a.getOrganizerId())).thenReturn(Optional.of("Alice"));

        mockMvc.perform(get("/api/v1/activities/{id}", a.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId().toString()))
                .andExpect(jsonPath("$.city").value("Abidjan"))
                .andExpect(jsonPath("$.organizerDisplayName").value("Alice"));
    }

    @Test
    void create_returns_201_with_payload() throws Exception {
        Activity created = sampleActivity();
        when(currentUserAccessor.requireCurrentUserId()).thenReturn(created.getOrganizerId());
        when(commandService.create(any(), eq(created.getOrganizerId()))).thenReturn(created);
        when(userDirectory.findDisplayName(created.getOrganizerId())).thenReturn(Optional.of("Alice"));

        ActivityRequest body = new ActivityRequest(
                "Brunch", "desc", "Abidjan", null, null, null,
                Instant.now().plus(1, ChronoUnit.HOURS), 10);

        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Brunch"))
                .andExpect(jsonPath("$.organizerDisplayName").value("Alice"));
    }

    @Test
    void create_returns_400_on_validation_failure() throws Exception {
        ActivityRequest invalid = new ActivityRequest(
                " ", null, "Abidjan", null, null, null,
                Instant.now().plus(1, ChronoUnit.HOURS), 10);

        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void update_returns_updated_activity() throws Exception {
        Activity a = sampleActivity();
        when(currentUserAccessor.requireCurrentUserId()).thenReturn(a.getOrganizerId());
        when(commandService.update(eq(a.getId()), any(), eq(a.getOrganizerId()))).thenReturn(a);
        when(userDirectory.findDisplayName(a.getOrganizerId())).thenReturn(Optional.of("Alice"));

        ActivityRequest body = new ActivityRequest(
                "Updated", "new", "Abidjan", null, null, null,
                Instant.now().plus(2, ChronoUnit.HOURS), 15);

        mockMvc.perform(put("/api/v1/activities/{id}", a.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId().toString()))
                .andExpect(jsonPath("$.organizerDisplayName").value("Alice"));
    }

    @Test
    void delete_returns_cancelled_activity() throws Exception {
        Activity a = sampleActivity();
        a.cancel();
        when(currentUserAccessor.requireCurrentUserId()).thenReturn(a.getOrganizerId());
        when(commandService.cancel(a.getId(), a.getOrganizerId())).thenReturn(a);
        when(userDirectory.findDisplayName(a.getOrganizerId())).thenReturn(Optional.of("Alice"));

        mockMvc.perform(delete("/api/v1/activities/{id}", a.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private Activity sampleActivity() {
        Instant now = Instant.now();
        return Activity.create("Brunch", "Sunday brunch",
                Location.of("Abidjan", "Riviera", 5.3, -4.0),
                now.plus(1, ChronoUnit.HOURS),
                10, UUID.randomUUID(), now);
    }
}
