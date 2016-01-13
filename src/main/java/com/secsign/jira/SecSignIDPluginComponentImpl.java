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