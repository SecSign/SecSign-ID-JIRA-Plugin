package com.secsign.jira.servlet.filter;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.seraph.filter.PasswordBasedLoginFilter;
import com.atlassian.seraph.interceptor.LoginInterceptor;
import com.secsign.jira.SecSignIDConstants;
import com.secsign.jira.ao.SecSignIDUsersActiveObject;

/**
 * @see https://docs.atlassian.com/atlassian-seraph/latest/apidocs/com/atlassian/seraph/filter/PasswordBasedLoginFilter.html
 * @author SecSign Technologies Inc.
 */
public class SecSignIDPasswordLoginFilter extends PasswordBasedLoginFilter {

    /**
     * logger instance for this class
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SecSignIDPasswordLoginFilter.class);
    
    /**
     * Represents a username password pair of user credentials.
     */
    private class SecSignIDUserPwdPair
    {
        final String userName;
        final String password;
        final boolean persistent;

        /**
         * Constructor
         * @param user
         * @param password
         * @param persistent
         */
        public SecSignIDUserPwdPair(final String user, final String password, final boolean persistent){
            userName = user;
            this.password = password;
            this.persistent = persistent;
        }
    }
    
    /**
     * The active objects instance
     * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/active-objects 
     */
    private final ActiveObjects ao;
    
    /**
     * User manager
     */
    //private final UserManager userManager;
    
    /**
     * Constructor
     * @param templateRenderer
     * @param ao
     */
    //public SecSignIDPasswordLoginFilter(ActiveObjects ao, UserManager userManager)
    public SecSignIDPasswordLoginFilter(ActiveObjects ao)
    {
        this.ao = ao;
        //this.userManager = userManager;
    }
    
    
    /**
     * Performs the actual authentication (if required) and returns the status code. Status code is chosen to be one of
     * these:
     * <p/>
     * The possible statuses are:
     * <ul>
     * <li> BaseLoginFilter.LOGIN_SUCCESS - the login was processed, and user was logged in
     * <li> BaseLoginFilter.LOGIN_FAILURE - the login was processed, the user gave a bad username or password
     * <li> BaseLoginFilter.LOGIN_ERROR - the login was processed, an exception occurred trying to log the user in
     * <li> BaseLoginFilter.LOGIN_NOATTEMPT - the login was no processed, no form parameters existed
     * </ul>
     * <p/>
     *
     * @param httpServletRequest  the HTTP request in play
     * @param httpServletResponse the HTTP response in play
     *
     * @return authentication status
     */
    @Override
    public String login(final HttpServletRequest request, final HttpServletResponse response)
    {
        // TODO: extract secsign id login field and put it into the user pwd pair to differ between a machine login and a secsign id login
        SecSignIDUserPwdPair userPair = extractJiraUserPasswordPair(request);
        
        // check username
        if(userPair.userName != null && userPair.password != null){
            ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getUser();
            if(loggedInUser == null){
                ApplicationUser appUser = ApplicationUsers.byKey(userPair.userName);
                
                if(appUser != null){
                    // check whether the user has been assigned a secsign id
                    String[] secSignIds = SecSignIDUsersActiveObject.getSecSignIdsForApplicationUser(ao, appUser);
                    if(secSignIds != null && secSignIds.length > 0){
                    
                        // check whether user is allowed to login using password
                        Integer isPasswordAllowed = SecSignIDUsersActiveObject.getPasswordLoginAllowedForApplicationUser(ao, appUser);
                        
                        logger.info("application user '" + userPair.userName + "' trys to login using password. password allowed: " + isPasswordAllowed + ", assigned secsign ids: " + SecSignIDUsersActiveObject.getSecSignIdsString(secSignIds));
                        
                        if(! isPasswordAllowed.equals(SecSignIDConstants.PasswordLoginIsAllowed)){
                            
                            // at least run the other login interceptors
                            List<LoginInterceptor> interceptors = getSecurityConfig().getInterceptors(LoginInterceptor.class);
                            for(LoginInterceptor loginInterceptor : interceptors){
                                loginInterceptor.beforeLogin(request, response, userPair.userName, userPair.password, userPair.persistent);
                            }
                            
                            // return "failed"
                            return LOGIN_FAILED;
                        }
                    } else {
                        logger.info("application user '" + userPair.userName + "' trys to login using password, but no secsign id was assigned. allow access using password.");
                    }
                }
            }
        }
        
        return super.login(request, response);
    }
    
    /**
     * Extracts username and password from given http servlet request
     * @param httpServletReq the http servlet request
     * 
     * @see com.secsign.jira.servlet.filter.SecSignIDPasswordLoginFilter.SecSignIDUserPwdPair
     * @see com.atlassian.seraph.filter.UserPasswordPair
     * 
     * @return the username password pair
     */
    protected SecSignIDUserPwdPair extractJiraUserPasswordPair(HttpServletRequest httpServletReq) {
        // String login = httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_FORM_SUBMIT_PARAM_NAME);
        String userName = httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_USER_PARAM_NAME);
        String password = httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_PWD_PARAM_NAME);
        boolean persistent = Boolean.getBoolean(httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_PERSITENCE_PARAM_NAME));
                
        return new SecSignIDUserPwdPair(userName, password, persistent);
    }    
    
    /**
     * Returns a username password pair for this request.
     * @param httpServletReq the http servlet request in play
     * @return user credentials or null
     */
    @Override
    protected UserPasswordPair extractUserPasswordPair(HttpServletRequest httpServletReq) {
        SecSignIDUserPwdPair ssidupp = extractJiraUserPasswordPair(httpServletReq);
        
        UserPasswordPair pair = new UserPasswordPair(ssidupp.userName, ssidupp.password, ssidupp.persistent);
        return pair;
    }    
}
