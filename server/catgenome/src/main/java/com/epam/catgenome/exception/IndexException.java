package com.epam.catgenome.exception;

/**
 * Created by Mariia_Zueva on 1/8/2017.
 */
public class IndexException extends RuntimeException {

    public IndexException(String message) {
        super(message);
    }

    public IndexException(String message, Throwable cause) {
        super(message, cause);
    }
}
