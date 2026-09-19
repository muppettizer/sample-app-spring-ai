package com.sample.app.ai.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ScreeningChatException.class)
    public ProblemDetail handleScreeningChatException(
            ScreeningChatException exception,
            HttpServletRequest request) {

        log.error("Screening chat failed: {}", request.getRequestURI(), exception);

        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The AI screening service is currently unavailable."
        );

        problemDetail.setTitle("AI Screening Error");
        problemDetail.setProperty("path", request.getRequestURI());

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex, HttpServletRequest request) {
        log.error(
                "Unhandled exception while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred."
        );

        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return problemDetail;
    }
}
