package com.shut.user.exception;

import com.shut.api.BadRequestException;
import com.shut.api.ConflictException;
import com.shut.api.NotFoundException;
import com.shut.auth.ForbiddenException;
import com.shut.auth.InvalidOtpException;
import com.shut.auth.InvalidPhoneException;
import com.shut.auth.TooManyOtpRequestsException;
import com.shut.auth.UnauthorizedException;
import com.shut.api.media.MediaNotFoundException;
import com.shut.api.media.MediaUnavailableException;
import com.shut.browse.ProviderNotFoundException;
import com.shut.pricing.PriceLimitReachedException;
import com.shut.profile.exception.ProfessionalProfileNotFoundException;
import com.shut.market.ListingNotFoundException;
import com.shut.market.MarketException;
import com.shut.venue.VenueBookingException;
import com.shut.venue.VenueNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    @ExceptionHandler(ProviderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleProviderNotFound(ProviderNotFoundException exception) {
        return Map.of("error", "PROVIDER_NOT_FOUND", "message", exception.getMessage());
    }

    @ExceptionHandler(VenueNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleVenueNotFound(VenueNotFoundException exception) {
        return Map.of("error", "VENUE_NOT_FOUND", "message", exception.getMessage());
    }

    @ExceptionHandler(ListingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleListingNotFound(ListingNotFoundException exception) {
        return Map.of("error", "LISTING_NOT_FOUND", "message", exception.getMessage());
    }

    /** An offer that cannot be made; {@code error} is MARKET_ plus the reason. */
    @ExceptionHandler(MarketException.class)
    public ResponseEntity<Map<String, String>> handleMarket(MarketException exception) {
        HttpStatus status = switch (exception.reason()) {
            case LISTING_NOT_AVAILABLE -> HttpStatus.NOT_FOUND;
            case INVALID_AMOUNT -> HttpStatus.BAD_REQUEST;
            case OWN_LISTING, ALREADY_OFFERED -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status)
                .body(Map.of("error", "MARKET_" + exception.reason().name(), "message", exception.getMessage()));
    }

    @ExceptionHandler(MediaNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMediaNotFound(MediaNotFoundException exception) {
        return Map.of("error", "MEDIA_NOT_FOUND", "message", exception.getMessage());
    }

    @ExceptionHandler(MediaUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleMediaUnavailable(MediaUnavailableException exception) {
        return Map.of("error", "MEDIA_UNAVAILABLE", "message", exception.getMessage());
    }

    @ExceptionHandler(InvalidPhoneException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidPhone(InvalidPhoneException exception) {
        return Map.of("error", "INVALID_PHONE", "message", exception.getMessage());
    }

    @ExceptionHandler(InvalidOtpException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidOtp(InvalidOtpException exception) {
        return Map.of("error", "INVALID_CODE", "message", exception.getMessage());
    }

    @ExceptionHandler(TooManyOtpRequestsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, String> handleTooManyOtpRequests(TooManyOtpRequestsException exception) {
        return Map.of("error", "TOO_MANY_REQUESTS", "message", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleUnauthorized(UnauthorizedException exception) {
        return Map.of("error", "UNAUTHORIZED", "message", exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleForbidden(ForbiddenException exception) {
        return Map.of("error", "FORBIDDEN", "message", exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(NotFoundException exception) {
        return Map.of("error", "NOT_FOUND", "message", exception.getMessage());
    }

    @ExceptionHandler(PriceLimitReachedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handlePriceLimit(PriceLimitReachedException exception) {
        return Map.of("error", "PRICE_LIMIT_REACHED", "message", exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(ConflictException exception) {
        return Map.of("error", exception.code(), "message", exception.getMessage());
    }

    /** A booking request that cannot be made; {@code error} is BOOKING_ plus the reason. */
    @ExceptionHandler(VenueBookingException.class)
    public ResponseEntity<Map<String, String>> handleBooking(VenueBookingException exception) {
        HttpStatus status = switch (exception.reason()) {
            case VENUE_NOT_AVAILABLE -> HttpStatus.NOT_FOUND;
            case INVALID_TIME -> HttpStatus.BAD_REQUEST;
            case OWN_VENUE, DAY_NOT_FREE, ALREADY_REQUESTED -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status)
                .body(Map.of("error", "BOOKING_" + exception.reason().name(), "message", exception.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(BadRequestException exception) {
        return Map.of("error", "BAD_REQUEST", "message", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadParameter(MethodArgumentTypeMismatchException exception) {
        return Map.of("error", "BAD_PARAMETER", "message", "Invalid value for " + exception.getName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage()).orElse("درخواست نامعتبر است");
        return Map.of("error", "VALIDATION_ERROR", "message", message);
    }
}
