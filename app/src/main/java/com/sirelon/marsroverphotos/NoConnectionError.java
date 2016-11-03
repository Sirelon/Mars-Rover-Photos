package com.sirelon.marsroverphotos;

/**
 * @author romanishin
 * @since 03.11.16 on 15:11
 */
public class NoConnectionError extends RuntimeException {

    public NoConnectionError() {
        super("No connection here");
    }
}
