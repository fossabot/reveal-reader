package com.jackcholt.reveal;

import java.io.IOException;

/**
 * Indicates that the YBK file format is invalid.
 */
public class InvalidFileFormatException extends IOException {
    private static final long serialVersionUID = -1891636805598049308L;

    /**
     * Constructor that allows a message to be passed.
     * 
     * @param message
     */
    public InvalidFileFormatException(final String message) {
        super(message);

    }
}
