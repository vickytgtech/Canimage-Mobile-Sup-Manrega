package com.skyindya.dwsupervisor2;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class ResponseObj {

    @SerializedName("Status")
    @Expose
    private String status;
    @SerializedName("PlanID")
    @Expose
    private String planID;
    @SerializedName("PrintNo")
    @Expose
    private String printNo;
    @SerializedName("ID")
    @Expose
    private String iD;
    @SerializedName("Message")
    @Expose
    private String message;
    @SerializedName("Status_Code")
    @Expose
    private String statusCode;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlanID() {
        return planID;
    }

    public void setPlanID(String planID) {
        this.planID = planID;
    }

    public String getPrintNo() {
        return printNo;
    }

    public void setPrintNo(String printNo) {
        this.printNo = printNo;
    }

    public String getID() {
        return iD;
    }

    public void setID(String iD) {
        this.iD = iD;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

}
