package ru.mephi.booking.exception;

public class ConflictException extends ApiException {
    public ConflictException(String message) { super("CONFLICT", message); }
}
