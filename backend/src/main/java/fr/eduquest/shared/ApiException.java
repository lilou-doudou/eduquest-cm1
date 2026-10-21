package fr.eduquest.shared;

import org.springframework.http.HttpStatus;

/** Erreur métier traduite en application/problem+json par {@link ProblemHandler}. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Long retryAfterSeconds;

    public ApiException(HttpStatus status, String code, String detail) {
        this(status, code, detail, null);
    }

    public ApiException(HttpStatus status, String code, String detail, Long retryAfterSeconds) {
        super(detail);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public Long retryAfterSeconds() { return retryAfterSeconds; }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ressource introuvable.");
    }
    public static ApiException conflict(String code, String detail) {
        return new ApiException(HttpStatus.CONFLICT, code, detail);
    }
    public static ApiException badRequest(String code, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, detail);
    }
    public static ApiException unprocessable(String code, String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, code, detail);
    }
    public static ApiException forbidden(String code, String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, code, detail);
    }
    public static ApiException unauthorized(String code, String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, detail);
    }
}
