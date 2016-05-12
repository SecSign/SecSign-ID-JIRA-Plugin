/**
 * (c) 2015 - 2016 SecSign Technologies Inc.
 *
 * Adding some functionality to the ViewUser.jspa page
 */
require(["jquery", "underscore"], function ($, _) {

        $(document).ready(function () {
			var atlassianToken = $('#atlassian-token').attr("content");
			var applicationName = $("meta[name=application-name]").data("name");
			if(!applicationName){
				// assume confluence
				applicationName = "confluence";
			} else {
				applicationName = applicationName.toLowerCase();
			}
			var isJira = (applicationName === "jira");

			// check the atlassion token?
			if(isJira){
				var isViewUserJsp = location.href.match(/ViewUser.jspa/i);
				var isCreateUserJsp = location.href.match(/AddUser*.jspa/i);

				if(isViewUserJsp){
					// get the view user detail section
					var viewUserDetailsSection = $("#viewUserDetails");
					viewUserDetailsSection.after($("#secsignid-details"));
				
					var appAndGroupSection = $(".view-user-applications-and-groups-module");
					appAndGroupSection.addClass("module");
					appAndGroupSection.addClass("vcard");
				}
			}
        });
});