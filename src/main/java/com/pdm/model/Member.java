package com.pdm.model;

public class Member {
    private String Member_ID;
    private String Full_Name;
    private String Type_ID;
    private String Type;
    private int Borrowing_Limit;
    private int Loan_Period;
    private String Email;

    public Member(String member_ID, String full_Name, String type_ID, String type, int borrow_Limit,
            int borrow_Period) {
        Member_ID = member_ID;
        Full_Name = full_Name;
        Type_ID = type_ID;
        Type = type;
        Borrowing_Limit = borrow_Limit;
        Loan_Period = borrow_Period;

    }

    public String getMember_ID() {
        return Member_ID;
    }

    public String getFull_Name() {
        return Full_Name;
    }

    public String getType_ID() {
        return Type_ID;
    }

    public String getType() {
        return Type;
    }

    public int getBorrowing_Limit() {
        return Borrowing_Limit;
    }

    public int getLoan_Period() {
        return Loan_Period;
    }

    public String getEmail() {
        return Email;
    }

}
