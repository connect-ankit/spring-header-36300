package com.inn.accept.headers;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class CustomSpringExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomSpringExceptionHandler.class);
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void tooManyAcceptHeaderException(HttpServletRequest req, HttpServletResponse authorizeResponse, Exception e) throws Exception {
        // Capture errors from Spring framework like Accept header having too many elements.
        if (AnnotationUtils.findAnnotation
                (e.getClass(), ResponseStatus.class) != null)
            throw e;
        authorizeResponse.setStatus(SC_BAD_REQUEST);
        authorizeResponse.setContentType("application/json;charset=utf-8");
        String errorDescription = e.getMessage();
        if (req.getHeader("Accept") != null && req.getHeader("Accept").split(",").length > 50) {
            errorDescription = "Too many Accept-header mime-types";
        }
        LOGGER.warn("Captured Spring Exception: {}, error: {}", e.toString(), errorDescription);
        authorizeResponse.setHeader("authz_sentry_status", Integer.toString(SC_BAD_REQUEST));
        authorizeResponse.setHeader("authz_sentrylogerror", errorDescription);
        String errorMessage = "{\"error\":\"Bad Request\",\"error_description\":\"" + errorDescription + "\"}";
        authorizeResponse.getOutputStream().write(errorMessage.getBytes());
    }
}
