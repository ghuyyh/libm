package com.pdm.model;

public class Publisher {
    private String Publisher_ID;
    private String Publisher_Name;
    private String Address;
    private String Contact_Email;

    public Publisher(String publisher_ID, String publisher_name, String address, String contact_email) {
        this.Publisher_ID = publisher_ID;
        this.Publisher_Name = publisher_name;
        this.Address = address;
        this.Contact_Email = contact_email;
    }

    public String getPublisher_ID() {
        return Publisher_ID;
    }

    public String getPublisher_Name() {
        return Publisher_Name;
    }

    public String getAddress() {
        return Address;
    }

    public String getContact_Email() {
        return Contact_Email;
    }

    

}