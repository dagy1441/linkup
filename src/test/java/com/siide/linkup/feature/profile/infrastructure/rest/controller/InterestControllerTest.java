package com.siide.linkup.feature.profile.infrastructure.rest.controller;

import com.siide.linkup.feature.profile.domain.InterestCatalog;
import com.siide.linkup.feature.profile.domain.model.Interest;
import com.siide.linkup.feature.profile.domain.model.InterestCategory;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InterestController.class,
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
class InterestControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean InterestCatalog catalog;

    @Test
    void list_returns_catalogue_payload() throws Exception {
        // Build a stub Interest via reflection — no public constructor.
        Interest yoga = buildInterest("yoga", "Yoga", "Yoga", InterestCategory.SPORT, "yoga", 12);
        Interest foot = buildInterest("foot", "Football", "Football", InterestCategory.SPORT, "soccer-ball", 10);
        when(catalog.findAllEnabled()).thenReturn(List.of(foot, yoga));

        mockMvc.perform(get("/api/v1/interests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("foot"))
                .andExpect(jsonPath("$[0].labelFr").value("Football"))
                .andExpect(jsonPath("$[0].category").value("SPORT"))
                .andExpect(jsonPath("$[1].slug").value("yoga"));
    }

    private static Interest buildInterest(String slug, String fr, String en, InterestCategory cat, String icon, int order) {
        Interest i = new Interest() {};
        ReflectionTestUtils.setField(i, "slug", slug);
        ReflectionTestUtils.setField(i, "labelFr", fr);
        ReflectionTestUtils.setField(i, "labelEn", en);
        ReflectionTestUtils.setField(i, "category", cat);
        ReflectionTestUtils.setField(i, "icon", icon);
        ReflectionTestUtils.setField(i, "enabled", true);
        ReflectionTestUtils.setField(i, "sortOrder", order);
        return i;
    }
}
