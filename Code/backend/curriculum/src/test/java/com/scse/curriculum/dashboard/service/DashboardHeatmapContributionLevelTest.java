package com.scse.curriculum.dashboard.service;

import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DashboardHeatmapContributionLevelTest {

    @Test
    void convertsInternalContributionLevelsToHeatmapSymbols() {

        assertEquals(
                "X",
                DashboardServiceImpl.toCoverageLevel(
                        ContributionLevel.I
                )
        );

        assertEquals(
                "XX",
                DashboardServiceImpl.toCoverageLevel(
                        ContributionLevel.D
                )
        );

        assertEquals(
                "XXX",
                DashboardServiceImpl.toCoverageLevel(
                        ContributionLevel.A
                )
        );

        assertNull(
                DashboardServiceImpl.toCoverageLevel(
                        null
                )
        );
    }

    @Test
    void keepsIntroduceWhenItIsTheOnlyContribution() {

        ContributionLevel result =
                DashboardServiceImpl.highestContributionLevel(
                        List.of(
                                ContributionLevel.I
                        )
                );

        assertEquals(
                ContributionLevel.I,
                result
        );
    }

    @Test
    void developmentOverridesIntroduce() {

        ContributionLevel result =
                DashboardServiceImpl.highestContributionLevel(
                        List.of(
                                ContributionLevel.I,
                                ContributionLevel.D
                        )
                );

        assertEquals(
                ContributionLevel.D,
                result
        );

        assertEquals(
                "XX",
                DashboardServiceImpl.toCoverageLevel(
                        result
                )
        );
    }

    @Test
    void applicationOverridesDevelopmentAndIntroduce() {

        ContributionLevel result =
                DashboardServiceImpl.highestContributionLevel(
                        List.of(
                                ContributionLevel.I,
                                ContributionLevel.D,
                                ContributionLevel.A
                        )
                );

        assertEquals(
                ContributionLevel.A,
                result
        );

        assertEquals(
                "XXX",
                DashboardServiceImpl.toCoverageLevel(
                        result
                )
        );
    }

    @Test
    void handlesNoContributionSafely() {

        assertNull(
                DashboardServiceImpl.highestContributionLevel(
                        List.of()
                )
        );

        assertNull(
                DashboardServiceImpl.highestContributionLevel(
                        null
                )
        );
    }
}