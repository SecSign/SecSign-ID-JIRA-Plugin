package com.secsign.jira;

/**
 * Wrapping class
 * 
 * @author SecSign Technologies Inc.
 */
public class SecSignIDJiraException extends Exception {

    /**
     * serial version uid
     */
    private static final long serialVersionUID = 2654740961129412362L;
    
    /**
     * localized/i18n message
     */
    private String i18nMessage;
    
    /**
     * error code
     */
    private int errorCode;

    /**
     * Constructor
     * @param message
     * @param i18nMessage
     * @param errorCode
     */
    public SecSignIDJiraException(String message, String i18nMessage, int errorCode){
        super(message);
        
        this.i18nMessage = i18nMessage;
        this.errorCode = errorCode;
    }

    /**
     * @return the localizedMessage
     */
    public String getI18nMessage() {
        return i18nMessage;
    }

    /**
     * @return the errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }
}
