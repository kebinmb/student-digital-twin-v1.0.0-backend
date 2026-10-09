package com.sdt.web_app.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameUtilTest {

    @Test
    void fullNameWithAllThreeParts() {
        assertThat(NameUtil.buildFullName("Maria", "Clara", "Santos"))
                .isEqualTo("Maria C. Santos");
    }

    @Test
    void fullNameWithoutMiddleName() {
        assertThat(NameUtil.buildFullName("Maria", null, "Santos"))
                .isEqualTo("Maria Santos");
    }

    @Test
    void fullNameWithBlankMiddleName() {
        assertThat(NameUtil.buildFullName("Maria", "", "Santos"))
                .isEqualTo("Maria Santos");
    }

    @Test
    void fullNameWithWhitespaceMiddleName() {
        assertThat(NameUtil.buildFullName("Maria", "   ", "Santos"))
                .isEqualTo("Maria Santos");
    }

    @Test
    void fullNameHandlesNullFirstName() {
        assertThat(NameUtil.buildFullName(null, "Clara", "Santos"))
                .isEqualTo("C. Santos");
    }

    @Test
    void fullNameHandlesNullLastName() {
        assertThat(NameUtil.buildFullName("Maria", null, null))
                .isEqualTo("Maria");
    }

    @Test
    void fullNameHandlesAllNull() {
        assertThat(NameUtil.buildFullName(null, null, null))
                .isEqualTo("");
    }

    @Test
    void fullNameTrimsWhitespace() {
        assertThat(NameUtil.buildFullName("  Maria  ", "Clara  ", "  Santos"))
                .isEqualTo("Maria C. Santos");
    }
}
