package org.ecocean.security;




import org.ecocean.*;

import java.io.File;
import java.util.HashMap;
import java.lang.Exception;

import org.ecocean.servlet.ServletUtilities;

import javax.servlet.http.HttpServletRequest;

import com.stormpath.sdk.client.Clients;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.api.ApiKeys;
import com.stormpath.sdk.api.ApiKey;
import com.stormpath.sdk.tenant.*;
import com.stormpath.sdk.application.*;
import com.stormpath.sdk.account.*;
import com.stormpath.sdk.application.*;
import com.stormpath.sdk.directory.*;
import com.stormpath.sdk.resource.ResourceException;
import com.stormpath.sdk.authc.AuthenticationResult;
import com.stormpath.sdk.authc.UsernamePasswordRequest;

/************************************************************
      DOCS:  http://docs.stormpath.com/java/quickstart/
*************************************************************/

public class Stormpath {

    private static String apiKeyPath = System.getProperty("user.home") + "/.stormpath/apiKey.properties";

    /*  note: docs say "The client instance is intended to be an application singleton. You should reuse this instance throughout your application code.
        You should not create multiple Client instances as it could negatively affect caching." ... so this is what we reuse */
    private static Client myClient = null;

    // we cache the application too.  is this lame?
    private static Application myApplication = null;

    public static Client getClient(HttpServletRequest request) {
        if (myClient != null) return myClient;
        String context = "context0";
        context = ServletUtilities.getContext(request);

        //TODO must be a better way to go from request -> path where .properties files are ???
        String propPath = request.getSession().getServletContext().getRealPath("/") + "/WEB-INF/classes/bundles/stormpathApiKey.properties";
//System.out.println("propPath -> " + propPath);
        ApiKey apiKey = ApiKeys.builder().setFileLocation(propPath).build();
        myClient = Clients.builder().setApiKey(apiKey).build();
        //If using Google App Engine, you must use Basic authentication:
        //Client client = Clients.builder().setApiKey(apiKey)
        //    .setAuthenticationScheme(AuthenticationScheme.BASIC)
        //    .build();
        return myClient;
    }

/*
    public Application getApplication() {
        if (myClient == null) return null;
        return getApplication(myClient);
    }
*/

    //just get default (no appName passed)
    public static Application getApplication(Client client) {
        return getApplication(client, null);
    }

    public static Application getApplication(Client client, String appName) {
        //NOTE DANGER! this caching assumes only one application will be used ever.  lame? maybe!
        if (myApplication != null) return myApplication;

        if (appName == null) appName = "animalus";  //default  TODO ok?  read from properties?  etc.
        Tenant tenant = client.getCurrentTenant();
        ApplicationList applications = tenant.getApplications(
            Applications.where(Applications.name().eqIgnoreCase(appName))
        );
        myApplication = applications.iterator().next();
        return myApplication;
    }

    //note: username and custom are optional (username becomes email address if not provided); the rest are required
    public static Account createAccount(Client client, String givenName, String surname, String email, String password, String username, HashMap<String,String> custom) throws Exception {
        if (isEmpty(givenName) || isEmpty(surname) || isEmpty(email) || isEmpty(password)) throw new Exception("missing required fields to create user");
        Account account = client.instantiate(Account.class);
        account.setGivenName(givenName);
        account.setSurname(surname);
        account.setEmail(email);
        account.setPassword(password);
        if (!isEmpty(username)) account.setUsername(username);

        if (custom != null) {
            CustomData customData = account.getCustomData();
            for (String k : custom.keySet()) {
                customData.put(k, custom.get(k));
            }
        }
        Application app = getApplication(client);
        app.createAccount(account);
        return account;
    }


    //note: "username" can also be email, apparently
    public static Account loginAccount(Client client, String username, String password) throws ResourceException {
        Application app = getApplication(client);
        UsernamePasswordRequest req = new UsernamePasswordRequest(username, password);
        AuthenticationResult res = app.authenticateAccount(req);
        return res.getAccount();
    }

    private static boolean isEmpty(String s) {
        return ((s == null) || s.equals(""));
    }


}