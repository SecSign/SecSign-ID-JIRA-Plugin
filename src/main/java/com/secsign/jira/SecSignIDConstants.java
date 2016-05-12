package com.secsign.jira;

import com.atlassian.seraph.RequestParameterConstants;

/**
 * Constants for the property keys
 * 
 * @author SecSign Technologies Inc.
 */
public interface SecSignIDConstants {
    /**
     * application key name for jira settings. this will perform better than asking the global settings 
     */
    public final String SecSignIDSettingsKey = "com.secsign.secsignid:jira-settings";
    
    /**
     * the login key: whether secsign id login shall be called or not
     */
    public final String SecSignID2FAOptionsKey = "com.secsign.jira.secsignidlogin";
    
    /**
     * the key name for the service name
     */
    public final String ServiceNameOptionsKey = "com.secsign.jira.servicename";
    
    /**
     * the key name for the service name
     */
    public final String DebugRuntimeOptionsKey = "com.secsign.jira.debug";
    
    /**
     * the key name for the id server url
     */
    public final String IDServerUrlOptionsKey = "com.secsign.jira.idserverurl";
    
    /**
     * the key name for the fallback id server url
     */
    public final String FallbackIDServerUrlOptionsKey = "com.secsign.jira.idserverurl.fallback";
    
    
    
    
    /**
     * Default secsign id server
     */
    public final String DefaultSecSignIDServer = "https://httpapi.secsign.com";
    
    /**
     * Default fallback secsign id server
     */
    public final String DefaultFallbackSecSignIDServer = "https://httpapi2.secsign.com";


    
    
    
    /**
     * Parameter name for return url
     */
    public static final String RETURN_URL_PARAM_NAME = "returnUrl";
    
    /**
     * Parameter name for return url
     */
    public static final String JIRA_LOGIN_URL_PARAM_NAME = "loginUrl";
    
    /**
     * Parameter name for jiras os_destination parameter
     */
    public static final String JIRA_OS_DESTINATION_PARAM_NAME = RequestParameterConstants.OS_DESTINATION; //"os_destination";
    
    /**
     * Parameter user_role
     */
    public static final String JIRA_USER_ROLE_PARAM_NAME = "user_role"; //"os_destination";
    

    /**
     * Parameter name of jira login
     */
    public static final String JIRA_LOGIN_PARAM_NAME = "jira-login";
    
    /**
     * Parameter name of secsign id login
     */
    public static final String SECSIGNID_LOGIN_PARAM_NAME = "secsignid-login";
    
    /**
     * Parameter name for jiras login form user name input field
     */
    public static final String JIRA_LOGIN_USER_PARAM_NAME = RequestParameterConstants.OS_USERNAME; //"os_username";
    
    /**
     * Parameter name for jiras login form password input field
     */
    public static final String JIRA_LOGIN_PWD_PARAM_NAME = RequestParameterConstants.OS_PASSWORD; //"os_password";
    
    /**
     * Parameter name for jiras login form
     */
    public static final String JIRA_LOGIN_PERSITENCE_PARAM_NAME = RequestParameterConstants.OS_COOKIE; //"os_cookie";
    
    /**
     * Parameter name for jiras login form submit button
     */
    public static final String JIRA_LOGIN_FORM_SUBMIT_PARAM_NAME = "login";
    
    
    
    
    /**
     * secsign authenticator servlet plugin path
     */
    public static final String SECSIGNID_SERVLET_PATH = "/plugins/servlet/secsignid";
    
    /**
     * default jsp
     */
    public static final String JIRA_DEFAULT_JSP_PATH = "default.jsp";
    
    
    /**
     *  dashboard jsp name
     */
    public static final String JIRA_LOGIN_JSP_PATH = "login.jsp";
    
    
    /**
     * dashboard jsp name
     */
    public static final String JIRA_DASHBOARD_JSP_PATH = "Dashboard.jspa";
    
    
    /**
     * mobile login url http://<url>/jira/plugins/servlet/mobile#login/myjirahome
     */
    //public static final String JIRA_MOBILE_LOGIN_PATH = "mobile#login";
    public static final String JIRA_MOBILE_LOGIN_PATH = "/plugins/servlet/mobile";
    
    
    /**
     * view user jsp name
     * e.g.: http://localhost:2990/jira/secure/admin/user/ViewUser.jspa?name=admin
     */
    public static final String JIRA_VIEW_USER_JSP_PATH = "ViewUser.jspa";
        
    
    /**
     * view profile jsp name
     * e.g.: http://localhost:2990/jira/secure/ViewProfile.jspa?name=admin
     */
    public static final String JIRA_VIEW_PROFILE_JSP_PATH = "ViewProfile.jspa";
    
    /**
     * path to userbrowser
     */
    public static final String JIRA_USER_BROWSER_JSP_PATH = "/secure/admin/user/UserBrowser.jspa";
    
    /**
     * sign up action url
     */
    public static final String JIRA_SIGNUP_JSP_PATH = "/secure/Signup"; // /secure/Signup!default.jspa;
    
    /**
     * whether the password login is still allowed
     */
    public static final Integer PasswordLoginIsAllowed = new Integer(1);
    
    /**
     * whether the password login is not allowed
     */
    public static final Integer PasswordLoginIsNotAllowed = new Integer(0);
    
}
