package com.tejas.distributedmatchingengine.controllers.advice;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.tejas.distributedmatchingengine.exceptions.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ExceptionControllerAdvice {
    /*
    * Spring parses the JSON encoded Order objects using the Jackson library.
    * If the incoming data is in the wrong format (for example, "buy" instead
    * of "BUY" for the type enum), then this error is thrown by Spring. This
    * exception handler informs the user to retry with the correct format.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParsingError(
            HttpMessageNotReadableException e) {
        // Jackson's InvalidFormatException is wrapped in
        // HttpMessageNotReadableException by Spring
        if(e.getCause() instanceof InvalidFormatException invalidFormatE) {
            List<JacksonException.Reference> path = invalidFormatE.getPath();

            // Get the field's name which was given the incorrectly formated
            // input
            String fieldName = (path != null && !path.isEmpty()) ?
                    path.getLast().getPropertyName() : "unknown";

            // Type of field that was to be deserialized
            Class<?> targetType = invalidFormatE.getTargetType();

            // List the allowed values if the field was of an Enum type
            if(targetType != null && targetType.isEnum()) {
                Object[] enumConstants = targetType.getEnumConstants();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        Map.of("error", "Invalid enum value",
                                "field", fieldName,
                                "rejectedValue", invalidFormatE.getValue(),
                                "allowedValues", Arrays.toString(enumConstants)
                        )
                );
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Invalid format",
                    "field", fieldName,
                    "rejectedValue", invalidFormatE.getValue()
            ));
        }
        // For the cases where format is valid but the user input isn't
        // readable for some other reason
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Malformed JSON request body"
        ));
    }

    // Return 404 when a user with the specified userID is not found
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleNonExistentUserError(
            UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()
                        + " not found.");
    }
}
