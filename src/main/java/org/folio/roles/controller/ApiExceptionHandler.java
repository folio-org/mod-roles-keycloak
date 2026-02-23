package org.folio.roles.controller;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.WARN;
import static org.folio.common.domain.model.error.ErrorCode.FOUND_ERROR;
import static org.folio.common.domain.model.error.ErrorCode.NOT_FOUND_ERROR;
import static org.folio.common.domain.model.error.ErrorCode.SERVICE_ERROR;
import static org.folio.common.domain.model.error.ErrorCode.UNKNOWN_ERROR;
import static org.folio.common.domain.model.error.ErrorCode.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.folio.common.domain.model.error.Error;
import org.folio.common.domain.model.error.ErrorCode;
import org.folio.common.domain.model.error.ErrorResponse;
import org.folio.common.domain.model.error.Parameter;
import org.folio.cql2pgjson.exception.CQLFeatureUnsupportedException;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.spring.cql.CqlQueryValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Log4j2
@RestControllerAdvice
public class ApiExceptionHandler {

  /**
   * Catches and handles all exceptions for type {@link UnsupportedOperationException}.
   *
   * @param exception {@link UnsupportedOperationException} to process
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedOperationException(UnsupportedOperationException exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, NOT_IMPLEMENTED, SERVICE_ERROR);
  }

  /**
   * Catches and handles all exceptions for type {@link org.springframework.web.bind.MethodArgumentNotValidException}.
   *
   * @param e {@link org.springframework.web.bind.MethodArgumentNotValidException} to process
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    logException(DEBUG, e);
    var validationErrors = Optional.of(e.getBindingResult()).map(Errors::getAllErrors).orElse(emptyList());
    var errorResponse = new ErrorResponse();
    validationErrors.forEach(error ->
      errorResponse.addErrorsItem(new Error()
        .message(error.getDefaultMessage())
        .code(ErrorCode.VALIDATION_ERROR)
        .type(MethodArgumentNotValidException.class.getSimpleName())
        .addParametersItem(new Parameter()
          .key(((FieldError) error).getField())
          .value(String.valueOf(((FieldError) error).getRejectedValue())))));
    errorResponse.totalRecords(errorResponse.getErrors().size());

    return buildResponseEntity(errorResponse, BAD_REQUEST);
  }

  /**
   * Catches and handles all exceptions for type {@link javax.validation.ConstraintViolationException}.
   *
   * @param exception {@link javax.validation.ConstraintViolationException} to process
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
    logException(DEBUG, exception);
    var errorResponse = new ErrorResponse();
    exception.getConstraintViolations().forEach(constraintViolation ->
      errorResponse.addErrorsItem(new Error()
        .message(String.format("%s %s", constraintViolation.getPropertyPath(), constraintViolation.getMessage()))
        .code(VALIDATION_ERROR)
        .type(ConstraintViolationException.class.getSimpleName())));
    errorResponse.totalRecords(errorResponse.getErrors().size());

    return buildResponseEntity(errorResponse, BAD_REQUEST);
  }

  /**
   * Catches and handles all exceptions for type {@link RequestValidationException}.
   *
   * @param exception {@link RequestValidationException} to process
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(RequestValidationException.class)
  public ResponseEntity<ErrorResponse> handleRequestValidationException(RequestValidationException exception) {
    logException(DEBUG, exception);
    var errorResponse = buildValidationError(exception, exception.getKey(), exception.getValue());
    return buildResponseEntity(errorResponse, BAD_REQUEST);
  }

  /**
   * Catches and handles all exceptions for type {@link RequestValidationException}.
   *
   * @param exception {@link RequestValidationException} to process
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<ErrorResponse> handleServiceException(ServiceException exception) {
    logException(DEBUG, exception);
    var parameters = singletonList(new Parameter().key(exception.getKey()).value(exception.getValue()));
    var errorResponse = buildErrorResponse(exception, parameters, SERVICE_ERROR);
    return buildResponseEntity(errorResponse, BAD_REQUEST);
  }

  /**
   * Catches and handles common request validation exceptions.
   *
   * @param exception {@link Exception} object to process
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler({
    IllegalArgumentException.class,
    CqlQueryValidationException.class,
    MissingRequestHeaderException.class,
    CQLFeatureUnsupportedException.class,
    InvalidDataAccessApiUsageException.class,
    HttpMediaTypeNotSupportedException.class,
    MethodArgumentTypeMismatchException.class,
  })
  public ResponseEntity<ErrorResponse> handleValidationExceptions(Exception exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, BAD_REQUEST, VALIDATION_ERROR);
  }

  /**
   * Catches and handles all exceptions for type {@link DataIntegrityViolationException}.
   *
   * @param exception {@link DataIntegrityViolationException} to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception.getMostSpecificCause(), BAD_REQUEST, SERVICE_ERROR);
  }

  /**
   * Catches and handles all exceptions for type {@link EntityExistsException}.
   *
   * @param exception {@link EntityExistsException} object
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body.
   */
  @ExceptionHandler(EntityExistsException.class)
  public ResponseEntity<ErrorResponse> handleEntityExistsException(EntityExistsException exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, BAD_REQUEST, FOUND_ERROR);
  }

  /**
   * Catches and handles common request service exceptions.
   *
   * @param exception {@link Exception} object to process
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler({IllegalStateException.class})
  public ResponseEntity<ErrorResponse> handleServiceLevelExceptions(Exception exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, BAD_REQUEST, SERVICE_ERROR);
  }

  /**
   * Catches and handles all JPA not found exceptions for types {@link JpaObjectRetrievalFailureException},
   * {@link EmptyResultDataAccessException}, {@link EntityNotFoundException}.
   *
   * @param exception {@link Exception} object
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body.
   */
  @ExceptionHandler({
    ObjectRetrievalFailureException.class, EmptyResultDataAccessException.class, EntityNotFoundException.class
  })
  public ResponseEntity<ErrorResponse> handleJpaNotFoundExceptions(Exception exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, NOT_FOUND, NOT_FOUND_ERROR);
  }

  /**
   * Catches and handles all exceptions for type {@link HttpMessageNotReadableException}.
   *
   * @param exception {@link org.springframework.http.converter.HttpMessageNotReadableException} object
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handlerHttpMessageNotReadableException(
    HttpMessageNotReadableException exception) {

    return Optional.ofNullable(exception.getCause())
      .map(Throwable::getCause)
      .filter(IllegalArgumentException.class::isInstance)
      .map(IllegalArgumentException.class::cast)
      .map(this::handleValidationExceptions)
      .orElseGet(() -> {
        logException(DEBUG, exception);
        return buildResponseEntity(exception, BAD_REQUEST, VALIDATION_ERROR);
      });
  }

  /**
   * Catches and handles all exceptions for type {@link MissingServletRequestParameterException}.
   *
   * @param exception {@link org.springframework.web.bind.MissingServletRequestParameterException} to process
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
    MissingServletRequestParameterException exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, BAD_REQUEST, VALIDATION_ERROR);
  }

  /**
   * Catches and handles all exceptions for type {@link KeycloakApiException}.
   *
   * @param exception {@link KeycloakApiException} to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(KeycloakApiException.class)
  public ResponseEntity<ErrorResponse> handleKeycloakApiException(KeycloakApiException exception) {
    logException(DEBUG, exception);
    var errorParameters = singletonList(new Parameter().key("cause").value(exception.getCause().getMessage()));
    var errorResponse = buildErrorResponse(exception, errorParameters, SERVICE_ERROR);
    return buildResponseEntity(errorResponse, exception.getStatus());
  }

  /**
   * Handles all uncaught exceptions.
   *
   * @param exception {@link Exception} object
   * @return {@link org.springframework.http.ResponseEntity} with {@link ErrorResponse} body.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllOtherExceptions(Exception exception) {
    logException(WARN, exception);
    return buildResponseEntity(exception, INTERNAL_SERVER_ERROR, UNKNOWN_ERROR);
  }

  private static ErrorResponse buildValidationError(Exception exception, String key, String value) {
    var error = new Error()
      .type(exception.getClass().getSimpleName())
      .code(VALIDATION_ERROR)
      .message(exception.getMessage())
      .parameters(List.of(new Parameter().key(key).value(value)));
    return new ErrorResponse().errors(List.of(error)).totalRecords(1);
  }

  private static ResponseEntity<ErrorResponse> buildResponseEntity(
    Throwable exception, HttpStatus status, ErrorCode code) {

    var errorResponse = new ErrorResponse()
      .errors(List.of(new Error()
        .message(exception.getMessage())
        .type(exception.getClass().getSimpleName())
        .code(code)))
      .totalRecords(1);

    return buildResponseEntity(errorResponse, status);
  }

  private static ResponseEntity<ErrorResponse> buildResponseEntity(ErrorResponse errorResponse, HttpStatus status) {
    return ResponseEntity.status(status).body(errorResponse);
  }

  private static ErrorResponse buildErrorResponse(Exception exception, List<Parameter> parameters, ErrorCode code) {
    var error = new Error()
      .type(exception.getClass().getSimpleName())
      .code(code)
      .message(exception.getMessage())
      .parameters(isNotEmpty(parameters) ? parameters : null);
    return new ErrorResponse().errors(List.of(error)).totalRecords(INTEGER_ONE);
  }

  private static void logException(Level level, Exception exception) {
    log.log(level, "Handling exception", exception);
  }
}
