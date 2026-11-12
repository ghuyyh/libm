package com.pdm.model;

public class Author {
    private String Author_ID;
    private String Author_Name;
    private String Biography;

    public Author(String Author_ID, String Author_Name, String Biography) {
        this.Author_ID = Author_ID;
        this.Author_Name = Author_Name;
        this.Biography = Biography;
    }

    public String getAuthor_ID() {
        return Author_ID;
    }

    public String getAuthor_Name() {
        return Author_Name;
    }

    public String getBiography() {
        return Biography;
    }
}
