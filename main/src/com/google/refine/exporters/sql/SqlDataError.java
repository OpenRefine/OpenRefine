package com.google.refine.exporters.sql;


public class SqlDataError {
    private int errorCode;
    private String errorMessage;
    
    public SqlDataError(int errorCode, String errorMessage) {
        super();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    
    public int getErrorCode() {
        return errorCode;
    }

    
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    
    public String getErrorMessage() {
        return errorMessage;
    }

    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
