package com.sdt.web_app.service;

import com.sdt.web_app.config.CacheConfig;
import com.sdt.web_app.dto.common.SliceResponse;
import com.sdt.web_app.utils.SortPropertyMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerformanceAndCachingOptimizationTest {

    @Test
    @DisplayName("SortPropertyMapper: Maps safe properties and rejects property path injection attempts")
    void testSortPropertyMapperSecurityAndValidation() {
        Map<String, String> allowedFields = Map.of(
                "updatedat", "updatedAt",
                "code", "code"
        );

        // Valid property
        Sort safeSort = SortPropertyMapper.createSafeSort("updatedAt", "DESC", "id", allowedFields);
        assertThat(safeSort.getOrderFor("updatedAt")).isNotNull();
        assertThat(safeSort.getOrderFor("updatedAt").getDirection()).isEqualTo(Sort.Direction.DESC);

        // Property Path Injection / Invalid property attempt
        assertThatThrownBy(() -> SortPropertyMapper.createSafeSort("user.password; DROP TABLE users;--", "ASC", "id", allowedFields))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field");

        // Entity-specific pageable helpers
        Pageable sectionPageable = SortPropertyMapper.createClassSectionPageable(0, 10, "sectionCode", "ASC");
        assertThat(sectionPageable.getSort().getOrderFor("sectionCode")).isNotNull();

        Pageable facultyPageable = SortPropertyMapper.createFacultyProfilePageable(0, 10, "employeeId", "ASC");
        assertThat(facultyPageable.getSort().getOrderFor("employeeId")).isNotNull();

        Pageable attendancePageable = SortPropertyMapper.createAttendanceRecordPageable(0, 10, "scannedAt", "DESC");
        assertThat(attendancePageable.getSort().getOrderFor("scannedAt")).isNotNull();

        Pageable receiptPageable = SortPropertyMapper.createCashierReceiptPageable(0, 10, "issuedAt", "DESC");
        assertThat(receiptPageable.getSort().getOrderFor("issuedAt")).isNotNull();

        Pageable riskScorePageable = SortPropertyMapper.createStudentRiskScorePageable(0, 10, "evaluatedAt", "DESC");
        assertThat(riskScorePageable.getSort().getOrderFor("evaluatedAt")).isNotNull();
    }

    @Test
    @DisplayName("SliceResponse: Correctly maps Slice metadata without total count calculation")
    void testSliceResponseMapping() {
        List<String> items = List.of("Item A", "Item B");
        Pageable pageable = PageRequest.of(0, 2);
        SliceImpl<String> slice = new SliceImpl<>(items, pageable, true);

        SliceResponse<String> response = SliceResponse.from(slice);

        assertThat(response.content()).containsExactly("Item A", "Item B");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.isFirst()).isTrue();
    }

    @Test
    @DisplayName("CacheConfig: Instantiates all required enterprise cache stores")
    void testCacheConfigStoreInitialization() {
        CacheConfig cacheConfig = new CacheConfig();
        CacheManager cacheManager = cacheConfig.cacheManager();

        assertThat(cacheManager.getCacheNames()).contains(
                CacheConfig.CACHE_CAMPUSES,
                CacheConfig.CACHE_DEPARTMENTS,
                CacheConfig.CACHE_PROGRAMS,
                CacheConfig.CACHE_COURSES,
                CacheConfig.CACHE_TERMS,
                CacheConfig.CACHE_ACADEMIC_YEARS,
                CacheConfig.CACHE_GRADING_SCALES,
                CacheConfig.CACHE_FEE_CATALOG,
                CacheConfig.CACHE_EQUITY_PROFILES
        );
    }
}
