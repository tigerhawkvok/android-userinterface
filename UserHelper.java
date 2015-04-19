package com.velociraptorsystems.userInterface;

/**
 * Created by Philip on 2015-03-27.
 */
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.regex.PatternSyntaxException;

import com.afollestad.materialdialogs.MaterialDialog;
import com.velociraptorsystems.winelist.*;

import org.json.JSONException;
import org.json.JSONObject;

public class UserHelper  extends HTTPFunctions2 {
    private String email = null;
    private SharedPreferences sharedPref;
    private Context base_c;
    private static final String PREFS_NAME = "storedPrefs";
    private static final String SERVER_SECRET = "serverSecretForCompute";
    private static final String ENCRYPTION_KEY = "serverSecretDecryptionKey";
    private static final String DEVICE_IDENTIFIER = "uniqueDeviceId";
    private static final String ENDPOINT = "endpointTargetUri";
    private static final String USERNAME = "email";
    private String ENDPOINT_TARGET = "";

    public UserHelper(Context c) {
        this.setContext(c);

        this.sharedPref = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            this.email = sharedPref.getString(USERNAME,null);
            this.ENDPOINT_TARGET = sharedPref.getString(ENDPOINT,null);
        }
        catch (Exception e) {
            this.email = null;
            this.ENDPOINT_TARGET = null;
        }
    }

    public UserHelper(Context c, String endpointUri) {
        this.setContext(c);

        this.sharedPref = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            this.email = sharedPref.getString(USERNAME,null);
            this.ENDPOINT_TARGET = sharedPref.getString(ENDPOINT,null);
        }
        catch (Exception e) {
            this.email = null;
            this.ENDPOINT_TARGET = null;
        }
        this.setEndpoint(endpointUri);
    }

    public void setContext(Context c) {
        this.base_c = c;
    }

    protected Context getContext() {
        return this.base_c;
    }


    public void setEndpoint(String uri) {

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(ENDPOINT,uri);
        editor.commit();
        this.ENDPOINT_TARGET = uri;
    }

    public String getEndpoint() {
        if (this.ENDPOINT_TARGET != null) {
            return this.ENDPOINT_TARGET;
        }
        Context c = this.getContext();
        this.sharedPref = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            this.ENDPOINT_TARGET = sharedPref.getString(ENDPOINT,null);
        }
        catch (Exception e) {
            this.ENDPOINT_TARGET = null;
        }
        return this.ENDPOINT_TARGET;
    }


    public void setUser(String userEmail) {
        if (!isValidEmail(userEmail)) {
            return;
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(USERNAME,userEmail);
        editor.commit();
        this.email = userEmail;
    }

    public String getUser() {
        if (this.email != null) {
            return this.email;
        }
        Context c = this.getContext();
        this.sharedPref = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            this.email = sharedPref.getString(USERNAME,null);
        }
        catch (Exception e) {
            this.email = null;
        }
        return this.email;
    }

    public void unsetUser() {
        this.setUser(null);
    }

    private String getServerSecret() {
        return sharedPref.getString(SERVER_SECRET,null);
    }

    private void setServerSecret(String secret) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SERVER_SECRET,secret);
        editor.commit();
    }

    private String getEncryptionKey() {
        String key = sharedPref.getString(ENCRYPTION_KEY,null);
        if (key == null) {
            // Generate a new one

            String newKey = this.getRandom();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(ENCRYPTION_KEY,newKey);
            editor.commit();
            key = newKey;
        }
        return key;
    }


    private String getDeviceIdentifier() {
        String id = sharedPref.getString(DEVICE_IDENTIFIER,null);
        if (id == null) {
            // Generate a new one

            String newId = this.getRandom(40);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(DEVICE_IDENTIFIER,newId);
            editor.commit();
            id = newId;
        }
        return id;
    }

    private String getAuthHash(String action, String entropy) throws NoSuchAlgorithmException {
        String secret = this.getServerSecret();
        String authString = entropy + secret + action + this.getAppVersion();
        return this.getSha1(authString);
    }

    protected String getAppVersion() {
        String version;
        Context c = this.getContext();
        PackageManager man = c.getPackageManager();
        PackageInfo info = new PackageInfo();
        try {
            info = man.getPackageInfo(c.getPackageName(), 0);
            version = info.versionName;
        }
        catch (PackageManager.NameNotFoundException e) {
            version = "1.0.0";
        }
        return version;
    }

    public void doQuery(String args) {
        HTTPFunctions2 h = new HTTPFunctions2();
        HTTPFunctions2.Checker async = new Checker(getContext(), getSwipeLayout()) {
            @Override
            public void callBack(JSONObject j) {
                handleCallback(j);
            }
        };
        h.setAsyncChecker(async);
        h.doGenericQuery(getContext(),getEndpoint(),args);
    }

    public void showUserLoginPrompt() throws NullPointerException {
        /***
         * Based off
         * https://github.com/afollestad/material-dialogs#input-dialogs
         ***/
        final Context a = this.getContext();
        if (a == null) {
            throw(new NullPointerException("Invalid context for UserHelper. Set the context first before calling showUserLoginPrompt()"));
        }
        String title, body;
        try {
            title = a.getResources().getString(R.string.user_login_title);
        }
        catch (Exception e) {
            title = "Please log in";
        }
        try {
            body = a.getResources().getString(R.string.user_login_body);
        }
        catch (Exception e) {
            body = "Please enter your login credentials";
        }

        // Build the view


        new MaterialDialog.Builder(a)
                .title(title)
                .content(body)
                //.input(usernameHint,usernamePrefill,loginCallback)
                //.input(passwordHint,passwordPrefill,loginCallback)
                .customView(R.layout.login_existing_user,true)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Stuff and things
                        EditText userField = (EditText) dialog.findViewById(R.id.username_field);
                        String username = userField.getText().toString();
                        if (!isValidEmail(username)) {
                            userField.setError(a.getString(R.string.username_bad_text));
                            userField.requestFocus();
                            return;
                        }
                        EditText passField = (EditText) dialog.findViewById(R.id.password_field);
                        String password = passField.getText().toString();
                        if (!isValidPassword(password,a)) {
                            passField.setError(a.getString(R.string.password_bad_password) + " at least " + a.getString(R.string.password_min_length) + " characters long.");
                            passField.requestFocus();
                            return;
                        }
                        Log.d("userHelperLoginPrompt","Got "+username+" and "+password);
                        dialog.dismiss();
                    }
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.dismiss();
                    }
                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        dialog.dismiss();
                        showUserRegisterPrompt();
                    }
                })
                .autoDismiss(false)
                .positiveText(R.string.user_login_positive)
                .negativeText(R.string.user_login_negative)
                .neutralText(R.string.user_login_neutral)
                .show();
    }

    public void showUserRegisterPrompt() throws NullPointerException{
        /***
         * Based off
         * https://github.com/afollestad/material-dialogs#input-dialogs
         ***/
        final Context a = this.getContext();
        String title, body;
        String firstNameHint, firstNamePrefill, lastNameHint, lastNamePrefill, handleHint, handlePrefill, phoneHint, phonePrefill;
        if (a == null) {
            throw(new NullPointerException("Invalid context for UserHelper. Set the context first before calling showUserLoginPrompt()"));
        }
        try {
            title = a.getResources().getString(R.string.user_login_title);
        }
        catch (Exception e) {
            title = "Please log in";
        }
        try {
            body = a.getResources().getString(R.string.user_login_body);
        }
        catch (Exception e) {
            body = "Please enter your login credentials";
        }
// Build the view


        new MaterialDialog.Builder(a)
                .title(title)
                .content(body)
                .customView(R.layout.login_new_user, true)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Stuff and things
                        EditText userField = (EditText) dialog.findViewById(R.id.username_field);
                        String username = userField.getText().toString();
                        if (!isValidEmail(username)) {
                            userField.setError(a.getString(R.string.username_bad_text));
                            userField.requestFocus();
                            return;
                        }
                        EditText passField = (EditText) dialog.findViewById(R.id.password_field);
                        String password = passField.getText().toString();
                        if (!isValidPassword(password, a)) {
                            passField.setError(a.getString(R.string.password_bad_password) + " at least " + a.getString(R.string.password_min_length) + " characters long.");
                            passField.requestFocus();
                            return;
                        }
                        Log.d("userHelperLoginPrompt", "Got " + username + " and " + password);
                        setUser(username);
                        registerNewDevice(password);
                        dialog.dismiss();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.dismiss();
                    }
                })
                .autoDismiss(false)
                .positiveText(R.string.user_login_create)
                .negativeText(R.string.user_login_negative)
                .show();
    }


    public void registerNewUser(String firstName, String lastName, String handle, String password, int phoneNumber) {

        // If the false return asks for totp ...
        // If the false return asks for phone verification ...
        String args = getAppNewRegistrationString(firstName,lastName,handle,password,phoneNumber);
        String uri = this.getEndpoint();
        HTTPFunctions h = new HTTPFunctions();
        h.doGenericQuery(this.getContext(),uri,args);
    }


    public void registerNewDevice(String password) {
        // If the false return asks for totp ...
        // If the false return asks for phone verification ...
        String args = getAppRegistrationString(password);
        this.doQuery(args);

    }

    public void handleCallback(JSONObject j) {
        /***
         * The primary callback handler.
         * Delegate back to child functions when complex, but handle the case-by-case returns.
         ***/
        try {
            String action = j.getString("action");
            switch (action) {
                case "register":
                    // Call the register back, with the provided vars
                    break;
                default:
                    return;
            }
        }
        catch (JSONException e) {
            Log.w("UserHelper","There was a JSON parsing error in the UserHelper callback.");
        }

    }




    protected String getAppNewRegistrationString(String firstName, String lastName, String handle, String password, int phoneNumber, int phoneVerify, int totp) {
        String user = this.getUser();
        if (user == null) {
            return null;
        }
        String args = "action=register";
        args += "&first_name=" + firstName;
        args += "&last_name=" + lastName;
        args += "&handle=" + handle;
        args += "&password=" + password;
        args += "&new_user=true";
        args += "&device=" + this.getDeviceIdentifier();
        args += "&username=" + user;
        args += "&key=" + this.getEncryptionKey();
        if (!isValidPhone(phoneNumber)) {
            return null;
        }
        args += "&phone=" + Integer.toString(phoneNumber);
        // If phoneVerify or totp are there, append them
        if (Integer.toString(phoneVerify).length() != 1) {
            args += "&phone_verify=" + Integer.toString(phoneVerify);
        }
        if (Integer.toString(totp).length() != 1) {
            args += "&totp=" + Integer.toString(totp);
        }
        return args;
    }
    protected String getAppNewRegistrationString(String firstName, String lastName, String handle, String password, int phoneNumber) {
        return getAppNewRegistrationString(firstName,lastName,handle,password,phoneNumber,0,0);
    }

    protected String getAppRegistrationString(String password, int phoneVerify, int totp) {
        // Only append phoneVerify if phoneVerify is 6 digits
        // Only append totp if totp is 6 digits
        return null;
    }

    protected String getAppRegistrationString(String password) {
        return getAppRegistrationString(password,0,0);
    }

    protected String getAppRegistrationString(String password, int codes) {
        // If either is absent, use the same for both
        return getAppRegistrationString(password,codes,codes);
    }

    protected String getAppAuthorizationString(String action) throws NoSuchAlgorithmException {
        String entropy = getRandom();
        String args = "action=" + action + "&entropy=" + entropy + "&key=" + this.getEncryptionKey();
        args += "&auth=" + this.getAuthHash(action, entropy);
        args += "&device_identifier=" + this.getDeviceIdentifier();
        args += "&user_id=" + this.getUser();
        args += "&app_version=" + this.getAppVersion();
        return args;
    }

    public String getRandom(int length) {
        Random r = new Random();
        r.nextDouble();
        String m = r.toString();
        r.nextDouble();
        m += r.toString();
        try {
            return getSha1(m).substring(0,length);
        }
        catch (NoSuchAlgorithmException e) {
            return convertToHex(Base64.encodeToString(m.getBytes(), Base64.NO_WRAP).getBytes());
        }
    }

    public String getRandom() {
        return getRandom(32);
    }


    public String getSha1(String message) throws NoSuchAlgorithmException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA");
        md.update(message.getBytes());
        byte[] hash = md.digest();
        //String ref64 = new String(Base64.encode(hash, Base64.NO_WRAP));
        String ref64 = convertToHex(hash);
        return ref64;
    }

    private String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }


    public final static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }
    public final static boolean isValidPhone(CharSequence target) {
        if (target == null) {
            return false;
        } else {

            return target.length() == 10 && android.util.Patterns.PHONE.matcher(target).matches();
        }

    }

    public final static boolean isValidPhone(int phone) {
        CharSequence target;
        target = Integer.toString(phone);
        return isValidPhone(target);
    }

    public final static boolean isValidPassword (CharSequence target, Context c) {
        if (target.toString().isEmpty()) {
            return false;
        }
        int minLength = 8;
        try {
            minLength = Integer.parseInt(c.getResources().getString(R.string.password_min_length));
        } catch (Exception e) {
            // Do nothing
        }
        if (target.length() < minLength) {
            return false;
        }
        int thresholdLength = 22;
        try {
            thresholdLength = Integer.parseInt(c.getString(R.string.password_threshold_length));
        }
        catch (Exception e) {
            // Do nothing
        }
        if (target.length() >= thresholdLength) {
            return true;
        }
        boolean foundMatch;
        try {
            foundMatch = target.toString().matches(c.getResources().getString(R.string.password_regex_pattern));
        }
        catch (PatternSyntaxException e) {
            foundMatch = false;
        }
        return foundMatch;
    }
}
