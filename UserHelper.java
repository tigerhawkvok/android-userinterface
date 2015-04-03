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
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import com.velociraptorsystems.userInterface.*;

import org.json.JSONException;
import org.json.JSONObject;

public class UserHelper extends HTTPFunctions {
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
  
  public Userhandler(Context c, String endpointUri) {
    UserHelper(c);
    this.setEndpoint(endpointUri);
  }

    public void setContext(Context c) {
        this.base_c = c;
    }

    public Context getContext() {
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

    public void registerNewUser(String firstName, String lastName, String handle, String password, int phoneNumber) {

        // If the false return asks for totp ...
        // If the false return asks for phone verification ...
        String args = getAppNewRegistrationString(firstName,lastName,handle,password,phoneNumber);
        String uri = this.getEndpoint();
        HTTPFunctions h = new HTTPFunctions();
        h.doGenericQuery(this.getContext(),uri,args);
    }


    public void registerNewDevice() {
        // If the false return asks for totp ...
        // If the false return asks for phone verification ...
    }

  @Override
    public void handleCallback(JSONObject j) {
    /***
     * Override the HTTPFunctions handleCallback to parse the JSON
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
  
  
  public void verifyPhone() {
    /***
     * Use a popupwindow / viewinflater to show an overlay 
     * and verify the text message sent to the user.
     ***/
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
}
