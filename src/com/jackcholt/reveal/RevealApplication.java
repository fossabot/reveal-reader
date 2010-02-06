/**
 * 
 */
package com.jackcholt.reveal;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Configuration;

/**
 * Reveal application.
 * 
 * @author Shon Vella
 * 
 */
public class RevealApplication extends Application {

    /**
     * Default Constructor.
     */
    public RevealApplication() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // clean up any left-over ongoing notifications
        NotificationManager notificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationMgr.cancelAll();

        // TODO - there is a lot of stuff in the Main activity OnStart, that could/should be moved here.
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        try {
            YbkService.stop(this);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
        super.onTerminate();
    }

}
