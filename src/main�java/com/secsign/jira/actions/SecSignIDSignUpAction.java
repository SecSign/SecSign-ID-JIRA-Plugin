package com.secsign.jira.actions;

import java.util.List;

import webwork.action.Action;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import com.secsign.jira.SecSignIDConstants;

/**
 * @see http://www.j-tricks.com/tutorials/extending-jira-actions
 * @author SecSign Technologies Inc.
 * 
 * @see https://docs.atlassian.com/jira/2.6/com/atlassian/jira/web/action/user/package-tree.html
 * @see https://docs.atlassian.com/software/jira/docs/api/5.2.11/com/atlassian/jira/web/action/user/class-use/Signup.html
 */
//public class SecSignIDSignUpAction extends com.atlassian.jira.web.action.user.Signup {
public class SecSignIDSignUpAction extends SecSignIDCreateJiraUserAction {

    /**
     * 
     */
    private static final long serialVersionUID = -2743380391930736768L;
    
    /**
     * logger instance for this class
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SecSignIDSignUpAction.class);
    
    // https://docs.atlassian.com/software/jira/docs/api/5.2.11/com/atlassian/jira/web/action/user/Signup.html
    // constructor:
    //      Signup(ApplicationProperties applicationProperties, UserService userService, UserUtil userUtil, JiraCaptchaService jiraCaptchaService, ExternalLinkUtil externalLinkUtil)
    
    
    // https://docs.atlassian.com/jira/6.2.7/com/atlassian/jira/web/action/user/Signup.html
    // constructor: 
    //      Signup(ApplicationProperties applicationProperties, UserService userService, UserUtil userUtil, JiraCaptchaService jiraCaptchaService, ExternalLinkUtil externalLinkUtil)
    
    
    // https://docs.atlassian.com/jira/7.0.4/com/atlassian/jira/web/action/user/Signup.html
    // constructor:
    //      Signup(ApplicationProperties applicationProperties, UserService userService, 
    //                      JiraCaptchaService jiraCaptchaService, ExternalLinkUtil externalLinkUtil, 
    //                      CreateUserApplicationHelper applicationHelper, ApplicationRoleManager roleManager, 
    //                      UserUtil userUtil, com.atlassian.webresource.api.assembler.PageBuilderService pageBuilderService) 


    private static final String ACTION_URL = "/secure/Signup!default.jspa";

    private static final String TEMPLATE_USER_ALREADY_LOGGED_IN = "alreadyloggedin";
    
    
    /*static {
        java.lang.reflect.Constructor[] cons = com.atlassian.jira.web.action.user.Signup.class.getConstructors();
        if(cons != null){
           for(java.lang.reflect.Constructor c : cons){
               System.out.println(c.toString());
           }
        }
    }*/
    
    // TODO: things which are missing, see ./target/container/tomcat7x/cargo-jira-home/webapps/jira/views/signup.jsp
    
    /**
     * When a user signs up, check the password. If something is wrong with the password, store all error messages in this list. it will be checked.
     * 
     * <page:applyDecorator name="auifieldgroup">
     *      <ww:if test="/passwordErrors/size > 0"><ul class="error"><ww:iterator value="/passwordErrors">
     *          <li><ww:property value="./snippet" escape="false"/></li>
     *      </ww:iterator></ul></ww:if>
     * </page:applyDecorator>
     */
    private List<String> passwordErrors;
    
    
    /**
     * Constructor
     * @param userService
     * @param userUtil
     * @param userManager
     * @param webInterfaceManager
     * @param eventPublisher
     */
    /*public SecSignIDSignUpAction(ApplicationProperties applicationProperties, 
            UserService userService, 
            UserUtil userUtil, 
            ActiveObjects ao, 
            I18nResolver i18nResolver) {
            super(applicationProperties, userService, userUtil, new JiraCaptchaServiceImpl(), ExternalLinkUtilImpl.getInstance());
        this.ao = ao;
        this.i18nResolver = i18nResolver;
    }*/
    /*public SecSignIDSignUpAction(ApplicationProperties applicationProperties,
            UserService userService,
            ApplicationRoleManager roleManager, 
            UserUtil userUtil,

            ActiveObjects ao, 
            I18nResolver i18nResolver) {
        
        // calling this constructor will be compiled but at runtime a org.springframework.beans.factory.BeanCreationException exception.
        // nested exception is java.lang.NoSuchMethodError: com.atlassian.jira.web.action.user.Signup...
        super(applicationProperties, userService, 
                ComponentAccessor.getComponent(JiraCaptchaService.class), 
                ComponentAccessor.getComponent(ExternalLinkUtil.class), 
                ComponentAccessor.getComponent(CreateUserApplicationHelper.class), 
                roleManager, userUtil, 
                ComponentAccessor.getComponent(PageBuilderService.class));

        
        this.ao = ao;
        this.i18nResolver = i18nResolver;
    }*/
    
    public SecSignIDSignUpAction(ActiveObjects ao,  I18nResolver i18nResolver) {
        super(ao, i18nResolver);
    }
    
    
    @Override
    public String doExecute()
    {
        /*
         <action name="com.secsign.jira.actions.SecSignIDSignUpAction" alias="Signup">
            <view name="success">/views/signup-success.jsp</view>
            <view name="limitexceeded">/views/signup-limitexceeded.jsp</view>
            <view name="alreadyloggedin">/views/signup-alreadyloggedin.jsp</view>
            <view name="error">/views/signup.jsp</view>
            <view name="input">/views/signup.jsp</view>
            <view name="modebreach">/views/modebreach.jsp</view>
            <view name="systemerror">/views/signup-systemerror.jsp</view>
        </action>
         */
        
        // 1) check whether a user is logged in. than no signup can be done.

        // since JIRA 7, getUser is deprecated: https://docs.atlassian.com/jira/7.0.4/com/atlassian/jira/security/JiraAuthenticationContext.html#getLoggedInUser--
        // the problem is to fit JIRA 6 compliance where getLoggedInUser() does not exists returning ApplicationUser
        // see warning at https://docs.atlassian.com/jira/7.0.4/com/atlassian/jira/security/JiraAuthenticationContext.html#getLoggedInUser--
        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getUser();
        if(loggedInUser != null){
            // a user is logged in. no sign up possible
            
            // TODO: first check whether those templates exists.
            // return TEMPLATE_USER_ALREADY_LOGGED_IN;
            
            return Action.ERROR;
        }
        
        
        try {
            super.doExecute();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            
            addErrorMessage(ex.getMessage());
        }
        
        // create redirect to default.
        return getRedirect(SecSignIDConstants.JIRA_DEFAULT_JSP_PATH);
    }
}
