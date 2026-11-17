package com.pdm.model;
import java.sql.Date;
public class Reservation {

     private String id;
    private String memid;
    private String memname;
    private String bookid;
    private String title;
    private Date date;
    private String status;
    private int availableCopies;

    public Reservation(String id, String memid, String memname,String bookid, String title, Date date, String status, int availableCopies) {
        this.id = id;
        this.memid = memid;
        this.memname = memname;
        this.bookid = bookid;
        this.title = title;
        this.date = date;
        this.status = status;
        this.availableCopies = availableCopies;
    }

    public String getId() {
        return id;
    }

    public String getMemid() {
        return memid;
    }

    public String getMemname() {
        return memname;
    }

    public String getTitle() {
        return title;
    }

    public Date getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public String getBookid() {
        return bookid;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }
}