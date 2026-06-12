package com.lsototalbouw.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UncheckedIOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Global exception handler providing central management of exceptions thrown by web controllers.
 *
 * <p>Uses Spring MVC's {@link ControllerAdvice} to catch specific exceptions (such as validation or business constraint
 * violations) and dynamically renders custom error pages instead of exposing raw stack traces.
 */
@ControllerAdvice
public class BusinessExceptionHandler {

    /**
     * Intercepts {@link IllegalArgumentException} instances thrown across the application.
     *
     * <p>Inspects the exception message text to dynamically resolve the status code to either
     * {@link HttpStatus#NOT_FOUND} (if the resource is missing) or {@link HttpStatus#BAD_REQUEST}
     * (if validation or business rules failed), mapping them to {@code error/business-error}.
     *
     * @param ex      the intercepted exception
     * @param request the current HTTP servlet request
     * @return a {@link ModelAndView} loaded with the localized error message and a back-link URL
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleBusinessException(IllegalArgumentException ex, HttpServletRequest request) {
        HttpStatus status = isNotFound(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        ModelAndView modelAndView = new ModelAndView("error/business-error", status);
        modelAndView.addObject("pageTitle", status == HttpStatus.NOT_FOUND ? "No encontrado" : "Solicitud no valida");
        modelAndView.addObject("errorTitle", status == HttpStatus.NOT_FOUND
                ? "No hemos encontrado ese registro"
                : "No se ha podido completar la accion");
        modelAndView.addObject("errorMessage", ex.getMessage());
        modelAndView.addObject("backUrl", backUrl(request));
        return modelAndView;
    }

    /**
     * Intercepts {@link UncheckedIOException} instances, usually representing file upload or writing failures.
     *
     * <p>Renders a localized bad request error page asking the user to double check the uploaded files.
     *
     * @param ex      the intercepted file input/output exception
     * @param request the current HTTP servlet request
     * @return a {@link ModelAndView} configured with file-specific error guidance
     */
    @ExceptionHandler(UncheckedIOException.class)
    public ModelAndView handleFileException(UncheckedIOException ex, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView("error/business-error", HttpStatus.BAD_REQUEST);
        modelAndView.addObject("pageTitle", "Documento no guardado");
        modelAndView.addObject("errorTitle", "No se ha podido guardar el archivo");
        modelAndView.addObject("errorMessage", "Revisa el archivo e intentalo de nuevo.");
        modelAndView.addObject("backUrl", backUrl(request));
        return modelAndView;
    }

    /**
     * Simple text heuristic to determine if an exception message refers to a resource not found.
     *
     * @param message the exception message to test
     * @return {@code true} if the message implies a missing resource, otherwise {@code false}
     */
    private boolean isNotFound(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("no encontrado")
                || normalized.contains("no encontrada")
                || normalized.contains("not found");
    }

    /**
     * Identifies a safe redirect URL from the request headers to allow returning to the previous screen.
     *
     * <p>Validates that the host header in the {@code Referer} HTTP request header matches the actual server name
     * to protect against open-redirect security vulnerabilities. Falls back to {@code /dashboard} if unsafe.
     *
     * @param request the current request
     * @return the verified referer path and query string, or {@code "/dashboard"} as a fallback
     */
    private String backUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/dashboard";
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getHost() != null && uri.getHost().equalsIgnoreCase(request.getServerName())) {
                String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();
                return uri.getPath() + query;
            }
        } catch (IllegalArgumentException ignored) {
            return "/dashboard";
        }
        return "/dashboard";
    }
}
