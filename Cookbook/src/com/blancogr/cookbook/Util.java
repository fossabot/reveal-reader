package com.blancogr.cookbook;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.blancogr.cookbook.R;

public class Util {
	public static void showSplashScreen(Context _this) {
	// Toast Splash with image :)
		Toast toast = new Toast(_this);
		LinearLayout lay = new LinearLayout(_this);
		lay.setOrientation(LinearLayout.HORIZONTAL);
		ImageView view = new ImageView(_this);
		view.setImageResource(R.drawable.splash);
		lay.addView(view);
		toast.setView(lay);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();
	}
}
