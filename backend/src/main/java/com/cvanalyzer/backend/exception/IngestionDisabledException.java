package com.cvanalyzer.backend.exception;

public class IngestionDisabledException extends RuntimeException {
    public IngestionDisabledException(String message){super(message);}
}
