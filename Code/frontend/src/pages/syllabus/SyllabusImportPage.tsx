import { useState } from "react"
import { Navigate, useNavigate } from "react-router-dom"
import { FileUp } from "lucide-react"

import AddSyllabusDialog from "@/components/syllabus/AddSyllabusDialog"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { saveSyllabusImportDraft } from "@/lib/syllabusImportDraft"
import { useAuthStore } from "@/store/authStore"

export default function SyllabusImportPage() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(true)
  const { isAuthenticated, user } = useAuthStore()
  const role = String(user?.role ?? "").replace(/^ROLE_/i, "").toUpperCase()
  const syllabusBasePath = role === "INSTRUCTOR" ? "/instructor/syllabus" : "/admin/syllabus"

  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (!role) return <Navigate to="/" replace />

  return (
    <div className="container mx-auto max-w-3xl p-6">
      <Card>
        <CardHeader>
          <CardTitle>Import Syllabus Template</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm leading-6 text-muted-foreground">
            Select the target course, extract the file, and review every mapped value in the editable Syllabus Form. Importing does not create or overwrite a syllabus.
          </p>
          <Button type="button" onClick={() => setOpen(true)}>
            <FileUp className="size-4" />
            Select Course & File
          </Button>
        </CardContent>
      </Card>

      <AddSyllabusDialog
        open={open}
        onOpenChange={setOpen}
        onCreateRequested={({ courseId, courseProgramId, importPreview }) => {
          const importDraftId = importPreview && courseId ? saveSyllabusImportDraft(importPreview, courseId, courseProgramId) : undefined
          navigate(
            `${syllabusBasePath}/create?${courseId ? `courseId=${courseId}&` : ""}${courseProgramId ? `courseProgramId=${courseProgramId}&` : ""}${importDraftId ? `importDraft=${encodeURIComponent(importDraftId)}` : ""}`.replace(/[?&]$/, ""),
            { state: importPreview ? { importPreview, targetCourseId: courseId, targetCourseProgramId: courseProgramId } : undefined },
          )
        }}
      />
    </div>
  )
}
