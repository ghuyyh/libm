package com.pdm.model;

import java.time.LocalDate;

public class Member {
    private String memberID;
    private String fullName;
    private String typeID;
    private String type;
    private int borrowingLimit;
    private int loanPeriod;
    private String email;
    private String identityCard;
    private LocalDate membershipExpiryDate;
    private String accountStatus;

    public Member(String memberID, String fullName, String typeID, String type, int borrowingLimit, int loanPeriod) {
        this.memberID = memberID;
        this.fullName = fullName;
        this.typeID = typeID;
        this.type = type;
        this.borrowingLimit = borrowingLimit;
        this.loanPeriod = loanPeriod;
    }

    public Member(String memberID, String fullName, String typeID, String type, int borrowingLimit, int loanPeriod,
            String email, String identityCard, LocalDate membershipExpiryDate, String accountStatus) {
        this(memberID, fullName, typeID, type, borrowingLimit, loanPeriod);
        this.email = email;
        this.identityCard = identityCard;
        this.membershipExpiryDate = membershipExpiryDate;
        this.accountStatus = accountStatus;
    }

    public Member(String memberID, String fullName) {
        this.memberID = memberID;
        this.fullName = fullName;
    }

    public String getMember_ID() {
        return memberID;
    }

    public String getFull_Name() {
        return fullName;
    }

    public String getType_ID() {
        return typeID;
    }

    public String getType() {
        return type;
    }

    public int getBorrowing_Limit() {
        return borrowingLimit;
    }

    public int getLoan_Period() {
        return loanPeriod;
    }

    public String getEmail() {
        return email;
    }

    public String getIdentity_Card() {
        return identityCard;
    }

    public LocalDate getMembership_Expiry_Date() {
        return membershipExpiryDate;
    }

    public String getAccount_Status() {
        return accountStatus;
    }
}
