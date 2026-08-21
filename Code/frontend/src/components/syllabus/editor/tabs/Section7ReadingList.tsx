import { useMemo, useState } from "react"
import {
  AlertCircle,
  BookOpen,
  ExternalLink,
  LibraryBig,
  Loader2,
  Plus,
  Trash2,
  X,
} from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  useAddSyllabusBook,
  useAllBooks,
  useCreateBook,
  useRemoveSyllabusBook,
  useSyllabusBooks,
} from "@/hooks/useSyllabusBooks"
import type {
  Book,
  CreateBookRequest,
  UsageType,
} from "@/types/book"

interface Props {
  syllabusId: number
  readOnly?: boolean
}

type AddMode =
  | "existing"
  | "new"

const USAGE_TYPES: {
  value: UsageType
  label: string
  description: string
  className: string
}[] = [
  {
    value: "REQUIRED",
    label: "Required",
    description:
      "Core material students are expected to use.",
    className:
      "border-rose-200 bg-rose-50 text-rose-700",
  },
  {
    value: "RECOMMENDED",
    label: "Recommended",
    description:
      "Suggested material that supports the course.",
    className:
      "border-blue-200 bg-blue-50 text-blue-700",
  },
  {
    value: "SUPPLEMENTARY",
    label: "Supplementary",
    description:
      "Additional material for broader or deeper study.",
    className:
      "border-slate-200 bg-slate-50 text-slate-700",
  },
]

const emptyBookForm = ():
  CreateBookRequest => ({
    title: "",
    author: "",
    publisher: "",
    year: undefined,
    edition: "",
    isbn: "",
    url: "",
  })

const usageConfig = (
  value?: UsageType,
) =>
  USAGE_TYPES.find(
    (item) =>
      item.value === value,
  )
  ?? USAGE_TYPES[2]

const validUrl = (
  value: string,
) => {
  if (!value.trim()) {
    return true
  }

  try {
    const parsed =
      new URL(value)

    return (
      parsed.protocol
        === "http:"
      || parsed.protocol
        === "https:"
    )
  } catch {
    return false
  }
}

export default function Section7ReadingList({
  syllabusId,
  readOnly = false,
}: Props) {
  const {
    data:
      syllabusBooks = [],
    isLoading:
      loadingSyllabusBooks,
  } =
    useSyllabusBooks(
      syllabusId,
    )

  const {
    data:
      allBooks = [],
    isLoading:
      loadingAllBooks,
  } =
    useAllBooks()

  const createBookMutation =
    useCreateBook()

  const addSyllabusBookMutation =
    useAddSyllabusBook()

  const removeSyllabusBookMutation =
    useRemoveSyllabusBook(
      syllabusId,
    )

  const [
    showForm,
    setShowForm,
  ] = useState(false)

  const [
    mode,
    setMode,
  ] =
    useState<AddMode>(
      "existing",
    )

  const [
    selectedBookId,
    setSelectedBookId,
  ] = useState("")

  const [
    usageType,
    setUsageType,
  ] =
    useState<UsageType>(
      "REQUIRED",
    )

  const [
    newBook,
    setNewBook,
  ] =
    useState<CreateBookRequest>(
      emptyBookForm(),
    )

  const [
    errors,
    setErrors,
  ] =
    useState<
      Record<
        string,
        string
      >
    >({})

  const linkedBookIds =
    useMemo(
      () =>
        new Set(
          syllabusBooks.map(
            (book) =>
              book.bookId,
          ),
        ),
      [syllabusBooks],
    )

  const availableBooks =
    useMemo(
      () =>
        allBooks
          .filter(
            (book) =>
              !linkedBookIds
                .has(book.id),
          )
          .slice()
          .sort(
            (
              left,
              right,
            ) =>
              left.title
                .localeCompare(
                  right.title,
                  "en",
                  {
                    numeric: true,
                  },
                ),
          ),
      [
        allBooks,
        linkedBookIds,
      ],
    )

  const sortedSyllabusBooks =
    useMemo(
      () =>
        [...syllabusBooks].sort(
          (
            left,
            right,
          ) =>
            Number(
              left.orderIndex
              ?? 0,
            )
            - Number(
              right.orderIndex
              ?? 0,
            )
            || left.bookTitle
              .localeCompare(
                right.bookTitle,
                "en",
                {
                  numeric: true,
                },
              ),
        ),
      [syllabusBooks],
    )

  const counts =
    useMemo(
      () => ({
        required:
          syllabusBooks.filter(
            (book) =>
              book.usageType
              === "REQUIRED",
          ).length,
        recommended:
          syllabusBooks.filter(
            (book) =>
              book.usageType
              === "RECOMMENDED",
          ).length,
        supplementary:
          syllabusBooks.filter(
            (book) =>
              book.usageType
              === "SUPPLEMENTARY",
          ).length,
      }),
      [syllabusBooks],
    )

  const isLoading =
    loadingSyllabusBooks
    || loadingAllBooks

  const nextOrder =
    sortedSyllabusBooks.reduce(
      (
        max,
        book,
      ) =>
        Math.max(
          max,
          Number(
            book.orderIndex
            ?? 0,
          ),
        ),
      0,
    )
    + 1

  const resetForm = () => {
    setShowForm(false)
    setMode("existing")
    setSelectedBookId("")
    setUsageType("REQUIRED")
    setNewBook(
      emptyBookForm(),
    )
    setErrors({})
  }

  const validate = () => {
    const nextErrors:
      Record<
        string,
        string
      > = {}

    if (
      mode === "existing"
      && !selectedBookId
    ) {
      nextErrors.bookId =
        "Select a library resource."
    }

    if (
      mode === "new"
      && !newBook.title
        .trim()
    ) {
      nextErrors.title =
        "Resource title is required."
    }

    if (
      mode === "new"
      && newBook.year
      && (
        newBook.year < 1000
        || newBook.year > 2100
      )
    ) {
      nextErrors.year =
        "Enter a valid publication year."
    }

    if (
      mode === "new"
      && newBook.url
      && !validUrl(
        newBook.url,
      )
    ) {
      nextErrors.url =
        "URL must start with http:// or https://."
    }

    setErrors(
      nextErrors,
    )

    return (
      Object.keys(
        nextErrors,
      ).length === 0
    )
  }

  const addExisting = () => {
    if (!validate()) {
      return
    }

    addSyllabusBookMutation
      .mutate(
        {
          syllabusId,
          bookId:
            Number(
              selectedBookId,
            ),
          usageType,
          orderIndex:
            nextOrder,
        },
        {
          onSuccess:
            resetForm,
        },
      )
  }

  const createAndAdd = () => {
    if (!validate()) {
      return
    }

    createBookMutation
      .mutate(
        {
          title:
            newBook.title
              .trim(),
          author:
            newBook.author
              ?.trim()
            || undefined,
          publisher:
            newBook.publisher
              ?.trim()
            || undefined,
          year:
            newBook.year,
          edition:
            newBook.edition
              ?.trim()
            || undefined,
          isbn:
            newBook.isbn
              ?.trim()
            || undefined,
          url:
            newBook.url
              ?.trim()
            || undefined,
        },
        {
          onSuccess:
            (
              createdBook:
                Book,
            ) => {
              addSyllabusBookMutation
                .mutate(
                  {
                    syllabusId,
                    bookId:
                      createdBook.id,
                    usageType,
                    orderIndex:
                      nextOrder,
                  },
                  {
                    onSuccess:
                      resetForm,
                  },
                )
            },
        },
      )
  }

  const handleAdd = () => {
    if (
      mode === "existing"
    ) {
      addExisting()
      return
    }

    createAndAdd()
  }

  const handleRemove = (
    bookId: number,
    title: string,
  ) => {
    const confirmed =
      window.confirm(
        `Remove "${title}" from this syllabus reading list? The shared library resource itself will not be deleted.`,
      )

    if (!confirmed) {
      return
    }

    removeSyllabusBookMutation
      .mutate(bookId)
  }

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center gap-2 text-slate-400">
        <Loader2 className="size-5 animate-spin" />
        Loading reading list...
      </div>
    )
  }

  return (
    <div className="space-y-5">
      <Card className="border border-blue-100 bg-blue-50/40 shadow-sm">
        <CardContent className="p-4">
          <div className="flex items-start gap-3">
            <LibraryBig className="mt-0.5 size-5 shrink-0 text-blue-500" />

            <div>
              <p className="text-sm font-semibold text-blue-800">
                Syllabus Reading List
              </p>

              <p className="mt-1 text-xs leading-5 text-blue-700">
                Link an existing library resource or create a new bibliographic record, then classify its use as Required, Recommended, or Supplementary.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {sortedSyllabusBooks.length === 0 && (
        <Card className="border border-rose-200 bg-rose-50/40 shadow-sm">
          <CardContent className="p-4">
            <div className="flex items-start gap-2">
              <AlertCircle className="mt-0.5 size-4 shrink-0 text-rose-600" />

              <p className="text-xs leading-5 text-rose-800">
                Reading List is required before submission. Add at least one academic resource.
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-3 md:grid-cols-3">
        <UsageMetric
          label="Required"
          value={counts.required}
        />
        <UsageMetric
          label="Recommended"
          value={counts.recommended}
        />
        <UsageMetric
          label="Supplementary"
          value={counts.supplementary}
        />
      </div>

      <Card className="border border-slate-200 shadow-sm">
        <CardHeader className="flex flex-row items-center justify-between gap-4 border-b border-slate-100 pb-3">
          <CardTitle className="flex items-center gap-2 text-sm font-bold text-slate-700">
            <BookOpen className="size-4 text-primary" />
            Resource List

            <Badge
              variant="outline"
              className="border-cyan-200 bg-cyan-50 text-primary"
            >
              {sortedSyllabusBooks.length}{" "}
              {sortedSyllabusBooks.length === 1
                ? "resource"
                : "resources"}
            </Badge>
          </CardTitle>

          {!readOnly && (
            <Button
              type="button"
              size="sm"
              onClick={() =>
                setShowForm(true)
              }
              disabled={showForm}
              className="h-8 gap-1.5 bg-primary text-xs text-white hover:bg-primary/90"
            >
              <Plus className="size-3.5" />
              Add Resource
            </Button>
          )}
        </CardHeader>

        <CardContent className="p-0">
          {sortedSyllabusBooks.length === 0
            && !showForm ? (
            <div className="px-6 py-12 text-center">
              <LibraryBig className="mx-auto mb-3 size-10 text-slate-200" />
              <p className="text-sm font-medium text-slate-500">
                No reading resources defined
              </p>
              <p className="mt-1 text-xs text-slate-400">
                Add a textbook, article, standard, website, or other academic resource.
              </p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {sortedSyllabusBooks.map(
                (book, index) => {
                  const usage =
                    usageConfig(
                      book.usageType,
                    )

                  return (
                    <div
                      key={book.bookId}
                      className="group flex items-start gap-4 p-4 transition hover:bg-slate-50/50"
                    >
                      <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-xs font-bold text-primary">
                        {index + 1}
                      </span>

                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="text-sm font-semibold leading-6 text-slate-800">
                            {book.bookTitle}
                          </p>

                          <Badge
                            variant="outline"
                            className={usage.className}
                          >
                            {usage.label}
                          </Badge>
                        </div>

                        <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
                          {book.author && (
                            <span>Author: {book.author}</span>
                          )}
                          {book.publisher && (
                            <span>Publisher: {book.publisher}</span>
                          )}
                          {book.year && (
                            <span>Year: {book.year}</span>
                          )}
                          {book.edition && (
                            <span>Edition: {book.edition}</span>
                          )}
                          {book.isbn && (
                            <span>ISBN: {book.isbn}</span>
                          )}
                        </div>

                        {book.url && (
                          <a
                            href={book.url}
                            target="_blank"
                            rel="noreferrer"
                            className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                          >
                            <ExternalLink className="size-3" />
                            Open Resource Link
                          </a>
                        )}
                      </div>

                      {!readOnly && (
                        <Button
                          type="button"
                          size="sm"
                          variant="ghost"
                          aria-label={`Remove ${book.bookTitle}`}
                          onClick={() =>
                            handleRemove(
                              book.bookId,
                              book.bookTitle,
                            )
                          }
                          disabled={removeSyllabusBookMutation.isPending}
                          className="size-8 shrink-0 p-0 text-slate-400 hover:bg-rose-50 hover:text-rose-600"
                        >
                          <Trash2 className="size-3.5" />
                        </Button>
                      )}
                    </div>
                  )
                },
              )}
            </div>
          )}

          {showForm
            && !readOnly && (
            <div className="border-t border-slate-200 bg-slate-50/70 p-4">
              <div className="mb-4 flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-bold uppercase tracking-wider text-slate-600">
                    Add Reading Resource
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    Reuse a shared library record when possible to avoid duplicate bibliographic data.
                  </p>
                </div>

                <Button
                  type="button"
                  size="sm"
                  variant="ghost"
                  onClick={resetForm}
                  className="size-7 p-0 text-slate-400 hover:text-slate-700"
                >
                  <X className="size-4" />
                </Button>
              </div>

              <div className="mb-4 grid gap-3 md:grid-cols-2">
                <div className="space-y-1">
                  <Label className="text-xs font-semibold text-slate-600">
                    Add Method
                  </Label>
                  <Select
                    value={mode}
                    onValueChange={(value) => {
                      setMode(
                        value as AddMode,
                      )
                      setErrors({})
                    }}
                  >
                    <SelectTrigger className="h-9 bg-white text-sm">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="existing">
                        Select from Existing Library
                      </SelectItem>
                      <SelectItem value="new">
                        Create New Resource
                      </SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1">
                  <Label className="text-xs font-semibold text-slate-600">
                    Usage Type *
                  </Label>
                  <Select
                    value={usageType}
                    onValueChange={(value) =>
                      setUsageType(
                        value as UsageType,
                      )
                    }
                  >
                    <SelectTrigger className="h-9 bg-white text-sm">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {USAGE_TYPES.map(
                        (type) => (
                          <SelectItem
                            key={type.value}
                            value={type.value}
                          >
                            {type.label}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>

                  <p className="text-[11px] text-slate-400">
                    {usageConfig(usageType).description}
                  </p>
                </div>
              </div>

              {mode === "existing" ? (
                <div className="space-y-1">
                  <Label className="text-xs font-semibold text-slate-600">
                    Library Resource *
                  </Label>

                  <Select
                    value={selectedBookId}
                    onValueChange={(value) => {
                      setSelectedBookId(value)
                      setErrors({})
                    }}
                  >
                    <SelectTrigger className="h-9 bg-white text-sm">
                      <SelectValue placeholder="Select resource..." />
                    </SelectTrigger>

                    <SelectContent>
                      {availableBooks.length === 0 ? (
                        <SelectItem
                          value="__empty"
                          disabled
                        >
                          No unlinked library resources available
                        </SelectItem>
                      ) : (
                        availableBooks.map(
                          (book) => (
                            <SelectItem
                              key={book.id}
                              value={String(book.id)}
                            >
                              {book.title}
                              {book.author
                                ? ` — ${book.author}`
                                : ""}
                            </SelectItem>
                          ),
                        )
                      )}
                    </SelectContent>
                  </Select>

                  <FieldError message={errors.bookId} />
                </div>
              ) : (
                <div className="grid gap-3 md:grid-cols-2">
                  <div className="space-y-1 md:col-span-2">
                    <Label className="text-xs font-semibold text-slate-600">
                      Resource Title *
                    </Label>
                    <Input
                      className="h-9 bg-white text-sm"
                      value={newBook.title}
                      onChange={(event) =>
                        setNewBook({
                          ...newBook,
                          title:
                            event.target.value,
                        })
                      }
                      placeholder="Book, article, standard, website..."
                    />
                    <FieldError message={errors.title} />
                  </div>

                  <div className="space-y-1">
                    <Label className="text-xs font-semibold text-slate-600">
                      Author
                    </Label>
                    <Input
                      className="h-9 bg-white text-sm"
                      value={newBook.author ?? ""}
                      onChange={(event) =>
                        setNewBook({
                          ...newBook,
                          author:
                            event.target.value,
                        })
                      }
                    />
                  </div>

                  <div className="space-y-1">
                    <Label className="text-xs font-semibold text-slate-600">
                      Publisher
                    </Label>
                    <Input
                      className="h-9 bg-white text-sm"
                      value={newBook.publisher ?? ""}
                      onChange={(event) =>
                        setNewBook({
                          ...newBook,
                          publisher:
                            event.target.value,
                        })
                      }
                    />
                  </div>

                  <div className="space-y-1">
                    <Label className="text-xs font-semibold text-slate-600">
                      Publication Year
                    </Label>
                    <Input
                      type="number"
                      min={1000}
                      max={2100}
                      className="h-9 bg-white text-sm"
                      value={newBook.year ?? ""}
                      onChange={(event) =>
                        setNewBook({
                          ...newBook,
                          year:
                            event.target.value
                              ? Number(
                                  event.target.value,
                                )
                              : undefined,
                        })
                      }
                    />
                    <FieldError message={errors.year} />
                  </div>

                  <div className="space-y-1">
                    <Label className="text-xs font-semibold text-slate-600">
                      Edition
                    </Label>
                    <Input
                      className="h-9 bg-white text-sm"
                      value={newBook.edition ?? ""}
                      onChange={(event) =>
                        setNewBook({
                          ...newBook,
                          edition:
                            event.target.value,
                        })
                      }
                    />
                  </div>

                  <div className="space-y-1">
                    <Label className="text-xs font-semibold text-slate-600">
                      ISBN
                    </Label>
                    <Input
                      className="h-9 bg-white text-sm"
                      value={newBook.isbn ?? ""}
                      onChange={(event) =>
                        setNewBook({
                          ...newBook,
                          isbn:
                            event.target.value,
                        })
                      }
                    />
                  </div>

                  <div className="space-y-1">
                    <Label className="text-xs font-semibold text-slate-600">
                      URL
                    </Label>
                    <Input
                      className="h-9 bg-white text-sm"
                      value={newBook.url ?? ""}
                      onChange={(event) =>
                        setNewBook({
                          ...newBook,
                          url:
                            event.target.value,
                        })
                      }
                      placeholder="https://..."
                    />
                    <FieldError message={errors.url} />
                  </div>
                </div>
              )}

              <div className="mt-4 flex justify-end gap-2">
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={resetForm}
                  className="h-8 text-xs"
                >
                  Cancel
                </Button>

                <Button
                  type="button"
                  size="sm"
                  onClick={handleAdd}
                  disabled={
                    createBookMutation.isPending
                    || addSyllabusBookMutation.isPending
                  }
                  className="h-8 gap-1.5 bg-primary text-xs text-white hover:bg-primary/90"
                >
                  {(createBookMutation.isPending
                    || addSyllabusBookMutation.isPending) && (
                    <Loader2 className="size-3.5 animate-spin" />
                  )}
                  Save Resource
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function UsageMetric({
  label,
  value,
}: {
  label: string
  value: number
}) {
  return (
    <Card className="border border-slate-200 shadow-sm">
      <CardContent className="pb-3 pt-3 text-center">
        <p className="text-2xl font-bold text-primary">
          {value}
        </p>
        <p className="mt-0.5 text-xs text-slate-500">
          {label}
        </p>
      </CardContent>
    </Card>
  )
}

function FieldError({
  message,
}: {
  message?: string
}) {
  if (!message) {
    return null
  }

  return (
    <p className="text-xs font-medium text-rose-500">
      {message}
    </p>
  )
}