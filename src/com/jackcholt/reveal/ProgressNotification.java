/**
 * 
 */
package com.jackcholt.reveal;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * @author shon
 * 
 */
public class ProgressNotification extends Notification {

    private Context mCtx;
    private int notifyId;;
    private int lastMax = 100;
    private int lastProgress = 0;

    /**
     * Constructs a notification used to show ongoing progress
     * 
     * @param ctx
     * @param iconId
     * @param label
     */
    public ProgressNotification(Context ctx, int notifyId, int iconId, String message) {
        this.mCtx = ctx;
        this.notifyId = notifyId;
        this.contentIntent = PendingIntent.getActivity(ctx, 0, new Intent(ctx, getClass()), 0);
        this.tickerText = message;
        this.icon = iconId;

        this.contentView = new RemoteViews(ctx.getPackageName(), R.layout.view_progress_notification);
        this.contentView.setTextViewText(R.id.text, message);
        this.contentView.setImageViewResource(R.id.image, iconId);
        this.contentView.setProgressBar(R.id.progress, 100, 0, false);

        this.flags = FLAG_ONGOING_EVENT;
    }

    /**
     * Shows the notification.
     */
    public void show() {
        NotificationManager mgr = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(notifyId, this);
    }

    /**
     * Hides the notification.
     */
    public void hide() {
        NotificationManager mgr = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.cancel(notifyId);
    }

    /**
     * Updates the progress.
     * 
     * @param max
     *            the 100% value for the progress bar.
     * @param progress
     *            the current value for the progress bar.
     */
    public void update(int max, int progress) {
        // only update if changed, because updating is expensive
        if (lastMax != max || lastProgress != progress) {
            contentView.setProgressBar(R.id.progress, max, progress, false);
            NotificationManager mgr = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
            mgr.notify(notifyId, this);
            lastMax = max;
            lastProgress = progress;
        }
    }

}
