import { useMemo } from "react";
import { useSearchParams } from "react-router-dom"; // Import useSearchParams
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { syllabusApi } from "@/api/syllabusApi";
import type { Syllabus } from "@/types/syllabus";
import { CourseNode } from "@/components/curriculum/CourseNode";
import Xarrow, { Xwrapper } from "react-xarrows";
import { Plus } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import SyllabusToolbar from "@/components/syllabus/SyllabusToolbar"; // Import Toolbar đồng bộ

export default function CurriculumMapPage() {
  const [searchParams] = useSearchParams();
  
  // Đọc params từ URL (đồng bộ với Toolbar)
  const searchVal = searchParams.get("q") || "";
  const selectedMajor = searchParams.get("major") || "all";
  const selectedCohort = searchParams.get("year") || "all";
  const selectedSemester = searchParams.get("semester") || "all";

  // Data fetching
  const { data: syllabuses, isLoading } = useQuery({
    queryKey: ["syllabuses-all"],
    queryFn: syllabusApi.getAll,
  });

  // Filter syllabuses based on URL Params
  const filteredSyllabuses = useMemo(() => {
    if (!syllabuses) return [];
    return syllabuses.filter((s) => {
      // Lọc theo Major (Sử dụng metadata để map)
      const majorMatch = selectedMajor === "all" || (s.major === selectedMajor);
      
      // Lọc theo Khóa
      const matchCohort = selectedCohort === "all" || (s.academicYear || "").trim() === selectedCohort;
      
      // Lọc theo Học kỳ
      const matchSemester = selectedSemester === "all" || String(s.semester) === selectedSemester;
      
      // Lọc theo Tìm kiếm
      const searchLower = searchVal.toLowerCase();
      const matchSearch =
        !searchVal ||
        s.courseCode?.toLowerCase().includes(searchLower) ||
        s.courseName?.toLowerCase().includes(searchLower);

      return majorMatch && matchCohort && matchSemester && matchSearch;
    });
  }, [syllabuses, selectedMajor, selectedCohort, selectedSemester, searchVal]);

  // Organize courses by semester (1 to 8)
  const semesters = useMemo(() => {
    const s: Record<number, Syllabus[]> = {};
    for (let i = 1; i <= 8; i++) s[i] = [];
    filteredSyllabuses.forEach((cp) => {
      const sem = parseInt(String(cp.semester ?? "1"), 10) || 1;
      if (!s[sem]) s[sem] = [];
      s[sem].push(cp);
    });
    return s;
  }, [filteredSyllabuses]);

  const arrows = useMemo(() => {
    if (!filteredSyllabuses) return [];
    const renderedCourses = filteredSyllabuses.filter(s => s.courseCode);
    const validArrows: Array<{ start: string; end: string; dashness: boolean; color: string }> = [];
    
    renderedCourses.forEach((dep) => {
      const depCode = dep.courseCode;
      const preStr = dep.prerequisites?.toLowerCase() || "";
      renderedCourses.forEach((pre) => {
        if (pre.courseCode === depCode) return;
        if (preStr.includes(pre.courseCode.toLowerCase())) {
          validArrows.push({ start: `node-${pre.courseCode}`, end: `node-${depCode}`, dashness: false, color: "#1e293b" });
        }
      });
    });
    return validArrows;
  }, [filteredSyllabuses]);

  return (
    <div className="p-6 h-full flex flex-col bg-[#FDFDF9]">
      {/* Header Area */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <div className="text-xs font-bold text-amber-600 uppercase tracking-wider mb-1">SCSE / CURRICULUM MAP</div>
          <h1 className="text-3xl font-bold text-[#1e293b]">Curriculum Mapping</h1>
          <p className="text-sm text-slate-500 mt-2">Semester-based course distribution generated from syllabus data.</p>
        </div>
        <Button className="bg-[#1e293b] hover:bg-slate-800 text-white rounded-md flex items-center gap-2">
          <Plus className="size-4" /> Create New Syllabus
        </Button>
      </div>

      {/* Thay thế bộ lọc cũ bằng Toolbar đồng bộ */}
      <SyllabusToolbar />

      {/* Main Card */}
      <Card className="flex-1 flex flex-col bg-white border-slate-200 shadow-sm rounded-xl overflow-hidden">
        <div className="p-6 border-b border-slate-100 flex justify-between">
          <h2 className="text-xl font-bold text-[#1e293b]">Course Distribution Map</h2>
          <div className="flex gap-3">
             <Button variant="outline" className="text-xs">EXPORT PDF</Button>
             <Button className="bg-[#0f172a] text-white text-xs">GENERATE MAPPING</Button>
          </div>
        </div>

        {isLoading ? (
          <div className="flex-1 flex items-center justify-center py-20">Loading curriculum map...</div>
        ) : filteredSyllabuses.length === 0 ? (
          <div className="flex-1 flex items-center justify-center py-20 text-slate-400">No data matches the selected filters.</div>
        ) : (
          <div className="flex-1 overflow-x-auto p-6 min-h-[500px]">
            <Xwrapper>
              <div className="flex gap-16 min-w-max pb-32">
                {[1, 2, 3, 4, 5, 6, 7, 8].map((sem) => (
                  <div key={sem} className="flex-1 min-w-[200px] flex flex-col gap-6">
                    <h3 className="font-bold text-center border-b border-slate-200 pb-3 uppercase text-sm">Semester {sem}</h3>
                    <div className="flex flex-col gap-6 items-center">
                      {semesters[sem]?.map((s) => (
                        <CourseNode
                          key={s.id}
                          id={`node-${s.courseCode}`}
                          courseName={s.courseName}
                          credits={3} // Giá trị giả định
                          courseTypes={s.courseTypes ? JSON.parse(s.courseTypes) : []}
                        />
                      ))}
                    </div>
                  </div>
                ))}
              </div>
              {arrows.map((arr, idx) => (
                <Xarrow key={idx} start={arr.start} end={arr.end} color={arr.color} strokeWidth={1.5} path="grid" dashness={arr.dashness} headSize={4} curveness={0.3} />
              ))}
            </Xwrapper>
          </div>
        )}
      </Card>
    </div>
  );
}