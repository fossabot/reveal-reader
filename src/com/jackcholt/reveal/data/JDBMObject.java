/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.IOException;
import java.io.Serializable;

import jdbm.RecordManager;

/**
 * Base class for the objects stored in JDBM
 * 
 * @author Shon Vella
 * 
 */
public abstract class JDBMObject implements Serializable, Cloneable {
    private static final long serialVersionUID = 6352923845369621224L;
    // id to retrieve object from JDBM
    protected long recID = 0;

    /**
     * Load a jdbm object from the DB
     * 
     * @param db
     * @param recID
     * @return null if object not found or is not a JDBMObject
     * @throws IOException
     */
    protected static JDBMObject load(RecordManager db, long recID) throws IOException {
        Object o = db.fetch(recID);
        if (o instanceof JDBMObject) {
            JDBMObject jdbmo = (JDBMObject) o;
            jdbmo.recID = recID;
            return jdbmo;
        }
        return null;
    }

    /**
     * Create a new jdbm object in the DB
     * 
     * @param db
     * @return recID
     * @throws IOException
     */
    protected long create(RecordManager db) throws IOException {
        recID = db.insert(this);
        return recID;
    }

    /**
     * Update an existing jdbm object in the DB
     * 
     * @param db
     * @throws IOException
     */
    protected void update(RecordManager db) throws IOException {
        db.update(recID, this);
    }

    /**
     * Update an existing jdbm object in the DB
     * 
     * @param db
     * @throws IOException
     */
    protected void delete(RecordManager db) throws IOException {
        db.delete(recID);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


}
