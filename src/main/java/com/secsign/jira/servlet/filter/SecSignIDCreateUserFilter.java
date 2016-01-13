package com.secsign.jira.servlet.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.secsign.jira.SecSignIDConstants;
import com.secsign.jira.ao.SecSignIDUsersActiveObject;
import com.secsign.jira.util.SecSignIDCharArrayWriter;
import com.secsign.jira.util.SecSignIDServletResponseWrapper;

/**
 * Filter the view user page and inject resources and code
 * 
 * @version 1.0
 * @author SecSign Technologies Inc.
 */
public class SecSignIDCreateUserFilter implements Filter {
    
    /**
     * The template renderer
     * 
     * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/atlassian-template-renderer
     * @see 
     */
    private final TemplateRenderer templateRenderer;
    
    
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
     * Name of the template
     */
    private String addUserSecSignIDFieldsTemplate = "/templates/secsignid-adduser-dialog.vm";
    
    /**
     * Constructor
     * @param templateRenderer
     * @param ao
     */
    public SecSignIDCreateUserFilter(TemplateRenderer templateRenderer, ActiveObjects ao, I18nResolver i18nResolver)
    {
        this.templateRenderer = templateRenderer;
        this.ao = ao;
        this.i18nResolver = i18nResolver;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        // create a wrapper to render the template into a string.
        // after the original page was rendered we inject our rendered fragment just before the end of body.
        
        SecSignIDServletResponseWrapper responseWrapper = new SecSignIDServletResponseWrapper((HttpServletResponse)response);
        chain.doFilter(request, responseWrapper);
        
        
        HttpServletRequest httpServletReq = (HttpServletRequest) request;
        
        String requestUri = httpServletReq.getRequestURI();
        String jiraUserKey = httpServletReq.getParameter("username");
        String secSignId = httpServletReq.getParameter("secsignid");
        String pwdAllowed = httpServletReq.getParameter("secsignid-pwdallowed");
     
        
        HashMap<String, Object> context = new HashMap<String, Object>();

        // put back into context. in case of an error, this will be set into the input field
        // the error case can triggered even by the jira-core AddUser.jspa action
        
        context.put("secsignid", secSignId);
        context.put("pwdallowed", pwdAllowed);
        context.put("showpwdloginoption", requestUri.contains   (SecSignIDConstants.JIRA_SIGNUP_JSP_PATH) ? "none" : "block");
        
        if(secSignId != null && secSignId.length() > 0){
            
            // check secsign id here to get nice error message.
            // the webaction SecSignIDAddUserAction with doValidation is called later.
            // but there is no way to pass information between filters and webactions due to the fact, 
            // that the filter is called always before the webaction
            
            String[] secSignIDs = SecSignIDUsersActiveObject.getArrayOfSecSignIds(secSignId, false);
            String secSignWithIllegalChars = SecSignIDUsersActiveObject.checkForIllegalCharactersInSecSignIds(secSignIDs);
            String errorMessage = null;
            if(secSignWithIllegalChars != null){
                errorMessage = i18nResolver.getText("secsignid.messages.error.action.illegalcharacters2", secSignWithIllegalChars) + " " + secSignWithIllegalChars;
            }
            
            if(errorMessage == null){
                // no error yet, do further checks
                ArrayList<String> duplicates = SecSignIDUsersActiveObject.checkForDuplicateSecSignIds(ao, jiraUserKey, secSignIDs);
                if(duplicates != null){
                    if(duplicates.size() == 1){
                        errorMessage = i18nResolver.getText("secsignid.messages.error.action.duplicate2", duplicates.get(0)) + " " + duplicates.get(0);
                    } else {
                        errorMessage = i18nResolver.getText("secsignid.messages.error.action.duplicate.plural2", duplicates.toString()) + " " + duplicates.toString();
                    }
                }
            }
            
            // either errorMessage is null or not. this is evaluated in the velocity template addUserSecSignIDFieldsTemplate
            context.put("error", errorMessage);
        } else {
            
            // no secsign id given, so probably we just need to inject our rendered template with all default values
            context.put("default", "true");
        }
        
        // render our own template
        SecSignIDCharArrayWriter renderedTemplate = new SecSignIDCharArrayWriter();
        templateRenderer.render(addUserSecSignIDFieldsTemplate, context, renderedTemplate);
      
        // add at the end of the create user form
        responseWrapper.inject(renderedTemplate, "</form>");
        
        // copy back the characters into the wrapper?
        // or only dealing with character arrays char[] ?

        // write our modified text to the real response
        PrintWriter responseOutput = response.getWriter();
        response.setContentLength(responseWrapper.length());
        responseOutput.write(responseWrapper.toString());
        responseOutput.close();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
