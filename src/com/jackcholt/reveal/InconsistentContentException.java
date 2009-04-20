package com.jackcholt.reveal;

import java.io.IOException;

/**
 * Indicates some data inconstancy (used to replace most usages of
 * IllegalStateException with a checked exception).
 */
public class InconsistentContentException extends IOException {
    // updated to reflect new derivation which will likely change the
    // serialization format
    private static final long serialVersionUID = -1891636805598049308L;

    /**
     * Constructor that allows a message to be passed.
     * 
     * @param message
     */
    public InconsistentContentException(final String message) {
        super(message);

    }
}
