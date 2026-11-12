package com.pdm.model;

public class BookCopy {
    private String Book_ID;
    private String Copy_ID;
    private String Shelf_Location;
    private String Status;
    private String note;

    public BookCopy(String Book_ID, String Copy_ID, String Shelf_Location, String Status, String note) {
        this.Book_ID = Book_ID;
        this.Copy_ID = Copy_ID;
        this.Shelf_Location = Shelf_Location;
        this.Status = Status;
        this.note = note;
    }

    public String getBook_ID() {
        return Book_ID;
    }

    public String getCopy_ID() {
        return Copy_ID;
    }

    public String getShelf_Location() {
        return Shelf_Location;
    }

    public String getStatus() {
        return Status;

    }

    public String getNote() {
        return note;
    }
}
