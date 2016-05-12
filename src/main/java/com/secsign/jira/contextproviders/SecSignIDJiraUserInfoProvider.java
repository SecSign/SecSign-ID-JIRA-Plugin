package com.secsign.jira.contextproviders;


import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.secsign.jira.SecSignIDConstants;
import com.secsign.jira.ao.SecSignIDUsersActiveObject;
import com.secsign.jira.util.SecSignIDLogger;
import com.secsign.jira.util.SecSignIDUserManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


/**
 * Class to provide content for the web panel module which is shown at 
 * profile details to see which secsign id was assigned to current jira user
 * 
 * @version 1.0
 * @author SecSign Technologies Inc.
 */
public class SecSignIDJiraUserInfoProvider extends AbstractJiraContextProvider {

    /**
     * logger instance for this class
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SecSignIDJiraUserInfoProvider.class);
    
    /**
     * The active objects instance
     * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/active-objects 
     */
    private final ActiveObjects ao;
    
    /**
     * Constructor
     * @param ao
     */
    public SecSignIDJiraUserInfoProvider(ActiveObjects ao) {
        this.ao = ao;
        
    }
   
    @Override
    public Map getContextMap(Map context) {
        super.getContextMap(context);
       
        
        // paramMap: {profileUser=foobar:1, currentUser=admin:1}
        ApplicationUser shownApplicationUser = null;
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getUser();
        boolean isAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER, loggedInUser);
        
        logger.debug("context map of user provider: " + SecSignIDLogger.toString(context));
        
        // context.get("currentUser"´) is the current logged in user
        Object profileUser = context.get("profileUser");
        String profileUserName = null;
                    
        logger.debug("profile user from context map is of type '" + (profileUser == null ? "null" : profileUser.getClass().getName()) + "'");
                    
        if(profileUser instanceof User){
            shownApplicationUser = ApplicationUsers.from((User)profileUser);
            profileUserName = ((User)profileUser).getName();
        } else if(profileUser instanceof ApplicationUser){
            shownApplicationUser = (ApplicationUser)profileUser;
            profileUserName = ((ApplicationUser)profileUser).getKey();
            
        } else if(profileUser instanceof String){
            shownApplicationUser = SecSignIDUserManager.getApplicationUser((String)profileUser);
            profileUserName = (String)profileUser;
        } else {
            logger.warn("unknown type of profile user in context: " + (profileUser == null ? "null" : profileUser.getClass().getName()));
            
            profileUserName = String.valueOf(profileUser);
        }
        
        logger.debug("got application user '" + String.valueOf(shownApplicationUser) + "' and profile user name '" +  profileUserName + "'");
        
        HashMap<String, Object> contextMap = new HashMap<String, Object>();
        
        String secSignIdsStr = null;
        Integer pwdAllowed = SecSignIDUsersActiveObject.getPasswordLoginAllowedForApplicationUser(ao, shownApplicationUser);
        
        if(shownApplicationUser == null){
            contextMap.put("jirauser", profileUserName);
            contextMap.put("jirausername", profileUserName);
            try {
                contextMap.put("jirauserurldec", URLEncoder.encode(profileUserName, "utf-8"));
            } catch (UnsupportedEncodingException ex) {
                // impossible
            }
            
            secSignIdsStr = SecSignIDUsersActiveObject.getSecSignIdsStringForApplicationUserKey(ao, profileUserName.toLowerCase());
            
        } else {
            contextMap.put("jirauser", shownApplicationUser.getKey());
            contextMap.put("jirausername", shownApplicationUser.getDisplayName());
            
            try {
                contextMap.put("jirauserurldec", URLEncoder.encode(shownApplicationUser.getKey(), "utf-8"));
            } catch (UnsupportedEncodingException ex) {
                // impossible
            }
            
            secSignIdsStr = SecSignIDUsersActiveObject.getSecSignIdsStringForApplicationUser(ao, shownApplicationUser);
        }
        
        contextMap.put("secsignidstr", secSignIdsStr);
        contextMap.put("pwdallowed", pwdAllowed);
        contextMap.put("is-pwdallowed-value", SecSignIDConstants.PasswordLoginIsAllowed);
        contextMap.put("logged-in-isadmin", String.valueOf(isAdmin));
        contextMap.putAll(context);
        
        logger.debug("complete context map of user provider: " + SecSignIDLogger.toString(contextMap));
        
        return contextMap;
    }

    public Map getContextMap(User user, JiraHelper jiraHelper) {
        // dont know why, but user and jiraHelper are always null
        return null;
    }
    
    public Map getContextMap(ApplicationUser arg0, JiraHelper arg1) {
        // stub to have JIRA 7 compliance
        return null;
    }
}
