//
//
//

package com.secsign.jira.actions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import webwork.action.Action;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.BuildUtilsInfoImpl;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.message.I18nResolver;
import com.secsign.jira.ao.SecSignIDUsersActiveObject;
import com.secsign.jira.util.SecSignIDLogger;
import com.secsign.jira.util.SecSignIDUserManager;


/**
 * Class to have all action according user related things together
 * 
 * @version 1.0
 * @author SecSign Technologies Inc.
 */
public class SecSignIDCreateJiraUserAction extends JiraWebActionSupport {

    /**
     * 
     */
    private static final long serialVersionUID = 8385747316965574148L;
    
    protected String email;
    protected String username;
    protected String fullname;
    protected String password;
    protected String secsignid;
    private boolean licenseExceeded;
    
    protected String referingUri;
    protected String returnUrl;
    
    
    /**
     * The active objects instance
     * @see https://developer.atlassian.com/docs/atlassian-platform-common-components/active-objects 
     */
    protected final ActiveObjects ao;
    
    /**
     * The I18n Resolver
     * @see https://docs.atlassian.com/sal-api/2.2.1/sal-api/apidocs/com/atlassian/sal/api/message/I18nResolver.html
     */
    protected final I18nResolver i18nResolver;

    /**
     * Constructor
     * @param ao
     * @param i18nResolver
     */
    public SecSignIDCreateJiraUserAction(ActiveObjects ao, I18nResolver i18nResolver) {
        super();
        
        this.ao = ao;
        this.i18nResolver = i18nResolver;
    }
    
    @Override
    public void doValidation(){
        
        HttpServletRequest httpRequest = getHttpRequest();
        SecSignIDLogger.debug(httpRequest.getParameterMap());
        
        if(httpRequest.getMethod().equalsIgnoreCase("GET")){
            super.doValidation();
            return;
        }
        
        checkLicense();
        
        // 1) check whether all information is there
        // 2) check whether user already exists
        // 3) check mandatory values
        // 4) check secsign id
        
        //
        // 1) / 2)
        //
        username = httpRequest.getParameter("username");
        if(username == null || username.length() < 1){
            addError("username", i18nResolver.getText("secsignid.messages.error.action.empty.username"));
        } else {
            ApplicationUser existingUser = SecSignIDUserManager.getApplicationUser(username);
            if(existingUser != null){
                addError("username", i18nResolver.getText("secsignid.messages.error.action.userexists"));
            }
        }
        
        
        //
        // 3)
        //
        // mandatory are: fullname, emailaddress
        fullname = httpRequest.getParameter("fullname");
        if(fullname == null || fullname.length() < 1){
            addError("fullname", i18nResolver.getText("secsignid.messages.error.action.empty.fullname"));
        }
        email = httpRequest.getParameter("email");
        if(email == null || email.length() < 1){
            addError("email", i18nResolver.getText("secsignid.messages.error.action.empty.email"));
        }
        password = httpRequest.getParameter("password");
        if(password == null || password.length() < 1){
            addError("password", i18nResolver.getText("secsignid.messages.error.action.empty.password"));
        } else if(httpRequest.getParameter("confirm") != null){
            if(!password.equals(httpRequest.getParameter("confirm"))){
                addError("confirm", i18nResolver.getText("secsignid.messages.error.action.nonmatch.password"));
            }
        }
        
        //
        // 4)
        //
        secsignid = httpRequest.getParameter("secsignid");
        if(secsignid != null && secsignid.length() > 0){
            String[] givenSecSignIds = SecSignIDUsersActiveObject.getArrayOfSecSignIds(secsignid);
            String secSignWithIllegalChars = SecSignIDUsersActiveObject.checkForIllegalCharactersInSecSignIds(givenSecSignIds);
            if(secSignWithIllegalChars != null){
                addError("user-create-secsignid", this.i18nResolver.getText("secsignid.messages.error.action.illegalcharacters", secSignWithIllegalChars));
                
                // add error message means, adding an error message which will be shown at the very top of the page.
                //addErrorMessage(i18nResolver.getText("secsignid.messages.error.action.illegalcharacters2", secSignWithIllegalChars) + " " + secSignWithIllegalChars);
            }
            
            ArrayList<String> duplicates = SecSignIDUsersActiveObject.checkForDuplicateSecSignIds(this.ao, username, givenSecSignIds);
            if(duplicates != null){

                /*addError("error", i18n.getText("secsignid.messages.error.action.duplicate") + " " +  intersection.toString());
                addError("secsignid", intersection.toString());*/
                
                // @see https://developer.atlassian.com/docs/common-coding-tasks/internationalising-your-plugin#Internationalisingyourplugin-Knowni18nIssues
                /*if(duplicates.size() == 1){
                    addErrorMessage(this.i18nResolver.getText("secsignid.messages.error.action.duplicate2", duplicates.get(0)) + " " + duplicates.get(0));
                } else {
                    addErrorMessage(this.i18nResolver.getText("secsignid.messages.error.action.duplicate.plural2", duplicates.toString()) + " " + duplicates.toString());
                }*/
                
                addError("user-create-secsignid", this.i18nResolver.getText("secsignid.messages.error.action.duplicate2", duplicates.get(0)) + " " + duplicates.get(0));
            }
        }
        
        super.doValidation();
    }
    
    @Override
    public String doDefault() throws Exception {
        checkLicense();
        
        
        HttpServletRequest httpRequest = getHttpRequest();
        
        String template = super.doDefault();
        
        referingUri = httpRequest.getHeader("referer");
        returnUrl = httpRequest.getHeader("referer"); // Yes, with the legendary misspelling.
        
        fullname = httpRequest.getParameter("fullname");
        username = httpRequest.getParameter("username");
        email = httpRequest.getParameter("email");
        password = null;
        secsignid = httpRequest.getParameter("secsignid");
        
        return template;
    }
    
    @Override
    public String doExecute() {
        
        checkLicense();
        
        // the doExecute is called only if doValidation does not have add any errors.
        String resultingTemplateName = null;
        try {
            resultingTemplateName = super.doExecute();
        } catch (Exception ex) {
            SecSignIDLogger.log(ex);
            
            addErrorMessage(ex.getMessage());
        }
        
        
        String jiraVersion = new BuildUtilsInfoImpl().getVersion();
        
        HttpServletRequest httpRequest = getHttpRequest();
        SecSignIDLogger.debug(httpRequest.getParameterMap());
        
        boolean isJIRA7 = jiraVersion.startsWith("7.");
        boolean isJIRA6 = jiraVersion.startsWith("6.");
        boolean isSignUp = httpRequest.getParameter("Signup") != null; 
        boolean isAddUser = httpRequest.getParameter("Create") != null || isJIRA6;

        secsignid = httpRequest.getParameter("secsignid");
        username = httpRequest.getParameter("username");
        fullname = httpRequest.getParameter("fullname");
        email = httpRequest.getParameter("email");
        password = httpRequest.getParameter("password");

        
        // check whether we have been called by create user button in AddUser.jsp or by sign up user in SignUp.jsp
        if ((isAddUser || isSignUp) && ! Action.ERROR.equals(resultingTemplateName)){
            
            // so far, no error occured. create a user with given information. if there is something wrong with the provided information 
            // it should have been checked in doValidation() before.
            
            ApplicationUser createdApplicationUser = null;
            if(isJIRA7){
                createdApplicationUser = createUserOnJIRA7();
            } else {
                // do not check whether it is JIRA 6 or below. the methods on JIRA 6 should work on JIRA 5 and JIRA 4
                // the only changes at the API concerning the returned objects have been made in JIRA 7
                createdApplicationUser = createUserOnJIRA6();
            }
       
            // check whether a secsign needs to be stored in database to be assigned to new cereated application user
            if(secsignid != null && secsignid.length() > 1){
                // does a secsign id was specified?
            
                // at this point the user should be created already
                // ApplicationUser createdAppUser = SecSignIDUserManager.getApplicationUser(umgr, username);
                
                SecSignIDLogger.log("secsign id '" + secsignid + "' is (are) assigned to created application user '" + username + "'.");
                
                // save secsign id <> app user mapping
                HashMap<String, String[]> secSignIdMappings = new HashMap<String, String[]>();
                // convert secsign id string into array
                String[] secSignIds = SecSignIDUsersActiveObject.getArrayOfSecSignIds(secsignid);
                
                secSignIdMappings.put(username, secSignIds);
                
                SecSignIDUsersActiveObject.saveSecSignIdUserMappings(ao, secSignIdMappings);
                
                // does a password login is still possible?
                Integer pwdAllowed = new Integer(0);
                if(httpRequest.getParameter("secsignid-pwdallowed") != null){
                    pwdAllowed = new Integer(1);
                }
                
                HashMap<String, Integer> passwordLoginAllowed = new HashMap<String, Integer>();
                passwordLoginAllowed.put(username, pwdAllowed);
                SecSignIDUsersActiveObject.savePasswordLoginIsAllowedUserMappings(ao, passwordLoginAllowed);
            }
            
            // no error occured, add success message to later response
            // for JIRA-Messages.Types 
            // @see https://docs.atlassian.com/aui/latest/docs/messages.html
            addMessageToResponse(i18nResolver.getText("secsignid.messages.usercreated", username), "Success", true, null);
        }
        
        return resultingTemplateName;
    }
    
    /**
     * Creates a new user on a jira 6 (and below) system and return the newly created user.
     * @return the new user
     */
    private ApplicationUser createUserOnJIRA6() {
        
        //[INFO] [talledLocalContainer] Schwerwiegend: Servlet.service() for servlet [action] in context with path [/jira] threw exception [Servlet execution threw an exception] with root cause
        //[INFO] [talledLocalContainer] java.lang.NoSuchMethodError: com.atlassian.jira.user.util.UserUtil.createUserNoNotification(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/atlassian/jira/user/ApplicationUser;
        //[INFO] [talledLocalContainer]   at com.secsign.jira.actions.SecSignIDCreateJiraUserAction.doExecute(SecSignIDCreateJiraUserAction.java:267)
        //[INFO] [talledLocalContainer]   at com.secsign.jira.actions.SecSignIDAddUserAction.doExecute(SecSignIDAddUserAction.java:114)
        
        
        // use reflection otherwise the exception above can be thrown
        UserUtil userUtil = ComponentAccessor.getUserUtil();
        Class<UserUtil> userUtilClass = UserUtil.class;
        
        // has to be the raw type to take either application user or the embedd api user class instance
        Class createdUserObjectType = null;
        
        try{
            SecSignIDLogger.debug("fetch method createUserNoNotification.");
            
            Method createUserMethod = userUtilClass.getMethod("createUserNoNotification", String.class, String.class, String.class, String.class);
            
            if(createUserMethod != null){
                createdUserObjectType = createUserMethod.getReturnType();
                
                // call/invoke the method to create a user. the resulting object does not matter
                createUserMethod.invoke(userUtil, username, password, email, fullname);
                
                SecSignIDLogger.log("created new application/directory user '" + username + "'.");
            } else {
                throw new NoSuchMethodException("Method com.atlassian.jira.user.util.UserUtil.createUserNoNotification does not exist!");
            }
        }
        catch(InvocationTargetException ex){
            SecSignIDLogger.log(ex.getTargetException());
        }
        catch(NoSuchMethodException ex){
            SecSignIDLogger.log(ex.getMessage());
            addErrorMessage(ex.getMessage());
            
            // TODO: inform user that the user cannot be created. tell the user to disable the modules for AddUser and SignUp
            
        } catch(NoSuchMethodError ex){
            SecSignIDLogger.log(ex.getMessage());
            addErrorMessage(ex.getMessage());
            
            // TODO: inform user that the user cannot be created. tell the user to disable the modules for AddUser and SignUp
            
        } catch(Exception ex){
            SecSignIDLogger.log(ex);
            addErrorMessage(ex.getMessage());
        }
        
        
        // get created application user
        UserManager umgr = getUserManager();
        
        ApplicationUser createdApplicationUser = SecSignIDUserManager.getApplicationUser(umgr, username);
        User createdUser = ApplicationUsers.toDirectoryUser(createdApplicationUser);
        Object createdUserObject = null;
        
        if(createdApplicationUser == null){
            SecSignIDLogger.log("could not get newly created application user '" + username + "'");
            addErrorMessage(i18nResolver.getText("secsignid.messages.error.action.noassignmenttogroup"));
        }
        
        if(!licenseExceeded && createdApplicationUser != null){
            
            // add user to default group 'jira-users'
            GroupManager groupMgr = ComponentAccessor.getGroupManager();
            Group jiraUsersDefaultGroup = groupMgr.getGroup("jira-users");
            if(jiraUsersDefaultGroup == null){
                // check whether another default group is available
                jiraUsersDefaultGroup = groupMgr.getGroup("jira-software-users");
            }
            
            if(jiraUsersDefaultGroup != null){
                try {
                    Class userClass = null;
                    try {
                        userClass = Class.forName("com.atlassian.crowd.embedded.api.User");
                        createdUserObject = createdUser;
                            
                    } catch(ClassNotFoundException ex){
                        SecSignIDLogger.log("could not find class 'com.atlassian.crowd.embedded.api.User'.");
                    }
                    
                    if(userClass == null){
                        userClass = createdUserObjectType;
                    }
                    
                    if(userClass != null){
                        SecSignIDLogger.debug("fetch method addUserToGroup to add object of type of " + String.valueOf(userClass));
                            
                        Method addUserToGroupMethod = userUtilClass.getMethod("addUserToGroup", Group.class, userClass);
                        if(addUserToGroupMethod != null){
                            addUserToGroupMethod.invoke(userUtil, jiraUsersDefaultGroup, createdUserObject);
                            SecSignIDLogger.log("application/directory user '" + createdUser + "' was put into group 'jira-users'.");
                        }
                    }
                } 
                catch(InvocationTargetException ex){
                    SecSignIDLogger.log(ex.getTargetException());
                }
                catch (NoSuchMethodException ex) {
                    SecSignIDLogger.log(ex.getMessage());
                }
                catch (NoSuchMethodError ex) {
                    SecSignIDLogger.log(ex.getMessage());
                }
                catch (Exception ex) {
                    SecSignIDLogger.log(ex);
                    
                    // dont break the process. if a user was not assigned to a group the admin has to assign it manually later.
                }
            } else {
                SecSignIDLogger.log("could not find group 'jira-users' nor 'jira-software-users'. therefor user '" + createdUserObject + "' will not be added to any group.");
            }
        } else {
            SecSignIDLogger.log("created application user '" + username + "' is not put into any JIRA user group because the license is exceeded.");
        }
        
        return createdApplicationUser;
    }

    /**
     * Creates a new user on a jira 7 system and return the newly created user.
     * @return the new user
     */
    private ApplicationUser createUserOnJIRA7() {
        // the following block is possible only in JIRA 7.0.x
        // use reflection to compile this class for JIRA 6.4.x
        
        UserManager umgr = getUserManager();
        UserUtil userUtil = ComponentAccessor.getUserUtil();
        Class<UserUtil> userUtilClass = UserUtil.class;
        
        /*Class userDetailClass = null;
        try{
            userDetailClass = Class.forName("com.atlassian.jira.user.UserDetails");
            Constructor ctor = userDetailClass.getDeclaredConstructor(String.class, String.class);
            ctor.setAccessible(true);
            
            Object userDetailsObject = ctor.newInstance(username, fullname);
            
            Method withEmailMethod = userDetailClass.getMethod("withEmail", String.class);
            withEmailMethod.invoke(userDetailsObject, email);
            
            Method withPasswordMethod = userDetailClass.getMethod("withPassword", String.class);
            withPasswordMethod.invoke(userDetailsObject, password);
            
            SecSignIDLogger.debug("fetch method createUser using UserDetails as parameter.");
            
            Method createUserMethod = umgr.getClass().getMethod("createUser", userDetailClass);
            createUserMethod.invoke(umgr,  userDetailsObject);
            
        } catch(ClassNotFoundException ex){
            SecSignIDLogger.log("could not find class 'com.atlassian.jira.user.UserDetails'.");
        } catch(Exception ex){
            SecSignIDLogger.log(ex);
        }*/
        
        // has to be the raw type to take either application user or the embedd api user class instance
        Class createdUserObjectType = null;
        
        try{
            SecSignIDLogger.debug("fetch method createUserNoNotification.");
            
            Method createUserMethod = userUtilClass.getMethod("createUserNoNotification", String.class, String.class, String.class, String.class);
            
            if(createUserMethod != null){
                createdUserObjectType = createUserMethod.getReturnType();
                
                // call/invoke the method to create a user. the resulting object does not matter
                createUserMethod.invoke(userUtil, username, password, email, fullname);
                
                SecSignIDLogger.log("created new application/directory user '" + username + "'.");
            } else {
                throw new NoSuchMethodException("Method com.atlassian.jira.user.util.UserUtil.createUserNoNotification does not exist!");
            }
        }
        catch(InvocationTargetException ex){
            SecSignIDLogger.log(ex.getTargetException());
        }
        catch(NoSuchMethodException ex){
            SecSignIDLogger.log(ex.getMessage());
            addErrorMessage(ex.getMessage());
            
            // TODO: inform user that the user cannot be created. tell the user to disable the modules for AddUser and SignUp
            
        } catch(NoSuchMethodError ex){
            SecSignIDLogger.log(ex.getMessage());
            addErrorMessage(ex.getMessage());
            
            // TODO: inform user that the user cannot be created. tell the user to disable the modules for AddUser and SignUp
            
        } catch(Exception ex){
            SecSignIDLogger.log(ex);
            addErrorMessage(ex.getMessage());
        }
        
       
        // get created application user
        ApplicationUser createdApplicationUser = SecSignIDUserManager.getApplicationUser(umgr, username);
        Object createdUserObject = null;
        
        if(createdApplicationUser == null){
            SecSignIDLogger.log("could not get newly created application user '" + username + "'");
            addErrorMessage(i18nResolver.getText("secsignid.messages.error.action.noassignmenttogroup"));
        }
        
        if(!licenseExceeded && createdApplicationUser != null){
            
            // add user to default group 'jira-users'
            GroupManager groupMgr = ComponentAccessor.getGroupManager();
            Group jiraUsersDefaultGroup = groupMgr.getGroup("jira-users");
            if(jiraUsersDefaultGroup == null){
                // check whether another default group is available
                jiraUsersDefaultGroup = groupMgr.getGroup("jira-software-users");
            }
            
            if(jiraUsersDefaultGroup != null){
                try {
                    Class userClass = null;
                    try {
                        userClass = Class.forName("com.atlassian.jira.user.ApplicationUser");
                        createdUserObject = createdApplicationUser;
                        
                    } catch(ClassNotFoundException ex1){
                        SecSignIDLogger.log("could not find class 'com.atlassian.jira.user.ApplicationUser'.");
                    }
                    if(userClass == null){
                        userClass = createdUserObjectType;
                    }
                    
                    if(userClass != null){
                        SecSignIDLogger.debug("fetch method addUserToGroup to add object of type of " + String.valueOf(userClass));
                            
                        Method addUserToGroupMethod = userUtilClass.getMethod("addUserToGroup", Group.class, userClass);
                        if(addUserToGroupMethod != null){
                            addUserToGroupMethod.invoke(userUtil, jiraUsersDefaultGroup, createdUserObject);
                            SecSignIDLogger.log("application/directory user '" + createdApplicationUser + "' was put into group 'jira-users'.");
                        }
                    }
                } 
                catch(InvocationTargetException ex){
                    SecSignIDLogger.log(ex.getTargetException());
                }
                catch (NoSuchMethodException ex) {
                    SecSignIDLogger.log(ex.getMessage());
                }
                catch (NoSuchMethodError ex) {
                    SecSignIDLogger.log(ex.getMessage());
                }
                catch (Exception ex) {
                    SecSignIDLogger.log(ex);
                    
                    // dont break the process. if a user was not assigned to a group the admin has to assign it manually later.
                }
            } else {
                SecSignIDLogger.log("could not find group 'jira-users' nor 'jira-software-users'. therefor user '" + createdUserObject + "' will not be added to any group.");
            }
        } else {
            SecSignIDLogger.log("created application user '" + username + "' is not put into any JIRA user group because the license is exceeded.");
        }
        
        return createdApplicationUser;
    }

    /**
     * checks whether the number of users are exceeded
     */
    protected void checkLicense()
    {
        // not implemented yet
        this.licenseExceeded = false;
        
        
        // @see https://answers.atlassian.com/questions/32995215/get-how-many-users-the-current-jira-licence-allows
        // get names of licences?
        
        // get the number of users which are allowed by the license in JIRA
        // jira get number of users license programatically
        // https://answers.atlassian.com/questions/135657/get-how-many-users-the-current-confluence-licence-allows
        
        /*
        LicenseManager licenseMgr = LicenseManager.getInstance();
        License jiraLicense = licenseMgr.getLicense("JIRA");
        int numberOfUsersAllowed = jiraLicense != null ? jiraLicense.getUsers() : 10;
        */
        
        
        /*
        int numberOfUsersAllowed = -1;
        
        // get number of current users in system
        Collection<ApplicationUser> allUsers = getUserManager().getAllApplicationUsers();
        int numberOfExistingUsers = allUsers != null ? allUsers.size() : 0;
        */
        
        
        // get number of active users. users are active if they are in a group or have assigned an application
        // @see https://answers.atlassian.com/questions/203084/how-can-i-find-all-jira-users-without-any-group-assignments
        // @see https://docs.atlassian.com/confluence/latest/com/atlassian/confluence/user/UserAccessor.html
        // @see https://answers.atlassian.com/questions/71796/jira-5.x-equivalent-of-useraccessor.getgroupnames
        
        /*
        GroupManager groupMgr = ComponentAccessor.getGroupManager();
        
        // https://docs.atlassian.com/software/jira/docs/api/latest/com/atlassian/jira/security/groups/GroupManager.html#getAllGroups--
        Collection<Group> allGroups = groupMgr.getAllGroups();
        ArrayList<String> groupNames = new ArrayList<String>();
        for(Group g : allGroups){
            groupNames.add(g.getName());
        }
        
        UserUtil userUtil = ComponentAccessor.getUserUtil();
        Set<ApplicationUser> usersInGroups = userUtil.getAllUsersInGroupNamesUnsorted(groupNames);
        int numberOfActiveUsers = usersInGroups != null ? usersInGroups.size() : 0;
        */
        
        /*
        // https://confluence.atlassian.com/display/JIRAKB/What+happens+when+I+reach+my+licenses+user+limit+in+JIRA
        if(numberOfExistingUsers >= numberOfUsersAllowed && numberOfUsersAllowed > 0){
            
            //addMessageToResponse(i18nResolver.getText("secsignid.messages.error.license.exceeded.description"), "Warning", true, null);
            //addMessageToResponse(i18nResolver.getText("secsignid.messages.error.license.exceeded", new Integer(numberOfExistingUsers), new Integer(numberOfExistingUsers)), "Info", true, null);
            
            addErrorMessage(i18nResolver.getText("secsignid.messages.error.license.exceeded.description"));
            addErrorMessage(i18nResolver.getText("secsignid.messages.error.license.exceeded", new Integer(numberOfExistingUsers), new Integer(numberOfExistingUsers)));
            
            this.licenseExceeded = true;
        }
        */
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the fullname
     */
    public String getFullname() {
        return fullname;
    }

    /**
     * @param fullname the fullname to set
     */
    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the secsignid
     */
    public String getSecsignid() {
        return secsignid;
    }

    /**
     * @param secsignid the secsignid to set
     */
    public void setSecsignid(String secsignid) {
        this.secsignid = secsignid;
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
     * @return the returnUrl
     */
    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     * @param returnUrl the returnUrl to set
     */
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
    
}
