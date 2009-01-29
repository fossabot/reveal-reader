package com.jackcholt.reveal;

public class InvalidFileFormatException extends RuntimeException {

    /**
     * Indicates that the YBK file format is invalid.
     */
    private static final long serialVersionUID = -9172040214660614293L;

    /**
     * Constructor that allows a message to be passed.
     * @param message
     */
    public InvalidFileFormatException(final String message) {
        super(message);
        
    }
}
