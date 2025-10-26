package ru.mephi.hotel.exception;

public class NotFoundException extends ApiException {
    public NotFoundException(String message) { super("NOT_FOUND", message); }
}
