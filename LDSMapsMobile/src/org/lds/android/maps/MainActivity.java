package org.lds.android.maps;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ZoomControls;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class MainActivity extends MapActivity {	
    /** Called when the activity is first created. */
	
	LinearLayout linearLayout;
	MapView mapView;
	ZoomControls mZoom;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        linearLayout = (LinearLayout) findViewById(R.id.zoomview);
        mapView = (MapView) findViewById(R.id.map_view);
        mZoom = (ZoomControls) mapView.getZoomControls();
        linearLayout.addView(mZoom);
    }
    
    @Override
    protected boolean isRouteDisplayed() {
    	return false;
    }
}