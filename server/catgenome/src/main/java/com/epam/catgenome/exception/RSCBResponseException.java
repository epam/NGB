package com.epam.catgenome.exception;

public class RSCBResponseException extends RuntimeException{

    public RSCBResponseException(String message) {
        super(message);
    }

    public RSCBResponseException(Throwable cause) {
        super(cause);
    }
}
