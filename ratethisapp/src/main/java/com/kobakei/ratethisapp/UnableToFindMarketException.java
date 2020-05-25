package com.kobakei.ratethisapp;

/**
 * Created by Sönke Gissel on 25.05.2020.
 */
public class UnableToFindMarketException extends RuntimeException {
    public UnableToFindMarketException(String errorMessage) {
        super(errorMessage);
    }
}
