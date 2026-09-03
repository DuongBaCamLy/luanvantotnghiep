package com.scse.curriculum.syllabus.dto;


import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusListContextResponse {


    private List<SyllabusListItemResponse> syllabuses;


    private SyllabusFilterResponse filters;

}