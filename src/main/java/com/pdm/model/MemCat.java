package com.pdm.model;

public class MemCat {
    String Member_Type_ID;
    String Type_Name;
    Long Total_Unpaid_Fine;

    public MemCat(String member_Type_ID, String type_Name, Long total_Unpaid_Fine) {
        Member_Type_ID = member_Type_ID;
        Type_Name = type_Name;
        Total_Unpaid_Fine = total_Unpaid_Fine;
    }

    public String getMember_Type_ID() {
        return Member_Type_ID;
    }

    public String getType_Name() {
        return Type_Name;
    }

    public Long getTotal_Unpaid_Fine() {
        return Total_Unpaid_Fine;
    }
}
