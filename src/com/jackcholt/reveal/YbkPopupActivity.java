/**
 * 
 */
package com.jackcholt.reveal;

import android.view.Menu;

/**
 * Sub-class YbkViewActivity to use dialog theme.
 * 
 * @author Shon Vella
 * 
 */
public class YbkPopupActivity extends YbkViewActivity {

	/**
	 * Don't show an options menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);

		return false;
	}

	@Override
	protected boolean isPopup() {
		return true;
	}

	@Override
	protected void setContentView() {

		setContentView(R.layout.view_popup_ybk);
	}
}
