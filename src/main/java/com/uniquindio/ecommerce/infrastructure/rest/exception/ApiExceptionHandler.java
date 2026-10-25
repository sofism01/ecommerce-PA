package com.uniquindio.ecommerce.infrastructure.rest.exception;

import com.uniquindio.ecommerce.application.dto.response.ErrorResponse;
import com.uniquindio.ecommerce.domain.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({org.springframework.dao.PessimisticLockingFailureException.class,
            org.springframework.dao.OptimisticLockingFailureException.class})
    public ResponseEntity<Object> concurrency(Exception ex, WebRequest request) {
        return response(HttpStatus.CONFLICT, "CONFLICTO_CONCURRENCIA", "El recurso cambió o está en uso. Consulta su estado e intenta nuevamente.", Map.of(), HttpHeaders.EMPTY, request);
    }

    @ExceptionHandler(com.uniquindio.ecommerce.application.security.CredencialesInvalidasException.class)
    public ResponseEntity<Object> unauthorized(Exception ex, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return response(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Credenciales o token inválidos.", Map.of(),
                headers, request);
    }

    @ExceptionHandler(com.uniquindio.ecommerce.application.security.AccesoDenegadoException.class)
    public ResponseEntity<Object> forbidden(Exception ex, WebRequest request) {
        return response(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "No tienes permiso para acceder a este recurso.", Map.of(), HttpHeaders.EMPTY, request);
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<Object> duplicate(CorreoDuplicadoException ex, WebRequest request) {
        return response(HttpStatus.CONFLICT, "CORREO_DUPLICADO", ex.getMessage(), Map.of(), HttpHeaders.EMPTY, request);
    }

    @ExceptionHandler(com.uniquindio.ecommerce.domain.query.ConsultaInvalidaException.class)
    public ResponseEntity<Object> invalidQuery(com.uniquindio.ecommerce.domain.query.ConsultaInvalidaException ex, WebRequest request) {
        return response(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", ex.getMessage(), Map.of(), HttpHeaders.EMPTY, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> details = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> details.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error -> details.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "La solicitud contiene datos inválidos.",
                details, headers, request);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> notFound(RecursoNoEncontradoException ex, WebRequest request) {
        return response(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", ex.getMessage(), Map.of(), HttpHeaders.EMPTY, request);
    }

    @ExceptionHandler(ReglaDominioException.class)
    public ResponseEntity<Object> businessRule(ReglaDominioException ex, WebRequest request) {
        String code = switch (ex) {
            case StockInsuficienteException stock -> "STOCK_INSUFICIENTE";
            case ModificacionBolsaNoPermitidaException bolsa -> "BOLSA_CERRADA";
            default -> "REGLA_DOMINIO";
        };
        return response(HttpStatus.CONFLICT, code, ex.getMessage(), Map.of(), HttpHeaders.EMPTY, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String code = switch (status.value()) {
            case 400 -> "DATOS_INVALIDOS";
            case 404 -> "RECURSO_NO_ENCONTRADO";
            case 405 -> "METODO_NO_PERMITIDO";
            case 406 -> "RESPUESTA_NO_ACEPTABLE";
            case 415 -> "TIPO_CONTENIDO_NO_ADMITIDO";
            default -> status.is5xxServerError() ? "ERROR_INTERNO" : "SOLICITUD_RECHAZADA";
        };
        String message = switch (status.value()) {
            case 400 -> "El JSON o los parámetros de la solicitud son inválidos. Revisa los UUID, tallas y tipos de datos.";
            case 404 -> "La ruta solicitada no existe.";
            case 405 -> "El método HTTP no está permitido para esta ruta.";
            case 406 -> "El formato de respuesta solicitado no está disponible.";
            case 415 -> "El cuerpo de la solicitud debe utilizar un tipo de contenido admitido, como application/json.";
            default -> status.is5xxServerError() ? "Ocurrió un error interno. Intenta nuevamente." : "La solicitud no pudo procesarse.";
        };
        if (status.is5xxServerError()) {
            log.error("Error al procesar la solicitud", ex);
        }
        return response(status, code, message, Map.of(), headers, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception ex, WebRequest request) {
        log.error("Error inesperado al procesar la solicitud", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error interno. Intenta nuevamente.",
                Map.of(), HttpHeaders.EMPTY, request);
    }

    private ResponseEntity<Object> response(HttpStatusCode status, String code, String message,
            Map<String, String> details, HttpHeaders headers, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        var error = new ErrorResponse(Instant.now(), status.value(), code, message, path, details);
        return ResponseEntity.status(status).headers(headers).contentType(MediaType.APPLICATION_JSON).body(error);
    }
}
