package com.secsign.jira.ao;


import net.java.ao.Preload;
import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;




/**
 * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/active-objects/developing-your-plugin-with-active-objects/the-active-objects-library/creating-entities
 * @author SecSign Technologies Inc.
 */
@Preload
public interface SecSignIDUsers extends RawEntity<String>{
    

    @PrimaryKey("JiraUserName")
    @NotNull
    String getJiraUserName();
    void setJiraUserName(String jiraUserName);
    
    
    String getSecSignId();
    void setSecSignId(String secSignId);
}
