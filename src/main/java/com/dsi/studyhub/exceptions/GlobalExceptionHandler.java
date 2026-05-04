package com.dsi.studyhub.exceptions;

import com.dsi.studyhub.dtos.ErrorResDto;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.HibernateException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    private boolean isDev() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) return true;
        for (String p : profiles) {
            if (p.equals("prod")) return false;
        }
        return true;
    }

    private String getRootCauseMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private String getStackTrace(Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private List<String> buildDetails(Throwable ex) {
        List<String> details = new ArrayList<>();
        details.add(getRootCauseMessage(ex));
        if (isDev()) {
            details.add(getStackTrace(ex));
        }
        return details;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResDto> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        logger.warning("Resource not found: " + ex.getMessage() + " at " + request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResDto> handleConflict(ConflictException ex, HttpServletRequest request) {
        logger.warning("Conflict: " + ex.getMessage() + " at " + request.getRequestURI());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResDto> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        logger.warning("Unauthorized: " + ex.getMessage() + " at " + request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResDto> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        logger.warning("Forbidden: " + ex.getMessage() + " at " + request.getRequestURI());
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResDto> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid username or password", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResDto> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Malformed JSON request body";
        String rootMsg = getRootCauseMessage(ex);
        if (rootMsg.contains("JsonParseException") || rootMsg.contains("MalformedInput")) {
            message = "Invalid JSON format in request body";
        } else if (rootMsg.contains("missing")) {
            message = rootMsg;
        }
        return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), List.of(rootMsg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResDto> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warning("Illegal argument: " + ex.getMessage() + " at " + request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(JDBCConnectionException.class)
    public ResponseEntity<ErrorResDto> handleJDBCConnection(JDBCConnectionException ex, HttpServletRequest request) {
        logger.severe("Database connection failed: " + getRootCauseMessage(ex));
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Database connection failed", request.getRequestURI(), buildDetails(ex));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResDto> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        logger.warning("Constraint violation: " + getRootCauseMessage(ex));
        String message = "Database constraint violated";
        String msg = ex.getMessage().toLowerCase();
        if (msg.contains("username")) message = "Username is already taken";
        else if (msg.contains("email")) message = "Email is already registered";
        else if (msg.contains("unique")) message = "A unique constraint was violated";
        return build(HttpStatus.CONFLICT, message, request.getRequestURI(), buildDetails(ex));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResDto> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        logger.warning("Data integrity violation: " + getRootCauseMessage(ex));
        String message = "A record with this value already exists";
        String msg = getRootCauseMessage(ex).toLowerCase();
        if (msg.contains("username")) message = "Username is already taken";
        else if (msg.contains("email")) message = "Email is already registered";
        else if (msg.contains("null")) message = "A required field is missing";
        return build(HttpStatus.CONFLICT, message, request.getRequestURI(), buildDetails(ex));
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResDto> handleSQL(SQLException ex, HttpServletRequest request) {
        logger.severe("SQL error: " + getRootCauseMessage(ex));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Database error occurred", request.getRequestURI(), buildDetails(ex));
    }

    @ExceptionHandler(HibernateException.class)
    public ResponseEntity<ErrorResDto> handleHibernate(HibernateException ex, HttpServletRequest request) {
        logger.severe("Hibernate error: " + getRootCauseMessage(ex));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Database operation failed", request.getRequestURI(), buildDetails(ex));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResDto> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        logger.severe("Data access error: " + getRootCauseMessage(ex));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Database error occurred", request.getRequestURI(), buildDetails(ex));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResDto> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Endpoint not found: " + request.getRequestURI(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResDto> handleGeneric(Exception ex, HttpServletRequest request) {
        logger.severe("Unhandled exception at " + request.getRequestURI() + ": " + getRootCauseMessage(ex));
        ex.printStackTrace();
        return build(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected server error",
            request.getRequestURI(),
            buildDetails(ex)
        );
    }

    private ResponseEntity<ErrorResDto> build(HttpStatus status, String message, String path, List<String> details) {
        ErrorResDto body = new ErrorResDto(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
