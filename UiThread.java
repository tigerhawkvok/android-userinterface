package com.velociraptorsystems.userInterface;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.base.Splitter;

public class UiThread extends Fragment {
    private static Handler sHandler;
    private static int NUMBER_OF_CORES =
            Runtime.getRuntime().availableProcessors();
    // A queue of Runnables
    private final static BlockingQueue<Runnable> mDecodeWorkQueue = new LinkedBlockingQueue<Runnable>();
    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    // Creates a thread pool manager
    private static final ThreadPoolExecutor mDecodeThreadPool = new ThreadPoolExecutor(
            NUMBER_OF_CORES,       // Initial pool size
            NUMBER_OF_CORES,       // Max pool size
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            mDecodeWorkQueue);

    public static Handler getHandler() {
        if (sHandler == null) {
            synchronized (UiThread.class) {
                if (sHandler == null) {
                    sHandler = new Handler(Looper.getMainLooper());
                }
            }
        }

        return sHandler;
    }

    public static void run(Runnable command) {
    	//mDecodeThreadPool.execute(command);
        getHandler().post(command);
    }
    
    public static void runOffThread(Runnable command) {
    	mDecodeThreadPool.execute(command);
    }

    public static void toast(final Context context, final String msg, final int duration) {
        run(new Runnable() {
            @Override
            public void run() {
            	try {
                Toast.makeText(context, msg, duration).show();
            	} catch (NullPointerException e) {
            		Log.e("toast","Tried to toast with a bad pointer");
            		e.printStackTrace();
            	}
            }
        });
    }

    public static void toast(final Context context, final String msg) {
        toast(context,msg,Toast.LENGTH_SHORT);
    }
    
    public static void drawCard(final Context context,final LinearLayout fragContainer, final Wine w) {
    	// Draw the card to the screen using the given fragments
    	//LinearLayout fragContainer = (LinearLayout) findViewById(R.id.wrapperLL);
    	run(new Runnable() {
    		@Override
    		public void run() {
		w.setContext(context);
		try {
			LinearLayout v = w.renderCard();
			if(v != null) {
				fragContainer.addView(v);
			}
		} catch (Exception e) {
			Log.w("drawCard","Didn't get the expected output");
		}
    		}
    	});
    }

    
    public static void chunkImage(final Context c, final SwipeRefreshLayout s, final Wine w, final ImageView im) {
    	runOffThread(new Runnable() {
    		@Override
    	public void run() {
                //s.setRefreshing(true);
    			Log.d("drawWine", "Chunking ...");
                w.setChunkState(true);
        String b64image = w.getImage64();

        // Chunk it as expected by the processor
        //String[] chunks = b64image.split("(?<=\\G.{3000})");
                Iterable<String> chunks = Splitter.fixedLength(2048).split(b64image);
        int i = 0;
        String chunked = "";
        for(String v : chunks) {
        	try {
        	chunked +=  "&i"+Integer.toString(i)+"="+Uri.encode(v);
        	} catch (OutOfMemoryError e) {
        		try {
        			w.removeLeastRecent();
        		UiThread.toast(c, "Couldn't process the image. Try again with a smaller image and report this.", Toast.LENGTH_LONG);
        		} catch (NullPointerException e2) {
        			Log.e("chunkImage","Got a null pointer exception trying to show a toast that there was a processing issue");
        		}
        		s.setRefreshing(false);
        		try {
        			w.setBitmap(null);
        		} catch (Exception e2) {
        			Log.e("chunkException","Got an exception trying to unset the image - "+e2.getMessage());
        			e2.printStackTrace();
        		}
        		return;
        	}
        	i++;
            if (i%500 == 0) {
                Log.v("drawWine","On chunk iteration "+Integer.toString(i));
            }
        }
        chunked+="&ilen="+Integer.toString(i);
                Log.v("drawWine","Set a chunked length of "+Integer.toString(i)+" parts");
        w.setChunkedArgs(chunked);
        //s.setRefreshing(false);
      }
    	});
    }
    
    public static void updateTextView(final TextView v,final String m) {
    	run(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.d("updateTextView","Updating textview -- initial as "+v.getText().toString());
				v.setText(m);
			}
    		
    	});
    }
    
    public static void updateImageView(final PopupWindow p, final ImageView i, final Bitmap b) {
    	run(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.d("updateImageView","Updating image view ...");
				try {
				i.setImageBitmap(null);
				Log.d("updateImageView","Removed old bitmap");
				} catch (Exception e) {
					Log.w("updateImageView","Could not unset old image");
					e.printStackTrace();
				}
				try {
					//i.wait(500);				
					i.setImageBitmap(b);	
					Log.d("updateImageView","Set image by getImage()");
					if(b == null) {
						Log.w("updateImageView", "Warning: Image was null");
					} else {
						// The content of the popup needs to update with a correct imageview! 
						// The content is all there, it's just not showing yet
						i.setScaleType(ScaleType.CENTER_INSIDE);
						p.update();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
    		
    	});
    }
}
