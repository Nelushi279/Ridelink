package com.ridelink.drivervehicle.exception;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestControllerAdvice
public class GlobalExceptionHandler {
 private static final Logger LOG=LoggerFactory.getLogger(GlobalExceptionHandler.class);
 private ResponseEntity<ApiError> error(int status,String message,Map<String,String> fields) {
  return ResponseEntity.status(status).body(new ApiError(Instant.now(),status,message,fields));
 }
 @ExceptionHandler(DriverNotFoundException.class)
 public ResponseEntity<ApiError> notFound(DriverNotFoundException e) { return error(404,e.getMessage(),Map.of()); }
 @ExceptionHandler(DuplicateDriverException.class)
 public ResponseEntity<ApiError> duplicate(DuplicateDriverException e) { return error(409,e.getMessage(),Map.of()); }
 @ExceptionHandler(InvalidDriverProfileException.class)
 public ResponseEntity<ApiError> invalid(InvalidDriverProfileException e) { return error(400,e.getMessage(),Map.of()); }
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
  Map<String,String> fields=new TreeMap<>();
  e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(),f.getDefaultMessage()));
  return error(400,"Invalid request data",fields);
 }
 @ExceptionHandler(HttpMessageNotReadableException.class)
 public ResponseEntity<ApiError> malformed(HttpMessageNotReadableException e) { return error(400,"Malformed JSON or unsupported request field",Map.of()); }
 @ExceptionHandler(Exception.class)
 public ResponseEntity<ApiError> unexpected(Exception e) {
  LOG.error("Unexpected driver service error",e); return error(500,"Internal server error",Map.of());
 }
}