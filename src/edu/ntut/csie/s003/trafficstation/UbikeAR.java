package edu.ntut.csie.s003.trafficstation;

import java.io.FileOutputStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import com.metaio.sdk.jni.Vector2d;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.SystemInfo;
import com.metaio.tools.io.AssetsManager;


@SuppressLint("InflateParams")
public class UbikeAR extends ARViewActivity implements SensorsComponentAndroid.Callback{
	private IAnnotatedGeometriesGroup mAnnotatedGeometriesGroup;
	private MyAnnotatedGeometriesGroupCallback mAnnotatedGeometriesGroupCallback;

	private IRadar mRadar;
	
	private JSONArray _ubikeStopList;
	//private ArrayList<LLACoordinate> _mLLAList = new ArrayList<LLACoordinate>();
	private ArrayList<IGeometry> _mIGeometryList = new ArrayList<IGeometry>();
	
	private LLACoordinate _myselfLLA;
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
	private static final long MIN_TIME_BW_UPDATES = 1000 * 30 * 1; // 1 minute //30sec
	private long _showRange = 1000;//1000 meters
	private String TAG = "lys_debug";
	private SharedPreferences _sp;
	public static String PREF = "pref";
	
	//progress dialog
	private ProgressDialog pd;
	//Dialog
		//Title
	private TextView _stopNameTextView;
	private ImageView _stopLogoImageView;
	private ImageView _refreshImageView;
	private String _ubikeStop="UBike";
	private String _ubikeStopIid = "";
	private String _ubikeStopNumber = "";
	private boolean _dialogIsShowing = false;
		//UBikeDialog
	private View _ubikeDialogView;
	private TextView _ubikeDistrictTextView;
	private TextView _ubikeAddressTextView;
	private TextView _ubikeStateTextView;
	private TextView _ubikeUdateTimeTextView;
	private TextView _ubikeVehiclesTextView;
	private TextView _ubikeParkingTextView;
	private TextView _ubikeHintTextView;
//	private String _ubikeDistrict = "信義區", _ubikeAddress = "復興南路 2 段 235 號",
//			       _ubikeState = "正式啟用", _ubikeUpdateTime = "2012/04/26 13:23:14",
//			       _ubikeVehicles = "23", _ubikeParking = "12", 
	private String _ubikeHint = "※臺北市微笑單車資料庫更新頻率為5分鐘1次";
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch(msg.what){
				case 100://click poi
					if(msg.obj!=null){
						try {
							JSONObject jo = new JSONObject(msg.obj.toString());
							String retCode = jo.getString("retCode");
							if(retCode.equals("1")){
								if(pd!=null)
									if(pd.isShowing())
										pd.dismiss();
								_ubikeStopList = new JSONArray(jo.getJSONArray("retVal").toString());
								//iid itemId
								//sv場站狀態 0暫停營運 1正式啟用
								//sno場站代號
								//sna場站名稱
								//sbi場站目前車輛數
								//bemp空位數量
								//sarea場站區域
								//mday資料更新時間yyyyMMddhhmmss
								//lat緯度
								//lng經度
								//ar地址
								//iid,sv,sno,sna,sbi,bemp,sarea,mday,lat,lng,ar
								for(int i=0; i<_ubikeStopList.length(); i++){
									JSONObject stop = _ubikeStopList.getJSONObject(i);
									if(stop.getString("sno").equals(_ubikeStopNumber)){
										inflateDialog();
										clickUbikePOI(stop);
										break;
									}
								}
							}
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
					break;
				case 101://first fetch runnable
					if(msg.obj!=null){
						try
						{
							if(pd!=null)
								if(pd.isShowing())
									pd.dismiss();
							JSONObject jo = new JSONObject(msg.obj.toString());
							String retCode = jo.getString("retCode");
							if(retCode.equals("1")){
								
								_ubikeStopList = new JSONArray(jo.getJSONArray("retVal").toString());
								//iid itemId
								//sv場站狀態 0暫停營運 1正式啟用
								//sno場站代號
								//sna場站名稱
								//sbi場站目前車輛數
								//bemp空位數量
								//sarea場站區域
								//mday資料更新時間yyyyMMddhhmmss
								//lat緯度
								//lng經度
								//ar地址
								//iid,sv,sno,sna,sbi,bemp,sarea,mday,lat,lng,ar
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
												try {
													for (int i=0; i<_ubikeStopList.length(); i++) {
														JSONObject info = _ubikeStopList.getJSONObject(i);
														LLACoordinate stop = new LLACoordinate(Double.valueOf(info.getString("lat")), Double.valueOf(info.getString("lng")), 0.0, 0.0);
														double distance = _myselfLLA.distanceTo(stop);
														info.put("distance", String.valueOf((int)distance));
														if(distance <= _showRange){
															IGeometry ig = createPOIGeometry(stop);
															ig.setName(info.toString());
															_mIGeometryList.add(ig);
															mAnnotatedGeometriesGroup.addGeometry(ig, info.toString());
															mRadar.add(ig);
														}
													}
												} catch (JSONException e) {
													// TODO: handle exception
												}
											}
										}
									});
								}
							}
						}
						catch(Exception e)
						{
						  Log.e("debug", e.toString());
						}
					}
					break;
				case 102://refresh runnable
					if(msg.obj!=null){
						if(pd!=null)
							if(pd.isShowing())
								pd.dismiss();
						try {
							JSONObject jo = new JSONObject(msg.obj.toString());
							String retCode = jo.getString("retCode");
							if(retCode.equals("1")){
								if(pd!=null)
									if(pd.isShowing())
										pd.dismiss();
								_ubikeStopList = new JSONArray(jo.getJSONArray("retVal").toString());
								//iid itemId
								//sv場站狀態 0暫停營運 1正式啟用
								//sno場站代號
								//sna場站名稱
								//sbi場站目前車輛數
								//bemp空位數量
								//sarea場站區域
								//mday資料更新時間yyyyMMddhhmmss
								//lat緯度
								//lng經度
								//ar地址
								//iid,sv,sno,sna,sbi,bemp,sarea,mday,lat,lng,ar
								for(int i=0; i<_ubikeStopList.length(); i++){
									final JSONObject stop = _ubikeStopList.getJSONObject(i);
									if(stop.getString("sno").equals(_ubikeStopNumber)){
										try {
											_ubikeDistrictTextView.setText(stop.getString("sarea"));
											_ubikeAddressTextView.setText(stop.getString("ar"));
											_ubikeStateTextView.setText(stop.getString("sv").equals("1")? "正式啟用" : "暫停營運");
											_ubikeUdateTimeTextView.setText(stop.getString("mday").substring(0,4) + "/" + stop.getString("mday").substring(4, 6) + "/" + stop.getString("mday").substring(6, 8) + "   " + stop.getString("mday").substring(8, 10) + ":" + stop.getString("mday").substring(10, 12) + ":" + stop.getString("mday").substring(12, 14));
										    _ubikeVehiclesTextView.setText(stop.getString("sbi"));
										    _ubikeParkingTextView.setText(stop.getString("bemp"));
										} catch (JSONException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										break;
									}
								}
							}
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
					break;
				case 999:
					Toast.makeText(UbikeAR.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
			}
		}
	};
	
	private Runnable ubikeFetchRunnable = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = new Message();
			try {
				HippoWebService connect = new HippoWebService();
				String api = "http://opendata.dot.taipei.gov.tw/opendata/gwjs_cityhall.json";
				String ret = connect.postMethod(api, "", "utf-8");
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
	
	private Runnable clickPOIRunnable = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = new Message();
			try {
				HippoWebService connect = new HippoWebService();
				String api = "http://opendata.dot.taipei.gov.tw/opendata/gwjs_cityhall.json";
				String ret = connect.postMethod(api, "", "utf-8");
			    if(ret!=null)
			    {
			    	msg.what = 100;
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
				String api = "http://opendata.dot.taipei.gov.tw/opendata/gwjs_cityhall.json";
				String ret = connect.postMethod(api, "", "utf-8");
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
	
	private void getData(){
		new Thread(ubikeFetchRunnable).start();
	}
	
	private void initLocation(){
		Bundle extras = UbikeAR.this.getIntent().getExtras();
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
		_showRange = (long)_sp.getInt(Settings.KEY_UBIKE_SEARCH_RANGE, 1000);
		
		// Set GPS tracking configuration
		boolean result = metaioSDK.setTrackingConfiguration("GPS", false);
		MetaioDebug.log("Tracking data loaded: " + result);
		
		initLocation();
		
		inflateDialog();
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		mSensors.registerCallback(this);
		SensorsComponentAndroid.LM_MINDISTANCE = MIN_DISTANCE_CHANGE_FOR_UPDATES;
		SensorsComponentAndroid.LM_MINTIME = MIN_TIME_BW_UPDATES;
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
		mSensors.pause();
		super.onPause();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		//_mLocationManager.removeUpdates(locationListener);
		mSensors.stop();
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
		return R.layout.ubike_ar;
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
		mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath(getApplicationContext(), "point_ubike.png"));
		mRadar.setRelativeToScreen(IGeometry.ANCHOR_BR);
		
		// let's create LLA objects
		
		// Load some POIs. Each of them has the same shape at its geoposition. We pass a string
		// (const char*) to IAnnotatedGeometriesGroup::addGeometry so that we can use it as POI title
		// in the callback, in order to create an annotation image with the title on it.
		
		getData();
	}

	private IGeometry createPOIGeometry(LLACoordinate lla)
	{
		String path = AssetsManager.getAssetPath(UbikeAR.this, "ubike_poi.obj");
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
		mSurfaceView.queueEvent(new Runnable()
		{
			@Override
			public void run()
			{
				mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath(getApplicationContext(), "point_ubike.png"));
				mRadar.setObjectTexture(geometry, AssetsManager.getAssetPath(getApplicationContext(), "red.png"));
				mAnnotatedGeometriesGroup.setSelectedGeometry(geometry);
			}
		});
		if(geometry == null)
			Log.i(TAG, "geometry is null");
		
		//iid itemId
		//sv場站狀態 0暫停營運 1正式啟用
		//sno場站代號
		//sna場站名稱
		//sbi場站目前車輛數
		//bemp空位數量
		//sarea場站區域
		//mday資料更新時間yyyyMMddhhmmss
		//lat緯度
		//lng經度
		//ar地址
		//iid,sv,sno,sna,sbi,bemp,sarea,mday,lat,lng,ar
		
		if((!_dialogIsShowing) && (geometry!=null)){
			_dialogIsShowing = true;
			if(pd!=null)
				if(pd.isShowing())
					pd.dismiss();
			pd = new ProgressDialog(UbikeAR.this);
			pd.setMessage("Loading...");
			pd.show();
			try {
				final JSONObject jo = new JSONObject(geometry.getName());
				_ubikeStop = jo.getString("sna");
				_ubikeStopIid = jo.getString("iid");
				_ubikeStopNumber = jo.getString("sno");
//				Log.i("debug", "On poi click, _ubikeStop="+_ubikeStop+" _ubikeIid="+_ubikeStopIid + " _ubikeStopNumber=" + _ubikeStopNumber);
				new Thread(clickPOIRunnable).start();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	final class MyAnnotatedGeometriesGroupCallback extends AnnotatedGeometriesGroupCallback
	{
		
		@Override
		public void onGeometryDistanceUpdated(IGeometry geometry,
				Object object, float distance, SensorValues sensorValues,
				Vector2d geoPosInViewport) {
			// TODO Auto-generated method stub
			super.onGeometryDistanceUpdated(geometry, object, distance, sensorValues,
					geoPosInViewport);
		}

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
			
			//iid itemId
			//sv場站狀態 0暫停營運 1正式啟用
			//sno場站代號
			//sna場站名稱
			//sbi場站目前車輛數
			//bemp空位數量
			//sarea場站區域
			//mday資料更新時間yyyyMMddhhmmss
			//lat緯度
			//lng經度
			//ar地址
			//iid,sv,sno,sna,sbi,bemp,sarea,mday,lat,lng,ar
			
			//String title = (String)userData; // as passed to addGeometry
			String texturePath="";
			Log.i("debug", "userData : " + userData.toString());
			try {
				JSONObject jo = new JSONObject(userData.toString());
				String title = jo.getString("sna");
				String distance = jo.getString("distance");
				texturePath = getAnnotationImageForTitle(title+","+distance);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

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
			String filepath = AssetsManager.getAssetPath(getApplicationContext(), "ubike_POI_bg" + (scale == 2 ? "@2x" : "") + ".png");
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
							mAnnotatedGeometriesGroup.removeGeometry(geometries.get(i));
						    metaioSDK.unloadGeometry(geometries.get(i));
						}
						try {
							if(_ubikeStopList != null){
								for (int i=0; i<_ubikeStopList.length(); i++) {
									JSONObject info = _ubikeStopList.getJSONObject(i);
									LLACoordinate stop = new LLACoordinate(Double.valueOf(info.getString("lat")), Double.valueOf(info.getString("lng")), 0.0, 0.0);
									double distance = _myselfLLA.distanceTo(stop);
									info.put("distance", String.valueOf((int)distance));
									if(distance <= _showRange){
										IGeometry ig = createPOIGeometry(stop);
										ig.setName(info.toString());
										_mIGeometryList.add(ig);
										mAnnotatedGeometriesGroup.addGeometry(ig, info.toString());
										mRadar.add(ig);
									}
								}
							}
						} catch (JSONException e) {
							// TODO: handle exception
						}
					}
				}
			});
		}
	}
	
	private void inflateDialog(){
		LayoutInflater inflater = UbikeAR.this.getLayoutInflater();
		_ubikeDialogView = inflater.inflate(R.layout.dialog_ubike, null);
		//InitFindViewById
		_stopNameTextView = (TextView) _ubikeDialogView.findViewById(R.id._dialogTitleTextView);
		_stopLogoImageView = (ImageView) _ubikeDialogView.findViewById(R.id._dialogTitleImageView);
		_refreshImageView = (ImageView) _ubikeDialogView.findViewById(R.id._dialogRefreshImageView);
		_ubikeHintTextView = (TextView) _ubikeDialogView.findViewById(R.id._ubikeHintTextView);
		_ubikeDistrictTextView = (TextView) _ubikeDialogView.findViewById(R.id._ubikeDistrictTextView);
		_ubikeAddressTextView = (TextView) _ubikeDialogView.findViewById(R.id._ubikeAddressTextView);
		_ubikeStateTextView = (TextView) _ubikeDialogView.findViewById(R.id._ubikeStatusTextView);
		_ubikeUdateTimeTextView = (TextView) _ubikeDialogView.findViewById(R.id._ubikeUpdateTimeTextView);
		_ubikeVehiclesTextView = (TextView) _ubikeDialogView.findViewById(R.id._uBikeVehicleAmountTextView);
		_ubikeParkingTextView = (TextView) _ubikeDialogView.findViewById(R.id._uBikeParkingAmoutnTextView);
	}
	
	private void clickUbikePOI(JSONObject stop)
	{
		//SetTitle
	    _stopLogoImageView.setImageDrawable(UbikeAR.this.getResources().getDrawable(R.drawable.ubike));
	    _stopNameTextView.setText(_ubikeStop);
	    _refreshImageView.setOnClickListener(new ImageView.OnClickListener()
	    {
			@Override
			public void onClick(View v) 
			{
				if(pd!=null)
					if(pd.isShowing())
						pd.dismiss();
				pd = new ProgressDialog(UbikeAR.this);
				pd.setMessage("Loading...");
				pd.show();
				new Thread(refreshRunnable).start();
			}	    	
	    });
	    
	    //iid itemId
		//sv場站狀態 0暫停營運 1正式啟用
		//sno場站代號
		//sna場站名稱
		//sbi場站目前車輛數
		//bemp空位數量
		//sarea場站區域
		//mday資料更新時間yyyyMMddhhmmss
		//lat緯度
		//lng經度
		//ar地址
		//iid,sv,sno,sna,sbi,bemp,sarea,mday,lat,lng,ar
	    
	    //SetData
		try {
			_ubikeHintTextView.setText(_ubikeHint);
			_ubikeDistrictTextView.setText(stop.getString("sarea"));
			_ubikeAddressTextView.setText(stop.getString("ar"));
			_ubikeStateTextView.setText(stop.getString("sv").equals("1")? "正式啟用" : "暫停營運");
			_ubikeUdateTimeTextView.setText(stop.getString("mday").substring(0,4) + "/" + stop.getString("mday").substring(4, 6) + "/" + stop.getString("mday").substring(6, 8) + "   " + stop.getString("mday").substring(8, 10) + ":" + stop.getString("mday").substring(10, 12) + ":" + stop.getString("mday").substring(12, 14));
		    _ubikeVehiclesTextView.setText(stop.getString("sbi"));
		    _ubikeParkingTextView.setText(stop.getString("bemp"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		_dialogIsShowing = false;
	    AlertDialog.Builder builder = new AlertDialog.Builder(UbikeAR.this);
	    builder.setView(_ubikeDialogView)
	           .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() 
	           {
	               @Override
	               public void onClick(DialogInterface dialog, int id){}
	           })      
	           .show();
	}
}
