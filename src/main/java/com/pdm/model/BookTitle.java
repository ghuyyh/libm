package com.pdm.model;

public class BookTitle {
    private String bookID;
    private String isbn;
    private String title;
    private String publisher;
    private int publishYear;

    public BookTitle(String bookID, String isbn, String title, String publisher, int publishYear) {
        this.bookID = bookID;
        this.isbn = isbn;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.title = title;
    }

    public String getBookID() {
        return bookID;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getPublisher() {
        return publisher;
    }

    public int getPublishYear() {
        return publishYear;
    }
}
