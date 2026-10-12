package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;

/** Opt-in recovery of ruled source cells. The legacy parser is never changed. */
final class Cohort2027PdfTableRecovery {
    record Edge(float x1, float y1, float x2, float y2) {}
    record Cell(float x, float top, float right, float bottom, String text) {}
    static String clean(String text) { return text == null ? "" : text.replaceAll("\\s+", " ").trim(); }

    static List<Cell> cells(PDPage page) throws IOException {
        Lines lines = new Lines(page); lines.processPage(page);
        return cells(page, lines);
    }

    static List<Cell> cells(PDPage page, boolean filledPolygonRules) throws IOException {
        Lines lines = new Lines(page, filledPolygonRules); lines.processPage(page);
        if(filledPolygonRules)lines.mergeRuleSegments();
        return cells(page, lines);
    }

    private static List<Cell> cells(PDPage page, Lines lines) throws IOException {
        float height = page.getCropBox().getHeight();
        List<Edge> horizontal = lines.edges.stream().filter(e -> Math.abs(e.y1-e.y2)<0.7 && Math.abs(e.x1-e.x2)>12).toList();
        List<Edge> vertical = lines.edges.stream().filter(e -> Math.abs(e.x1-e.x2)<0.7 && Math.abs(e.y1-e.y2)>10).toList();
        TreeSet<Float> boundaries = new TreeSet<>();
        for (Edge e : horizontal) boundaries.add(Math.round((height-e.y1)*2)/2f);
        for (Edge e : vertical) { boundaries.add(Math.round((height-e.y1)*2)/2f); boundaries.add(Math.round((height-e.y2)*2)/2f); }
        List<Float> ys = new ArrayList<>(boundaries); List<Cell> result = new ArrayList<>();
        PDFTextStripperByArea extractor = new PDFTextStripperByArea(); extractor.setSortByPosition(true);
        List<float[]> boxes = new ArrayList<>();
        for (int n=0;n+1<ys.size();n++) {
            float top=ys.get(n), bottom=ys.get(n+1); if(bottom-top<5) continue;
            TreeSet<Float> columns = new TreeSet<>();
            for(Edge e:vertical) if(height-Math.max(e.y1,e.y2)<=top+1 && height-Math.min(e.y1,e.y2)>=bottom-1
                    && (lines.stroked.contains(e) || horizontal.stream().anyMatch(h->Math.abs(height-h.y1-top)<1.2
                    && (Math.abs(h.x1-e.x1)<1.2 || Math.abs(h.x2-e.x1)<1.2))))
                columns.add(Math.round(e.x1*2)/2f);
            // A Word cell can have a horizontal border but no right vertical border.
            // Use its painted endpoints, so text overflowing the enclosing table remains in one cell.
            for(Edge h:horizontal)if(Math.abs(height-h.y1-top)<1.2) {
                columns.add(Math.round(h.x1*2)/2f);columns.add(Math.round(h.x2*2)/2f);
            }
            List<Float> xs=new ArrayList<>(columns);
            for(int c=0;c+1<xs.size();c++) {
                float left=xs.get(c),right=xs.get(c+1); if(right-left<8) continue;
                boolean upper=horizontal.stream().anyMatch(e->Math.abs(height-e.y1-top)<1.2 && Math.min(e.x1,e.x2)<=left+1 && Math.max(e.x1,e.x2)>=right-1);
                boolean lower=horizontal.stream().anyMatch(e->Math.abs(height-e.y1-bottom)<1.2 && Math.min(e.x1,e.x2)<=left+1 && Math.max(e.x1,e.x2)>=right-1);
                // Word sometimes ends an inner table's vertical rules without a bottom rule.
                // Its actual line endpoint is source geometry, not an inferred row height.
                boolean terminal=vertical.stream().anyMatch(e->(Math.abs(e.x1-left)<1 || Math.abs(e.x1-right)<1)
                        && Math.abs(height-Math.min(e.y1,e.y2)-bottom)<1);
                boolean initial=vertical.stream().anyMatch(e->(Math.abs(e.x1-left)<1 || Math.abs(e.x1-right)<1)
                        && Math.abs(height-Math.max(e.y1,e.y2)-top)<1);
                if((!upper && !initial) || (!lower && !terminal)) continue;
                String name="cell"+boxes.size();boxes.add(new float[]{left,top,right,bottom});
                extractor.addRegion(name,new Rectangle2D.Float(left+0.4f,top+0.4f,right-left-0.8f,bottom-top-0.8f));
            }
        }
        extractor.extractRegions(page);
        for(int i=0;i<boxes.size();i++){float[] b=boxes.get(i);result.add(new Cell(b[0],b[1],b[2],b[3],clean(extractor.getTextForRegion("cell"+i))));}
        return result;
    }

    static List<String> recover(PDDocument document, int start, int end, SyllabusImportData data) throws IOException {
        return recover(document,start,end,data,false);
    }

    static List<String> recover(PDDocument document, int start, int end, SyllabusImportData data, boolean filledPolygonRules) throws IOException {
        List<String> incomplete = new ArrayList<>();
        List<com.scse.curriculum.syllabus.importer.dto.TopicImportData> topics=new ArrayList<>();
        List<SyllabusImportData.WeeklyActivityItem> weekly=new ArrayList<>();
        List<SyllabusImportData.CloPloMappingItem> mappings=new ArrayList<>();
        boolean generalFinished=false;
        GeneralContext generalContext = new GeneralContext();
        boolean contentImage=false;
        TableContext tableContext = new TableContext(filledPolygonRules);
        var assessmentRows = new ArrayList<List<String>>();
        var assessmentHeaders = new ArrayList<String>();
        var assessmentContext = new AssessmentTableContext();
        for(int page=start;page<=end;page++) {
            Lines graphics=new Lines(document.getPage(page-1),filledPolygonRules);graphics.processPage(document.getPage(page-1));
            if(filledPolygonRules)graphics.mergeRuleSegments();
            List<Cell> pageCells=cells(document.getPage(page-1),graphics);
            final boolean continuedContent=generalContext.key.equals("content");
            float firstTop=pageCells.stream().map(Cell::top).min(Float::compare).orElse(-1f);
            for(Cell label:pageCells)if(label.text.equalsIgnoreCase("Content")
                    || (continuedContent && label.text.isBlank() && label.top==firstTop && label.x==generalContext.labelX)) {
                if(graphics.images.stream().anyMatch(box->box.width>50 && box.height>25
                        && box.y>=label.top-1 && box.y+box.height<=label.bottom+1 && box.x>=label.right-1))contentImage=true;
            }
            if(!generalFinished || generalContext.key.equals("reading list"))
                recoverGeneralPage(pageCells,data,generalContext,document.getPage(page-1));
            if(pageCells.stream().anyMatch(c->c.text.equalsIgnoreCase("Reading list")))generalFinished=true;
            recoverRows(pageCells,topics,weekly,mappings,tableContext,document.getPage(page-1));
            recoverCloSourceAreas(document.getPage(page-1), graphics, pageCells, data);
            if(filledPolygonRules)recoverDsMergedGeneralAreas(document.getPage(page-1),graphics,data);
            recoverAssessmentCells(pageCells, assessmentHeaders, assessmentRows, assessmentContext);
        }
        if (!assessmentRows.isEmpty()) {
            var assessments = new ArrayList<com.scse.curriculum.syllabus.importer.dto.AssessmentImportData>();
            var contributions = new ArrayList<SyllabusImportData.AssessmentCloMappingItem>();
            for (var row : assessmentRows) {
                var weight = java.util.regex.Pattern.compile("(?s)^(.*?)\\s*\\((\\d+(?:\\.\\d+)?)\\s*%\\)\\s*$").matcher(row.get(0));
                if (!weight.matches()) continue;
                String name = clean(weight.group(1));
                assessments.add(com.scse.curriculum.syllabus.importer.dto.AssessmentImportData.builder()
                        .name(name).assessmentType(name).weightPercent(Float.valueOf(weight.group(2)))
                        .orderIndex(assessments.size()+1).build());
                for (int column=1;column<row.size() && column<assessmentHeaders.size();column++) {
                    String code=assessmentHeaders.get(column).replaceAll("\\s+","");
                    String value=row.get(column).replaceAll("\\s+","");
                    if (code.matches("(?i)CLO\\d+") && value.matches("\\d+(?:\\.\\d+)?%"))
                        contributions.add(SyllabusImportData.AssessmentCloMappingItem.builder().assessmentIndex(assessments.size())
                                .cloCode(code).percentage(Double.valueOf(value.substring(0,value.length()-1))).build());
                }
            }
            if (assessments.size()==assessmentRows.size()) {
                data.setAssessments(assessments);data.setAssessmentCloMappings(contributions);
                var table=new LinkedHashMap<String,Object>();table.put("title","Assessment plan — original source cells");
                table.put("headers",assessmentHeaders);table.put("rows",assessmentRows);
                data.getSourceProvenance().computeIfAbsent("assessmentPlan",k->new LinkedHashMap<>()).put("sourceTable",
                        SyllabusImportData.SourceFieldProvenance.builder().state(SyllabusImportData.SourceFieldState.PARSED)
                                .sourceLabel(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(table)).build());
            }
        }
        if(!topics.isEmpty() && topics.size()>=data.getTopics().size()) {
            // Content weighting is independent of the weekly schedule. Preserve the schedule's
            // CLO cells there, without manufacturing Content-to-week/CLO associations by title.
            data.setTopics(topics);data.setTopicCloMappings(new ArrayList<>());
            if (data.getSourceProvenance() == null) data.setSourceProvenance(new LinkedHashMap<>());
            if(tableContext.malformedContentRows==0)data.getSourceProvenance().computeIfAbsent("content", ignored -> new LinkedHashMap<>()).put("sourceRows",
                    SyllabusImportData.SourceFieldProvenance.builder().state(SyllabusImportData.SourceFieldState.PARSED)
                            .sourceLabel("Content table: independent ruled source cells, including page continuations").build());
        }
        if(tableContext.malformedContentRows>0)incomplete.add("SOURCE_NOT_VERIFIED: " + tableContext.malformedContentRows
                + " Content rows contain overlapping or unreadable source cells; verify all row boundaries before confirmation.");
        if(contentImage)incomplete.add("SOURCE_NOT_VERIFIED: Content contains an image table. Its rows require source verification before confirmation.");
        if(!topics.isEmpty() && topics.size()<data.getTopics().size()) {
            incomplete.add("Only " + topics.size() + " ruled content rows were recovered; source row boundaries require verification before confirmation.");
        }
        if(!weekly.isEmpty() && weekly.size()<data.getWeeklyActivities().size()) {
            incomplete.add("Weekly table recovery is incomplete; verify source activities before confirmation.");
            if (data.getSourceProvenance() == null) data.setSourceProvenance(new LinkedHashMap<>());
            data.getSourceProvenance().computeIfAbsent("plannedActivities", ignored -> new LinkedHashMap<>()).put("sourceRows",
                    SyllabusImportData.SourceFieldProvenance.builder().state(SyllabusImportData.SourceFieldState.UNRESOLVED)
                            .sourceLabel("Only source rows with explicit week cells are retained; other weekly rows require source review").build());
        }
        if(!weekly.isEmpty())data.setWeeklyActivities(weekly);
        if(!mappings.isEmpty() && mappings.size()>=data.getCloPloMappings().size())data.setCloPloMappings(mappings);
        return incomplete;
    }
    /** The enclosing CLO cell spans inner competency-table rules and must be read as a whole. */
    private static void recoverCloSourceAreas(PDPage page, Lines lines, List<Cell> cells, SyllabusImportData data) {
        float height=page.getCropBox().getHeight();
        var vertical=lines.edges.stream().filter(e->Math.abs(e.x1-e.x2)<0.7 && Math.abs(e.y1-e.y2)>30).toList();
        var horizontal=lines.edges.stream().filter(e->Math.abs(e.y1-e.y2)<0.7 && Math.abs(e.x1-e.x2)>100).toList();
        var labelAreas=new ArrayList<Rectangle2D.Float>();
        var candidates=new ArrayList<CloSourceArea>();
        for(var left:vertical) for(var right:vertical) {
            if(right.x1-left.x1<60 || right.x1-left.x1>200)continue;
            if(Math.abs(Math.min(left.y1,left.y2)-Math.min(right.y1,right.y2))>1.5
                    || Math.abs(Math.max(left.y1,left.y2)-Math.max(right.y1,right.y2))>1.5)continue;
            float top=height-Math.max(left.y1,left.y2),bottom=height-Math.min(left.y1,left.y2);
            labelAreas.add(new Rectangle2D.Float(left.x1+1,top+1,(right.x1-1)-(left.x1+1),(bottom-1)-(top+1)));
            candidates.add(new CloSourceArea(right.x1,top,bottom));
        }
        // Decode a page once for all label regions, then once for matched value regions.
        // Region coordinates and processing order are identical to individual readArea calls.
        var labels=readAreas(page,labelAreas);
        var valueAreas=new ArrayList<Rectangle2D.Float>();
        var keys=new ArrayList<String>();
        for(int index=0;index<candidates.size();index++) {
            var area=candidates.get(index);
            String label=labels.get(index);
            String key=label.toLowerCase(Locale.ROOT).replaceFirst("^\\d+\\.\\s*", "");
            if(!Set.of("course learning outcomes","course designation","study and examination requirements").contains(key))continue;
            float end=horizontal.stream().filter(e->(Math.abs(height-e.y1-area.top)<1.5 || Math.abs(height-e.y1-area.bottom)<1.5)
                    && Math.min(e.x1,e.x2)<=area.right+1).map(e->Math.max(e.x1,e.x2)).max(Float::compare).orElse(area.right);
            if(end>area.right+50) {
                keys.add(key);
                valueAreas.add(new Rectangle2D.Float(area.right+1,area.top+1,(end-1)-(area.right+1),(area.bottom-1)-(area.top+1)));
            }
        }
        var values=readAreas(page,valueAreas);
        for(int index=0;index<keys.size();index++) {
            if(keys.get(index).equals("course learning outcomes"))recoverClos(values.get(index),data);
            else assignGeneral(keys.get(index),values.get(index),data);
        }
        // Hierarchical CLOs use separate nested description cells, including page continuations.
        for(var cell:cells) if(cell.text.matches("(?is).*\\bCLO\\s*\\d+(?:\\.|:)\\s+\\p{L}.*"))recoverClos(cell.text,data);
    }
    private record CloSourceArea(float right,float top,float bottom) {}

    /** DS filled rules join across rows; derive merged label cells from their actual full-width borders. */
    private static void recoverDsMergedGeneralAreas(PDPage page,Lines lines,SyllabusImportData data) {
        float height=page.getCropBox().getHeight();
        var vertical=lines.edges.stream().filter(e->Math.abs(e.x1-e.x2)<0.01 && Math.abs(e.y1-e.y2)>50).toList();
        var horizontal=lines.edges.stream().filter(e->Math.abs(e.y1-e.y2)<0.01 && Math.abs(e.x1-e.x2)>60).toList();
        var labels=new ArrayList<Rectangle2D.Float>();var values=new ArrayList<Rectangle2D.Float>();
        for(var left:vertical)for(var right:vertical) {
            if(right.x1-left.x1<60 || right.x1-left.x1>200)continue;
            float top=height-Math.min(Math.max(left.y1,left.y2),Math.max(right.y1,right.y2));
            float bottom=height-Math.max(Math.min(left.y1,left.y2),Math.min(right.y1,right.y2));
            var rules=horizontal.stream().filter(e->Math.min(e.x1,e.x2)<=left.x1+1 && Math.max(e.x1,e.x2)>=right.x1-1
                    && height-e.y1>=top-1 && height-e.y1<=bottom+1).toList();
            var boundaries=new TreeSet<Float>();rules.forEach(e->boundaries.add(height-e.y1));
            var ys=new ArrayList<>(boundaries);
            for(int index=0;index+1<ys.size();index++) {
                float begin=ys.get(index),end=ys.get(index+1);if(end-begin<5)continue;
                float far=rules.stream().filter(e->Math.abs(height-e.y1-begin)<1 || Math.abs(height-e.y1-end)<1)
                        .map(e->Math.max(e.x1,e.x2)).max(Float::compare).orElse(right.x1);
                if(far<=right.x1+50)continue;
                labels.add(new Rectangle2D.Float(left.x1+1,begin+1,right.x1-left.x1-2,end-begin-2));
                values.add(new Rectangle2D.Float(right.x1+1,begin+1,far-right.x1-2,end-begin-2));
            }
        }
        var labelTexts=readAreas(page,labels);var matched=new ArrayList<Rectangle2D.Float>();var keys=new ArrayList<String>();
        for(int index=0;index<labelTexts.size();index++) {
            String key=labelTexts.get(index).toLowerCase(Locale.ROOT).replaceFirst("^\\d+\\.\\s*", "");
            if(Set.of("course learning outcomes","course designation","study and examination requirements").contains(key)) {
                keys.add(key);matched.add(values.get(index));
            }
        }
        var texts=readAreas(page,matched);
        for(int index=0;index<keys.size();index++) {
            if(keys.get(index).equals("course learning outcomes"))recoverClos(texts.get(index),data);
            else assignGeneral(keys.get(index),texts.get(index),data);
        }
    }

    private static List<String> readAreas(PDPage page,List<Rectangle2D.Float> areas) {
        if(areas.isEmpty())return List.of();
        try {
            var text=new PDFTextStripperByArea();text.setSortByPosition(true);
            for(int index=0;index<areas.size();index++)text.addRegion(Integer.toString(index),areas.get(index));
            text.extractRegions(page);
            var values=new ArrayList<String>(areas.size());
            for(int index=0;index<areas.size();index++)values.add(clean(text.getTextForRegion(Integer.toString(index))));
            return values;
        } catch(IOException error) { throw new IllegalStateException("Cannot read source cell",error); }
    }
    private static final class AssessmentTableContext { float start=-1,end=-1,firstRight=-1; }
    private static void recoverAssessmentCells(List<Cell> cells,List<String> headers,List<List<String>> output,AssessmentTableContext context) {
        var rows=new TreeMap<Float,List<Cell>>();
        for(var cell:cells)rows.computeIfAbsent(cell.top,k->new ArrayList<>()).add(cell);
        float start=context.start,end=context.end;
        for(var row:rows.values()) {
            row.sort(Comparator.comparing(Cell::x));
            for(var cell:row) if(cell.text.replaceAll("\\s+","").equalsIgnoreCase("AssessmentType")) {
                start=cell.x;end=row.get(row.size()-1).right;
                context.start=start;context.end=end;context.firstRight=cell.right;
                var selected=row.stream().filter(c->c.x>=cell.x).toList();
                headers.clear();selected.forEach(c->headers.add(c.text));break;
            }
            if(start<0)continue;
            final float begin=start,finish=end;
            var selected=row.stream().filter(c->c.x>=begin-1 && c.right<=finish+1).toList();
            if(selected.isEmpty() || Math.abs(selected.get(0).x-start)>1)continue;
            if(Math.abs(selected.get(0).right-context.firstRight)>1)continue;
            String first=selected.get(0).text;
            if(first.isBlank() && selected.stream().anyMatch(c->c.text.replaceAll("\\s+","").matches("(?i)CLO\\d+"))) {
                for(int n=1;n<selected.size();n++)if(!selected.get(n).text.isBlank() && n<headers.size())headers.set(n,selected.get(n).text);
            }
            if(selected.size()==headers.size() && first.matches("(?s).*\\(\\d+(?:\\.\\d+)?\\s*%\\)\\s*$"))output.add(selected.stream().map(Cell::text).toList());
        }
    }
    private static String topicKey(String text) {
        return clean(text).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
    }
    static boolean matchesWeeklyTopic(String content, String weekly) {
        String name=topicKey(content), actual=topicKey(weekly);
        return !name.isBlank() && (actual.equals(name)
                || actual.matches(java.util.regex.Pattern.quote(name) + " part \\d+"));
    }

    private static final class TableContext {
        final boolean filledPolygonRules;
        TableContext(){this(false);}
        TableContext(boolean filledPolygonRules){this.filledPolygonRules=filledPolygonRules;}
        String kind="";
        List<String> headers=List.of();
        float startX=0;
        int malformedContentRows=0;
        List<Cell> contentColumns=List.of();
    }

    static void recoverRows(List<Cell> cells,List<com.scse.curriculum.syllabus.importer.dto.TopicImportData> topics,
                            List<SyllabusImportData.WeeklyActivityItem> weekly,List<SyllabusImportData.CloPloMappingItem> mappings) {
        recoverRows(cells, topics, weekly, mappings, new TableContext(),null);
    }

    private static void recoverRows(List<Cell> cells,List<com.scse.curriculum.syllabus.importer.dto.TopicImportData> topics,
                            List<SyllabusImportData.WeeklyActivityItem> weekly,List<SyllabusImportData.CloPloMappingItem> mappings,
                            TableContext context,PDPage page) {
        TreeMap<Float,List<Cell>> rows=new TreeMap<>();
        for(Cell cell:cells)rows.computeIfAbsent(cell.top,k->new ArrayList<>()).add(cell);
        String kind=context.kind;List<String> headers=context.headers;float startX=context.startX;
        for(List<Cell> row:rows.values()) {
            row.sort(Comparator.comparing(Cell::x));
            if(context.filledPolygonRules && page!=null)row=mergeDsTopicHeaderFragments(row,page);
            List<String> values=row.stream().map(Cell::text).toList();
            if(values.size()<2 && !(context.filledPolygonRules && kind.equals("content")
                    && row.size()==1 && row.get(0).text.contains("EXAM WEEK")))continue;
            if(values.stream().map(v->v.replaceAll("\\s+", "").toLowerCase(Locale.ROOT)).anyMatch(v->Set.of(
                    "examinationforms","examinationform","studyandexaminationrequirements","readinglist").contains(v))) {
                kind="";headers=List.of();continue;
            }
            int firstTopic=-1,firstWeek=-1,firstClo=-1;
            for(int i=0;i<values.size();i++) {
                String word=values.get(i).replaceAll("\\s+","").toLowerCase(Locale.ROOT);
                if(word.equals("topic"))firstTopic=i;
                if(word.equals("week"))firstWeek=i;
                if(Set.of("clo","clos","clo/plo","clo-plo","slo").contains(word) && i+2<values.size()
                        && values.get(i+1).matches("(?i)(?:PLO|SLO)?\\d+"))firstClo=i;
            }
            boolean contentHeader=firstTopic>=0 && firstTopic+2<values.size()
                    && (values.get(firstTopic+1).replaceAll("\\s+", "").equalsIgnoreCase("Weight")
                        || context.filledPolygonRules && isDsWeightHeader(values.get(firstTopic+1)))
                    && values.get(firstTopic+2).replaceAll("\\s+", "").equalsIgnoreCase("Level");
            int begin=firstWeek>=0?firstWeek:contentHeader?firstTopic:firstClo;
            if(begin>=0) {
                kind=firstWeek>=0?"weekly":contentHeader?"content":"matrix";
                headers=new ArrayList<>(values.subList(begin,values.size()));
                // An enclosing Word table can leave an empty spacer cell beside the nested table.
                while(!headers.isEmpty() && headers.get(headers.size()-1).isBlank())headers.remove(headers.size()-1);
                if(kind.equals("content") && headers.size()==3)context.contentColumns=new ArrayList<>(row.subList(begin,begin+3));
                startX=row.get(begin).x;continue;
            }
            final float rowStart=startX;
            List<Cell> inside=row.stream().filter(c->c.x>=rowStart-1).toList();
            if(inside.size()>headers.size() && inside.subList(headers.size(),inside.size()).stream().allMatch(c->c.text.isBlank()))
                inside=inside.subList(0,headers.size());
            if(context.filledPolygonRules && kind.equals("content") && context.contentColumns.size()==3
                    && !inside.isEmpty() && inside.get(0).text.contains("EXAM WEEK")
                    && inside.stream().skip(1).allMatch(cell->cell.text.isBlank())
                    && Math.abs(inside.get(0).x-context.contentColumns.get(0).x)<1
                    && Math.abs(inside.get(0).right-context.contentColumns.get(2).right)<1) {
                topics.add(com.scse.curriculum.syllabus.importer.dto.TopicImportData.builder()
                        .name(inside.get(0).text).orderInWeek(topics.size()+1).build());
                continue;
            }
            if(kind.equals("content") && inside.size()!=headers.size() && context.contentColumns.size()==3 && page!=null) {
                List<Cell> restored=new ArrayList<>();
                for(Cell column:context.contentColumns) {
                    List<Cell> fragments=row.stream().filter(c->c.x>=column.x-1 && c.right<=column.right+1
                            && c.right>column.x && c.x<column.right).toList();
                    if(fragments.isEmpty() || fragments.get(0).x>column.x+1 || fragments.get(fragments.size()-1).right<column.right-1)break;
                    Cell first=fragments.get(0);
                    try {
                        PDFTextStripperByArea fullCell=new PDFTextStripperByArea();fullCell.setSortByPosition(true);
                        fullCell.addRegion("whole",new Rectangle2D.Float(column.x,first.top,column.right-column.x,first.bottom-first.top));
                        fullCell.extractRegions(page);
                        restored.add(new Cell(column.x,first.top,column.right,first.bottom,clean(fullCell.getTextForRegion("whole"))));
                    } catch(IOException error){restored.clear();break;}
                }
                if(restored.size()==3)inside=restored;
            }
            if(inside.size()!=headers.size())continue;
            values=inside.stream().map(Cell::text).toList();
            // Some source Content tables contain a merged examination-week row.
            // Preserve that actual row, without appending the Examination forms section.
            if(kind.equals("content") && values.size()==3 && page!=null && values.get(2).isBlank()
                    && !values.get(1).matches("\\d+(?:\\.\\d+)?")
                    && String.join(" ",values).contains("EXAM WEEK")) {
                Cell first=inside.get(0),last=inside.get(2);
                String whole=readArea(page,first.x,first.top,last.right,first.bottom);
                values=List.of(whole,"","");
            }
            if(kind.equals("content") && values.size()==3 && !values.get(0).isBlank()
                    && (values.get(1).isBlank() || values.get(1).matches("\\d+(?:\\.\\d+)?")
                        || context.filledPolygonRules && values.get(1).matches("\\d+(?:\\.\\d+)?(?:\\s*\\+\\s*\\d+(?:\\.\\d+)?)+"))
                    && (values.get(2).isBlank() || values.get(2).matches("[ITU](?:\\s*,?\\s*[ITU])*"))) {
                topics.add(com.scse.curriculum.syllabus.importer.dto.TopicImportData.builder()
                        .name(values.get(0)).contentWeight(values.get(1)).contentLevel(values.get(2)).teachingLevel(values.get(2))
                        .orderInWeek(topics.size()+1).build());
            } else if(kind.equals("content") && values.size()==3 && !values.get(0).isBlank()) {
                if(!values.get(0).matches("(?i)(?:CLO)?\\d+"))context.malformedContentRows++;
            } else if(kind.equals("weekly") && values.get(0).matches("\\d+") && values.size()>=5) {
                String topic=values.get(1),clo=weeklyCell(headers,values,"clo"),assessment=weeklyCell(headers,values,"assessment"),
                        activities=weeklyCell(headers,values,"activit"),resources=weeklyCell(headers,values,"resource");
                if(page!=null) {
                    String merged=readArea(page,inside.get(1).x,inside.get(1).top,inside.get(inside.size()-1).right,inside.get(1).bottom);
                    if(merged.matches("(?i)(?:MIDTERM|FINAL)\\s+EXAM(?:INATION)?(?:\\s+WEEK.*)?")) {
                        topic=merged;clo="";assessment="";activities="";resources="";
                    }
                }
                weekly.add(SyllabusImportData.WeeklyActivityItem.builder().week(Integer.valueOf(values.get(0)))
                        .topic(topic).clo(clo).assessments(assessment)
                        .learningActivities(activities).resources(resources).build());
            } else if(kind.equals("matrix") && values.get(0).matches("\\d+")) {
                for(int c=1;c<values.size();c++)if(values.get(c).matches("(?i)x{1,3}|[1-4]|[A-Z](?:\\s*,\\s*[A-Z])*")) {
                    String header=headers.get(c);String code=header.matches("\\d+")?"PLO"+header:header;
                    String mark=values.get(c).matches("(?i)x+") ? values.get(c).toLowerCase(Locale.ROOT) : values.get(c);
                    mappings.add(SyllabusImportData.CloPloMappingItem.builder().cloCode("CLO"+values.get(0))
                            .ploCode(code).value(mark).contributionWeight(mark.matches("x+")?mark.length()*100f/3:null).build());
                }
            }
        }
        context.kind=kind; context.headers=headers; context.startX=startX;
    }

    private static boolean isDsWeightHeader(String text) {
        return clean(text).matches("(?i)Weight(?:\\s*\\(Theory\\s*\\+\\s*Lab\\.?\\))?");
    }
    /** Re-read the complete source region when an enclosing border cuts the actual Topic cell. */
    private static List<Cell> mergeDsTopicHeaderFragments(List<Cell> row,PDPage page) {
        for(int weight=1;weight+1<row.size();weight++) {
            if(!isDsWeightHeader(row.get(weight).text) || !row.get(weight+1).text.equalsIgnoreCase("Level"))continue;
            for(int first=weight-1;first>=0;first--) {
                String fragment=row.subList(first,weight).stream().map(Cell::text).reduce("",String::concat).replaceAll("\\s+","");
                if(fragment.length()>5)break;
                if(!fragment.equalsIgnoreCase("Topic"))continue;
                if(weight-first==1)return row;
                Cell begin=row.get(first),last=row.get(weight-1);
                var joined=new Cell(begin.x,begin.top,last.right,begin.bottom,readArea(page,begin.x,begin.top,last.right,begin.bottom));
                var restored=new ArrayList<Cell>(row.subList(0,first));restored.add(joined);restored.addAll(row.subList(weight,row.size()));
                return restored;
            }
        }
        return row;
    }

    private static String weeklyCell(List<String> headers,List<String> values,String heading) {
        for(int index=0;index<headers.size();index++)if(headers.get(index).replaceAll("\\s+","").toLowerCase(Locale.ROOT).contains(heading))return values.get(index);
        return "";
    }

    private static String readArea(PDPage page,float left,float top,float right,float bottom) {
        try {
            var text=new PDFTextStripperByArea();text.setSortByPosition(true);
            text.addRegion("source",new Rectangle2D.Float(left,top,right-left,bottom-top));
            text.extractRegions(page);return clean(text.getTextForRegion("source"));
        } catch(IOException error) { throw new IllegalStateException("Cannot read source cell",error); }
    }

    private static final class GeneralContext {
        String key="",text="";float labelX,valueX,right;
    }

    private static void recoverGeneralPage(List<Cell> cells,SyllabusImportData data,GeneralContext context,PDPage page) {
        recoverGeneral(cells,data);
        TreeMap<Float,List<Cell>> rows=new TreeMap<>();
        for(Cell cell:cells)rows.computeIfAbsent(cell.top,k->new ArrayList<>()).add(cell);
        boolean firstRow=true;
        for(List<Cell> row:rows.values()) {
            row.sort(Comparator.comparing(Cell::x));Cell label=row.get(0);
            String key=clean(label.text).toLowerCase(Locale.ROOT);
            if(firstRow && key.isBlank() && !context.key.isBlank() && row.size()>1
                    && Math.abs(label.x-context.labelX)<20 && Math.abs(label.right-context.valueX)<20) {
                String continuation=readArea(page,label.right,label.top,row.get(row.size()-1).right,label.bottom);
                if(!continuation.isBlank()) {
                    context.text=clean(context.text+" "+continuation);
                    assignGeneral(context.key,context.text,data);
                }
            }
            firstRow=false;
            if(row.size()<2 || key.isBlank())continue;
            if(key.equals("course designation") || key.equals("course objectives") || key.equals("course aims")
                    || key.equals("content") || key.equals("reading list") || key.equals("study and examination requirements")
                    || key.equals("examination forms") || key.equals("examination form")) {
                context.key=key;context.labelX=label.x;context.valueX=label.right;
                context.right=row.get(row.size()-1).right;
                context.text=readArea(page,label.right,label.top,context.right,label.bottom);
                assignGeneral(key,context.text,data);
            } else if(!key.isBlank()) context.key="";
        }
    }

    private static void assignGeneral(String key,String value,SyllabusImportData data) {
        switch(key) {
            case "course designation" -> data.setCourseDesignation(value);
            case "course objectives", "course aims" -> data.setObjectives(value);
            case "content" -> data.setContentNote(value);
            case "examination forms", "examination form" -> data.setExamForms(value);
            case "study and examination requirements" -> data.setExamRequirements(value);
            case "reading list" -> {
                data.setReadings(new ArrayList<>());
                if(!value.isBlank())data.getReadings().add(SyllabusImportData.ReadingItem.builder()
                        .title(value).type("RAW_SOURCE").build());
            }
            default -> { }
        }
    }

    static void recoverGeneral(List<Cell> cells,SyllabusImportData data) {
        for(Cell label:cells) {
            String key=clean(label.text()).toLowerCase(Locale.ROOT);
            Cell value=cells.stream().filter(c->Math.abs(c.x-label.right)<1.5 && Math.abs(c.top-label.top)<1.5 && Math.abs(c.bottom-label.bottom)<1.5).findFirst().orElse(null);
            if(value==null || value.text.isBlank())continue;
            switch(key) {
                case "course designation" -> data.setCourseDesignation(value.text);
                case "course objectives", "course aims" -> data.setObjectives(value.text);
                case "person responsible for the course" -> data.setPersonResponsible(value.text);
                case "language", "language of instruction" -> data.setLanguage(value.text);
                case "relation to curriculum" -> {
                    data.setRelation(value.text);
                    if(data.getCourseTypes()!=null && !value.text.toLowerCase(Locale.ROOT).contains(data.getCourseTypes().toLowerCase(Locale.ROOT)))data.setCourseTypes(null);
                }
                case "teaching methods" -> data.setTeachingMethods(value.text);
                case "examination forms", "examination form" -> data.setExamForms(value.text);
                case "study and examination requirements" -> data.setExamRequirements(value.text);
                case "required and recommended prerequisites for joining the course" -> data.setPrerequisites(value.text);
                case "course learning outcomes" -> recoverClos(value.text,data);
                case "credit points" -> {
                    String credits=value.text;
                    var total=java.util.regex.Pattern.compile("(?i)Number of credits\\s*:\\s*(.+?)(?=Lecture\\s*:|Laboratory\\s*:|$)").matcher(credits);
                    if(total.find())data.setCreditPoints(total.group(1).trim());
                    else if(credits.matches("\\d+(?:\\.\\d+)?"))data.setCreditPoints(credits);
                    var lecture=java.util.regex.Pattern.compile("(?i)Lecture\\s*:\\s*(.+?)(?=Laboratory\\s*:|$)").matcher(credits);
                    if(lecture.find())data.setLectureCredits(lecture.group(1).trim());
                    var lab=java.util.regex.Pattern.compile("(?i)Laboratory\\s*:\\s*(.+)$").matcher(credits);
                    if(lab.find())data.setLaboratoryCredits(lab.group(1).trim());
                }
                default -> {
                    if(key.startsWith("workload")) recoverWorkload(value.text,data);
                    if(key.startsWith("semester"))data.setSemester(value.text);
                }
            }
        }
    }

    static void recoverClos(String text,SyllabusImportData data) {
        text=text.split("(?i)Competency(?:\\s+Course learning outcome(?:\\s*\\(CLO\\))?)?\\s+level",2)[0];
        var matcher=java.util.regex.Pattern.compile("(?i)\\bCLO\\s*(\\d+)(?!\\d|\\.\\d)(?:\\s*[.:](?!\\d)\\s*|\\s+)(.*?)(?=\\bCLO\\s*\\d+(?!\\d|\\.\\d)(?:\\s*[.:](?!\\d)|\\s+)|$)").matcher(text);
        while(matcher.find()) {
            String code="CLO"+matcher.group(1);String description=clean(matcher.group(2));
            data.getClos().stream().filter(c->code.equalsIgnoreCase(c.getCode())).findFirst()
                    .ifPresent(c->c.setDescription(description));
        }
    }

    static void recoverWorkload(String text,SyllabusImportData data) {
        var total=java.util.regex.Pattern.compile("(?i)Total workload\\s*:\\s*(.+?)(?=Contact hours|$)").matcher(text);
        if(total.find())data.setWorkloadTotal((text.matches("(?is).*\\bEstimated\\b.*") ? "(Estimated) " : "") + total.group(1).trim());
        var contact=java.util.regex.Pattern.compile("(?i)Contact hours(?:\\s*\\([^)]*\\))?\\s*:\\s*(.+?)(?=Private study|Student responsibility|$)").matcher(text);
        if(contact.find())data.setWorkloadContact(contact.group(1).trim());
        var study=java.util.regex.Pattern.compile("(?i)Private study including examination preparation,? specified in hours\\s*:\\s*(.+?)(?=Student responsibility|$)").matcher(text);
        if(study.find())data.setWorkloadPrivate(study.group(1).trim());
    }

    private static final class Lines extends PDFGraphicsStreamEngine {
        final List<Edge> edges=new ArrayList<>(); final List<Edge> pending=new ArrayList<>();
        final Set<Edge> stroked=new HashSet<>();
        final List<Edge> filledBorders=new ArrayList<>();
        final List<Rectangle2D.Float> images=new ArrayList<>();
        Point2D current=new Point2D.Float();Point2D initial=current;
        final boolean filledPolygonRules;
        Lines(PDPage page){this(page,false);}
        Lines(PDPage page,boolean filledPolygonRules){super(page);this.filledPolygonRules=filledPolygonRules;}
        public void appendRectangle(Point2D a,Point2D b,Point2D c,Point2D d){
            int begin=pending.size();add(a,b);add(b,c);add(c,d);add(d,a);
            double width=Math.max(Math.max(a.getX(),b.getX()),Math.max(c.getX(),d.getX()))-Math.min(Math.min(a.getX(),b.getX()),Math.min(c.getX(),d.getX()));
            double height=Math.max(Math.max(a.getY(),b.getY()),Math.max(c.getY(),d.getY()))-Math.min(Math.min(a.getY(),b.getY()),Math.min(c.getY(),d.getY()));
            if(width<2 || height<2)filledBorders.addAll(pending.subList(begin,pending.size()));
        }
        private void add(Point2D a,Point2D b){pending.add(new Edge((float)a.getX(),(float)a.getY(),(float)b.getX(),(float)b.getY()));}
        public void moveTo(float x,float y){current=new Point2D.Float(x,y);initial=current;}
        public void lineTo(float x,float y){Point2D next=new Point2D.Float(x,y);add(current,next);current=next;}
        public Point2D getCurrentPoint(){return current;}
        public void closePath(){add(current,initial);current=initial;}
        public void curveTo(float a,float b,float c,float d,float e,float f){current=new Point2D.Float(e,f);}
        public void drawImage(PDImage image){
            var transform=getGraphicsState().getCurrentTransformationMatrix();
            var a=transform.transformPoint(0,0);var b=transform.transformPoint(1,1);
            images.add(new Rectangle2D.Float((float)Math.min(a.x,b.x),getPage().getCropBox().getHeight()-(float)Math.max(a.y,b.y),
                    (float)Math.abs(b.x-a.x),(float)Math.abs(b.y-a.y)));
        }
        public void clip(int rule){} public void endPath(){pending.clear();filledBorders.clear();}
        private void painted(){stroked.addAll(pending);edges.addAll(pending);endPath();}
        public void strokePath(){painted();}
        public void fillPath(int rule){
            if(filledPolygonRules && filledBorders.isEmpty() && pending.size()==4
                    && pending.stream().allMatch(e->Math.abs(e.x1-e.x2)<0.01 || Math.abs(e.y1-e.y2)<0.01)) {
                float minX=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
                for(var edge:pending){minX=Math.min(minX,Math.min(edge.x1,edge.x2));maxX=Math.max(maxX,Math.max(edge.x1,edge.x2));
                    minY=Math.min(minY,Math.min(edge.y1,edge.y2));maxY=Math.max(maxY,Math.max(edge.y1,edge.y2));}
                float width=maxX-minX,height=maxY-minY;
                if((width>0 && width<2 && height>2) || (height>0 && height<2 && width>2))edges.addAll(pending);
            }
            edges.addAll(filledBorders);endPath();
        }
        /** Filled rules can be painted as several adjacent rectangles along one table border. */
        void mergeRuleSegments() {
            var horizontal=new TreeMap<Float,List<Edge>>();var vertical=new TreeMap<Float,List<Edge>>();
            var other=new ArrayList<Edge>();
            for(var edge:edges) {
                if(Math.abs(edge.y1-edge.y2)<0.01 && Math.abs(edge.x1-edge.x2)>2)
                    horizontal.computeIfAbsent(edge.y1,k->new ArrayList<>()).add(edge);
                else if(Math.abs(edge.x1-edge.x2)<0.01 && Math.abs(edge.y1-edge.y2)>2)
                    vertical.computeIfAbsent(edge.x1,k->new ArrayList<>()).add(edge);
                else if(Math.abs(edge.x1-edge.x2)>2 && Math.abs(edge.y1-edge.y2)>2)other.add(edge);
            }
            edges.clear();edges.addAll(other);
            for(var entry:horizontal.entrySet())mergeAxis(entry.getKey(),entry.getValue(),true);
            for(var entry:vertical.entrySet())mergeAxis(entry.getKey(),entry.getValue(),false);
            // These are visible painted rules, including inner column dividers.
            stroked.clear();stroked.addAll(edges);
        }
        private void mergeAxis(float coordinate,List<Edge> input,boolean horizontal) {
            input.sort(Comparator.comparingDouble(e->horizontal?Math.min(e.x1,e.x2):Math.min(e.y1,e.y2)));
            float begin=Float.NaN,end=Float.NaN;
            for(var edge:input) {
                float low=horizontal?Math.min(edge.x1,edge.x2):Math.min(edge.y1,edge.y2);
                float high=horizontal?Math.max(edge.x1,edge.x2):Math.max(edge.y1,edge.y2);
                if(Float.isNaN(begin)){begin=low;end=high;}
                else if(low<=end+0.7f)end=Math.max(end,high);
                else {edges.add(horizontal?new Edge(begin,coordinate,end,coordinate):new Edge(coordinate,begin,coordinate,end));begin=low;end=high;}
            }
            if(!Float.isNaN(begin))edges.add(horizontal?new Edge(begin,coordinate,end,coordinate):new Edge(coordinate,begin,coordinate,end));
        }
        public void fillAndStrokePath(int rule){painted();} public void shadingFill(org.apache.pdfbox.cos.COSName name){}
    }
}
