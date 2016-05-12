package com.secsign.jira.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.URL;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.login.LoginManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.BuildUtilsInfoImpl;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.seraph.auth.DefaultAuthenticator;
import com.atlassian.seraph.config.SecurityConfig;
import com.atlassian.seraph.config.SecurityConfigFactory;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.secsign.jira.SecSignIDConstants;
import com.secsign.jira.SecSignIDJiraException;
import com.secsign.jira.ao.SecSignIDUsersActiveObject;
import com.secsign.jira.servlet.filter.SecSignIDAuthenticationFilter;
import com.secsign.jira.util.SecSignIDLogger;
import com.secsign.jira.util.SecSignIDServerConnector;

/**
 * The SecSign ID authenticator. 
 * 
 * @version 1.0
 * @author SecSign Technologies Inc.
 * 
 * @see https://docs.atlassian.com/atlassian-seraph/latest/sso.html
 * @see https://confluence.atlassian.com/display/DEV/Single+Sign-on+Integration+with+JIRA+and+Confluence
 */
public class SecSignIDAuthenticator extends HttpServlet {
    /**
     * serial version uid
     */
    private static final long serialVersionUID = 4925328498249464512L;
    
    /**
     * logger instance for this class
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SecSignIDAuthenticator.class);

    
    /**
     * Name of the secsign id seraph filter
     */
    public static final String SECSIGNID_AUTHENTICATOR = "seraph_secsignid_authenticator_user";

    /**
     * Name of a session attribute which can contain the id server response
     */
    private static final String AUTHSESSION_KEY = "seraph_secsignid_authsession";
    

    // templates
    private String loginTemplate = "/templates/secsignid-login.vm";
    private String accessPassTemplate = "/templates/secsignid-accesspass.vm";
    

    /**
     * JIRA system application properties instance
     * 
     * @see https://docs.atlassian.com/jira/6.4.1/com/atlassian/jira/config/properties/ApplicationProperties.html
     */
    private final ApplicationProperties applicationProperties;
    
    /**
     * The template renderer
     * 
     * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/atlassian-template-renderer
     */
    private final TemplateRenderer templateRenderer;
    
    /**
     * Jiras authentication context to get the currently logged in user and his locales etc.
     * Here it is used to log in a certain application user to whom the secsign id belongs
     * 
     * @see https://docs.atlassian.com/jira/6.2.1/com/atlassian/jira/security/JiraAuthenticationContext.html
     * @see https://developer.atlassian.com/static/javadoc/jira/4.2.4/reference/com/atlassian/jira/security/JiraAuthenticationContext.html
     */
    private final JiraAuthenticationContext authenticationContext;
    
    /**
     * The I18n Resolver
     * @see https://docs.atlassian.com/sal-api/2.2.1/sal-api/apidocs/com/atlassian/sal/api/message/I18nResolver.html
     */
    private final I18nResolver i18nResolver;
    
    /**
     * The settings factory to retrieve properties and other information
     * 
     * @see https://docs.atlassian.com/sal-api/2.2.1/sal-api/apidocs/com/atlassian/sal/api/pluginsettings/PluginSettingsFactory.html
     * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/shared-access-layer/sal-code-samples
     * @see https://developer.atlassian.com/static/javadoc/sal/2.0/reference/com/atlassian/sal/api/pluginsettings/PluginSettingsFactory.html
     */
    private final PluginSettingsFactory pluginSettingsFactory;
    
    /**
     * The active objects instance
     * 
     * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/active-objects 
     */
    private final ActiveObjects ao;
    
    /**
     * User manager
     */
    private final UserManager userManager;
    
    /**
     * Constructor
     * @param templateRenderer the template renderer
     * @param authenticationContext the authentication context to check whether a user is logged.
     * @param pluginSettingsFactory the plugin factory for access to plugin settings
     * @param ao the active objects instance
     */
    public SecSignIDAuthenticator(ApplicationProperties applicationProperties, TemplateRenderer templateRenderer, I18nResolver i18nResolver, JiraAuthenticationContext authenticationContext, PluginSettingsFactory pluginSettingsFactory, ActiveObjects ao, UserManager userManager) {
        this.applicationProperties = applicationProperties;
        this.templateRenderer = templateRenderer;
        this.i18nResolver = i18nResolver;
        this.authenticationContext = authenticationContext;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.ao = ao;
        this.userManager = userManager;
    }
    
    
    
    /**
     * Handles a get request to servlet url
     */
    protected void doGet(HttpServletRequest httpServletReq, HttpServletResponse httpServletResp) throws IOException {
        
        // first check, whether a user is already logged in... redirect to dashboard
        HttpSession session = httpServletReq.getSession();
        Object appUserObject = session.getAttribute(DefaultAuthenticator.LOGGED_IN_KEY);
        UserProfile userProfile = null;
        if(appUserObject == null){
            userProfile = userManager.getRemoteUser(httpServletReq);
        }
        
        if(appUserObject != null || userProfile != null){
            // a user is already logged in. redirect to base url
            httpServletResp.sendRedirect(SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq));
            return;
        }
        
      
        httpServletResp.setContentType("text/html;charset=utf-8");
        
        // create context map which is available in templates
        HashMap<String, Object> context = getAuthenticatorContextMap(httpServletReq);
        HashMap<String, Object> authsession = (HashMap<String, Object>) session.getAttribute(AUTHSESSION_KEY);
        if(authsession != null){
            // add authsession to context
            context.putAll(authsession);
            
            // render template for access path
            this.templateRenderer.render(accessPassTemplate, context, httpServletResp.getWriter());
        } else {
            this.templateRenderer.render(loginTemplate, context, httpServletResp.getWriter());
        }
    }
    
    
    
    /**
     * handles a post request to servlet
     */
    protected void doPost(HttpServletRequest httpServletReq, HttpServletResponse httpServletResp) throws IOException {
        httpServletResp.setContentType("text/html;charset=utf-8");
        
        // create context map which is available in templates
        HashMap<String, Object> context = getAuthenticatorContextMap(httpServletReq);
        HashMap<String, Object> existingAuthSession = (HashMap<String, Object>) httpServletReq.getSession().getAttribute(AUTHSESSION_KEY);
        
        logger.debug("httpServletReq.getPathInfo()=" + httpServletReq.getPathInfo());
        logger.debug(SecSignIDLogger.toString(httpServletReq.getParameterMap()));
        
        if(httpServletReq.getParameter(SecSignIDConstants.SECSIGNID_LOGIN_PARAM_NAME) != null){
            
            // secsign id whom the session shall be created
            String secSignId = httpServletReq.getParameter("secsignid");
            
            
            if(existingAuthSession != null){
                // we found an existing authentication session.
                // use this rather than getting a new one because the user might have clicked reload at the browser
                
                String secSignIdInExistingSession = (String) existingAuthSession.get("secsignid");
                if(secSignIdInExistingSession.equals(secSignId)){

                    // is the secsign id still the same?
                        
                    // {authsessionid=6821477223339041056, 
                    //  servicename=Jira, 
                    //  secsignid=titus, 
                    //  serviceaddress=localhost, 
                    //  authsessionstate=1, 
                    //  requestid=ADDE6E13ECD78B371F474E53003B1B6C4E55F5384BEF5C041530E66E2A609F98, 
                    //  authsessionicondata=iVBORw0KGgoAAAANSUhEUgAAAFgAAABYCAYAAABxlTA0AAAAGXRFWHR.....}
    
                    // TODO: do some further checks? is the authentication session still active?
                    
                    context.putAll(existingAuthSession);
                       
                    // render template for access path
                    this.templateRenderer.render(accessPassTemplate, context, httpServletResp.getWriter());
                    
                    return;
                }
            }
            
            
            /*
             * the user hit the secsign id login form.
             * when the form is submitted, the secsign id and other values are posted.
             * 
             * check whether secsign id is empty or has illegal characters.
             * after that check whether secsign id is assigned to a jira user.
             * then send to secsign id server to request an authentication session.
             */
            boolean secSignIdCheckSuccessFull = true;
            
            // check whether a secsign id was given. otherwise the request to the id server is unnecessary
            if(secSignId == null || secSignId.length() < 1){
                context.put("errormsg", i18nResolver.getText("secsignid.messages.error.nosecsignid"));
                context.put("errormsgkey", "secsignid.messages.error.nosecsignid");
                
                secSignIdCheckSuccessFull = false;
            } else {
                // check whether there are some illegal characters. if so return with an error message because the request to the id server is unnecessary
                if(SecSignIDUsersActiveObject.secSignIdContainsIllegalCharacters(secSignId)){
                    context.put("errormsg", i18nResolver.getText("secsignid.messages.error.illegalcharacters"));
                    context.put("errormsgkey", "secsignid.messages.error.illegalcharacters");
                    
                    secSignIdCheckSuccessFull = false;
                    
                    logger.error("Cannot request authentication session for '" + secSignId + "' because the secsign contains illegal characters.");
                }
            }
            
            // check whether secsign id belongs to jira user
            ApplicationUser applicationUserOfSecSignId = SecSignIDUsersActiveObject.getApplicationUserForSecSignId(ao, secSignId);
            if(applicationUserOfSecSignId == null){
             
                // better to have a non descriptive error message? so some investigator will not figure out that a certain secsign id exists but is not assigned to a certain jira user on that system
                context.put("errormsg", i18nResolver.getText("secsignid.messages.error.nosecsignidassigned") + " '" + secSignId + "'.");
                context.put("errormsgkey", "secsignid.messages.error.nosecsignidassigned");
                
                secSignIdCheckSuccessFull = false;
                
                logger.error("Cannot request authentication session for '" + secSignId + "' because the secsign id is not assigned to any jira user.");
            }
            
            
            // @see https://confluence.atlassian.com/jira/managing-users-185729439.html#ManagingUsers-Deactivatingauser
            // @see https://docs.atlassian.com/jira/6.4.1/com/atlassian/jira/user/ApplicationUser.html
            if(applicationUserOfSecSignId != null && !applicationUserOfSecSignId.isActive()){
                
                // better to have a non descriptive error message? so some investigator will not figure out that a certain jira user is not active 
                context.put("errormsg", i18nResolver.getText("secsignid.messages.error.noactiveuser", secSignId));
                context.put("errormsgkey", "secsignid.messages.error.noactiveuser");
                
                secSignIdCheckSuccessFull = false;
                
                logger.error("Cannot request authentication session for '" + secSignId + "' because the jira user with the assigned secsign id is not active.");
            }
            
            
            // check of secsign id and jira users was not successfull. some error was found.
            if(!secSignIdCheckSuccessFull){
                // render login form. error messages have been put to context before
                this.templateRenderer.render(loginTemplate, context, httpServletResp.getWriter());
                
                // nothing else to do.
                return;
            }
           
            
            // get service name of plugin settings. this will take some time unfortunatly
            String serviceName = (String) pluginSettingsFactory.createSettingsForKey(SecSignIDConstants.SecSignIDSettingsKey).get(SecSignIDConstants.ServiceNameOptionsKey);
            String serviceAddress = SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq);
            String secSignIDServerUrl = SecSignIDServerConnector.getSecSignIdServerUrl(pluginSettingsFactory);
            String jiraVersion = new BuildUtilsInfoImpl().getVersion();
            
            if(serviceName == null || serviceName.length() < 1){
                serviceName = this.applicationProperties.getString("jira.title");
            }
            
            logger.info("create a new authentication session for '" + httpServletReq.getParameter("secsignid") + "' and service '" + serviceName + "' at server '" + secSignIDServerUrl + "'.");
            
            // create hashmap of all parameter used for the "normal" secsign id api which uses url encoded parameter lists
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("secsignid", secSignId);
            params.put("request", "ReqRequestAuthSession");
            params.put("serviceaddress", serviceAddress);
            params.put("servicename", serviceName);
            params.put("pluginname", "JIRA - " + jiraVersion);
            
            HashMap<String, Object> response = null;
            try{
                // put id server url to context. in those cases it can be used in velocity templates: $idserverurl
                context.put("idserverurl", secSignIDServerUrl);
                
                response = SecSignIDServerConnector.sendRequestToSecSignIdServer(params, secSignIDServerUrl);
                
                // an error response will ,ook like:
                // {error=500, errormsg=A SecSign ID does not exist with that name., errorcode=20000}
                if(response.containsKey("error")){
                    
                    // just do debug logging because due to the later thrown exception this will be logged anyway
                    logger.error("ERROR: got error from id server: " + response.toString());
                    
                    String errorMsg = (String)response.get("errormsg");
                    int errorCode = -1;
                    try{
                        errorCode = Integer.parseInt((String) response.get("errorcode"));
                    } catch(NumberFormatException ex){
                        // empty. does not matter
                    }
                    
                    // for most of the error codes, see ....secappserver.mobile.SecSignIDException
                    if(errorCode == 20000){
                        throw new SecSignIDJiraException(errorMsg, i18nResolver.getText("secsignid.messages.error.secsignid.notexisting"), errorCode);    
                    } else if(errorCode == 20006){
                        // This SecSign ID is frozen due to concurrent login requests. The user needs to reactivate his account first. This can be done by tapping on the right SecSign ID at the smartphone.
                        throw new SecSignIDJiraException(errorMsg, i18nResolver.getText("secsignid.messages.error.secsignid.frozen"), errorCode);
                    }
                    
                    // delegate error
                    throw new SecSignIDJiraException(errorMsg, errorMsg, errorCode);
                }
                
                
                
                // format secsign id serverurl
                URL secSignIDServerUrlObj = new URL(secSignIDServerUrl);
                String idServerUrlForMobileRequests = secSignIDServerUrlObj.getAuthority();
                if(SecSignIDConstants.DefaultSecSignIDServer.equals(secSignIDServerUrl)){
                    idServerUrlForMobileRequests = idServerUrlForMobileRequests.replaceAll("httpapi", "id1");
                }
                
                response.put("idserverurl", idServerUrlForMobileRequests);
                
                // {authsessionid=6821477223339041056, 
                //  servicename=Jira, 
                //  secsignid=titus, 
                //  serviceaddress=localhost, 
                //  authsessionstate=1, 
                //  requestid=ADDE6E13ECD78B371F474E53003B1B6C4E55F5384BEF5C041530E66E2A609F98, 
                //  authsessionicondata=iVBORw0KGgoAAAANSUhEUgAAAFgAAABYCAYAAABxlTA0AAAAGXRFWHR.....}

                
                // add response to context
                context.putAll(response);
                
                // save response in session object
                HttpSession session = httpServletReq.getSession();
                session.setAttribute(AUTHSESSION_KEY, response);
                
                // render template for access path
                this.templateRenderer.render(accessPassTemplate, context, httpServletResp.getWriter());
                
                return;
            } catch(ConnectException ex){
                logger.error(ex.getMessage(), ex);
                
                // an error happend. probably we need to go back...
                context.put("errormsg", ex.getMessage());
                context.put("errormsgkey", "secsignid.messages.error.connection");
            }
            catch(SecSignIDJiraException ex){
                logger.error(ex.getClass().getName() + ": " + ex.getMessage());
                
                // an error happend. probably we need to go back...
                context.put("errormsg", ex.getI18nMessage());
            }
            catch(Exception ex){
                logger.error(ex.getMessage(), ex);
                
                // an error happend. probably we need to go back...
                context.put("errormsg", ex.getMessage());
            }
        } else if(httpServletReq.getParameter("secsignid-cancel-button") != null){
            
            String secSignIDServerUrl = SecSignIDServerConnector.getSecSignIdServerUrl(pluginSettingsFactory);
            
            logger.info("cancel authentication session for '" + httpServletReq.getParameter("secsignid") + "' at server '" + secSignIDServerUrl + "'.");
            
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("secsignid", httpServletReq.getParameter("secsignid"));
            params.put("request", "ReqCancelAuthSession");
            params.put("requestid", httpServletReq.getParameter("requestid"));
            params.put("authsessionid", httpServletReq.getParameter("authsessionid"));
            
            HashMap<String, Object> response = null;
            try{
                
                // cancel authentication session
                response = SecSignIDServerConnector.sendRequestToSecSignIdServer(params, secSignIDServerUrl);
                if(response.containsKey("error")){
                    throw new Exception((String)response.get("errormsg"));
                }
                
                // session was canceled
                //context.put("warnmsg", "Authentication session was canceled");
                context.put("warnmsg", i18nResolver.getText("secsignid.messages.warning.canceled"));
                context.put("warnmsgkey", "secsignid.messages.warning.canceled");
            }
            catch(Exception ex){
                ex.printStackTrace();
                
                // an error happend. probably we need to go back...
                context.put("errormsg", ex.getMessage());
            }
            
            // save response in session object
            HttpSession session = httpServletReq.getSession();
            session.setAttribute(AUTHSESSION_KEY, null);
            
        } else if(httpServletReq.getParameter("secsignid-checkauthsession") != null){
            // check authentication session
            String secSignIDServerUrl = SecSignIDServerConnector.getSecSignIdServerUrl(pluginSettingsFactory);
            HttpSession session = httpServletReq.getSession();
            
            logger.debug("check authentication session for '" + httpServletReq.getParameter("secsignid") + "' at server '" + secSignIDServerUrl + "'.");
            
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("secsignid", httpServletReq.getParameter("secsignid"));
            params.put("request", "ReqGetAuthSessionState");
            params.put("requestid", httpServletReq.getParameter("requestid"));
            params.put("authsessionid", httpServletReq.getParameter("authsessionid"));
            
            HashMap<String, Object> response = SecSignIDServerConnector.sendRequestToSecSignIdServer(params, secSignIDServerUrl);
            
            // user hit check auth session button
            int sessionState = SecSignIDServerConnector.SESSION_STATE_NOSTATE;
            int oldSessionState = SecSignIDServerConnector.SESSION_STATE_PENDING;
            try{
                sessionState = Integer.parseInt((String) response.get("authsessionstate"));
                
                // actually impossible that existingAuthSession is null... but just to ensure no unexpected exception will occur
                if(existingAuthSession != null){
                    oldSessionState = Integer.parseInt((String) existingAuthSession.get("authsessionstate"));
                }
            } catch(NumberFormatException ex){
                logger.error("server sent unparsable session state: " + ex.getMessage());
            }
            
            
            // log when session state changes
            // if(sessionState != SecSignIDServerConnector.SESSION_STATE_PENDING){
            if(sessionState != oldSessionState){
                logger.info("authentication session changed state to " + sessionState + " at server '" + secSignIDServerUrl + "'.");
                
                // actually impossible that existingAuthSession is null... but just to ensure no unexpected exception will occur
                if(existingAuthSession != null){
                    existingAuthSession.put("authsessionstate", String.valueOf(sessionState));
                }
            }
            
            if("url".equals(httpServletReq.getParameter("responseformat"))){
                // javascript api which polls
                String urlEncodedParameterList = SecSignIDServerConnector.createResponseString(response);
                
                // write straight to socket outputstream
                PrintWriter pw = httpServletResp.getWriter();
                pw.write(urlEncodedParameterList);
                pw.flush();
                
                return;
            }
            
            if(sessionState == SecSignIDServerConnector.SESSION_STATE_AUTHENTICATED){
                
                // user has authenticated itself via mobile app
                ApplicationUser appUser = SecSignIDUsersActiveObject.getApplicationUserForSecSignId(ao, (String)response.get("secsignid"));
                
                // log in user!
                if(appUser != null){
                    logger.info("auth session authenticated, login secsign id '" + response.get("secsignid") + "' assigned to app user '" + appUser.getUsername() + "'");
                   
                    // this logs in the user
                    if(this.loginApplicationUser(appUser, httpServletReq)){
                   
                        // check whether we shall do a redirect
                        String returnUrl = httpServletReq.getParameter(SecSignIDConstants.RETURN_URL_PARAM_NAME);
                        String osDestination = httpServletReq.getParameter(SecSignIDConstants.JIRA_OS_DESTINATION_PARAM_NAME);
                        
                        if(returnUrl != null && !returnUrl.equals(SecSignIDConstants.JIRA_LOGIN_JSP_PATH)){
                            if(returnUrl.startsWith(SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq))){
                                httpServletResp.sendRedirect(returnUrl);
                            } else {
                                httpServletResp.sendRedirect(SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq) + returnUrl);
                            }
                        }
                        else if(osDestination != null && !osDestination.equals(SecSignIDConstants.JIRA_LOGIN_JSP_PATH)){
                            if(osDestination.startsWith(SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq))){
                                httpServletResp.sendRedirect(osDestination);
                            } else {
                                httpServletResp.sendRedirect(SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq) + osDestination);
                            }
                        }
                        else {
                            httpServletResp.sendRedirect(SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq));
                        }
                        
                        // sent redirect. nothing else to do.
                        return;
                    }
                    
                    
                    context.put("errormsg", i18nResolver.getText("secsignid.messages.error.user.nologin") + " '" + response.get("secsignid") + "'.");
                    context.put("errormsgkey", "secsignid.messages.error.user.nologin");
                } else {
                
                    //context.put("errormsg", "There is no jira user with the assigned secsign id '" + response.get("secsignid") + "'.");
                    context.put("errormsg", i18nResolver.getText("secsignid.messages.error.nosecsignidassigned") + " '" + response.get("secsignid") + "'.");
                    context.put("errormsgkey", "secsignid.messages.error.nosecsignidassigned");
                }
                
                // no need to keep auth session any longer
                session.setAttribute(AUTHSESSION_KEY, null);
                
            } else {
                boolean isStillPendingOrFetched = false;
                switch(sessionState){
                    case SecSignIDServerConnector.SESSION_STATE_PENDING:
                    case SecSignIDServerConnector.SESSION_STATE_FETCHED:
                        // no errror case.
                        isStillPendingOrFetched = true;
                        break;
                        
                        
                    case SecSignIDServerConnector.SESSION_STATE_SUSPENDED:
                        //context.put("errormsg", "The SecSign ID Server has retracted the access pass for security reasons.");
                        context.put("errormsg", i18nResolver.getText("secsignid.messages.error.session.suspended"));
                        context.put("errormsgkey", "secsignid.messages.error.session.suspended");
                        break;
                    case SecSignIDServerConnector.SESSION_STATE_DENIED:
                        //context.put("errormsg", "You have denied the access pass.");
                        context.put("errormsg", i18nResolver.getText("secsignid.messages.error.session.denied"));
                        context.put("errormsgkey", "secsignid.messages.error.session.denied");
                        break;
                    case SecSignIDServerConnector.SESSION_STATE_CANCELED:
                        //context.put("warnmsg", "Authentication session was canceled");
                        context.put("errormsg", i18nResolver.getText("secsignid.messages.warning.canceled"));
                        context.put("warnmsgkey", "secsignid.messages.warning.canceled");
                        break;
                    case SecSignIDServerConnector.SESSION_STATE_EXPIRED:
                      //context.put("errormsg", "Authentication session is expired");
                        context.put("errormsg", i18nResolver.getText("secsignid.messages.error.session.expired"));
                        context.put("errormsgkey", "secsignid.messages.error.session.expired");
                        break;
                        
                        
                    case SecSignIDServerConnector.SESSION_STATE_NOSTATE: // fall through
                    case SecSignIDServerConnector.SESSION_STATE_INVALID:
                    default:
                        //context.put("errormsg", "Authentication session is expired");
                        context.put("errormsg", i18nResolver.getText("secsignid.messages.error.session.invalid"));
                        context.put("errormsgkey", "secsignid.messages.error.session.invalid");
                        break;
                }
                
                if(! isStillPendingOrFetched){
                    // no need to keep auth session any longer. because auth session is now invalid
                    session.setAttribute(AUTHSESSION_KEY, null);
                }
            }
        }
        
        this.templateRenderer.render(loginTemplate, context, httpServletResp.getWriter());
    }


    /**
     * Gets a pre-initialized context map
     * @param httpServletReq the servlet reuqest to get parameter which are stored in the context map
     * @return the context map
     */
    private HashMap<String, Object> getAuthenticatorContextMap(HttpServletRequest httpServletReq) {
        
        SecurityConfig securityConfig = SecurityConfigFactory.getInstance();
        HashMap<String, Object> context = new HashMap<String, Object>();
        
        // the request object itself
        context.put("req", httpServletReq);
        
        String returnUrl = httpServletReq.getParameter(SecSignIDConstants.RETURN_URL_PARAM_NAME);
        
        // e.g. http://localhost:2990/jira/login.jsp?permissionViolation=true&amp;os_destination=${originalurl}&amp;page_caps=&amp;user_role="
        String jiraLoginUrl = SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq) + securityConfig.getLoginURL();
        
        if(returnUrl != null && returnUrl.contains("/mobile")){
            
            jiraLoginUrl = SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq) + "/plugins/servlet/mobile#login/myjirahome";
        }
        
        context.put(SecSignIDConstants.RETURN_URL_PARAM_NAME, returnUrl);
        context.put(SecSignIDConstants.JIRA_LOGIN_URL_PARAM_NAME, jiraLoginUrl);
        context.put(SecSignIDConstants.JIRA_OS_DESTINATION_PARAM_NAME, returnUrl);
        context.put("originalurl", returnUrl);
       
        
        return context;
    }


    /**
     * Logs in a given user. The user will be stored as attribute of the reuqest session
     * @param appUser the application user which shall be loged in
     * @param request the servlet request object instance
     */
    private boolean loginApplicationUser(final ApplicationUser appUser, HttpServletRequest request){
        /*
        LoginService loginService = ComponentAccessor.getComponent(LoginService.class);
        // ApplicationUser user = ComponentAccessor.getUserUtil().getUserByKey("....");
       
        // https://answers.atlassian.com/questions/8624588/how-to-login-user-programmatically
        loginService.resetFailedLoginCount(appUser.getDirectoryUser());
        LoginResult loginResult = loginService.authenticate(appUser.getDirectoryUser(), null);
        log.log("loginResult=" + loginResult.getReason());
        */
        
        // log.log("login application user '" + appUser.getKey() + "'.");
        
        //com.atlassian.seraph.auth.RoleMapper roleMapper = SecurityConfigFactory.getInstance().getRoleMapper();
        //if(roleMapper.canLogin(appUser, request)){
        
            // first, login user in jiras authentication content
            authenticationContext.setLoggedInUser(appUser);
            
            HttpSession session = request.getSession();
            
            // set attributes in request session. this actually logs in user
            session.setAttribute(SecSignIDAuthenticator.SECSIGNID_AUTHENTICATOR, appUser);
            session.setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, appUser);
            session.setAttribute(DefaultAuthenticator.LOGGED_OUT_KEY, null);

            // get login manager and "login" user to update the login details
            // @see https://developer.atlassian.com/static/javadoc/jira/reference/com/atlassian/jira/security/login/LoginManager.html
            // @https://docs.atlassian.com/jira/5.0.6/com/atlassian/jira/security/login/LoginManagerImpl.html
            LoginManager loginManager = ComponentAccessor.getComponent(LoginManager.class);
            loginManager.onLoginAttempt(request, appUser.getKey(), true);
            
            return true;
        //}
        //
        //
        //return false;
    }
}
