package com.scse.curriculum.dashboard.service;

import com.scse.curriculum.plo.entity.Plo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceImplPloSelectionTest {

    @Test
    void keepsOnlyLatestActiveVersionForEachPloCode() {

        Plo plo1V1 =
                plo(
                        101,
                        "PLO1",
                        1,
                        true);

        Plo plo1V2 =
                plo(
                        102,
                        "PLO1",
                        2,
                        true);

        Plo plo1V3Inactive =
                plo(
                        103,
                        "PLO1",
                        3,
                        false);

        Plo plo2V1 =
                plo(
                        201,
                        "PLO2",
                        1,
                        true);

        List<Plo> result =
                DashboardServiceImpl
                        .latestActivePloVersions(
                                List.of(
                                        plo1V1,
                                        plo1V2,
                                        plo1V3Inactive,
                                        plo2V1));

        assertThat(result)
                .extracting(Plo::getId)
                .containsExactly(
                        102,
                        201);
    }


    @Test
    void treatsNullActiveAsActiveAndNullVersionAsVersionOne() {

        Plo old =
                plo(
                        301,
                        "PLO3",
                        null,
                        null);

        Plo newer =
                plo(
                        302,
                        "PLO3",
                        2,
                        true);

        List<Plo> result =
                DashboardServiceImpl
                        .latestActivePloVersions(
                                List.of(
                                        old,
                                        newer));

        assertThat(result)
                .extracting(Plo::getId)
                .containsExactly(
                        302);
    }


    @Test
    void normalizesPloCodeCaseAndWhitespace() {

        Plo old =
                plo(
                        401,
                        "plo4",
                        1,
                        true);

        Plo newer =
                plo(
                        402,
                        " PLO4 ",
                        2,
                        true);

        List<Plo> result =
                DashboardServiceImpl
                        .latestActivePloVersions(
                                List.of(
                                        old,
                                        newer));

        assertThat(result)
                .hasSize(1);

        assertThat(result.get(0).getId())
                .isEqualTo(402);
    }


    @Test
    void usesNewestRowWhenSameCodeAndVersionExist() {

        Plo first =
                plo(
                        501,
                        "PLO5",
                        2,
                        true);

        Plo second =
                plo(
                        502,
                        "PLO5",
                        2,
                        true);

        List<Plo> result =
                DashboardServiceImpl
                        .latestActivePloVersions(
                                List.of(
                                        first,
                                        second));

        assertThat(result)
                .hasSize(1);

        assertThat(result.get(0).getId())
                .isEqualTo(502);
    }


    private static Plo plo(
            Integer id,
            String code,
            Integer versionNumber,
            Boolean active) {

        return Plo.builder()
                .id(id)
                .code(code)
                .description(
                        "Test " + code)
                .versionNumber(
                        versionNumber)
                .isActive(active)
                .build();
    }
}