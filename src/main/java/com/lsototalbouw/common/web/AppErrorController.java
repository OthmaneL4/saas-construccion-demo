package com.lsototalbouw.common.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Global controller for handling container-level HTTP errors in the application.
 *
 * <p>Implements Spring Boot's {@link ErrorController} to catch unhandled status codes (e.g., 404, 500)
 * and maps them to a consistent user-friendly Thymeleaf error page.
 */
@Controller
public class AppErrorController implements ErrorController {

    /**
     * Entry point for resolving general errors dispatched by the servlet container.
     *
     * @param request the HTTP servlet request containing the error details
     * @return a {@link ModelAndView} configured for the appropriate error view and status code
     */
    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        return errorView(status, titleFor(status), messageFor(status));
    }

    /**
     * Direct GET mapping to present a 403 Forbidden error page.
     *
     * @return a {@link ModelAndView} with a forbidden status and user permissions warning
     */
    @GetMapping("/error/403")
    public ModelAndView forbidden() {
        return errorView(HttpStatus.FORBIDDEN,
                "No tienes permisos para acceder",
                "Tu usuario no tiene acceso a esta zona. Si necesitas entrar, pide permisos al propietario de la cuenta.");
    }

    /**
     * Builds the standard model and view for displaying business-friendly errors.
     *
     * @param status  the {@link HttpStatus} corresponding to the error code
     * @param title   the user-facing short title of the error
     * @param message the user-facing descriptive reason or mitigation instructions
     * @return a configured {@link ModelAndView} mapping to the ThymeLeaf layout {@code error/business-error}
     */
    private ModelAndView errorView(HttpStatus status, String title, String message) {
        ModelAndView modelAndView = new ModelAndView("error/business-error", status);
        modelAndView.addObject("pageTitle", title);
        modelAndView.addObject("errorTitle", title);
        modelAndView.addObject("errorMessage", message);
        modelAndView.addObject("backUrl", "/dashboard");
        return modelAndView;
    }

    /**
     * Resolves the {@link HttpStatus} status code from the servlet request attributes.
     *
     * @param request the current servlet request
     * @return the resolved {@link HttpStatus}, or {@link HttpStatus#INTERNAL_SERVER_ERROR} if not found or invalid
     */
    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode instanceof Integer code) {
            HttpStatus status = HttpStatus.resolve(code);
            if (status != null) {
                return status;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Returns a localized user-facing title based on the resolved HTTP status code.
     *
     * @param status the HTTP status to translate
     * @return a descriptive title string in Spanish
     */
    private String titleFor(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            return "Pagina no encontrada";
        }
        if (status == HttpStatus.FORBIDDEN) {
            return "No tienes permisos para acceder";
        }
        if (status.is4xxClientError()) {
            return "Solicitud no valida";
        }
        return "Ha ocurrido un error";
    }

    /**
     * Returns a localized user-facing description based on the resolved HTTP status code.
     *
     * @param status the HTTP status to translate
     * @return a descriptive instruction message in Spanish
     */
    private String messageFor(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            return "La pagina o el registro solicitado no existe o ya no esta disponible.";
        }
        if (status == HttpStatus.FORBIDDEN) {
            return "Tu usuario no tiene acceso a esta zona.";
        }
        if (status.is4xxClientError()) {
            return "Revisa la direccion o vuelve al panel principal para continuar.";
        }
        return "No se ha podido completar la operacion. Vuelve al panel principal e intentalo de nuevo.";
    }
}
