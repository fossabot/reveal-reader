package com.jackcholt.reveal;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * DonateDialog for online Donations system
 * 
 * by Dave Packham
 */

public class DonateDialog extends Dialog {
    Context mCtx;
    final String TAG = this.getClass().getSimpleName();
    private ProgressDialog mProgDialog;

    public DonateDialog(Context ctx) {
        super(ctx);

        mCtx = ctx;

        setContentView(R.layout.dialog_donate);

        mProgDialog = new ProgressDialog(ctx);
        mProgDialog.setCancelable(true);
        mProgDialog.setMessage(ctx.getResources().getText(R.string.please_wait));
        mProgDialog.show();
        
        // Need to use an AsyncTask to avoid doing network stuff on the UI thread.
        new LoadPaypal().execute();
    }

    public static DonateDialog create(Context ctx) {
        return new DonateDialog(ctx);
    }

    private class LoadPaypal extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... arg0) {
            return Util.areNetworksUp(mCtx);
        }

        @Override
        protected void onPostExecute(Boolean isNetworkUp) {
            if (isNetworkUp) {
                mCtx.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=7668278")));
            } else {
                Log.w(TAG, mCtx.getResources().getString(R.string.cant_get_donation_website));
                Toast.makeText(mCtx, mCtx.getResources().getString(R.string.cant_get_donation_website),
                        Toast.LENGTH_LONG).show();
            }

            mProgDialog.dismiss();
            super.onPostExecute(isNetworkUp);
        }
    }
}