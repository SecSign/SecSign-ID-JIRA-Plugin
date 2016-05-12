package com.secsign.jira.actions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.auditing.*;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.message.I18nResolver;
import com.secsign.jira.SecSignIDConstants;
import com.secsign.jira.ao.SecSignIDUsersActiveObject;
import com.secsign.jira.util.SecSignIDLogger;
import com.secsign.jira.util.SecSignIDUserManager;

/**
 *
 * @see https://docs.atlassian.com/jira/6.4.1/com/atlassian/jira/web/action/JiraWebActionSupport.html
 * @see https://jira.atlassian.com/secure/attachment/42730/JiraWebActionSupport.java
 * 
 * @author SecSign Technologies Inc.
 */
public class SecSignIDAction extends JiraWebActionSupport {

    /**
     * 
     */
    private static final long serialVersionUID = 6842235411471448500L;
    
    
    /**
     * logger instance for this class
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SecSignIDAction.class);
    
    
    /**
     * using the helper will  use the jira api directly. 
     * otherwise the SAL interface I18nResolver shall be used if this plugin shall run in the other atlassian appications as well.
     * 
     * @see https://docs.atlassian.com/jira/latest/com/atlassian/jira/util/I18nHelper.html
     * 
     * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/shared-access-layer
     * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/shared-access-layer/sal-services
     * 
     */
    //private com.atlassian.jira.util.I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
    
    /**
     * The active objects instance
     * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/active-objects 
     */
    private final ActiveObjects ao;
    
    /**
     * The I18n Resolver
     * @see https://docs.atlassian.com/sal-api/2.2.1/sal-api/apidocs/com/atlassian/sal/api/message/I18nResolver.html
     */
    private final I18nResolver i18nResolver;
    
    
    /**
     * Constructor
     */
    public SecSignIDAction(ActiveObjects ao, I18nResolver i18nResolver) {
        super();
        this.ao = ao;
        this.i18nResolver = i18nResolver;
    }
    
    
    /**
     * Validates the input
     */
    @Override
    protected void doValidation() {
        
        HttpServletRequest httpServletRequest = getHttpRequest();
        
        logger.debug("doValidation / request parameter map: " + SecSignIDLogger.toString(httpServletRequest.getParameterMap()));
        
        String sentSecSignId = httpServletRequest.getParameter("secsignid");
        String jiraUserKey = null;
        try {
            jiraUserKey = URLDecoder.decode(httpServletRequest.getParameter("jirauser"), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            // impossible. every vm must support UTF-8
        }
        
        
        ApplicationUser shownAppUser = SecSignIDUserManager.getApplicationUser(getUserManager(), jiraUserKey);
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getUser();
        loggedInIsAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER, loggedInUser);
        
        jiraUserName = jiraUserKey;
        jiraUserDisplayName = shownAppUser != null ? shownAppUser.getDisplayName() : jiraUserKey;
        
        secSignId = sentSecSignId;
        referingUri = httpServletRequest.getParameter("referinguri");
        returnUrl = httpServletRequest.getParameter(SecSignIDConstants.RETURN_URL_PARAM_NAME);
        
        /*if(shownAppUser == null){
            addErrorMessage(i18nResolver.getText("secsignid.messages.error.action.noappuser", jiraUserKey));
            return;
        }*/
        

        String passWordAllowed = httpServletRequest.getParameter("pwd-allowed");        
        boolean isPwdLoginAllowed = true;
        if(passWordAllowed == null || !"1".equals(passWordAllowed)){
            isPwdLoginAllowed = false;
        }
        pwdAllowed = new Integer(isPwdLoginAllowed ? 1 : 0);
        
        if(sentSecSignId == null || sentSecSignId.length() < 1){
            // all secsign ids shall be removed
            return;
        }
        
        
        String[] givenSecSignIds = SecSignIDUsersActiveObject.getArrayOfSecSignIds(sentSecSignId);
        
        // check whether there are some strange characters within the secsign id
        for(int i = 0; i < givenSecSignIds.length; i++){
            if(SecSignIDUsersActiveObject.secSignIdContainsIllegalCharacters(givenSecSignIds[i])){
                
                // https://docs.atlassian.com/jira/latest/com/atlassian/jira/util/I18nHelper.html
                //I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
                
                /*addError("error", i18n.getText("secsignid.messages.error.action.illegalcharacters") + " " + givenSecSignIds[i]);
                addError("secsignid", givenSecSignIds[i]);*/
                
                //@see https://developer.atlassian.com/docs/common-coding-tasks/internationalising-your-plugin#Internationalisingyourplugin-Knowni18nIssues
                addErrorMessage(i18nResolver.getText("secsignid.messages.error.action.illegalcharacters2", givenSecSignIds[i]) + " " + givenSecSignIds[i]);
                
                // return immediately...
                return;
            }
        }
        
        ArrayList<String> duplicates = SecSignIDUsersActiveObject.checkForDuplicateSecSignIds(ao, jiraUserKey, givenSecSignIds);
        if(duplicates != null){

            /*addError("error", i18n.getText("secsignid.messages.error.action.duplicate") + " " +  intersection.toString());
            addError("secsignid", intersection.toString());*/
            
            // @see https://developer.atlassian.com/docs/common-coding-tasks/internationalising-your-plugin#Internationalisingyourplugin-Knowni18nIssues
            if(duplicates.size() == 1){
                addErrorMessage(i18nResolver.getText("secsignid.messages.error.action.duplicate2", duplicates.get(0)) + " " + duplicates.get(0));
            } else {
                addErrorMessage(i18nResolver.getText("secsignid.messages.error.action.duplicate.plural2", duplicates.toString()) + " " + duplicates.toString());
            }
        }
    }

   
    /**
     * The method doDialog is called when the dialog shall be shown: http://localhost:2990/jira/secure/SecSignID!dialog.jsp
     * @return the name of the velocity template name defined in atlassian-plugin.xml
     */
    public String doDialog(){
        
        // get posted values and store them in local variables to make them available in velocity template
        // e.g. $jiraUserName
        
        HttpServletRequest httpRequest = getHttpRequest();
        logger.debug("doDialog / request parameter map: " + SecSignIDLogger.toString(httpRequest.getParameterMap()));
        
        try {
            //jiraUserName = URLDecoder.decode(httpRequest.getParameter("jirauser"), "utf-8");
            jiraUserName = URLDecoder.decode(httpRequest.getParameter("username"), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            // impossible. every vm must support UTF-8
        }
        
        
        
        ApplicationUser shownAppUser = SecSignIDUserManager.getApplicationUser(getUserManager(), jiraUserName);
        if(shownAppUser == null){
            jiraUserDisplayName = jiraUserName;
            secSignId = SecSignIDUsersActiveObject.getSecSignIdsStringForApplicationUserKey(ao, jiraUserName.toLowerCase());
            pwdAllowed = SecSignIDUsersActiveObject.getPasswordLoginAllowedForApplicationUserKey(ao, jiraUserName.toLowerCase());
            
            //addErrorMessage(i18nResolver.getText("secsignid.messages.error.action.noappuser", jiraUserName));
        } else {
        
            jiraUserDisplayName = shownAppUser.getDisplayName();
        
            secSignId = SecSignIDUsersActiveObject.getSecSignIdsStringForApplicationUser(ao, shownAppUser);
            pwdAllowed = SecSignIDUsersActiveObject.getPasswordLoginAllowedForApplicationUser(ao, shownAppUser);
        }
        referingUri = httpRequest.getHeader("referer");
        returnUrl = httpRequest.getHeader("referer"); // Yes, with the legendary misspelling.
        
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getUser();
        loggedInIsAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER, loggedInUser);
        
        // return name of dialog template
        return INPUT;
    }
    
    /**
     * Execute the post. This method is called only if doValidation() did not set any error
     */
    public String doExecute()
    {
        HttpServletRequest httpServletRequest = getHttpRequest();
        
        logger.debug("doExecute / request parameter map: " + SecSignIDLogger.toString(httpServletRequest.getParameterMap()));
        logger.debug("doExecute / request url: " + httpServletRequest.getRequestURL().toString());
        
        String sentSecSignId = httpServletRequest.getParameter("secsignid");
        String jiraUserKey = null;
        try {
            jiraUserKey = URLDecoder.decode(httpServletRequest.getParameter("jirauser"), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            // impossible. every vm must support UTF-8
        }
        
        String passWordAllowed = httpServletRequest.getParameter("pwd-allowed");        
        boolean isPwdLoginAllowed = true;
        if(passWordAllowed == null || !"1".equals(passWordAllowed)){
            isPwdLoginAllowed = false;
        }
        pwdAllowed = new Integer(isPwdLoginAllowed ? 1 : 0);
        
        ApplicationUser userToUpdate = SecSignIDUserManager.getApplicationUser(getUserManager(), jiraUserKey);
        
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getUser();
        loggedInIsAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER, loggedInUser);
        
        // convert secsign id string into array
        String[] secSignIds = SecSignIDUsersActiveObject.getArrayOfSecSignIds(sentSecSignId);
       
        HashMap<String, String[]> mapping = new HashMap<String, String[]>();
        mapping.put(userToUpdate.getKey(), secSignIds);
        
        // save jira user / secsign id mappings
        SecSignIDUsersActiveObject.saveSecSignIdUserMappings(ao, mapping);
        updatedSecSignIds = SecSignIDUsersActiveObject.getSecSignIdsString(secSignIds);
        
        // TODO. check whether admin is logged in?
        HashMap<String, Integer> pwdAllowedUserMapping = new HashMap<String, Integer>();
        pwdAllowedUserMapping.put(userToUpdate.getKey(), isPwdLoginAllowed ? SecSignIDConstants.PasswordLoginIsAllowed : SecSignIDConstants.PasswordLoginIsNotAllowed);
        
        // save options for certain jira user
        SecSignIDUsersActiveObject.savePasswordLoginIsAllowedUserMappings(ao, pwdAllowedUserMapping);
        
            
        // write update into audit...
        String auditSummary = "SecSign IDs updated";
        String eventUrl = "SecSignID!dialog.jspa";
        StringBuffer requestUrl = httpServletRequest.getRequestURL();
        
        logger.debug("request url: " + requestUrl);
        
        if(requestUrl.indexOf(SecSignIDConstants.JIRA_VIEW_USER_JSP_PATH) >= 0){
            eventUrl = SecSignIDConstants.JIRA_VIEW_USER_JSP_PATH.substring(0, 8);
        } else if(requestUrl.indexOf(SecSignIDConstants.JIRA_VIEW_PROFILE_JSP_PATH) >= 0){
            eventUrl = SecSignIDConstants.JIRA_VIEW_PROFILE_JSP_PATH.substring(0,11);
        }
        
        logger.debug("event url: " + eventUrl);
        
        // https://developer.atlassian.com/static/javadoc/jira/reference/com/atlassian/jira/auditing/AffectedUser.html
        AffectedUser affectedUser = new AffectedUser(userToUpdate);
        
        // https://developer.atlassian.com/static/javadoc/jira/reference/com/atlassian/jira/auditing/RecordRequest.html
        RecordRequest auditRecord = new RecordRequest(AuditingCategory.USER_MANAGEMENT, auditSummary, 
                eventUrl, loggedInUser, null).forObject(affectedUser);
        
        //
        // https://developer.atlassian.com/static/javadoc/jira/reference/com/atlassian/jira/auditing/AuditingManager.html
        AuditingManager auditMgr = ComponentAccessor.getComponent(AuditingManager.class);
        auditMgr.store(auditRecord);
        
        // https://developer.atlassian.com/static/javadoc/jira/reference/com/atlassian/jira/auditing/AuditingService.html
        /*AuditingService auditService = ComponentAccessor.getComponent(AuditingService.class);
        auditService.storeRecord(AuditingCategory.USER_MANAGEMENT.name(), 
               auditSummary,
               eventUrl,
               affectedUser, 
               null, // Iterable<ChangedValue> values,
               null); // Iterable<AssociatedItem> associatedItems*/
        
        // send complete with redirect
        String redirectUrl = httpServletRequest.getParameter(SecSignIDConstants.RETURN_URL_PARAM_NAME);
        if(redirectUrl == null){
            // add httpServletRequest.getContextPath() as prefix?
            // redirectUrl = httpServletRequest.getContextPath() + SecSignIDConstants.JIRA_VIEW_PROFILE_JSP_PATH + "?name=" + jiraUserKey;
            
            
            redirectUrl = SecSignIDConstants.JIRA_VIEW_PROFILE_JSP_PATH + "?name=" + jiraUserKey;
        }
        
        // go back to view, but the page will not be reloaded 
        return returnComplete(redirectUrl);
    }

    
    /*
     * 
     * local variables to have access from velocity template.
     * in the velocity template you can access the values via the names of the getter and setter:
     * 
     * e.g. getServiceName() and setServiceName(String)
     *      in the velocity template, you can access the value using: $serviceName
     *      also you can assign values which you can evaluate in the actions methods for the commands.
     *      
     * another chance might be, to get the context map of the underlying servlet. when you have the context map, you can push
     * the values into the map. they will be available in the template using the key-name of what was put into the context hashmap.
     * 
     * @see SecSignIDJiraUserInfoProvider#getContextMap()
     * 
     * @see https://developer.atlassian.com/static/javadoc/opensymphony-webwork/1.4-atlassian-17/reference/webwork/action/ActionContext.html
     */
    
    
    private String updatedSecSignIds;
    private String secSignId;
    private String jiraUserName;
    private String jiraUserDisplayName;
    private String referingUri;
    private String returnUrl;
    private Integer pwdAllowed;
    private Integer isPwdAllowedValue = SecSignIDConstants.PasswordLoginIsAllowed;
    private boolean loggedInIsAdmin;
    
    /**
     * @return the secSignId
     */
    public String getSecSignId() {
        return secSignId;
    }

    /**
     * @param secSignId the secSignId to set
     */
    public void setSecSignId(String secSignId) {
        this.secSignId = secSignId;
    }

    /**
     * @return the jiraUserName
     */
    public String getJiraUserName() {
        return jiraUserName;
    }

    /**
     * @param jiraUserName the jiraUserName to set
     */
    public void setJiraUserName(String jiraUserName) {
        this.jiraUserName = jiraUserName;
    }

    /**
     * @return the jiraUserDisplayName
     */
    public String getJiraUserDisplayName() {
        return jiraUserDisplayName;
    }

    /**
     * @param jiraUserDisplayName the jiraUserDisplayName to set
     */
    public void setJiraUserDisplayName(String jiraUserDisplayName) {
        this.jiraUserDisplayName = jiraUserDisplayName;
    }

    /**
     * @return the updatedSecSignIds
     */
    public String getUpdatedSecSignIds() {
        return updatedSecSignIds;
    }

    /**
     * @param updatedSecSignIds the updatedSecSignIds to set
     */
    public void setUpdatedSecSignIds(String updatedSecSignIds) {
        this.updatedSecSignIds = updatedSecSignIds;
    }

    /**
     * @return the referingUri
     */
    public String getReferingUri() {
        return referingUri;
    }

    /**
     * @param referingUri the referingUri to set
     */
    public void setReferingUri(String referingUri) {
        this.referingUri = referingUri;
    }

    /**
     * @return the referingUri
     */
    public String getReturnUrl() {
        return returnUrl;
    }


    /**
     * @param referingUri the referingUri to set
     */
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    /**
     * @return the loggedInIsAdmin
     */
    public boolean getLoggedInIsAdmin() {
        return loggedInIsAdmin;
    }

    /**
     * @param loggedInIsAdmin the loggedInIsAdmin to set
     */
    public void setLoggedInIsAdmin(boolean loggedInIsAdmin) {
        this.loggedInIsAdmin = loggedInIsAdmin;
    }

    /**
     * @return the pwdAllowed
     */
    public Integer getPwdAllowed() {
        return pwdAllowed;
    }

    /**
     * @param pwdAllowed the pwdAllowed to set
     */
    public void setPwdAllowed(Integer pwdAllowed) {
        this.pwdAllowed = pwdAllowed;
    }

    /**
     * @return the isPwdAllowedValue
     */
    public Integer getIsPwdAllowedValue() {
        return isPwdAllowedValue;
    }

    /**
     * @param isPwdAllowedValue the isPwdAllowedValue to set
     */
    public void setIsPwdAllowedValue(Integer isPwdAllowedValue) {
        this.isPwdAllowedValue = isPwdAllowedValue;
    }
}

