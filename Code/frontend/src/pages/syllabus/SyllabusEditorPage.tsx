import { useEffect, useRef } from "react"
import { useNavigate, useParams } from "react-router-dom"

import { Button } from "@/components/ui/button"
import SyllabusEditorShell from "@/components/syllabus/editor/SyllabusEditorShell"
import { useSyllabus } from "@/hooks/useSyllabus"

import {
  measureSyllabusEditorLoad,
} from "@/performance/syllabusEditorPerformance"


export default function SyllabusEditorPage() {

  const { id } = useParams()
  const navigate = useNavigate()


 const startTime = useRef<number>(0);

useEffect(() => {
  startTime.current = performance.now();
}, []);


  const syllabusId = id ? Number(id) : NaN
  const isValidId =
    Number.isFinite(syllabusId)


  const {
    data: syllabus,
    isLoading,
    isError,
  } = useSyllabus(
    isValidId ? syllabusId : null
  )


  /**
   * NFR-01.2
   * Đo thời gian từ lúc mở editor
   * đến khi dữ liệu syllabus sẵn sàng
   */
  useEffect(() => {

    if (
      syllabus &&
      isValidId
    ) {

      measureSyllabusEditorLoad(
        syllabusId,
        startTime.current
      )

    }

  }, [
    syllabus,
    syllabusId,
    isValidId
  ])



  if (!isValidId) {

    return (
      <div className="p-6 space-y-4">

        <h1 className="text-xl font-bold text-slate-900">
          Invalid syllabus ID
        </h1>


        <Button
          variant="outline"
          onClick={() => navigate(-1)}
        >
          Back
        </Button>

      </div>
    )
  }



  if (isLoading) {

    return (
      <div
        className="
        flex
        h-64
        items-center
        justify-center
        text-slate-500
        "
      >
        Loading syllabus...
      </div>
    )

  }



  if (
    isError ||
    !syllabus
  ) {

    return (

      <div className="p-6 space-y-4">

        <h1 className="text-xl font-bold text-slate-900">
          Syllabus Not Found
        </h1>


        <p className="text-sm text-slate-500">
          This syllabus does not exist or you do not have permission to access it.
        </p>


        <Button
          variant="outline"
          onClick={() => navigate(-1)}
        >
          Back
        </Button>


      </div>

    )

  }



  return (

    <SyllabusEditorShell
      syllabus={syllabus}
      readOnly={
        syllabus.status !== "DRAFT"
      }
    />

  )
}