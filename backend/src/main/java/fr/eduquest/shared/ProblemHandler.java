package fr.eduquest.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/** Erreurs au format RFC 9457, sans trace technique publique. */
@RestControllerAdvice
public class ProblemHandler {
    private static final Logger log = LoggerFactory.getLogger(ProblemHandler.class);

    public static ProblemDetail problem(HttpStatus status, String code, String detail, String instance) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("https://eduquest.local/problems/" + code.toLowerCase().replace('_', '-')));
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        if (instance != null) pd.setInstance(URI.create(instance));
        pd.setProperty("traceId", MDC.get(TraceIdFilter.MDC_KEY));
        return pd;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> api(ApiException ex, HttpServletRequest req) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(ex.status());
        if (ex.retryAfterSeconds() != null) builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()));
        return builder.body(problem(ex.status(), ex.code(), ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Certains champs sont invalides.", req.getRequestURI());
        List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage() == null ? "invalide" : fe.getDefaultMessage()))
                .toList();
        pd.setProperty("fieldErrors", fields);
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> unreadable(Exception ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Requête mal formée.", req.getRequestURI()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> mediaType(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Format non pris en charge.", req.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> method(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(problem(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Méthode non autorisée.", req.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> noResource(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ressource introuvable.", req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest req) {
        // Pas de corps de requête ni de message utilisateur dans les journaux.
        log.error("Erreur interne sur {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
                .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Une erreur interne est survenue.", req.getRequestURI()));
    }
}
