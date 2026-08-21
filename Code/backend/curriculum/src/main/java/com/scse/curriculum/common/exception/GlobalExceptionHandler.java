package com.scse.curriculum.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import com.scse.curriculum.common.security.UnsafeInputException;
import com.scse.curriculum.syllabus.dto.SubmissionValidationResponse;
import com.scse.curriculum.syllabus.exception.SyllabusSubmissionValidationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ResourceNotFoundException ex) {
        return Map.of("message", ex.getMessage());
    }


    @ExceptionHandler(ForbiddenOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleForbidden(ForbiddenOperationException ex) {
        return Map.of("message", ex.getMessage());
    }

    /**
     * Spring Security method authorization (@PreAuthorize) throws
     * AccessDeniedException/AuthorizationDeniedException before the controller
     * body is executed. Handle it explicitly so denied requests return 403
     * instead of falling through to the generic 500 handler.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAccessDenied(AccessDeniedException ex) {
        return Map.of(
                "message",
                "Bạn không có quyền thực hiện thao tác này.");
    }

    @ExceptionHandler(SyllabusSubmissionValidationException.class)
    public ResponseEntity<SubmissionValidationResponse> handleSubmissionValidation(
            SyllabusSubmissionValidationException ex) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ex.getValidation());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(DataIntegrityViolationException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleBadCredentials(BadCredentialsException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(AccountStatusException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAccountStatus(AccountStatusException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        // Gộp lại 1 message tổng để FE dễ hiển thị, vẫn giữ chi tiết theo field
        String message = errors.values().stream().findFirst().orElse("Dữ liệu không hợp lệ");
        errors.put("message", message);
        return errors;
    }


    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleIllegalState(IllegalStateException ex) {
        return Map.of(
                "message",
                ex.getMessage() != null
                        ? ex.getMessage()
                        : "Trạng thái dữ liệu không cho phép thao tác này");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("message", ex.getMessage() != null ? ex.getMessage() : "null");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleException(Exception ex) {
        ex.printStackTrace(); // Optional
        return Map.of("message", ex.getMessage() != null ? ex.getMessage() : ex.toString(), "cause", ex.getCause() != null ? ex.getCause().toString() : "null");
    }
    @ExceptionHandler(UnsafeInputException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public Map<String, String> handleUnsafeInput(
        UnsafeInputException ex) {

    return Map.of(
            "message",
            ex.getMessage()
    );
}
}