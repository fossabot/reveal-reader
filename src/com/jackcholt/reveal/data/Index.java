/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

/**
 * Generic wrapper around BTree that makes it not as klunky to use for storing references to objects.
 * 
 * @author Shon Vella
 * 
 */
public class Index<K extends Serializable, V extends JDBMObject> {
    private RecordManager mDb;
    private BTree<K, Long> mBtree;

    /**
     * Contructor
     * 
     * @param db
     *            database
     * @param btree
     *            btree
     */
    public Index(RecordManager db, BTree<K, Long> btree) {
        mDb = db;
        mBtree = btree;
    }

    /**
     * Contructor that creates the underlying BTree
     * 
     * @param db
     *            database
     * @throws IOException
     */
    public Index(RecordManager db, Comparator<K> comparator) throws IOException {
        mDb = db;
        mBtree = BTree.createInstance(db, comparator);
    }

    /**
     * Contructor
     * 
     * @param db
     *            database
     * @param btree
     *            btree
     * @throws IOException
     */
    public Index(RecordManager db, long recID) throws IOException {
        mDb = db;
        mBtree = BTree.load(db, recID);
    }

    /**
     * Put object reference into index
     * 
     * @param key
     * @param value
     * @return true if succeeded, false if failed because the key was already in the index
     * @throws IOException
     */
    public boolean put(K key, V value) throws IOException {
        return mBtree.insert(key, value.recID, false) == null;
    }

    /**
     * Get referred-to object reference from index
     * 
     * @param key
     * @return true if succeeded, false if failed because the key was already in the index
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public V get(K key) throws IOException {
        Long ref = (Long) mBtree.find(key);
        return (ref != null) ? (V) JDBMObject.load(mDb, ref) : null;
    }

    /**
     * Get referred-to object reference from index or the one that would follow it if it doesn't exist
     * 
     * @param key
     * @return true if succeeded, false if failed because the key was already in the index
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public List<V> getStartsWith(K key) throws IOException {
        List<V> values = new ArrayList<V>();
        Tuple<K, Long> tuple = new Tuple<K, Long>();
        String prefix = key.toString();
        TupleBrowser<K, Long> browser = mBtree.browse(key);
        while (browser.getNext(tuple)) {
            if (!tuple.getKey().toString().startsWith(prefix)) {
                break;
            } else {
                V value = (V) JDBMObject.load(mDb, tuple.getValue());
                if (value != null)
                    values.add(value);
            }
        }
        return values;
    }

    /**
     * @return
     * @throws IOException
     * @see jdbm.btree.BTree#browse()
     */
    public TupleBrowser<K, Long> browse() throws IOException {
        return mBtree.browse();
    }

    /**
     * @param key
     * @return
     * @throws IOException
     * @see jdbm.btree.BTree#browse(java.lang.Object)
     */
    public TupleBrowser<K, Long> browse(K key) throws IOException {
        return mBtree.browse(key);
    }

    /**
     * @return
     * @see jdbm.btree.BTree#getRecid()
     */
    public long getRecid() {
        return mBtree.getRecid();
    }

    /**
     * @return
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return mBtree.hashCode();
    }

    /**
     * @param key
     * @return
     * @throws IOException
     * @see jdbm.btree.BTree#remove(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public V remove(K key) throws IOException {
        Long ref = (Long) mBtree.remove(key);
        return (ref != null) ? (V) JDBMObject.load(mDb, ref) : null;
    }

    /**
     * @return
     * @see jdbm.btree.BTree#size()
     */
    public long entryCount() {
        return mBtree.entryCount();
    }

}
