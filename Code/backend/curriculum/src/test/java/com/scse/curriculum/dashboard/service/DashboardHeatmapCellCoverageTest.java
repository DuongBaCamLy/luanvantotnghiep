package com.scse.curriculum.dashboard.service;

import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.plo.entity.Plo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardHeatmapCellCoverageTest {

    @Test
    void buildsCellFromExactCloMappings() {

        Plo plo =
                Plo.builder()
                        .id(100)
                        .code("PLO1")
                        .description("Program outcome 1")
                        .versionNumber(2)
                        .isActive(true)
                        .build();

        Clo clo1 =
                Clo.builder()
                        .id(201)
                        .code("CLO1")
                        .description(
                                "Understand programming fundamentals")
                        .descriptionVn(
                                "Hiểu các nguyên lý lập trình")
                        .orderIndex(1)
                        .build();

        Clo clo2 =
                Clo.builder()
                        .id(202)
                        .code("CLO2")
                        .description(
                                "Apply programming techniques")
                        .descriptionVn(
                                "Áp dụng kỹ thuật lập trình")
                        .orderIndex(2)
                        .build();

        CloPloMapping mapping1 =
                CloPloMapping.builder()
                        .id(301)
                        .clo(clo1)
                        .plo(plo)
                        .level(ContributionLevel.I)
                        .contributionWeight(0.3333f)
                        .build();

        CloPloMapping mapping2 =
                CloPloMapping.builder()
                        .id(302)
                        .clo(clo2)
                        .plo(plo)
                        .level(ContributionLevel.A)
                        .contributionWeight(1.0f)
                        .build();

        DashboardHeatmapResponse.CellCoverage cell =
                DashboardServiceImpl
                        .buildCellCoverage(
                                plo,
                                List.of(
                                        mapping2,
                                        mapping1
                                )
                        );

        assertThat(cell.getPloId())
                .isEqualTo(100);

        assertThat(cell.getPloCode())
                .isEqualTo("PLO1");

        // Highest contribution:
        // I + A -> A -> XXX
        assertThat(cell.getLevel())
                .isEqualTo("XXX");

        assertThat(cell.getMappingCount())
                .isEqualTo(2);

        // Must follow CLO order, not input/query order.
        assertThat(cell.getCloCodes())
                .containsExactly(
                        "CLO1",
                        "CLO2"
                );

        assertThat(cell.getCloContributions())
                .hasSize(2);

        DashboardHeatmapResponse.CloContribution first =
                cell.getCloContributions().get(0);

        assertThat(first.getCloId())
                .isEqualTo(201);

        assertThat(first.getCloCode())
                .isEqualTo("CLO1");

        assertThat(first.getDescription())
                .isEqualTo(
                        "Understand programming fundamentals"
                );

        assertThat(first.getDescriptionVn())
                .isEqualTo(
                        "Hiểu các nguyên lý lập trình"
                );

        assertThat(first.getLevel())
                .isEqualTo("X");

        DashboardHeatmapResponse.CloContribution second =
                cell.getCloContributions().get(1);

        assertThat(second.getCloCode())
                .isEqualTo("CLO2");

        assertThat(second.getLevel())
                .isEqualTo("XXX");
    }


    @Test
    void buildsEmptyCellWhenPloHasNoMappings() {

        Plo plo =
                Plo.builder()
                        .id(101)
                        .code("PLO2")
                        .versionNumber(1)
                        .isActive(true)
                        .build();

        DashboardHeatmapResponse.CellCoverage cell =
                DashboardServiceImpl
                        .buildCellCoverage(
                                plo,
                                List.of()
                        );

        assertThat(cell.getPloId())
                .isEqualTo(101);

        assertThat(cell.getPloCode())
                .isEqualTo("PLO2");

        assertThat(cell.getLevel())
                .isNull();

        assertThat(cell.getMappingCount())
                .isZero();

        assertThat(cell.getCloCodes())
                .isEmpty();

        assertThat(cell.getCloContributions())
                .isEmpty();
    }


    @Test
    void selectsDevelopmentAsHighestForIntroduceAndDevelopment() {

        Plo plo =
                Plo.builder()
                        .id(102)
                        .code("PLO3")
                        .build();

        Clo clo1 =
                Clo.builder()
                        .id(211)
                        .code("CLO1")
                        .description("CLO1")
                        .orderIndex(1)
                        .build();

        Clo clo2 =
                Clo.builder()
                        .id(212)
                        .code("CLO2")
                        .description("CLO2")
                        .orderIndex(2)
                        .build();

        CloPloMapping introduce =
                CloPloMapping.builder()
                        .clo(clo1)
                        .plo(plo)
                        .level(ContributionLevel.I)
                        .build();

        CloPloMapping develop =
                CloPloMapping.builder()
                        .clo(clo2)
                        .plo(plo)
                        .level(ContributionLevel.D)
                        .build();

        DashboardHeatmapResponse.CellCoverage cell =
                DashboardServiceImpl
                        .buildCellCoverage(
                                plo,
                                List.of(
                                        introduce,
                                        develop
                                )
                        );

        assertThat(cell.getLevel())
                .isEqualTo("XX");

        assertThat(cell.getMappingCount())
                .isEqualTo(2);

        assertThat(cell.getCloCodes())
                .containsExactly(
                        "CLO1",
                        "CLO2"
                );
    }
}