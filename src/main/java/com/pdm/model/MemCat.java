package com.pdm.model;

import java.math.BigDecimal;

public class MemCat {
    String Member_Type_ID;
    String Type_Name;
    BigDecimal Total_Unpaid_Fine;

    public MemCat(String member_Type_ID, String type_Name, BigDecimal total_Unpaid_Fine) {
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

    public BigDecimal getTotal_Unpaid_Fine() {
        return Total_Unpaid_Fine;
    }
}
