package com.cvanalyzer.backend.exception;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
 private static final Logger log=LoggerFactory.getLogger(GlobalExceptionHandler.class);

 @ExceptionHandler(ResourceNotFoundException.class)
 ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException ex,HttpServletRequest req){
  return response(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND",ex.getMessage(),List.of(),req);
 }
 @ExceptionHandler(NoResourceFoundException.class)
 ResponseEntity<ApiErrorResponse> noResource(NoResourceFoundException ex,HttpServletRequest req){
  return response(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Resource not found",List.of(),req);
 }
 @ExceptionHandler({ConstraintViolationException.class,MethodArgumentTypeMismatchException.class})
 ResponseEntity<ApiErrorResponse> badRequest(Exception ex,HttpServletRequest req){
  return response(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Request validation failed",List.of(),req);
 }
 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<ApiErrorResponse> invalidBody(MethodArgumentNotValidException ex,HttpServletRequest req){
  var fields=ex.getBindingResult().getFieldErrors().stream()
    .map(e->new ApiErrorResponse.FieldError(e.getField(),e.getDefaultMessage())).toList();
  return response(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Request validation failed",fields,req);
 }
 @ExceptionHandler(CvParsingException.class)
 ResponseEntity<ApiErrorResponse> cvParsing(CvParsingException ex,HttpServletRequest req){
  return response(ex.status(),ex.code(),ex.getMessage(),List.of(),req);
 }
 @ExceptionHandler(RecommendationException.class)
 ResponseEntity<ApiErrorResponse> recommendation(RecommendationException ex,HttpServletRequest req){
  String trace=traceId();log.warn("recommendation_failure traceId={} code={}",trace,ex.code());
  return response(ex.status(),ex.code(),ex.getMessage(),List.of(),req,trace);
 }
 @ExceptionHandler(MaxUploadSizeExceededException.class)
 ResponseEntity<ApiErrorResponse> uploadTooLarge(MaxUploadSizeExceededException ex,HttpServletRequest req){
  return response(HttpStatus.PAYLOAD_TOO_LARGE,"CV_FILE_TOO_LARGE",
   "CV file exceeds the 5 MB limit",List.of(),req);
 }
 @ExceptionHandler(MissingServletRequestPartException.class)
 ResponseEntity<ApiErrorResponse> missingPart(MissingServletRequestPartException ex,HttpServletRequest req){
  return response(HttpStatus.BAD_REQUEST,"CV_FILE_REQUIRED","Multipart field 'file' is required",
   List.of(),req);
 }
 @ExceptionHandler(ExternalServiceException.class)
 ResponseEntity<ApiErrorResponse> upstream(ExternalServiceException ex,HttpServletRequest req){
  String trace=traceId();
  log.error("upstream_service_failure traceId={} type={}",trace,ex.getClass().getSimpleName());
  return response(HttpStatus.BAD_GATEWAY,"UPSTREAM_UNAVAILABLE","Job provider unavailable",List.of(),req,trace);
 }
 @ExceptionHandler(IngestionDisabledException.class)
 ResponseEntity<ApiErrorResponse> ingestionDisabled(IngestionDisabledException ex,HttpServletRequest req){
  return response(HttpStatus.FORBIDDEN,"INGESTION_DISABLED",ex.getMessage(),List.of(),req);
 }
 @ExceptionHandler(ProviderConfigurationException.class)
 ResponseEntity<ApiErrorResponse> providerConfiguration(ProviderConfigurationException ex,
   HttpServletRequest req){
  return response(HttpStatus.SERVICE_UNAVAILABLE,"PROVIDER_NOT_CONFIGURED",ex.getMessage(),List.of(),req);
 }
 @ExceptionHandler(DataAccessException.class)
 ResponseEntity<ApiErrorResponse> database(DataAccessException ex,HttpServletRequest req){
  String trace=traceId();
  log.error("database_failure traceId={} type={}",trace,ex.getClass().getSimpleName());
  return response(HttpStatus.INTERNAL_SERVER_ERROR,"DATABASE_UNAVAILABLE",
    "Database operation failed",List.of(),req,trace);
 }
 @ExceptionHandler(Exception.class)
 ResponseEntity<ApiErrorResponse> unexpected(Exception ex,HttpServletRequest req){
  String trace=traceId();
  log.error("unexpected_failure traceId={}",trace,ex);
  return response(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR",
    "An unexpected error occurred",List.of(),req,trace);
 }
 private ResponseEntity<ApiErrorResponse> response(HttpStatus status,String code,String message,
   List<ApiErrorResponse.FieldError> fields,HttpServletRequest req){
  return response(status,code,message,fields,req,traceId());
 }
 private ResponseEntity<ApiErrorResponse> response(HttpStatus status,String code,String message,
   List<ApiErrorResponse.FieldError> fields,HttpServletRequest req,String trace){
  var body=new ApiErrorResponse(Instant.now(),status.value(),code,message,req.getRequestURI(),fields,trace);
  return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
 }
 private String traceId(){return UUID.randomUUID().toString().substring(0,8);}
}
