package com.dws.challenge.exception;

public class ServerBusyException extends RuntimeException {

    public ServerBusyException(String message) {
        super(message);
    }

    public ServerBusyException(String message,Throwable cause) {
        super(message,cause);
    }
}
