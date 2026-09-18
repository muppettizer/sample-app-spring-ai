package com.sample.app.ai.error;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception ex, HttpServletRequest request) {
        // Log full exception with stacktrace for diagnostics
        log.error("Unhandled exception while processing request {} {}", request.getMethod(), request.getRequestURI(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Internal Server Error");
        // Include exception class in the detail to aid clients/monitoring while keeping the stacktrace in logs
        String detail = (ex.getMessage() != null ? ex.getMessage() : "Unexpected error");
        problemDetail.setDetail(ex.getClass().getName() + ": " + detail);
        // Set the request path as the instance so clients can correlate
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.internalServerError().body(problemDetail);
    }
}
