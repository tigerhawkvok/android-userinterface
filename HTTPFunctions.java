package com.velociraptorsystems.userInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;

public class HTTPFunctions extends Fragment {
  private SharedPreferences sharedPref;
  private static final String PREFS_NAME = "storedPrefs";
  private static final String ENDPOINT = "endpointTargetUri";
  String DEFAULT_ARGS = "";
  int DEFAULT_START = 0;
  int DEFAULT_STEP = 10;
  Activity ref;
  SwipeRefreshLayout swipeLayout = null;
  
  public void setRefActivity(Activity a) {
    this.ref = a;
  }
  
  public void setSwipeRefreshLayout(SwipeRefreshLayout s) {
    this.swipeLayout = s;
  }
  
  
  public void unsetDefaultArgs() {
    DEFAULT_ARGS = "";
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

  public Boolean isConnectedToInternet(Context c) {
    // Return true:false based on status ...
    ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	  
  }
    public boolean doGenericQuery(Context c, String target, String args) {
        String uri = target + "?" + args;
        this.creator = null;
        if(!this.isConnectedToInternet(c)) {
            return false;
        }
        Checker async = new Checker(c,this.swipeLayout);
        async.setEndpoint(target);
        async.execute(args);
        return true;
    }

    public boolean doGenericQuery(Context c, String args) {
      String uri = this.getEndpoint() + "?" + args;
        this.creator = null;
        if(!this.isConnectedToInternet(c)) {
            return false;
        }
        Checker async = new Checker(c,this.swipeLayout);
        async.execute(args);
        return true;
    }

  public String sha512(String to_hash) throws NoSuchAlgorithmException {
    MessageDigest md;
    md = MessageDigest.getInstance("SHA512");
    md.update(to_hash.getBytes());
    byte[] hash = md.digest();
    return convertToHex(hash);
  }

  public String getRandom() throws NoSuchAlgorithmException {
    Random r = new Random();
    r.nextDouble();
    MessageDigest md;
    md = MessageDigest.getInstance("SHA");
    md.update(r.toString().getBytes());
    byte[] hash = md.digest();
    String ref64 = new String(Base64.encode(hash, 0));
    ref64 = convertToHex(hash);
    return ref64;
  }
  
  public String getFingerprint(Context c) {
    PackageManager man = c.getPackageManager();
    PackageInfo info = new PackageInfo();
    String fingerprint64 = null;
    String fingerprint = null;
    try {
      info = man.getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNATURES);    	
      for (Signature signature : info.signatures) {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA");
        md.update(signature.toByteArray());
        byte[] hash = md.digest();
        fingerprint64 = new String(Base64.encode(hash, 0));
        fingerprint = convertToHex(hash);
        Log.d("Hash key", fingerprint64);                
        Log.d("Hash key", fingerprint);                
      }
           

    } catch (NoSuchAlgorithmException e) {
      Log.e("No such algorithm", e.toString());
    } 
    catch (Exception e) {
      Log.e("Exception", e.toString()+" while trying to get fingerprint - "+fingerprint);
      e.printStackTrace();
    }
    return fingerprint;
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

  
  private class Checker extends AsyncTask<String,Void,Boolean>{
    Context c;
    SwipeRefreshLayout swipeLayout;
    Boolean finished = false;
    String ENDPOINT = getEndpoint();
    Callable<String> callbackFunc = null;
    
    public Checker(Context context,SwipeRefreshLayout s) {
      this.c = context;
      this.swipeLayout = s;

    }
      public void setEndpoint(String target) {
          this.ENDPOINT = target;
      }

    protected void onPreExecute(){
      super.onPreExecute();
      //display progressdialog.
      Log.d("Checker","Doing async HTTP check");
      try {
        this.swipeLayout.setRefreshing(true);
      } catch(Exception e) {
        Log.w("progressAnimation","Couldn't start progress animation");
      }
    }

    public void setCallback(Callable<String> callback) {
      this.callbackFunc = callback;
    }
    
    protected Boolean doInBackground(String ...params){
      int code=0;
      Boolean r=false;
      Boolean skipToast = false;
      String m;
      
      try {
        if (this.finished) {
          this.onPostExecute(this.finished,"");
          return true;
        }
        URL u = new URL (ENDPOINT);
        HttpURLConnection huc =  (HttpURLConnection) u.openConnection();
        huc.setRequestMethod("POST");
        HttpURLConnection.setFollowRedirects(false);
        huc.connect();
        code = huc.getResponseCode();
        Log.d("Sync","Got response code - "+Integer.toString(code));
        if(code==200) {
          // do POST
          String response = doHttpPost(params[0]);
          Log.d("Sync","Async response - "+response);
          // parse the JSON, set result appropriately
          try {
            JSONObject j = new JSONObject(response);
            r = j.getBoolean("status");
            try {
              if(r) {
            	  try {
            	this.finished = j.getBoolean("finished");
            	  } catch (JSONException e) {
            		  this.finished = false;
            	  }
            	
            	m = "That'll do, pig. I'm finished? "+Boolean.toString(finished);            	
            	if(!finished) {
                  this.callBack(j);
                  skipToast = true;
            	}
            	else {
                  m = "Operation complete";
            	}
              }
              else m = "Failed to save data to server - "+j.get("error");
              Log.d("Sync",m);
              if(skipToast) m = "";
            } catch (Exception e) {
              Log.e("Exception","Could not run callback (got "+Boolean.toString(r)+")");
              e.printStackTrace();
              m = "Couldn't execute callback";
            }
          } catch(Exception e) {
            Log.w("Exception","Exception in parsing json, bad server response");
            Log.w("Exception",response);
            e.printStackTrace();
            m = "Bad server response, try again later";
          }
        }
        else {
          m = "Unable to connect to server";
          Log.w("Network",m);
        }
      } catch (MalformedURLException e) {
        m = "The url "+ENDPOINT+" is malformed.";
        Log.w("Exception",m);
        e.printStackTrace();
      } catch (IOException e) {
        Log.w("Exception","Performing the asynchronous connection returned an IO Exception");
        e.printStackTrace();
        m = "Could not reach server, try again later";
      }
      this.onPostExecute(r, m);
      return r;
    }
    
    protected void onPostExecute(Boolean result,String message){
      super.onPostExecute(result);
      try {
        // Do any post functions
        this.swipeLayout.setRefreshing(false);
      } catch(Exception e) {
        Log.w("progressAnimation","Couldn't stop progress animation");
        e.printStackTrace();
      }
      try {
        //showToast(c,message);
        if (message != "") {
          UiThread.toast(c, message, Toast.LENGTH_SHORT);
        }
      }
      catch(Exception e) {
        Log.w("Sync","Could not toast status - '"+message+"'");
        Log.w("Sync","Toast error - "+e);
      }
      try {
          this.swipeLayout.setRefreshing(false);
      }
      catch (Exception e) {
          Log.v("progressAnimation","Second stop didn't work");
      }
    }
    
    private String doHttpPost(String parameters) {
      /*
       * Alternate ways:
       * https://stackoverflow.com/questions/19803598/android-http-post-sending-json
       * https://stackoverflow.com/questions/19277337/android-post-data-via-url
       */
      HttpURLConnection connection;
      OutputStreamWriter request = null;

      URL url = null;   
      String response = null;         

      try
        {
    	  Log.d("Sync","Initiating post - params "+parameters);
          url = new URL(ENDPOINT);
          connection = (HttpURLConnection) url.openConnection();
          connection.setDoOutput(true);
          connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
          connection.setRequestMethod("POST");    
          Log.d("Sync","Connection variable set - "+connection.toString());
          
          request = new OutputStreamWriter(connection.getOutputStream());
          // Append the app signature to the parameters
          request.write(parameters);
          request.flush();
          request.close();            
          Log.d("Sync","Request successfully made");
          String line = "";               
          InputStreamReader isr = new InputStreamReader(connection.getInputStream());
          BufferedReader reader = new BufferedReader(isr);
          StringBuilder sb = new StringBuilder();
          while ((line = reader.readLine()) != null)
            {
              sb.append(line + "\n");
            }
          Log.d("Sync","Response handled");
          response = sb.toString();
          // handle response
          isr.close();
          reader.close();
          return response;

        }
      catch(IOException e)
        {
          // Error
          Log.w("Exception","IO Exception making HTTP POST request to "+ENDPOINT);
        }
      return null;
    }
    protected void callBack(JSONObject j) {
      // Based on the passed action, do different things
      try {
        Log.d("callback","Executing callback!");
        try {
          this.callbackFunc.call(j);
        }
        catch (Exception e) {
          Log.e("Exception","Callback function exception");
          e.printStackTrace();
        }
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        UiThread.toast(c, "Callback Exception", Toast.LENGTH_LONG);
        Log.e("Exception","General callback exception");
        e.printStackTrace();
      }
    }
  }
  
  public void showToast(final Context context, final String toast)
  {
    try {
      new Thread() {
        @Override
        public void run() {
          Looper.prepare();
          Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
          Looper.loop();
        }
      }.start();
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
  }
  
}
