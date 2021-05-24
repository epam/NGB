package com.epam.catgenome.exception;

public class BlastRequestException extends Exception {

    public BlastRequestException(final String message, final Throwable cause) {
        super(message, cause);
    }
    public BlastRequestException(final String message) {
        super(message);
    }
}
