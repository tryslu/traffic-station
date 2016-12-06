package edu.ntut.csie.s003.trafficstation;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
public class BusAR extends ARViewActivity implements SensorsComponentAndroid.Callback{
	private IAnnotatedGeometriesGroup mAnnotatedGeometriesGroup;
	private MyAnnotatedGeometriesGroupCallback mAnnotatedGeometriesGroupCallback;

	private IRadar mRadar;
	
	private ArrayList<String> _taipeiBusStopList;
	private ArrayList<IGeometry> _mIGeometryList = new ArrayList<IGeometry>();
	
	private LLACoordinate _myselfLLA;
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
	private static final long MIN_TIME_BW_UPDATES = 1000 * 30 * 1; // 30sec
	private long _showRange = 500;//500 meters
	private String TAG = "lys_debug";
	private SharedPreferences _sp;
	public static String PREF = "pref";
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch(msg.what){
				case 100://click poi
					if(msg.obj!=null){
						_queryString = msg.obj.toString();
						new Thread(fetchRunnable).start();
						if(pd!=null)
							if(pd.isShowing())
								pd.dismiss();
						pd = new ProgressDialog(BusAR.this);
						pd.setMessage("Loading...");
						pd.show();
					}
					break;
				case 101://fetch runnable
					if(msg.obj!=null){
						_tagNode = new HtmlCleaner().clean(msg.obj.toString());
						getBusData();
						if(pd!=null)
							if(pd.isShowing())
								pd.dismiss();
						ClickBusPOI();
					}
					break;
				case 102://refresh runnable
					_tagNode = new HtmlCleaner().clean(msg.obj.toString());
					getBusData();
					if(pd!=null)
						if(pd.isShowing())
							pd.dismiss();

					_busList.clear();
					
					for(int i=0; i<_busNameList.size(); i++)
					{
					    HashMap<String, String> item = new HashMap<String,String>();
					    item.put("bus_name", _busNameList.get(i));   // key, value (公車車號)
					    item.put("bus_time", _busTimeList.get(i));   // key, value (抵達時間)
					    item.put("bus_terminal", _busTerminalList.get(i)); 
					    _busList.add(item);
					}
					
					_busAdapter = new SimpleAdapter
					    (
						    BusAR.this,
						    _busList,
						    R.layout.bus_list,
						    new String[]{"bus_terminal", "bus_name", "bus_time"},  // key的名字
						    new int[]{R.id._busTerminalTextView, R.id._busNameTextView, R.id._busTimeTextView}  // show在哪個textView id
			            );
					
					_busListView.setAdapter(_busAdapter);
					runOnUiThread(new Runnable() {
				        @Override
				        public void run() {
				        	_busAdapter.notifyDataSetChanged();
				        }
				    });
					
					//If List.size > 10, set ListView Height
					if(_busList.size()>10)
					{	
						int _listHeight = 0;//it is the ListView Height
						View _listItem = _busAdapter.getView(0, null, _busListView);
				        _listItem.measure(0, 0);
				        _listHeight = _listItem.getMeasuredHeight()+_busListView.getDividerHeight();//item height
				        
						LinearLayout.LayoutParams _listViewParam = (LinearLayout.LayoutParams) _busListView.getLayoutParams();
						_listViewParam.height = (_listHeight*10);
					    _busListView.setLayoutParams(_listViewParam);
					}
					break;
				case 999:
					Toast.makeText(BusAR.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
			}
		}
	};
	
	private String _queryString = "";
	private TagNode _tagNode;
	private void getBusData(){
		TagNode[] ttego1 = _tagNode.getElementsByAttValue("class", "ttego1", true, false);
		TagNode[] ttego1Detail;
		TagNode[] ttego2 = _tagNode.getElementsByAttValue("class", "ttego2", true, false);
		TagNode[] ttego2Detail;
		
		_busNameList = new ArrayList<String>();
		_busTimeList = new ArrayList<String>();
		_busTerminalList = new ArrayList<String>();
		int i;
		for(i=0; i<ttego2.length; i++){
			ttego1Detail = ttego1[i].getElementsByName("td", true);
			ttego2Detail = ttego2[i].getElementsByName("td", true);
			
			_busNameList.add(ttego1Detail[0].getText().toString());
			_busTerminalList.add(ttego1Detail[2].getText().toString());
			_busTimeList.add(ttego1Detail[3].getText().toString());
			
			_busNameList.add(ttego2Detail[0].getText().toString());
			_busTerminalList.add(ttego2Detail[2].getText().toString());
			_busTimeList.add(ttego2Detail[3].getText().toString());
		}
		if(ttego1.length != ttego2.length){
			ttego1Detail = ttego1[i].getElementsByName("td", true);
			_busNameList.add(ttego1Detail[0].getText().toString());
			_busTerminalList.add(ttego1Detail[2].getText().toString());
			_busTimeList.add(ttego1Detail[3].getText().toString());
		}
	}
	
	private Runnable fetchRunnable = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = new Message();
			try {
				HippoWebService connect = new HippoWebService();
				String[] split = _queryString.split(",");
				_busStop = split[1];
				String api = "http://pda.5284.com.tw/MQS/businfo4.jsp?SLID=" + split[0];
				String ret = connect.getMethod(api, "utf-8");
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
	private Runnable refreshRunnable = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = new Message();
			try {
				HippoWebService connect = new HippoWebService();
				String[] split = _queryString.split(",");
				_busStop = split[1];
				String api = "http://pda.5284.com.tw/MQS/businfo4.jsp?SLID=" + split[0];
				String ret = connect.getMethod(api, "utf-8");
			    if(ret!=null)
			    {
			    	msg.what = 102;
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
	
	//progress dialog
	private ProgressDialog pd;
	//Dialog
		//Title
	private TextView _stopNameTextView;
	private ImageView _stopLogoImageView;
	private ImageView _refreshImageView;
	private String _busStop="Bus";
	private boolean _dialogIsShowing = false;
		//BusDialog
	private ListView _busListView;
	private SimpleAdapter _busAdapter;
	private ArrayList<HashMap<String, String>> _busList;	
	private ArrayList<String> _busNameList, _busTimeList, _busTerminalList;
	
	private void initLocation(){
		Bundle extras = BusAR.this.getIntent().getExtras();
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
		_showRange = (long)_sp.getInt(Settings.KEY_BUS_SEARCH_RANGE, 500);
		
		// Set GPS tracking configuration
		boolean result = metaioSDK.setTrackingConfiguration("GPS", false);
		MetaioDebug.log("Tracking data loaded: " + result);
		
		String[] taipeiBusStop = getResources().getStringArray(R.array.taipeibusstop);
		_taipeiBusStopList = new ArrayList<String>(Arrays.asList(taipeiBusStop));
		
		initLocation();
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
		return R.layout.bus_ar;
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
		mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath(getApplicationContext(), "point_bus.png"));
		mRadar.setRelativeToScreen(IGeometry.ANCHOR_BR);
		
		// let's create LLA objects
		
		// Load some POIs. Each of them has the same shape at its geoposition. We pass a string
		// (const char*) to IAnnotatedGeometriesGroup::addGeometry so that we can use it as POI title
		// in the callback, in order to create an annotation image with the title on it.
		
		for (String stopInfomation : _taipeiBusStopList) {
			//0=index, 1=busStopNumber, 2=Chinese name 3=latitude 4=longitude
			String[] split = stopInfomation.split(",");
			LLACoordinate stop = new LLACoordinate(Double.valueOf(split[3]), Double.valueOf(split[4]), 0.0, 0.0);
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
		String path = AssetsManager.getAssetPath(BusAR.this, "bus_poi.obj");
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
					mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath(getApplicationContext(), "point_bus.png"));
					mRadar.setObjectTexture(geometry, AssetsManager.getAssetPath(getApplicationContext(), "red.png"));
					mAnnotatedGeometriesGroup.setSelectedGeometry(geometry);
				}
			});
		}
		if(geometry == null)
			Log.i(TAG, "geometry is null");
		if((!_dialogIsShowing) && (geometry!=null)){
			final String[] split = geometry.getName().toString().split(",");
			Log.i(TAG, "geometry.getName().toString()=" + geometry.getName().toString());
			Log.i(TAG, "split[0]=" + split[0]);
			Log.i(TAG, "split[1]=" + split[1]);
			_busStop = split[2];
			_dialogIsShowing = true;
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					Message msg = new Message();
					msg.what = 100;
					msg.obj = split[1]+","+split[2];
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
			String title = split[2];//split{index,stopNumber,Chinese name,latitude,longitude,distance}
			String distance = split[5];
			String texturePath = getAnnotationImageForTitle(title+","+distance);

			return metaioSDK.createGeometryFromImage(texturePath, true, false);
		}
		
		@Override
		public void onFocusStateChanged(IGeometry geometry, Object userData,
				EGEOMETRY_FOCUS_STATE oldState, EGEOMETRY_FOCUS_STATE newState) 
		{
			MetaioDebug.log("onFocusStateChanged for "+(String)userData+", "+oldState+"->"+newState);
//			if(!_dialogIsShowing){
				if(newState == EGEOMETRY_FOCUS_STATE.EGFS_SELECTED){
//					final String[] split = userData.toString().split(",");
//					_busStop = split[2];
//					Log.i(TAG, "busStop is " + _busStop + "  onFocusStateChanged:"+newState);
//					_dialogIsShowing = true;
//					new Thread(new Runnable() {
//						@Override
//						public void run() {
//							// TODO Auto-generated method stub
//							Message msg = new Message();
//							msg.what = 100;
//							msg.obj = split[1]+","+split[2];
//							mHandler.sendMessage(msg);
//						}
//					}).start();
					mAnnotatedGeometriesGroup.setSelectedGeometry(null);
				}
//			}
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
			String filepath = AssetsManager.getAssetPath(getApplicationContext(), "bus_POI_bg" + (scale == 2 ? "@2x" : "") + ".png");
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
						
						for (String stopInfomation : _taipeiBusStopList) {
							//0=index, 1=busStopNumber, 2=Chinese name 3=latitude 4=longitude
							String[] split = stopInfomation.split(",");
							LLACoordinate stop = new LLACoordinate(Double.valueOf(split[3]), Double.valueOf(split[4]), 0.0, 0.0);
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
	
	private void ClickBusPOI()
	{
	    AlertDialog.Builder builder = new AlertDialog.Builder(BusAR.this);
	    LayoutInflater inflater = BusAR.this.getLayoutInflater();
	    View view = inflater.inflate(R.layout.dialog_bus, null);
	    _busList = new ArrayList<HashMap<String,String>>();
	    
	    //InitFindViewById
		_stopNameTextView = (TextView) view.findViewById(R.id._dialogTitleTextView);
		_stopLogoImageView = (ImageView) view.findViewById(R.id._dialogTitleImageView);	
		_busListView = (ListView) view.findViewById(R.id._busListView);
		_refreshImageView = (ImageView) view.findViewById(R.id._dialogRefreshImageView);
		
		
		
		for(int i=0; i<_busNameList.size(); i++)
		{
		    HashMap<String, String> item = new HashMap<String,String>();
		    item.put("bus_name", _busNameList.get(i));   // key, value (公車車號)
		    item.put("bus_time", _busTimeList.get(i));   // key, value (抵達時間)
		    item.put("bus_terminal", _busTerminalList.get(i)); 
		    _busList.add(item);
		}
		
		_busAdapter = new SimpleAdapter
		    (
			    BusAR.this,
			    _busList,
			    R.layout.bus_list,
			    new String[]{"bus_terminal", "bus_name", "bus_time"},  // key的名字
			    new int[]{R.id._busTerminalTextView, R.id._busNameTextView, R.id._busTimeTextView}  // show在哪個textView id
            );
		_busListView.setAdapter(_busAdapter);
		
		//If List.size > 10, set ListView Height
		if(_busList.size()>10)
		{	
			int _listHeight = 0;//it is the ListView Height
			View _listItem = _busAdapter.getView(0, null, _busListView);
	        _listItem.measure(0, 0);
	        _listHeight = _listItem.getMeasuredHeight()+_busListView.getDividerHeight();//item height
	        
			LinearLayout.LayoutParams _listViewParam = (LinearLayout.LayoutParams) _busListView.getLayoutParams();
			_listViewParam.height = (_listHeight*10);
		    _busListView.setLayoutParams(_listViewParam);
		}
		
		//SetTitle
	    _stopLogoImageView.setImageDrawable(BusAR.this.getResources().getDrawable(R.drawable.bus_icon));
	    _stopNameTextView.setText(_busStop);
	    //_refreshImageView.setImageDrawable(getResources().getDrawable(R.drawable.refresh));
	    
	    _refreshImageView.setOnClickListener(new ImageView.OnClickListener()
	    {
			@Override
			public void onClick(View v) 
			{
				new Thread(refreshRunnable).start();
				if(pd!=null)
					if(pd.isShowing())
						pd.dismiss();
				pd = new ProgressDialog(BusAR.this);
				pd.setMessage("Loading...");
				pd.show();
			}
	    });

	    _dialogIsShowing = false;
	    builder.setView(view)
	           .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() 
	           {
	               @Override
	               public void onClick(DialogInterface dialog, int id){}
	           })
	           .show();
	}
}
