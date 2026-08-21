import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Edit2 } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface CourseCardProps {
  id: number;
  courseCode: string;
  courseName: string;
  courseTypeName: string;
  onEdit: () => void;
  onClick: () => void;
}

// Map course types to colors like the image
const getColorByType = (type: string) => {
  const t = type.trim().toUpperCase()

  if (
    t === "GENERAL" ||
    t === "GENERAL EDUCATION"
  ) {
    return "bg-blue-400 text-white"
  }

  if (
    t === "COMPULSORY" ||
    t === "COMPULSORY COURSE"
  ) {
    return "bg-amber-400 text-black"
  }

  if (
    t === "ELECTIVE" ||
    t === "ELECTIVE COURSE"
  ) {
    return "bg-green-400 text-black"
  }

  return "bg-gray-200 text-black"
}

export function CourseCard({
  id,
  courseCode,
  courseName,
  courseTypeName,
  onEdit,
  onClick,
}: CourseCardProps) {
  const colorClass = getColorByType(courseTypeName);

  return (
    <Card 
      id={`course-${id}`}
      className={cn(
        "relative w-full p-3 shadow-md rounded-2xl flex flex-col items-center justify-center text-center cursor-pointer transition-transform hover:scale-105 group border-2 border-slate-700/20",
        colorClass
      )}
      onClick={onClick}
    >
      <div className="text-xs font-semibold uppercase opacity-80 mb-1">{courseCode}</div>
      <div className="text-sm font-bold leading-tight px-1">{courseName}</div>

      {/* Hover action overlay */}
      <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex space-x-1">
        <Button
          size="icon"
          variant="secondary"
          className="h-6 w-6 bg-white text-black hover:bg-gray-100 shadow"
          onClick={(e) => {
            e.stopPropagation();
            onEdit();
          }}
          title="Edit placement"
        >
          <Edit2 className="h-3 w-3" />
        </Button>
      </div>
    </Card>
  );
}
