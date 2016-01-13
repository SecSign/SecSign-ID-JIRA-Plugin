/*
 * 
 */
package com.secsign.jira.listener;



import java.util.HashMap;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.event.user.UserDeletedEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.user.UserEventType;
import com.secsign.jira.ao.SecSignIDUsers;
import com.secsign.jira.ao.SecSignIDUsersActiveObject;
import com.secsign.jira.util.SecSignIDLogger;

/**
 * Class which handles all events concerning user updates.
 * 
 * @see https://developer.atlassian.com/jiradev/jira-platform/other/tutorial-writing-jira-event-listeners-with-the-atlassian-event-library
 * @see https://confluence.atlassian.com/jira/listeners-185729466.html
 * 
 * @author SecSign Technologies Inc.
 */
public class SecSignIDUserEventListener implements InitializingBean, DisposableBean {
   
    private final EventPublisher eventPublisher;
    
    /**
     * Instance for active objects handling
     */
    private final ActiveObjects ao;
    
    /**
     * Constructor.
     * @param eventPublisher injected {@code EventPublisher} implementation.
     */
    public SecSignIDUserEventListener(EventPublisher eventPublisher, ActiveObjects ao) {
        this.eventPublisher = eventPublisher;
        this.ao = ao;
    }
    
    
    /*@EventListener
    public void onUserEvent(com.atlassian.jira.event.user.UserEvent userEvent){
        int eventType = userEvent.getEventType();
        
        SecSignIDLogger.log("eventType=" + eventType + " (" + getNameForEventType(eventType) + ") for user '" + userEvent.getUser() + "'");
        
        if(eventType == UserEventType.USER_CREATED){
            
        } else if(eventType == UserEventType.USER_LOGIN){
            // @see https://docs.atlassian.com/jira/6.2.6/com/atlassian/jira/event/user/UserEventType.html#USER_LOGIN
            com.atlassian.jira.user.ApplicationUser loggedInUser = 
                            com.atlassian.jira.component.ComponentAccessor.getJiraAuthenticationContext().getUser();
            com.atlassian.crowd.embedded.api.User initiatingUser = userEvent.getInitiatingUser();
            if(loggedInUser != null){
                SecSignIDLogger.log("logged in app user '" + loggedInUser.getKey() + "'");
            }
            
            if(loggedInUser != null && initiatingUser != null){
                // secsign id checked it in.
            } else {
                // do further checks
            }
        }
    }*/
    
    @EventListener
    public void onUserDeletedEvent(UserDeletedEvent userDeletedEvent)
    {
        String userName = userDeletedEvent.getUsername();
        String secSignIds = "";
        
        
        //ActiveObjects ao = com.atlassian.jira.component.ComponentAccessor.getOSGiComponentInstanceOfType(ActiveObjects.class);
        // use constructor injection instead...
        
        
        try{
            SecSignIDUsers deletedEntity = SecSignIDUsersActiveObject.deleteSecSignIDMappingFromDatabase(ao, userName);
            if(deletedEntity != null){
                secSignIds = deletedEntity.getSecSignId();
            }
        } catch(Exception ex){
            SecSignIDLogger.log("cannot delete secsign ids for user '" + userName + "': " + ex.getMessage());
            SecSignIDLogger.log(ex);
            
            HashMap<String, String[]> newMappings = new HashMap<String, String[]>();
            newMappings.put(userName, null);
            
            
            SecSignIDUsersActiveObject.saveSecSignIdUserMappings(ao, newMappings);
        }
        
        SecSignIDLogger.log("deleted '" + userName + "' and all assigned secsign ids '" + secSignIds + "' from database.");
    }
    
    /**
     * Gets a himan readyble name for the event type.
     * @param eventType
     * @return
     */
    private String getNameForEventType(int eventType) {
        switch(eventType)
        {
            case UserEventType.USER_SIGNUP: return "USER_SIGNUP";
            case UserEventType.USER_CREATED: return "USER_CREATED";
            case UserEventType.USER_FORGOTPASSWORD: return "USER_FORGOTPASSWORD";
            case UserEventType.USER_FORGOTUSERNAME: return "USER_FORGOTUSERNAME";
            case UserEventType.USER_CANNOTCHANGEPASSWORD: return "USER_CANNOTCHANGEPASSWORD";
            case UserEventType.USER_LOGIN: return "USER_LOGIN";
            case UserEventType.USER_LOGOUT: return "USER_LOGOUT";
            default: return "UNKNOWN";
        }
    }


    /**
     * Called when the plugin has been enabled.
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // register ourselves with the EventPublisher
        eventPublisher.register(this);
    }

    /**
     * Called when the plugin is being disabled or removed.
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        // unregister ourselves with the EventPublisher
        eventPublisher.unregister(this);
    }
}
