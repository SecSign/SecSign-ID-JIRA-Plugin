package com.secsign.jira.servlet.filter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.util.JiraUrlCodec;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.seraph.auth.DefaultAuthenticator;
import com.secsign.jira.SecSignIDConstants;
import com.secsign.jira.util.SecSignIDLogger;


/**
 * Class which is called in filter chain to filter all requests.
 * Depending on the request uri the filter may redirect the user to the secsign id login page than rendering the jira login page.
 * 
 * @version 1.0
 * @author SecSign Technologies Inc.
 */
public class SecSignIDAuthenticationFilter implements Filter {

    /**
     * User manager
     */
    private final UserManager userManager;
    
    
    /**
     * Constructor
     * @param userManager
     */
    public SecSignIDAuthenticationFilter(UserManager userManager)
    {
        this.userManager = userManager;
    }
    
    @Override
    public void init(FilterConfig filterConfig){
    }
    
    
    @Override
    public void destroy() {
    }

    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        
        HttpServletRequest httpServletReq = (HttpServletRequest) request;
        String requestUri = httpServletReq.getRequestURI();
        
        if(requestUri.contains(SecSignIDConstants.JIRA_DASHBOARD_JSP_PATH) || 
           requestUri.contains(SecSignIDConstants.JIRA_LOGIN_JSP_PATH) ||
           requestUri.contains(SecSignIDConstants.JIRA_MOBILE_LOGIN_PATH)){
        
        
            HttpServletResponse httpServletResp = (HttpServletResponse) response;
            HttpSession session = httpServletReq.getSession();
            
            //
            // check whether a user is logged in or not
            //
            
            Object appUserObject = session.getAttribute(DefaultAuthenticator.LOGGED_IN_KEY);
            UserProfile userProfile = null;
            if(appUserObject == null){
                userProfile = userManager.getRemoteUser(httpServletReq);
            }
            
            
            if(appUserObject != null || userProfile != null){
                // a user is already logged in. nothing else to do?
                if(requestUri.contains(SecSignIDConstants.JIRA_LOGIN_JSP_PATH)){
                    
                    httpServletResp.sendRedirect(SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq));
                    
                } else {
                    
                    // a user is logged in and the login path was not called explicitly. just finish the filter chain
                    filterChain.doFilter(httpServletReq, httpServletResp);
                }
                
                return;
            }
            
            //
            // check whether the user wishes to login with secsign id
            //
            boolean jiraLoginRequested = httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_PARAM_NAME) != null;
            if(jiraLoginRequested){
                filterChain.doFilter(request, response);
                return;
            }
            
            //
            // Check whether a login process is currently processed
            //
            if(httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_FORM_SUBMIT_PARAM_NAME) != null &&
               httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_USER_PARAM_NAME) != null &&
               httpServletReq.getParameter(SecSignIDConstants.JIRA_LOGIN_PWD_PARAM_NAME) != null){
                
                // user currently logs in using the default username/password form
                filterChain.doFilter(request, response);
                return;
            }
            
            //
            // check whether we have a mobile login page?
            //
            boolean isMobile = false;
            if(requestUri.contains(SecSignIDConstants.JIRA_MOBILE_LOGIN_PATH)){
                // actually no chance to check whether a login is required...
                isMobile = true;
            }
         
            
            //
            // secsign id login requested
            //
            
            String jiraBaseUrl = SecSignIDAuthenticationFilter.getJiraBaseUrl(httpServletReq);
            
            //SecSignIDLogger.log("requestUri=" + requestUri);
            //SecSignIDLogger.log("httpServletReq.getParameterMap()=" + httpServletReq.getParameterMap());
            //SecSignIDLogger.log("authenticationContext.getUser()=" + authenticationContext.getUser());
            //SecSignIDLogger.log("authenticationContext.getLoggedInUser()=" + authenticationContext.getLoggedInUser());
            //SecSignIDLogger.log("userManager.getRemoteUsername(httpServletReq)=" + userManager.getRemoteUsername(httpServletReq));
            //SecSignIDLogger.log("session.getAttribute(DefaultAuthenticator.LOGGED_IN_KEY)=" + session.getAttribute(DefaultAuthenticator.LOGGED_IN_KEY));
            //SecSignIDLogger.log("ComponentAccessor.getApplicationProperties()=" + ComponentAccessor.getApplicationProperties());
            
            String osDestination = httpServletReq.getParameter(SecSignIDConstants.JIRA_OS_DESTINATION_PARAM_NAME);
            if(osDestination == null){
                StringBuffer requestUrl = httpServletReq.getRequestURL();
                if(requestUrl.indexOf(SecSignIDConstants.JIRA_LOGIN_JSP_PATH) < 0){
            
                    int baseUrlIndex = requestUrl.indexOf(jiraBaseUrl);
                    osDestination = requestUrl.substring(baseUrlIndex + jiraBaseUrl.length());
                }
            }
            
            // cut of tailing / 
            if(jiraBaseUrl.length() > 0 && jiraBaseUrl.endsWith("/")){
                jiraBaseUrl = jiraBaseUrl.substring(0, jiraBaseUrl.length()-1);
            }
            
            if(osDestination != null && osDestination.length() > 0){
                httpServletResp.sendRedirect(jiraBaseUrl + SecSignIDConstants.SECSIGNID_SERVLET_PATH + "?" + SecSignIDConstants.RETURN_URL_PARAM_NAME + "=" + JiraUrlCodec.encode(osDestination)); // + "&mobile=" + isMobile);
            } else {
                httpServletResp.sendRedirect(jiraBaseUrl + SecSignIDConstants.SECSIGNID_SERVLET_PATH); // + "?mobile=" + isMobile);
            }
            
            
            // redirect was sent. nothing else to do. there is no need to go up the filter chain.
            return;   
        }
        
        // no need for a redirect, just go through the normal filters of the servlet
        filterChain.doFilter(request, response);
    }

    
    /**
     * Gets the base url. Use a static field because creating application properties can take couple of seconds which will slow down everything
     * @return
     */
    public static String getJiraBaseUrl(HttpServletRequest httpServletReq) {
        
        String requestUrlStr = null;
        try {
            if(httpServletReq != null){
                URL requestUrl = new URL(httpServletReq.getRequestURL().toString());
                requestUrlStr = requestUrl.getProtocol() + "://" + requestUrl.getAuthority();
            }
        } catch (MalformedURLException ex) {
            SecSignIDLogger.log(ex.getMessage());
        }
        
        
        
        // https://confluence.atlassian.com/display/JIRAKB/Base+URL+mismatches+in+UPM+due+to+wrong+port+for+HTTPS
        // https://confluence.atlassian.com/doc/configuring-the-server-base-url-148592.html
        // 
        // https://answers.atlassian.com/questions/78587/base-url-change-does-not-work
        // https://confluence.atlassian.com/display/CONFKB/Incorrect+Links+in+JIRA+Issues+Macro
        
        String baseUrlFromAppProperties = ComponentAccessor.getApplicationProperties().getString("jira.baseurl");
        String baseUrl = baseUrlFromAppProperties;
        
        // now parse url and cut of host and port   
        try {
            String baseUrlPath = new URL(baseUrlFromAppProperties).getPath();
            
            // if jira base url has a path e.g. titus.sec.intern/jira
            if(requestUrlStr != null){
                if(baseUrlPath.length() > 0){
                    requestUrlStr += baseUrlPath.charAt(0) == '/' ? baseUrlPath : "/" + baseUrlPath;
                }
                
                baseUrl = requestUrlStr;
            }
        } catch (Exception ex) {
            SecSignIDLogger.log(ex.getMessage());
        }
            
        return baseUrl;
    }
}
