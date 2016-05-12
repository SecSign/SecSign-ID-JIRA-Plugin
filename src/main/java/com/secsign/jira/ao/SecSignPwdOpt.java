package com.secsign.jira.ao;


import net.java.ao.Preload;
import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;




/**
 * Entity or active object for the options of a single user 
 * e.g.: if password login still allowed
 * 
 * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/active-objects/developing-your-plugin-with-active-objects/the-active-objects-library/creating-entities
 * 
 * @author SecSign Technologies Inc.
 */
@Preload
public interface SecSignPwdOpt extends RawEntity<String>{
    
    // when the datamodel needs to be updated, 
    // @see https://answers.atlassian.com/questions/130325/how-to-handle-active-objects-data-model-changes-when-upgrading-a-plugin
    // @see https://developer.atlassian.com/docs/atlassian-platform-common-components/active-objects/developing-your-plugin-with-active-objects/upgrading-your-plugin-and-handling-data-model-updates

    @PrimaryKey("JiraUserName")
    @NotNull
    String getJiraUserName();
    void setJiraUserName(String jiraUserName);
    
    
    Integer getPwdLoginAllowed();
    void setPwdLoginAllowed(Integer pwdLoginAllowed);
}
