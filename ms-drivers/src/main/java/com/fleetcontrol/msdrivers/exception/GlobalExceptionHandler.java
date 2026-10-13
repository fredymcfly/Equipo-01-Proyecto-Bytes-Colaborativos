
package com.fleetcontrol.msdrivers.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DriverAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleDriverAlreadyExists(
            DriverAlreadyExistsException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("code", "DRIVER_ALREADY_EXISTS"));
    }


    @ExceptionHandler(DriverValidationException.class)
    public ResponseEntity<Map<String, String>> handleDriverValidation(
            DriverValidationException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "VALIDATION_ERROR"));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "VALIDATION_ERROR"));
    }



    @ExceptionHandler(DriverNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDriverNotFound(
            DriverNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "DRIVER_NOT_FOUND"));
    }

}
