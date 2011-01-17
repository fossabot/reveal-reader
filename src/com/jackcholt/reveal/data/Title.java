package com.jackcholt.reveal.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class contains the data for titles available for download
 * 
 * @author jwiggins
 * 
 */
public class Title {
    protected int id = 0;
    protected int fileSize = 0;
    protected String name;
    protected String description;
    protected String fileName;
    protected String fileFormat;
    protected String url;
    protected Date created;

    protected final SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd");
    protected final SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy");

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setId(String id, int defaultValue) {
        try {
            this.id = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            this.id = defaultValue;
        }
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileSize(String size, int defaultValue) {
        try {
            this.fileSize = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            this.fileSize = defaultValue;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setCreated(String date, Date defaultValue) {
        try {
            this.created = parseFormat.parse(date);
        } catch (ParseException e) {
            this.created = defaultValue;
        }
    }

    public String toString() {
        return name;
    }

    public String getSynopsis() {
        StringBuilder information = new StringBuilder();

        if (name != null) {
            information.append(name + "\n\n");
        }

        if (fileSize > 0) {
            information.append("Size: " + fileSize + " KB\n");
        }
        if (created != null) {
            information.append("Created: " + displayFormat.format(created) + "\n");
        }
        if (description != null) {
            information.append("Description: " + description + "\n");
        }
        return information.toString();
    }

    public void clear() {
        id = 0;
        fileSize = 0;
        name = null;
        description = null;
        fileName = null;
        fileFormat = null;
        url = null;
        created = null;
    }

    public Title copy() {
        Title newTitle = new Title();

        newTitle.id = this.id;
        newTitle.fileSize = this.fileSize;
        newTitle.name = this.name;
        newTitle.description = this.description;
        newTitle.fileName = this.fileName;
        newTitle.fileFormat = this.fileFormat;
        newTitle.url = this.url;
        newTitle.created = this.created;

        return newTitle;
    }
}
