/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.IOException;

/**
 * Checked wrapper for Perst StorageError or other problems accessing YbkDAO.
 * 
 * @author Shon Vella
 * 
 */
public class StorageException extends IOException {

    /**
     * 
     */
    public StorageException() {
    }

    /**
     * @param detailMessage
     */
    public StorageException(String detailMessage) {
        super(detailMessage);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param cause
     */
    public StorageException(Throwable cause) {
        super();
        initCause(cause);
    }

    /**
     * @param cause
     * @param detailMessage
     */
    public StorageException(Throwable cause, String detailMessage) {
        super(detailMessage);
        initCause(cause);
    }

}
