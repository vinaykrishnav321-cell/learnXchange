package com.learnxchange.util;

/** Wraps SQL failures so upper layers don't depend on java.sql. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
