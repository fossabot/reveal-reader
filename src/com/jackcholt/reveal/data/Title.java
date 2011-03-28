package com.jackcholt.reveal.data;

import java.util.Date;

/**
 * This class contains the data for titles available for download
 * 
 * @author jwiggins
 *
 */
public class Title {
    public int id;
    public int fileSize;
    public String name;
    public String description;
    public String fileName;
    public String fileFormat;
    public String url;
    public Date created;
    
    public String toString() {
        return name;
    }
}
