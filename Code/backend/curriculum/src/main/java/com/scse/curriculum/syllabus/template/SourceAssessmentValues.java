package com.scse.curriculum.syllabus.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import java.util.*;

/** Separates unknown source values from the existing NOT NULL database defaults. */
public final class SourceAssessmentValues {
    private static final ObjectMapper JSON = new ObjectMapper();
    private SourceAssessmentValues() {}
    public static Map<String,Object> missing(Integer order, String name, Float weight, Float min, Float max) {
        var fields=new ArrayList<String>();
        if(weight==null)fields.add("weightPercent");if(min==null)fields.add("minScore");if(max==null)fields.add("maxScore");
        var row=new LinkedHashMap<String,Object>();row.put("orderIndex",order);row.put("name",name);row.put("fields",fields);return row;
    }
    public static String annotate(String notes,List<CreateSyllabusRequest.AssessmentDTO> requested) {
        if(notes==null || notes.isBlank())return notes;
        try {
            var root=JSON.readTree(notes);
            if(!(root instanceof ObjectNode object) || !root.path("sourceFaithful2027").asBoolean())return notes;
            var rows=requested.stream().map(a->missing(a.getOrderIndex(),a.getName(),a.getWeightPercent(),a.getMinScore(),a.getMaxScore())).toList();
            object.set("sourceAssessmentMissingFields",JSON.valueToTree(rows));return JSON.writeValueAsString(object);
        }catch(java.io.IOException error){return notes;} // Existing free-text notes remain supported.
    }
    public static Float visible(String notes,Integer order,String name,String field,Float stored) {
        if(notes==null || notes.isBlank())return stored;
        try {
            var root=JSON.readTree(notes);if(!root.path("sourceFaithful2027").asBoolean())return stored;
            for(var row:root.path("sourceAssessmentMissingFields")) {
                if(row.path("orderIndex").asInt()==Objects.requireNonNullElse(order,1) && row.path("name").asText().equals(name))
                    for(var missing:row.path("fields"))if(missing.asText().equals(field))return null;
            }
            return stored;
        }catch(java.io.IOException error){return stored;} // Legacy notes need not be JSON.
    }
}
