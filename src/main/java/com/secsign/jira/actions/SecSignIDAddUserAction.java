package com.secsign.jira.actions;

import javax.servlet.http.HttpServletRequest;

import webwork.action.Action;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.util.BuildUtilsInfoImpl;
import com.atlassian.sal.api.message.I18nResolver;
import com.secsign.jira.SecSignIDConstants;
import com.secsign.jira.util.SecSignIDLogger;

/**
 * @see http://www.j-tricks.com/tutorials/extending-jira-actions
 * @author SecSign Technologies Inc.
 * 
 * @see https://docs.atlassian.com/jira/6.4.1/com/atlassian/jira/web/action/admin/user/AddUser.html
 */
//public class SecSignIDAddUserAction extends com.atlassian.jira.web.action.admin.user.AddUser {
public class SecSignIDAddUserAction extends SecSignIDCreateJiraUserAction {

    // https://developer.atlassian.com/jiradev/jira-platform/guides/issues/tutorial-displaying-content-in-a-dialog-in-jira

    /**
     * 
     */
    private static final long serialVersionUID = 5944289662765765958L;
    
    
    // TODO: what is missing: @see ./target/jira/webapp/secure/admin/user/views/adduser.jsp
    
    /**
     * Does the password can be set?
     * @see ./target/jira/webapp/secure/admin/user/views/adduser.jsp
     * 
     * <ww:if test="/hasPasswordWritableDirectory == true">
     *       <ui:soy moduleKey="'com.atlassian.auiplugin:aui-experimental-soy-templates'" template="'aui.form.passwordField'">
     *           <ui:param name="'labelContent'"><ww:text name="'common.words.password'" /></ui:param>
     *           <ui:param name="'id'">password</ui:param>
     *           <ui:param name="'placeholderText'"><ww:text name="'admin.adduser.password.placeholder'"/></ui:param>
     *           <ui:param name="'autocomplete'">off</ui:param>
     *           <ui:param name="'infoMessage'"><ww:text name="'admin.adduser.if.you.do.not.enter.a.password'"/></ui:param>
     *           <ui:param name="'errorTexts'" value="/passwordErrors" />
     *       </ui:soy>
     * </ww:if>
     */
    private boolean hasPasswordWritableDirectory = true;
    
   
    /**
     * TODO: get the selectable applications. 
     * 
     * in JIRA 7 only application user exist than in JIRA 6 where application user and directory user could have been created
     * in JIRA 7 when creating a new user, the user can be put into a default group like jira-users. Then the new user will be assigned all applications automatically which belong to this group
     * 
     * <ww:if test="/selectableApplications/size > 0">
     *       <ui:soy moduleKey="'jira.webresources:application-selector'" template="'JIRA.Templates.ApplicationSelector.applicationSelector'">
     *           <ui:param name="'legend'" value="text('admin.adduser.application.selection.heading')" />
     *           <ui:param name="'selectableApplications'" value="/selectableApplications" />
     *           <ui:param name="'additionalClasses'">group</ui:param>
     *       </ui:soy>
     * </ww:if>
     * 
     * @see https://docs.atlassian.com/jira/latest/com/atlassian/jira/bc/user/UserApplicationHelper.ApplicationSelection.html
     */
    //private java.util.List<com.atlassian.jira.bc.user.UserApplicationHelper.ApplicationSelection> selectableApplications = null; // UserApplicationHelper can not be found in JIRA 6.4.4
    
    
    /**
     * When the process of adding a new user is canceled where to redirect to?
     */
    private String cancelUrl = SecSignIDConstants.JIRA_USER_BROWSER_JSP_PATH;
    
    /**
     * Constructor
     * @param userService
     * @param userUtil
     * @param userManager
     * @param webInterfaceManager
     * @param eventPublisher
     * @throws Exception 
     */
    /*public SecSignIDAddUserAction(UserService userService, 
                                  UserUtil userUtil, 
                                  UserManager userManager, 
                                  WebInterfaceManager webInterfaceManager, 
                                  EventPublisher eventPublisher,
                                  ActiveObjects ao, 
                                  I18nResolver i18nResolver) {
        super(userService, userUtil, userManager, webInterfaceManager, eventPublisher);
        this.ao = ao;
        this.i18nResolver = i18nResolver;
    }*/
    /*public SecSignIDAddUserAction(UserService userService,
            UserManager userManager, WebInterfaceManager webInterfaceManager,
            EventPublisher eventPublisher,
            ApplicationRoleManager roleManager,
            
            ActiveObjects ao, 
            I18nResolver i18nResolver) {
        super(userService, userManager, webInterfaceManager, eventPublisher, ComponentAccessor.getComponent(CreateUserApplicationHelper.class), roleManager);
        
        this.userUtil = null;
        this.ao = ao;
        this.i18nResolver = i18nResolver;
    }*/
    public SecSignIDAddUserAction(ActiveObjects ao, I18nResolver i18nResolver) {
        super(ao, i18nResolver);
    }
    
    @Override
    public String doExecute()
    {
        try {
            super.doExecute();
        } catch (Exception ex) {
            SecSignIDLogger.log(ex.getMessage());
            
            addErrorMessage(ex.getMessage());
        }
        
        HttpServletRequest httpServletRequest = getHttpRequest();
        
        // do we want to create another user immediatly after this?
        if (httpServletRequest.getParameter("createAnother") != null){
            
            secsignid = null;
            username = null;
            username = null;
            email = null;
            password = null;
            
            return Action.INPUT;
        }
        
        String jiraVersion = new BuildUtilsInfoImpl().getVersion();
        String redirectUrl = httpServletRequest.getParameter(SecSignIDConstants.RETURN_URL_PARAM_NAME);
        if(redirectUrl == null){
            redirectUrl = httpServletRequest.getContextPath() + SecSignIDConstants.JIRA_USER_BROWSER_JSP_PATH;
        }
        
        
        if(jiraVersion.startsWith("6.") && httpServletRequest.getParameter("inline") != null){
            // return Action.SUCCESS;

            // create redirect to user browser
            // return returnCompleteWithInlineRedirect(SecSignIDConstants.JIRA_USER_BROWSER_JSP_PATH);
            return returnComplete(redirectUrl);
        }
        
        
        // go back to user overview
        return getRedirect(redirectUrl);
    }
    
    /**
     * @return the hasPasswordWritableDirectory
     */
    public boolean isHasPasswordWritableDirectory() {
        return hasPasswordWritableDirectory;
    }

    /**
     * @param hasPasswordWritableDirectory the hasPasswordWritableDirectory to set
     */
    public void setHasPasswordWritableDirectory(boolean hasPasswordWritableDirectory) {
        this.hasPasswordWritableDirectory = hasPasswordWritableDirectory;
    }

    /**
     * @return the cancelUrl
     */
    public String getCancelUrl() {
        return cancelUrl;
    }

    /**
     * @param cancelUrl the cancelUrl to set
     */
    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }
}
