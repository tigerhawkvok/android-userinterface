package com.velociraptorsystems.userInterface;

import android.app.Activity;
import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.app.Activity.*;
import java.lang.Runnable;

/**
 * Created by Philip on 2015-04-13.
 */
public class UiDecorator {
    private SwipeRefreshLayout sl;
    private Activity parentActivity;

    public UiDecorator (Activity a, int swipeLayoutId) {
        parentActivity = a;
        sl = (SwipeRefreshLayout) a.findViewById(swipeLayoutId) ;
        setSwipeLayoutColors();
        //sl.setEnabled(false);
    }

    private void setSwipeLayoutColors() {
        sl.setColorSchemeResources(
                R.color.MaterialBlue500,
                R.color.MaterialTeal500,
                R.color.MaterialOrange500,
                R.color.MaterialRed500);
    }

    public void enableSwipeToRefresh(SwipeRefreshLayout.OnRefreshListener refreshListener) {
        sl.setEnabled(true);
        sl.setOnRefreshListener(refreshListener);
    }


    public void disableSwipeToRefresh() {
        sl.setEnabled(false);
        sl.setOnRefreshListener(null);
        sl.requestDisallowInterceptTouchEvent(true);
    }

    public void setRefreshing(final boolean state) {
        try {
            parentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
            sl.setRefreshing(state);
                }
            });
        }
        catch (Exception e) {
            Log.e("UiDecoratorRefreshing", "Could not set refreshing - " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void startRefreshing() {
        this.setRefreshing(true);
    }
    public void stopRefreshing() {
        this.setRefreshing(false);
    }
}
