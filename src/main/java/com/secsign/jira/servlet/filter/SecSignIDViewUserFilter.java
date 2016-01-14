package com.secsign.jira.servlet.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.secsign.jira.contextproviders.SecSignIDJiraUserInfoProvider;
import com.secsign.jira.util.SecSignIDCharArrayWriter;
import com.secsign.jira.util.SecSignIDLogger;
import com.secsign.jira.util.SecSignIDServletResponseWrapper;

/**
 * Filter the view user page and inject resources and code
 * 
 * @version 1.0
 * @author SecSign Technologies Inc.
 */
public class SecSignIDViewUserFilter implements Filter {

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
     * Name of the template
     */
    private String viewUserTemplate = "/templates/secsignid-viewuser.vm";
    
    /**
     * Constructor
     * @param templateRenderer
     * @param ao
     */
    public SecSignIDViewUserFilter(TemplateRenderer templateRenderer, ActiveObjects ao)
    {
        this.templateRenderer = templateRenderer;
        this.ao = ao;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
    
    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        SecSignIDLogger.debug(request.getParameterMap());
        
        // create a wrapper to render the template into a string.
        // after the original page was rendered we inject our rendered fragment just before the end of body.
        SecSignIDServletResponseWrapper responseWrapper = new SecSignIDServletResponseWrapper((HttpServletResponse)response);
        chain.doFilter(request, responseWrapper); // this usually takes 1second (on titus laptop) which is 98% of the time
        
        HashMap<String, Object> context = new HashMap<String, Object>();
        context.put("profileUser", request.getParameter("name"));
        context.put("jirauser", request.getParameter("name"));
        context.put("jirauserurldec", URLEncoder.encode(request.getParameter("name"), "utf-8"));
        
        SecSignIDJiraUserInfoProvider infoProvider = new SecSignIDJiraUserInfoProvider(ao);
        context = (HashMap<String, Object>) infoProvider.getContextMap(context);
        
        SecSignIDLogger.debug(context, "context map of view user filter");
        
        // render the template fragment into the ViewUser.jspa
        // after that, the hidden fields will be put to the right place using javascript
        // @see atlassian-plugin.xml the webresource: <web-resource key="secsignid-viewuser" name="SecSign ID Web Resource for Jira ViewUser.jsp">
        
        // render our own template
        SecSignIDCharArrayWriter renderedTemplate = new SecSignIDCharArrayWriter();
        templateRenderer.render(viewUserTemplate, context, renderedTemplate);
      
        responseWrapper.inject(renderedTemplate);
      
        
        // copy back the characters into the wrapper?
        // or only dealing with character arrays char[] ?

        // Write our modified text to the real response
        PrintWriter responseOutput = response.getWriter();
        response.setContentLength(responseWrapper.length());
        responseOutput.write(responseWrapper.toString());
        responseOutput.close();
    }
}
