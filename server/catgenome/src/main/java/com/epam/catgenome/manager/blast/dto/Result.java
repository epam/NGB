package com.epam.catgenome.manager.blast.dto;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Result<T> {
    private T payload;
    private String message;
    private String status;

    public Result(final T payload, final String message, final String status) {
        this.payload = payload;
        this.message = message;
        this.status = status;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(final T payload) {
        this.payload = payload;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }
}
