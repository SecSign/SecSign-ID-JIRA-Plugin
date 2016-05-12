package com.secsign.jira;

import com.atlassian.sal.api.ApplicationProperties;
import com.secsign.jira.SecSignIDPluginComponent;

/**
 * The implementation of the secsign id plugin/secsign id add-on
 * 
 * @version 1.0
 * @author SecSign Technologies Inc.
 */
public class SecSignIDPluginComponentImpl implements SecSignIDPluginComponent
{
    
    // TODO: show all secsign id mappings with no existing application user and offer admin to delete those
    // TODO: add a new option to enable/disable debugging in the PLugin. see SecSignIDLogger and SecSignIDConstants
    
    // TODO: add link in user hover?
    // see section system.user.hover.links
    // https://developer.atlassian.com/jiradev/jira-architecture/web-fragments/user-accessible-locations
    
    // TODO: the options and user overview could be put into a generic tab panel?
    // http://jiradev.com/project-tab-panel.html
    
    // TODO: login and if no user could be found, create a new user?
    // see screenshot "Limit to Google Apps" 
    // https://marketplace.atlassian.com/plugins/com.pawelniewiadomski.jira.jira-openid-authentication-plugin
    
    // TODO: add more information and properties to limit and restrict who can loginfrom where?
    // see screenshot "Limit to Google Apps" 
    // https://marketplace.atlassian.com/plugins/com.pawelniewiadomski.jira.jira-openid-authentication-plugin
    
    /**
     * instance of application properties
     */
    private final ApplicationProperties applicationProperties;

    /**
     * Constructor
     * @param applicationProperties
     */
    public SecSignIDPluginComponentImpl(ApplicationProperties applicationProperties)
    {
        this.applicationProperties = applicationProperties;
    }

    /**
     * interface implementation of getName()
     */
    public String getName()
    {
        if(null != applicationProperties)
        {
            return "SecSign ID:" + applicationProperties.getDisplayName();
        }
        
        return "SecSign ID Plugin";
    }
}