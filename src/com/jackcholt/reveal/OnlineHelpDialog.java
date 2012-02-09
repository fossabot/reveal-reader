package com.jackcholt.reveal;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


/**
 * HelpDialog for online HELP system
 * 
 * by Dave Packham
 */

public class OnlineHelpDialog extends Dialog {
    final String TAG = this.getClass().getSimpleName();
    Context mCtx;
    private ProgressDialog mProgDialog;
    
    public OnlineHelpDialog(Context ctx) {
        super(ctx);

        mCtx = ctx;
        
        setContentView(R.layout.dialog_help);
        setTitle(R.string.help_dialog_title);
        
        mProgDialog = new ProgressDialog(ctx);
        mProgDialog.setCancelable(true);
        mProgDialog.setMessage(ctx.getResources().getText(R.string.please_wait));
        mProgDialog.show();
        
        new LoadHelp().execute();
    }

    private class LinkWebViewClient extends WebViewClient {
    	@Override
    	public boolean shouldOverrideUrlLoading(WebView view, String url) {
    		view.loadUrl(url);
    		return true;
    	}
    } 
    
    public static OnlineHelpDialog create(Context ctx) {
        return new OnlineHelpDialog(ctx);
    }
    
    private class LoadHelp extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... arg0) {
            return Util.areNetworksUp(mCtx);
        }

        @Override
        protected void onPostExecute(Boolean isNetworkUp) {
            if (isNetworkUp) {
                WebView wv = (WebView) findViewById(R.id.helpView);
                wv.setWebViewClient(new LinkWebViewClient()); 
                wv.clearCache(false);
                wv.getSettings().setJavaScriptEnabled(true);
                wv.loadUrl("http://sites.google.com/site/revealonlinehelp/");
                show();
            } else {
                Log.w(TAG, mCtx.getResources().getString(R.string.cant_get_help_website));
                Toast.makeText(mCtx, mCtx.getResources().getString(R.string.cant_get_help_website),
                        Toast.LENGTH_LONG).show();
            }
            
            mProgDialog.dismiss();
            super.onPostExecute(isNetworkUp);
        }
    }
}