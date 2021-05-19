package com.epam.catgenome.exception;

public class BlastRequestException extends Exception {

    public BlastRequestException(String message, Throwable cause) {
        super(message, cause);
    }
    public BlastRequestException(String message) {
        super(message);
    }
}