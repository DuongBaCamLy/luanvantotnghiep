import {
  useMutation,
  useQueryClient,
} from "@tanstack/react-query"

import {
  deleteUser,
} from "@/api/userApi"
import {
  t,
} from "@/i18n"
import {
  Button,
} from "@/components/ui/button"
import {
  Alert,
  AlertDescription,
} from "@/components/ui/alert"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import type {
  UserAccountResponse,
} from "@/types/user"

interface Props {
  open: boolean
  onOpenChange:
    (open: boolean) => void
  user:
    UserAccountResponse | null
}

function getErrorMessage(
  error: unknown
): string {
  if (
    typeof error === "object"
    && error !== null
    && "response" in error
  ) {
    const response = (
      error as {
        response?: {
          data?: {
            message?: string
          }
        }
      }
    ).response

    if (
      response?.data?.message
    ) {
      return response.data.message
    }
  }

  if (
    error instanceof Error
  ) {
    return error.message
  }

  return t(
    "userDelete.failed"
  )
}

export default function DeleteUserDialog({
  open,
  onOpenChange,
  user,
}: Props) {
  const queryClient =
    useQueryClient()

  const mutation =
    useMutation({
      mutationFn: () =>
        deleteUser(
          user!.id
        ),

      onSuccess: () => {
        queryClient.invalidateQueries({
          queryKey: ["users"],
        })

        onOpenChange(false)
      },
    })

  const description =
    t(
      "userDelete.description",
      {
        username:
          user?.username
          ?? "",
      }
    )

  const errorMessage =
    mutation.isError
      ? getErrorMessage(
          mutation.error
        )
      : null

  return (
    <Dialog
      open={open}
      onOpenChange={
        onOpenChange
      }
    >
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>
            {t(
              "userDelete.title"
            )}
          </DialogTitle>

          <DialogDescription>
            {description}
          </DialogDescription>
        </DialogHeader>

        {errorMessage && (
          <Alert variant="destructive">
            <AlertDescription>
              {errorMessage}
            </AlertDescription>
          </Alert>
        )}

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() =>
              onOpenChange(
                false
              )
            }
          >
            {t(
              "common.cancel"
            )}
          </Button>

          <Button
            variant="destructive"
            onClick={() =>
              mutation.mutate()
            }
            disabled={
              mutation.isPending
              || !user
            }
          >
            {mutation.isPending
              ? t(
                  "userDelete.deleting"
                )
              : t(
                  "common.delete"
                )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}