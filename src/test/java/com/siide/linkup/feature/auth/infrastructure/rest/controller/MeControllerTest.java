package com.siide.linkup.feature.auth.infrastructure.rest.controller;

import com.siide.linkup.feature.auth.application.CurrentUserService;
import com.siide.linkup.feature.auth.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MeController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                ServletWebSecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        },
        // Skip the JWT converter — it would try to load security beans we don't want here.
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.siide\\.linkup\\.core\\.security\\..*"
        )
)
@AutoConfigureMockMvc(addFilters = false)
class MeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CurrentUserService currentUserService;

    @Test
    void returns_current_user_payload() throws Exception {
        User user = User.provision("kc-42", "alice@linkup.io", "Alice", Set.of("user"));
        when(currentUserService.getCurrent()).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.keycloakId").value("kc-42"))
                .andExpect(jsonPath("$.email").value("alice@linkup.io"))
                .andExpect(jsonPath("$.displayName").value("Alice"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("user"));
    }
}
