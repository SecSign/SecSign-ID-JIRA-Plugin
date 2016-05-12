package com.secsign.jira.util;

import java.util.Collection;


import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;

/**
 * Helping class for easier access to jira methods
 * 
 * @version 1.0
 * @author SecSign Technologies Inc.
 */
public class SecSignIDUserManager {

    /**
     * logger instance for this class
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SecSignIDUserManager.class);
    
    /**
     * Gets an instance of an application user by key or by name
     * @param userKeyOrName the application user key. if no application user could be fopund, the key is treated as name
     * @return the jira application user
     */
    public static ApplicationUser getApplicationUser(String userKeyOrName){
        return SecSignIDUserManager.getApplicationUser(null, userKeyOrName);
    }
    
    /**
     * Gets an instance of an application user by key or by name
     * @param umgr UserManager
     * @param userKeyOrName the application user key. if no application user could be fopund, the key is treated as name
     * @return the jira application user
     */
    public static ApplicationUser getApplicationUser(UserManager umgr, String userKeyOrName){
        if(umgr == null){
            umgr = ComponentAccessor.getUserManager();
        }
        
        // application user keys are always lower case
        String userKeyLowerCase = userKeyOrName.toLowerCase();
        ApplicationUser appUser = umgr.getUserByKey(userKeyLowerCase);
        if(appUser == null){
            appUser = umgr.getUserByName(userKeyOrName);
            
            logger.warn("did not find an application user with key '" + userKeyOrName + "'. found application user by using as name: " + String.valueOf(appUser));
            
            Collection<ApplicationUser> allApplicationUser = umgr.getAllApplicationUsers();
            for(ApplicationUser au : allApplicationUser){
                logger.debug("app user: key='" + au.getKey() + "' username='" + au.getUsername() + "' name='" + au.getName() + "' displayname='" + au.getName() + "'");
            }
        }
        
        return appUser;
    }
    
    /**
     * Gets the key of an application user.
     * @param applicationUser the application user to get the ely from
     * @return the key of an application user.
     */
    public static String getApplicationUserKeyOrName(ApplicationUser applicationUser)
    {
        if(applicationUser == null){
            logger.error("cannot get application user key, given application user is null");
            return null;
        }
        
        // @see https://developer.atlassian.com/static/javadoc/jira/reference/com/atlassian/jira/user/ApplicationUser.html
        String appUserName = applicationUser.getKey();
        if(appUserName == null){
            appUserName = applicationUser.getUsername();
        }
        return appUserName;
    }
}
