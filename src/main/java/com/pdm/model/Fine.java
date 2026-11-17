package com.pdm.model;
import java.math.BigDecimal;
import java.sql.Date;

public class Fine {

    private String Loan_ID;
    private String Detail_ID;
    private String Member_ID;
    private String Member_Name;
    private String Book_Title;
    private String Copy_ID;
    private Date Due_Date;
    private Date Return_Date;
    private int Days_Late;
    private BigDecimal Fine_Amount;
    private String Payment_Status;

    public Fine(String loan_ID, String detail_ID, String member_ID, String member_Name, String book_Title,
                String copy_ID, Date due_Date, Date return_Date, int days_Late,
                BigDecimal fine_Amount, String payment_Status) {
        Loan_ID = loan_ID;
        Detail_ID = detail_ID;
        Member_ID = member_ID;
        Member_Name = member_Name;
        Book_Title = book_Title;
        Copy_ID = copy_ID;
        Due_Date = due_Date;
        Return_Date = return_Date;
        Days_Late = days_Late;
        Fine_Amount = fine_Amount;
        Payment_Status = payment_Status;
    }

    public String getLoan_ID() {
        return Loan_ID;
    }
    public void getDetail_ID(String detail_ID) {
        Detail_ID = detail_ID;
    }
    public String getMember_ID() {
        return Member_ID;
    }
    public String getMember_Name() {
        return Member_Name;
    }
    public String getBook_Title() {
        return Book_Title;
    }       
    public String getCopy_ID() {
        return Copy_ID;
    }
    public Date getDue_Date() {
        return Due_Date;
    }
    public Date getReturn_Date() {
        return Return_Date;
    }
    public int getDays_Late() {
        return Days_Late;
    }
    public BigDecimal getFine_Amount() {
        return Fine_Amount;
    }
    public String getPayment_Status() {
        return Payment_Status;
    }

}