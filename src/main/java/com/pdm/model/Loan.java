package com.pdm.model;

public class Loan {
    private String Loan_ID;
    private String MemberId;
    private double amount;
    private double interestRate;
    private int term;

    //
    public Loan() {
    }

    public Loan(String Loan_ID, String MemberId, double amount, double interestRate, int term) {
        this.Loan_ID = Loan_ID;
        this.MemberId = MemberId;
        this.amount = amount;
        this.interestRate = interestRate;
        this.term = term;
    }

    public String getLoan_ID() {
        return Loan_ID;
    }



    public String getMemberId() {
        return MemberId;
    }


    public double getAmount() {
        return amount;
    }



    public double getInterestRate() {
        return interestRate;
    }


    public int getTerm() {
        return term;
    }


}
