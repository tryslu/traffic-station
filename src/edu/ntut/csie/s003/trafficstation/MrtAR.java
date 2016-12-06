package edu.ntut.csie.s003.trafficstation;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.SensorsComponentAndroid;
import com.metaio.sdk.jni.AnnotatedGeometriesGroupCallback;
import com.metaio.sdk.jni.EGEOMETRY_FOCUS_STATE;
import com.metaio.sdk.jni.IAnnotatedGeometriesGroup;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IGeometryVector;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.IRadar;
import com.metaio.sdk.jni.LLACoordinate;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.SensorValues;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.SystemInfo;
import com.metaio.tools.io.AssetsManager;


@SuppressLint("InflateParams")
public class MrtAR extends ARViewActivity implements SensorsComponentAndroid.Callback{
	private IAnnotatedGeometriesGroup mAnnotatedGeometriesGroup;
	private MyAnnotatedGeometriesGroupCallback mAnnotatedGeometriesGroupCallback;

	private IRadar mRadar;
	
	private ArrayList<String> _taipeiMrtStopList;
	private ArrayList<IGeometry> _mIGeometryList = new ArrayList<IGeometry>();
	
	private LLACoordinate _myselfLLA;
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
	//private static final long MIN_TIME_BW_UPDATES = 1000 * 30 * 1; // 30sec
	private long _showRange = 1000;//1000 meters
	private String TAG = "lys_debug";
	private SharedPreferences _sp;
	public static String PREF = "pref";
	
	//Dialog
		//Title
	private TextView _stopNameTextView;
	private ImageView _stopLogoImageView;
	private ImageView _refreshImageView;
	private String _mrtStop="MRT";
	private boolean _dialogIsShowing = false;
		//MRTDialog
	private Spinner _mrtStartSpinner, _mrtDestinationSpinner;
	private ArrayList<String> _mrtList;
	private String _mrtSelectStart = "松山機場", _mrtSelectDestination = "松山機場";
	private ArrayAdapter<String> _mrtAdapter;
	private Button _mrtSearchButton;
	private TextView _mrtTicketTextView, _mrtCardTextView, _mrtOtherTextView;
	private TextView _mrtTicketCostTextView, _mrtCardCostTextView, _mrtOtherCostTextView;
	private JSONArray _ticket;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch(msg.what){
				case 100://click poi
					if(msg.obj!=null){
						clickMrtPOI();
					}
					break;
				case 101://fetch runnable
					if(msg.obj!=null){
						try {
							Log.i(TAG, "get ticket jsonArray");
							Log.i(TAG, "jsonArray content : " + msg.obj.toString());
							_ticket = new JSONArray(msg.obj.toString());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					break;
				case 102://refresh runnable
					break;
				case 999:
					Toast.makeText(MrtAR.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
			}
		}
	};
	
	private Runnable fetchRunnable = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = new Message();
			try {
				HippoWebService connect = new HippoWebService();
				String api = "http://data.taipei.gov.tw/opendata/apply/json/MzRGNkFFMkUtNEYyMi00ODk2LTlEMzItQUVFOENBMzY5QUVE";
				String ret = connect.getMethod2(api, "utf-8");
			    if(ret!=null)
			    {
			    	msg.what = 101;
			    	msg.obj = ret;
			    }
			    else
			    {
			    	msg.what = 999;
			    	msg.obj = "Error";
			    }
			} catch (Exception e) {
				// TODO: handle exception
				msg.what = 999;
				msg.obj = e.toString();
			}
			mHandler.sendMessage(msg);
		}
	};
	
	private void initLocation(){
		Bundle extras = MrtAR.this.getIntent().getExtras();
		if(extras!=null){
			_myselfLLA = new LLACoordinate(extras.getDouble("my_lat"),
										   extras.getDouble("my_lng"),
										   0.0,
										   0.0);
		}else{
			_myselfLLA = new LLACoordinate(0.0, 0.0, 0.0, 0.0);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		_sp = getSharedPreferences(Settings.PREF, Context.MODE_PRIVATE);
		_showRange = (long)_sp.getInt(Settings.KEY_MRT_SEARCH_RANGE, 1000);
		
		// Set GPS tracking configuration
		boolean result = metaioSDK.setTrackingConfiguration("GPS", false);
		MetaioDebug.log("Tracking data loaded: " + result);
		
		String[] taipeiMrtStop = getResources().getStringArray(R.array.mrtstop);
		_taipeiMrtStopList = new ArrayList<String>(Arrays.asList(taipeiMrtStop));
		
		String[] singleMrtStop = getResources().getStringArray(R.array.mrtsinglestop);
		_mrtList = new ArrayList<String>(Arrays.asList(singleMrtStop));
		
		initLocation();
		
		new Thread(fetchRunnable).start();
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		mSensors.registerCallback(this);
		SensorsComponentAndroid.LM_MINDISTANCE = MIN_DISTANCE_CHANGE_FOR_UPDATES;
		//SensorsComponentAndroid.LM_MINTIME = MIN_TIME_BW_UPDATES;
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mSensors.resume();
		LLACoordinate lla = mSensors.getLocation();
		if(lla!=null){
			_myselfLLA = new LLACoordinate(lla);
		}
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		//mSensors.pause();
		super.onPause();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		//mSensors.stop();
		super.onStop();
	}

	@Override
	protected void onDestroy()
	{
		//mSensors handle in ARView Activity.
		
		// Break circular reference of Java objects
		if (mAnnotatedGeometriesGroup != null)
		{
			mAnnotatedGeometriesGroup.registerCallback(null);
		}
		
		if (mAnnotatedGeometriesGroupCallback != null)
		{
			mAnnotatedGeometriesGroupCallback.delete();
			mAnnotatedGeometriesGroupCallback = null;
		}

		super.onDestroy();
		System.gc();
	}
	
	@Override
	public void onDrawFrame()
	{
		if (metaioSDK != null && mSensors != null)
		{
			SensorValues sensorValues = mSensors.getSensorValues();

			float heading = 0.0f;
			if (sensorValues.hasAttitude())
			{
				float m[] = new float[9];
				sensorValues.getAttitude().getRotationMatrix(m);

				Vector3d v = new Vector3d(m[6], m[7], m[8]);
				v = v.normalize();

				heading = (float)(-Math.atan2(v.getY(), v.getX()) - Math.PI/2.0);
			}
			
			Rotation rot = new Rotation((float)(Math.PI/2.0), 0.0f, -heading);
			for (IGeometry geo : _mIGeometryList)
			{
				if (geo != null)
				{
					geo.setRotation(rot);
				}
			}
		}
		super.onDrawFrame();
	}

	@Override
	protected int getGUILayout()
	{
		return R.layout.mrt_ar;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler()
	{
		return null;
	}

	@Override
	protected void loadContents()
	{
		//Log.i(TAG, "loadContent");
		
		mAnnotatedGeometriesGroup = metaioSDK.createAnnotatedGeometriesGroup();
		mAnnotatedGeometriesGroupCallback = new MyAnnotatedGeometriesGroupCallback();
		mAnnotatedGeometriesGroup.registerCallback(mAnnotatedGeometriesGroupCallback);
		mAnnotatedGeometriesGroup.setBottomPadding(400);
		
		// Clamp geometries' Z position to range [5000;200000] no matter how close or far they are away.
		// This influences minimum and maximum scaling of the geometries (easier for development).
		metaioSDK.setLLAObjectRenderingLimits(50, 200);

		// Set render frustum accordingly
		metaioSDK.setRendererClippingPlaneLimits(10, 220000);
		
		// create radar
		mRadar = metaioSDK.createRadar();
		mRadar.setBackgroundTexture(AssetsManager.getAssetPath(getApplicationContext(), "radar_back.png"));
		mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath(getApplicationContext(), "point_mrt.png"));
		mRadar.setRelativeToScreen(IGeometry.ANCHOR_BR);
		
		// let's create LLA objects
		
		// Load some POIs. Each of them has the same shape at its geoposition. We pass a string
		// (const char*) to IAnnotatedGeometriesGroup::addGeometry so that we can use it as POI title
		// in the callback, in order to create an annotation image with the title on it.
		
		for (String stopInfomation : _taipeiMrtStopList) {
			//0=index, 3=Chinese name, 5=latitude, 6=longitude
			String[] split = stopInfomation.split(",");
			LLACoordinate stop = new LLACoordinate(Double.valueOf(split[5]), Double.valueOf(split[6]), 0.0, 0.0);
			double distance = _myselfLLA.distanceTo(stop);
			if(distance <= _showRange){
				IGeometry ig = createPOIGeometry(stop);
				ig.setName(stopInfomation+","+String.valueOf((int)distance));
				_mIGeometryList.add(ig);
				mAnnotatedGeometriesGroup.addGeometry(ig, stopInfomation+","+String.valueOf((int)distance));
				mRadar.add(ig);
			}
		}
	}

	private IGeometry createPOIGeometry(LLACoordinate lla)
	{
		String path = AssetsManager.getAssetPath(MrtAR.this, "mrt_poi.obj");
		if (path != null)
		{
			IGeometry geo = metaioSDK.createGeometry(path);
			geo.setTranslationLLA(lla);
			geo.setLLALimitsEnabled(true);
			geo.setScale(100);
			return geo;
		}
		else
		{
			MetaioDebug.log(Log.ERROR, "Missing files for POI geometry");
			return null;
		}
	}

	@Override
	protected void onGeometryTouched(final IGeometry geometry)
	{
		MetaioDebug.log("Geometry selected: "+geometry);
		if(mSurfaceView!=null){
			mSurfaceView.queueEvent(new Runnable()
			{
				@Override
				public void run()
				{
					mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath(getApplicationContext(), "point_mrt.png"));
					mRadar.setObjectTexture(geometry, AssetsManager.getAssetPath(getApplicationContext(), "red.png"));
					mAnnotatedGeometriesGroup.setSelectedGeometry(geometry);
				}
			});
		}
		if(geometry == null)
			Log.i(TAG, "geometry is null");
		if((!_dialogIsShowing) && (geometry!=null)){
			_dialogIsShowing = true;
			final String[] split = geometry.getName().toString().split(",");
			_mrtStop = split[3];
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					Message msg = new Message();
					msg.what = 100;
					msg.obj = split[3];
					mHandler.sendMessage(msg);
				}
			}).start();
		}
	}

	final class MyAnnotatedGeometriesGroupCallback extends AnnotatedGeometriesGroupCallback
	{
		@Override
		public IGeometry loadUpdatedAnnotation(IGeometry geometry, Object userData,
				IGeometry existingAnnotation)
		{
			if (userData == null)
			{
				return null;
			}

			if (existingAnnotation != null)
			{
				// We don't update the annotation if e.g. distance has changed
				return existingAnnotation;
			}

			//String title = (String)userData; // as passed to addGeometry
			String[] split = userData.toString().split(",");
			String title = split[3];//split{index,x,x,Chinese name,x,latitude,longitude,distance}
			String distance = split[7];
			String texturePath = getAnnotationImageForTitle(title+","+distance);

			return metaioSDK.createGeometryFromImage(texturePath, true, false);
		}
		
		@Override
		public void onFocusStateChanged(IGeometry geometry, Object userData,
				EGEOMETRY_FOCUS_STATE oldState, EGEOMETRY_FOCUS_STATE newState) 
		{
			MetaioDebug.log("onFocusStateChanged for "+(String)userData+", "+oldState+"->"+newState);
			if(newState == EGEOMETRY_FOCUS_STATE.EGFS_SELECTED){
				mAnnotatedGeometriesGroup.setSelectedGeometry(null);
			}
		}
	}
	
	private String getAnnotationImageForTitle(String titleAndDistance)
	{
		Bitmap billboard = null;
		String[] split = titleAndDistance.split(",");
		String title = split[0];
		String distance = split[1];
		try
		{
			final String texturepath = getCacheDir() + "/" + title + ".png";
			Paint mPaint = new Paint();

			// Load background image and make a mutable copy
			
			float dpi = SystemInfo.getDisplayDensity(getApplicationContext());
			int scale = dpi > 240 ? 2 : 1;
			String filepath = AssetsManager.getAssetPath(getApplicationContext(), "mrt_POI_bg" + (scale == 2 ? "@2x" : "") + ".png");
			Bitmap mBackgroundImage = BitmapFactory.decodeFile(filepath);

			billboard = mBackgroundImage.copy(Bitmap.Config.ARGB_8888, true);

			Canvas c = new Canvas(billboard);

			//mPaint.setColor(Color.WHITE);
			mPaint.setColor(Color.BLACK);
			mPaint.setTextSize(48);
			mPaint.setTypeface(Typeface.DEFAULT);
			//mPaint.setTextAlign( Paint.Align.CENTER );
			mPaint.setTextAlign(Paint.Align.CENTER);
			
//			float y = 40 * scale;
//			float x = 30 * scale;

			// Draw POI name
			if (title.length() > 0)
			{
				String n = title.trim();

				//final int maxWidth = 160 * scale;
				final int maxWidth = 320 * scale;
				
				int i = mPaint.breakText(n, true, maxWidth, null);

				int xPos = (c.getWidth() / 2);
				//int xPos = 0 + 24 + 12;
				int yPos = (int) ((c.getHeight() / 2) - ((mPaint.descent() + mPaint.ascent())  / 2 ) - 12) ; 
				c.drawText(n.substring(0, i), xPos, yPos, mPaint);
				
				// Draw second line if valid
//				if (i < n.length())
//				{
//					 n = n.substring(i);
//					 y += 20 * scale;
//					 i = mPaint.breakText(n, true, maxWidth, null);
//
//					 if (i < n.length())
//					 {
//							i = mPaint.breakText(n, true, maxWidth - 20*scale, null);
//							c.drawText(n.substring(0, i) + "...", x, y, mPaint);
//					 }
//					 else
//					 {
//							c.drawText(n.substring(0, i), x, y, mPaint);
//					 }
//				}
				
				//Draw POI distance
				mPaint.setTextSize(36);
				mPaint.setTextAlign( Paint.Align.RIGHT );
				int xPosi = (c.getWidth() - 36 + 12);
				int yPosi = (int) ((c.getHeight())  - 12);// - ((mPaint.descent() + mPaint.ascent()) / 2)) ;
				c.drawText("約 " + distance +"m", xPosi, yPosi, mPaint);
				
			}

			// Write texture file
			try
			{
				FileOutputStream out = new FileOutputStream(texturepath);
				billboard.compress(Bitmap.CompressFormat.PNG, 90, out);
				MetaioDebug.log("Texture file is saved to "+texturepath);
				return texturepath;
			}
			catch (Exception e)
			{
				MetaioDebug.log("Failed to save texture file");
				e.printStackTrace();
			}
		}
		catch (Exception e)
		{
			MetaioDebug.log("Error creating annotation texture: " + e.getMessage());
			MetaioDebug.printStackTrace(Log.DEBUG, e);
			return null;
		}
		finally
		{
			if (billboard != null)
			{
				billboard.recycle();
				billboard = null;
			}
		}

		return null;
	}

	@Override
	public void onGravitySensorChanged(float[] gravity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onHeadingSensorChanged(float[] orientation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLocationSensorChanged(LLACoordinate location) {
		// TODO Auto-generated method stub
		_myselfLLA = new LLACoordinate(location);
		Log.i("debug", "my location lat:" + location.getLatitude() + " lng:" + location.getLongitude());
		if(mSurfaceView!=null){
			mSurfaceView.queueEvent(new Runnable()
			{
				@Override
				public void run()
				{
					if(mAnnotatedGeometriesGroup!=null && mRadar!=null){
						_mIGeometryList.clear();
						mRadar.removeAll();
						IGeometryVector geometries = metaioSDK.getLoadedGeometries();
						for (int i=0; i<geometries.size(); i++)
						{
						    metaioSDK.unloadGeometry(geometries.get(i));
						}
						
						for (String stopInfomation : _taipeiMrtStopList) {
							//0=index, 3=Chinese name, 5=latitude, 6=longitude
							String[] split = stopInfomation.split(",");
							LLACoordinate stop = new LLACoordinate(Double.valueOf(split[5]), Double.valueOf(split[6]), 0.0, 0.0);
							double distance = _myselfLLA.distanceTo(stop);
							if(distance <= _showRange){
								IGeometry ig = createPOIGeometry(stop);
								ig.setName(stopInfomation+","+String.valueOf((int)distance));
								_mIGeometryList.add(ig);
								mAnnotatedGeometriesGroup.addGeometry(ig, stopInfomation+","+String.valueOf((int)distance));
								mRadar.add(ig);
							}
						}
					}
				}
			});
		}
	}
	
	private void clickMrtPOI()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(MrtAR.this);
	    LayoutInflater inflater = MrtAR.this.getLayoutInflater();
	    View view = inflater.inflate(R.layout.dialog_mrt, null);
	    
	    //InitFinViewById
		_stopNameTextView = (TextView) view.findViewById(R.id._dialogTitleTextView);
		_stopLogoImageView = (ImageView) view.findViewById(R.id._dialogTitleImageView);	
		_refreshImageView = (ImageView) view.findViewById(R.id._dialogRefreshImageView);
		
		_mrtStartSpinner = (Spinner) view.findViewById(R.id._mrtStartSpinner);
		_mrtDestinationSpinner = (Spinner) view.findViewById(R.id._mrtDestinationSpinner);
		_mrtSearchButton = (Button) view.findViewById(R.id._mrtSearchButton);
		_mrtTicketTextView = (TextView) view.findViewById(R.id._mrtTicketTextView);
        _mrtCardTextView = (TextView) view.findViewById(R.id._mrtCardTextView);
        _mrtOtherTextView = (TextView) view.findViewById(R.id._mrtOtherTextView);
        _mrtTicketCostTextView = (TextView) view.findViewById(R.id._mrtTicketCostTextView);
        _mrtCardCostTextView = (TextView) view.findViewById(R.id._mrtCardCostTextView);
        _mrtOtherCostTextView = (TextView) view.findViewById(R.id._mrtOtherCostTextView);
        
        _mrtTicketTextView.setVisibility(View.GONE);
        _mrtCardTextView.setVisibility(View.GONE);
        _mrtOtherTextView.setVisibility(View.GONE);
		
        _mrtTicketCostTextView.setVisibility(View.GONE);
        _mrtCardCostTextView.setVisibility(View.GONE);
        _mrtOtherCostTextView.setVisibility(View.GONE);
        
	    //SetTitle
	    _stopLogoImageView.setImageDrawable(getResources().getDrawable(R.drawable.mrt_state_button));
	    _stopNameTextView.setText(_mrtStop);	 
	    _refreshImageView.setVisibility(View.GONE);
	    
	    _mrtAdapter = new ArrayAdapter<String>
		(
		    MrtAR.this, R.layout.mrt_list, _mrtList
		);	    
	    _mrtDestinationSpinner.setAdapter(_mrtAdapter);
	    _mrtDestinationSpinner.setSelection(0);
	    _mrtStartSpinner.setAdapter(_mrtAdapter);
	    _mrtStartSpinner.setSelection(0);

	    _dialogIsShowing = false;
	    builder.setView(view)
	           .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() 
	           {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	            	   dialog.dismiss();
	               }
	           })      
	           .show();
	    
	    _mrtStartSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener()
	    {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
			{
				_mrtSelectStart = _mrtStartSpinner.getSelectedItem().toString();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
	    });
	    
	    _mrtDestinationSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener()
	    {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
			{
				_mrtSelectDestination = _mrtDestinationSpinner.getSelectedItem().toString();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
	    });
	    
	    _mrtSearchButton.setOnClickListener(new Button.OnClickListener()
	    {
			@Override
			public void onClick(View v) 
			{
//				"Departure_Station":"小碧潭"
//				"Destination_Station":"台北車站"
//				"One_Way_Ticket":"30"
//				"EasyCard":"24"
//				"EasyCard_Senior_Disabled":"12"
//				"X":null
//				"Y":null
				try {
					if(_ticket!=null){
						for(int i=0; i<_ticket.length(); i++){
							JSONObject jo = new JSONObject(_ticket.getJSONObject(i).toString());
							if(jo.getString("Departure_Station").equals(_mrtSelectStart) && jo.getString("Destination_Station").equals(_mrtSelectDestination)){
								_mrtTicketCostTextView.setText(jo.getString("One_Way_Ticket") + "元");
								_mrtCardCostTextView.setText(jo.getString("EasyCard") + "元");
								_mrtOtherCostTextView.setText(jo.getString("EasyCard_Senior_Disabled") + "元");
							}
						}
						_mrtTicketTextView.setVisibility(View.VISIBLE);
				        _mrtCardTextView.setVisibility(View.VISIBLE);
				        _mrtOtherTextView.setVisibility(View.VISIBLE);
						
				        _mrtTicketCostTextView.setVisibility(View.VISIBLE);
				        _mrtCardCostTextView.setVisibility(View.VISIBLE);
				        _mrtOtherCostTextView.setVisibility(View.VISIBLE);
					}
				} catch (Exception e) {
					// TODO: handle exception
				}			
			}	    	
	    });
	}
}
