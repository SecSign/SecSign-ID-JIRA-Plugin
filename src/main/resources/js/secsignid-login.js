/**
 * (c) 2015 SecSign Technologies Inc.
 *
 * Adding some functionality to the login screen
 */
require(["jquery", "underscore"], function ($, _) {

        $(document).ready(function () {

			// check whether we have a mobile page
     		/*var query = window.location.search.substring(1);   
	    	var vars = query.split("&");
	    	var params = {};
      		for (var i=0;i<vars.length;i++) {    
            	var pair = vars[i].split("=");
            	params[pair[0]] = pair[1];
			}
			
			if(params["mobile"] === "true"){
			}*/

        	var injectSecSignIdLogin = function(retry){
        		var atlassianToken = $('#atlassian-token').attr("content");
				var applicationName = $("meta[name=application-name]").data("name");
				var contextpath = $("meta[name=ajs-context-path]").attr("content");
				
				if(!applicationName){
					// assume confluence
					applicationName = "confluence";
				} else {
					applicationName = applicationName.toLowerCase();
				}
				var isJira = (applicationName === "jira");
				
				// now try to get the login form
				var loginForm = $(isJira ? "#login-form, #loginform" : "form.login-form-container");
				
				if (!loginForm.length || !loginForm.attr("action") || loginForm.attr("action").indexOf("WebSudo") != -1) {
					// no login form and no websudo action
					
					// http://localhost:2990/jira/Dashboard.jspa will sometime take more time before loginform is available
					if(retry){
						// if the dashboard has not been loaded a login form could be missing.
						// just retry the loginform detection a second time
						window.setTimeout(function(){
							injectSecSignIdLogin(false);
						}, 1000);
					}
					return;
				}
				
				// remove the dirty warning in case jira detects some values in the login form we cannot remove it
				if (_.isFunction(loginForm.removeDirtyWarning)) {
					// remove dirty warning
					loginForm.removeDirtyWarning();
				}
				
				var secSignIdLoginUrl = AJS.params.baseURL + "/plugins/servlet/secsignid";
				/*if(contextpath){
					var arr = window.location.href.split("/");
					secSignIdLoginUrl = arr[0] + "//" + arr[2] + contextpath + secSignIdLoginUrl;
				}*/
		
				// inject a link to secsign id login servlet
				var secSignIdHtmlLogin = '<div id="secsignid-login-jira-login"><div class="divider"></div>\
						<div class="container">\
							<div class="secsignid-logo" title="SecSign Technologies Inc."></div>\
							<a href="' + secSignIdLoginUrl + '" title="SecSign ID Login">Login with SecSign ID</a>\
						</div>\
						</div>';
				if(isJira){
					loginForm.append(secSignIdHtmlLogin);
				} else {
					$(".login-section").append(secSignIdHtmlLogin);
				}
			}; // end of function.
			
			// call function first
			injectSecSignIdLogin(true);
        });
    });