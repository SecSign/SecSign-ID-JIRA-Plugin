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
     * Represents a username password pair of user credentials.
     */
    private static class SecSignIDUserPwdPair
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
    
    
    @Override
    public String login(final HttpServletRequest request, final HttpServletResponse response)
    {
        // TODO: extract secsign id login field and put it into the user pwd pair to differ between a machine login and a secsign id login
        SecSignIDUserPwdPair userPair = extractUserPasswordPair(request, true);
        
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
                        
                        if(! isPasswordAllowed.equals(SecSignIDConstants.PasswordLoginIsAllowed)){
                            // at least run the other login interceptors
                            List<LoginInterceptor> interceptors = getSecurityConfig().getInterceptors(LoginInterceptor.class);
                            for(LoginInterceptor loginInterceptor : interceptors){
                                loginInterceptor.beforeLogin(request, response, userPair.userName, userPair.password, userPair.persistent);
                            }
                            
                            return LOGIN_FAILED;
                        }
                    }
                }
            }
        }
        
        return super.login(request, response);
    }
    
    
    protected SecSignIDUserPwdPair extractUserPasswordPair(HttpServletRequest httpServletReq, boolean ownClass) {
     // String login = httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_FORM_SUBMIT_PARAM_NAME);
        String userName = httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_USER_PARAM_NAME);
        String password = httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_PWD_PARAM_NAME);
        boolean persistent = Boolean.getBoolean(httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_PERSITENCE_PARAM_NAME));
                
        return new SecSignIDUserPwdPair(userName, password, persistent);
    }    
    
    @Override
    protected UserPasswordPair extractUserPasswordPair(HttpServletRequest httpServletReq) {
        SecSignIDUserPwdPair ssidupp = extractUserPasswordPair(httpServletReq, true);
        
        UserPasswordPair pair = new UserPasswordPair(ssidupp.userName, ssidupp.password, ssidupp.persistent);
        return pair;
    }    
}
