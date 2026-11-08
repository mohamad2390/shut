package com.shut.user.exception;

import com.shut.profile.exception.ProfessionalProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(UserNotFoundException exception) {
        return Map.of("error", "USER_NOT_FOUND", "message", exception.getMessage());
    }

    @ExceptionHandler(ProfessionalProfileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleProfileNotFound(ProfessionalProfileNotFoundException exception) {
        return Map.of("error", "PROFESSIONAL_PROFILE_NOT_FOUND", "message", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage()).orElse("درخواست نامعتبر است");
        return Map.of("error", "VALIDATION_ERROR", "message", message);
    }
}
