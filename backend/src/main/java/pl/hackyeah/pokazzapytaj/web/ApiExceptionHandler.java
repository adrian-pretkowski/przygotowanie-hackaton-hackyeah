package pl.hackyeah.pokazzapytaj.web;

import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.hackyeah.pokazzapytaj.vision.ModelResponseException;

/** Maps errors to ProblemDetail (JSON) so the frontend can show a readable message. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ModelResponseException.class)
    ProblemDetail unusableModelResponse(ModelResponseException e) {
        log.warn("Nieużywalna odpowiedź modelu: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(RateLimitException.class)
    ProblemDetail rateLimited(RateLimitException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Limit zapytań do API - spróbuj za chwilę");
    }

    @ExceptionHandler(AnthropicServiceException.class)
    ProblemDetail anthropicError(AnthropicServiceException e) {
        log.error("Błąd API Anthropic: status={} {}", e.statusCode(), e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Błąd API modelu (" + e.statusCode() + "): " + e.getMessage());
    }
}
