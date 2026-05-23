//package com.example.backend.web;
//
//import com.example.backend.dto.ApiMessage;
//import jakarta.validation.ConstraintViolationException;
//import org.springframework.http.*;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<ApiMessage> handleIllegalArgument(IllegalArgumentException ex) {
//        String msg = ex.getMessage();
//        if ("Email already registered".equalsIgnoreCase(msg)) {
//            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiMessage(msg));
//        }
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiMessage(msg));
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
//        Map<String, Object> body = new HashMap<>();
//        body.put("message", "Validation failed");
//        body.put("errors", errors);
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ApiMessage> handleConstraint(ConstraintViolationException ex) {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiMessage(ex.getMessage()));
//    }
//
//    @ExceptionHandler(AuthenticationException.class)
//    public ResponseEntity<ApiMessage> handleAuth(AuthenticationException ex) {
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiMessage("Invalid credentials or unauthorized"));
//    }
//
//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<ApiMessage> handleDenied(AccessDeniedException ex) {
//        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiMessage("Forbidden"));
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiMessage> handleGeneric(Exception ex) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiMessage("Unexpected error"));
//    }
//}



package com.example.backend.web;

import com.example.backend.dto.ApiMessage;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Business / validation errors ──────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage();
        if ("Email already registered".equalsIgnoreCase(msg)) {
            // 409 Conflict — frontend checks this status to surface error on email field
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiMessage(msg));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiMessage(msg));
    }

    /**
     * Spring @Valid / @Validated field-level errors.
     * Returns:
     * {
     *   "message": "Validation failed",
     *   "errors": {
     *     "email":    "Invalid email format",
     *     "password": "Password does not meet complexity requirements",
     *     ...
     *   }
     * }
     * Frontend reads body.errors and maps each key to its field.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("message", "Validation failed");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiMessage> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiMessage(ex.getMessage()));
    }

    // ── Spring Security authentication errors ─────────────────────────────────

    /**
     * BadCredentialsException  → wrong email or password
     * DisabledException        → account not verified (enabled = false)
     * LockedException          → account locked
     *
     * All return 401 with a specific "message" string.
     * Frontend in login.component.ts checks err.error.message to show the right text.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiMessage> handleAuth(AuthenticationException ex) {
        if (ex instanceof BadCredentialsException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiMessage("Invalid email or password"));
        }
        if (ex instanceof DisabledException) {
            // Must match the string checked in login.component.ts:
            //   if (msg === 'Account not verified') { ... }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiMessage("Account not verified"));
        }
        if (ex instanceof LockedException) {
            // Must match:  if (msg === 'Account locked') { ... }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiMessage("Account locked"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiMessage("Authentication failed"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiMessage> handleDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiMessage("Forbidden"));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiMessage> handleGeneric(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiMessage("An unexpected error occurred. Please try again."));
    }
}