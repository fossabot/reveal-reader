package com.jackcholt.reveal.data;

/**
 * This class represents a category for title organization
 * 
 * @author jwiggins
 * 
 */
public class Category {
    protected int id;
    protected String name;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public void clear() {
        this.id = 0;
        this.name = null;
    }

    public Category copy() {
        Category newCategory = new Category();

        newCategory.id = this.id;
        newCategory.name = this.name;

        return newCategory;
    }
}
