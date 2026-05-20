package com.siide.linkup.feature.activity.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationTest {

    @Test
    void of_city_only_creates_valid_location() {
        Location l = Location.ofCity("Abidjan");
        assertThat(l.getCity()).isEqualTo("Abidjan");
        assertThat(l.getAddressLine()).isNull();
        assertThat(l.getLatitude()).isNull();
        assertThat(l.getLongitude()).isNull();
    }

    @Test
    void blank_city_is_rejected() {
        assertThatThrownBy(() -> Location.ofCity(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city");
    }

    @Test
    void latitude_out_of_range_is_rejected() {
        assertThatThrownBy(() -> Location.of("Abidjan", null, 95.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude");
    }

    @Test
    void longitude_out_of_range_is_rejected() {
        assertThatThrownBy(() -> Location.of("Abidjan", null, 0.0, -181.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitude");
    }

    @Test
    void blank_address_line_is_normalised_to_null() {
        Location l = Location.of("Abidjan", "  ", null, null);
        assertThat(l.getAddressLine()).isNull();
    }

    @Test
    void equality_is_value_based() {
        Location a = Location.of("Abidjan", "Riviera", 5.3, -4.0);
        Location b = Location.of("Abidjan", "Riviera", 5.3, -4.0);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
