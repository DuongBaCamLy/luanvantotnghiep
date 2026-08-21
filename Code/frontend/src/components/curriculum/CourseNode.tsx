import { cn } from "@/lib/utils"

export interface CourseNodeProps {
  id: string; // DOM ID for Xarrow
  courseName: string;
  credits: number;
  courseTypes: string[]; // COMPULSORY | ELECTIVE | GENERAL
  onClick?: () => void;
}

export function CourseNode({ id, courseName, credits, courseTypes, onClick }: CourseNodeProps) {
  // Determine color based on courseTypes
 let bgClass =
  "bg-slate-200 border-slate-400"

const normalizedTypes =
  (courseTypes ?? []).map(
    (type) =>
      type.trim().toUpperCase(),
  )

if (
  normalizedTypes.includes("GENERAL")
) {
  bgClass =
    "bg-[#00b0f0] border-sky-600 shadow-[inset_0_0_10px_rgba(255,255,255,0.4)]"
} else if (
  normalizedTypes.includes("COMPULSORY")
) {
  bgClass =
    "bg-[#ffff00] border-yellow-500 shadow-[inset_0_0_10px_rgba(255,255,255,0.5)]"
} else if (
  normalizedTypes.includes("ELECTIVE")
) {
  bgClass =
    "bg-[#92d050] border-green-600 shadow-[inset_0_0_10px_rgba(255,255,255,0.3)]"
}

  return (
    <div
      id={id}
      onClick={onClick}
      className={cn(
        "relative flex flex-col items-center justify-center p-3 text-center cursor-pointer transition-transform hover:scale-105 z-10",
        "w-44 min-h-[70px] rounded-full border border-b-4 border-r-2", // Pill shape with 3D button effect
        "text-slate-900 text-xs font-semibold leading-tight",
        bgClass
      )}
      style={{
        boxShadow: "2px 4px 6px rgba(0,0,0,0.15), inset -2px -2px 6px rgba(0,0,0,0.1)",
      }}
    >
      <div className="line-clamp-3 px-2">
        {courseName} ({credits}C)
      </div>
    </div>
  )
}
