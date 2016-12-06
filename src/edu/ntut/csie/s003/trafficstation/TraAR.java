package edu.ntut.csie.s003.trafficstation;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import android.widget.Button;
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
public class TraAR extends ARViewActivity implements SensorsComponentAndroid.Callback{
	private IAnnotatedGeometriesGroup mAnnotatedGeometriesGroup;
	private MyAnnotatedGeometriesGroupCallback mAnnotatedGeometriesGroupCallback;

	private IRadar mRadar;
	
	private ArrayList<String> _traStopList;
	private ArrayList<String> _traStopNumberList;
	private ArrayList<IGeometry> _mIGeometryList = new ArrayList<IGeometry>();
	
	private LLACoordinate _myselfLLA;
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
	private static final long MIN_TIME_BW_UPDATES = 1000 * 30 * 1; // 30sec
	private static final String NORTH = "0";
	private static final String SOUTH = "1";
	private long _showRange = 5000;//5000 meters
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
	private String _traStop="TRA";
	private boolean _dialogIsShowing = false;
		//TRADialog
	private ListView _traListView;
	private Button _traNorthButton, _traSouthButton;
	private TextView _traHintTextView;
	private SimpleAdapter _traAdapter;
	private ArrayList<HashMap<String, String>> _traNorthList, _traSouthList;	
	private ArrayList<String> _traTypeList, _traNumList, _traDepartureTimeList, _traDestinationList, _traUpOrDownList, _traStateList;
	private String _traHint = "※列車實際運行時刻以車站公告為主";
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch(msg.what){
				case 100://click poi
					if(msg.obj!=null){
						for(int i=0; i<_traStopNumberList.size(); i++){
							//0=number, 1=chinese name, 2=english name, 3=areaNumber(not use)
							String[] split = _traStopNumberList.get(i).split(",");
							if(split[1].equals(msg.obj.toString())){
								_queryStopNumber = split[0];
								break;
							}
						}
						Log.i(TAG, "queryStop = " + msg.obj.toString() + "  _queryStopNumber = " + _queryStopNumber);
						new Thread(fetchRunnable).start();
					}
					break;
				case 101://fetch runnable
					if(msg.obj!=null){
//						Log.i(TAG, "msg.obj = " + msg.obj.toString());
						_tagNode = new HtmlCleaner().clean(msg.obj.toString());
						getTrainData();
						clickTraPOI();
						if(pd!=null)
							if(pd.isShowing())
								pd.dismiss();
					}
					break;
				case 102://refresh runnable
					if(msg.obj!=null){
						_tagNode = new HtmlCleaner().clean(msg.obj.toString());
						getTrainData();
						if(pd!=null)
							if(pd.isShowing())
								pd.dismiss();

						_traNorthList = new ArrayList<HashMap<String,String>>();
						_traSouthList = new ArrayList<HashMap<String,String>>();
						
						//SetButtonEnable
						if(_traNorthButton.isEnabled()){
							_traNorthButton.setEnabled(true);
							_traSouthButton.setEnabled(false);
					        SetTRAListView(_traSouthList, SOUTH);
						}else if(_traSouthButton.isEnabled()){
							_traNorthButton.setEnabled(false);
							_traSouthButton.setEnabled(true);
					        SetTRAListView(_traNorthList, NORTH);
						}else{
							_traNorthButton.setEnabled(false);
							_traSouthButton.setEnabled(true);
					        SetTRAListView(_traNorthList, NORTH);
						}
					}
					break;
				case 999:
					Toast.makeText(TraAR.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
			}
		}
	};
	
	private String _searchDate = "";
	private String _queryStopNumber = "";
	private TagNode _tagNode;
	private void getTrainData(){
		TagNode[] temp = _tagNode.getElementsByName("form", true);
		TagNode[] train = temp[0].getElementsByName("script", true);
		//train[0]=上行,  train[1]=下行
		String trainUp = train[0].getText().toString();
		String trainDown = train[1].getText().toString();
		trainUp = trainUp.replaceAll("'\\);TRSearchResult.push\\('", ",");
		trainUp = trainUp.replace("TRSearchResult.push('", "");
		trainUp = trainUp.replace("')", "");
//		Log.i(TAG, "String trainUp =" + trainUp);
		trainDown = trainDown.replaceAll("'\\);TRSearchResult.push\\('", ",");
		trainDown = trainDown.replace("TRSearchResult.push('", "");
		trainDown = trainDown.replace("')", "");
//		Log.i(TAG, "String trainDown =" + trainDown);
		//0=車種, 1=車號, 2=發車時間, 3=往X, 4=(0上行1下行), 5=慢X分
		String[] trainUpSplit = trainUp.split(",");
		String[] trainDownSplit = trainDown.split(",");
		
		_traTypeList = new ArrayList<String>();
		_traNumList = new ArrayList<String>();
		_traDepartureTimeList = new ArrayList<String>();
		_traDestinationList = new ArrayList<String>();
		_traUpOrDownList = new ArrayList<String>();
        _traStateList = new ArrayList<String>();
        int i;
        for(i=0; i<trainUpSplit.length; i++){
        	if(i%6==0){
        		_traTypeList.add(trainUpSplit[i]);
        	}else if(i%6==1){
        		_traNumList.add(trainUpSplit[i]);
        	}else if(i%6==2){
        		_traDepartureTimeList.add(trainUpSplit[i]);
        	}else if(i%6==3){
        		_traDestinationList.add(trainUpSplit[i]);
        	}else if(i%6==4){
        		_traUpOrDownList.add(trainUpSplit[i]);
        	}else if(i%6==5){
        		if(trainUpSplit[i].equals("") || trainUpSplit[i].equals(";")){
        			_traStateList.add("");
        		}else if(trainUpSplit[i].equals("0") || trainUpSplit[i].equals("0;")){
        			_traStateList.add("準點");
        		}else if(trainUpSplit[i].indexOf(";")!=-1){
        			_traStateList.add("慢" + trainUpSplit[i].substring(0, trainUpSplit[i].indexOf(";")) + "分");
        		}else{
        			_traStateList.add("慢" + trainUpSplit[i] + "分");
        		}
        	}
        }
        
        for(i=0; i<(trainDownSplit.length); i++){
        	if(i%6==0){
        		_traTypeList.add(trainDownSplit[i]);
        	}else if(i%6==1){
        		_traNumList.add(trainDownSplit[i]);
        	}else if(i%6==2){
        		_traDepartureTimeList.add(trainDownSplit[i]);
        	}else if(i%6==3){
        		_traDestinationList.add(trainDownSplit[i]);
        	}else if(i%6==4){
        		_traUpOrDownList.add(trainDownSplit[i]);
        	}else if(i%6==5){
        		if(trainDownSplit[i].equals("") || trainDownSplit[i].equals(";")){
        			_traStateList.add("");
        		}else if(trainDownSplit[i].equals("0") || trainDownSplit[i].equals("0;")){
        			_traStateList.add("準點");
        		}else if(trainDownSplit[i].indexOf(";")!=-1){
        			_traStateList.add("慢" + trainDownSplit[i].substring(0, trainDownSplit[i].indexOf(";")) + "分");
        		}else{
        			_traStateList.add("慢" + trainDownSplit[i] + "分");
        		}
        	}
        }
        
//		_traTypeList.add("自強");
//		_traNumList.add("144");
//		_traDepartureTimeList.add("22:04");
//		_traDestinationList.add("七堵");
//		_traUpOrDownList.add("0");//上行
//		_traStateList.add("準點");
	}
	
	private Runnable fetchRunnable = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = new Message();
			try {
				HippoWebService connect = new HippoWebService();
				String api = "http://twtraffic.tra.gov.tw/twrail/mobile/StationSearchResult.aspx?searchdate=" + _searchDate + "&fromstation=" + _queryStopNumber;
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
	private Runnable refreshRunnable = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = new Message();
			try {
				HippoWebService connect = new HippoWebService();
				String api = "http://twtraffic.tra.gov.tw/twrail/mobile/StationSearchResult.aspx?searchdate=" + _searchDate + "&fromstation=" + _queryStopNumber;
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
	
	private void initLocation(){
		Bundle extras = TraAR.this.getIntent().getExtras();
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
		_showRange = (long)_sp.getInt(Settings.KEY_TRA_SEARCH_RANGE, 5000);
		
		// Set GPS tracking configuration
		boolean result = metaioSDK.setTrackingConfiguration("GPS", false);
		MetaioDebug.log("Tracking data loaded: " + result);
		
		String[] traStop = getResources().getStringArray(R.array.trastop);
		_traStopList = new ArrayList<String>(Arrays.asList(traStop));
		
		String[] traStopNumber = getResources().getStringArray(R.array.trastopnumber);
		_traStopNumberList = new ArrayList<String>(Arrays.asList(traStopNumber));
		
		Calendar c = Calendar.getInstance();
		int month = c.get(Calendar.MONTH) + 1;
		_searchDate = c.get(Calendar.YEAR) + "/" + month + "/" + c.get(Calendar.DAY_OF_MONTH);
		
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
		mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath(getApplicationContext(), "point_tra.png"));
		mRadar.setRelativeToScreen(IGeometry.ANCHOR_BR);
		
		// let's create LLA objects
		
		// Load some POIs. Each of them has the same shape at its geoposition. We pass a string
		// (const char*) to IAnnotatedGeometriesGroup::addGeometry so that we can use it as POI title
		// in the callback, in order to create an annotation image with the title on it.
		
		for (String stopInfomation : _traStopList) {
			//0=index, 3=Chinese name, 5=address, 6=latitude, 7=longitude
			String[] split = stopInfomation.split(",");
			LLACoordinate stop = new LLACoordinate(Double.valueOf(split[6]), Double.valueOf(split[7]), 0.0, 0.0);
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
		String path = AssetsManager.getAssetPath(TraAR.this, "tra_poi.obj");
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
					mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath(getApplicationContext(), "point_tra.png"));
					mRadar.setObjectTexture(geometry, AssetsManager.getAssetPath(getApplicationContext(), "red.png"));
					mAnnotatedGeometriesGroup.setSelectedGeometry(geometry);
				}
			});
		}
		if(geometry == null)
			Log.i(TAG, "geometry is null");
		if((!_dialogIsShowing) && (geometry!=null)){
			_dialogIsShowing = true;
			if(pd!=null)
				if(pd.isShowing())
					pd.dismiss();
			pd = new ProgressDialog(TraAR.this);
			pd.setMessage("Loading...");
			pd.show();
			//0=index, 3=Chinese name, 5=address, 6=latitude, 7=longitude
			final String[] split = geometry.getName().toString().split(",");
			_traStop = split[3];
			String searchName = "臺北";
			if(split[3].lastIndexOf("火車站")!=-1){
				searchName = split[3].substring(0, split[3].lastIndexOf("火車站"));
			}
			final String obj = searchName;
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					Message msg = new Message();
					msg.what = 100;
					msg.obj = obj;
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
			String title = split[3];//split{//0=index, 3=Chinese name, 5=address, 6=latitude, 7=longitude, 8=distance}
			String distance = split[8];
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
			String filepath = AssetsManager.getAssetPath(getApplicationContext(), "tra_POI_bg" + (scale == 2 ? "@2x" : "") + ".png");
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
//		Log.i("debug", "my location lat:" + location.getLatitude() + " lng:" + location.getLongitude());
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
						
						for (String stopInfomation : _traStopList) {
							//0=index, 3=Chinese name, 5=address, 6=latitude, 7=longitude
							String[] split = stopInfomation.split(",");
							LLACoordinate stop = new LLACoordinate(Double.valueOf(split[6]), Double.valueOf(split[7]), 0.0, 0.0);
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
	
	private void clickTraPOI()
	{
	    AlertDialog.Builder builder = new AlertDialog.Builder(TraAR.this);
	    LayoutInflater inflater = TraAR.this.getLayoutInflater();
	    View view = inflater.inflate(R.layout.dialog_tra, null);
	    	    
	    //InitFindViewById
		_stopNameTextView = (TextView) view.findViewById(R.id._dialogTitleTextView);
		_stopLogoImageView = (ImageView) view.findViewById(R.id._dialogTitleImageView);	
		_refreshImageView = (ImageView) view.findViewById(R.id._dialogRefreshImageView);
		
		_traListView = (ListView) view.findViewById(R.id._traListView);	
		_traSouthButton = (Button) view.findViewById(R.id._traSouthBoundLineButton);
		_traNorthButton = (Button) view.findViewById(R.id._traNorthBoundLinebutton);
		_traHintTextView = (TextView) view.findViewById(R.id._traHintTextView);
		_traNorthList = new ArrayList<HashMap<String,String>>();
		_traSouthList = new ArrayList<HashMap<String,String>>();
		
		_traHintTextView.setText(_traHint);
		//InitListView
		_traNorthButton.setEnabled(false);
        
        SetTRAListView(_traNorthList, NORTH);
		
		_traNorthButton.setOnClickListener(new Button.OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				//SetButtonEnable
				_traNorthButton.setEnabled(false);
				_traSouthButton.setEnabled(true);
				
				//ClearList			
				_traNorthList.clear();
		        
		        SetTRAListView(_traNorthList, NORTH);
			}			
		});
		
		_traSouthButton.setOnClickListener(new Button.OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				//SetButtonEnable
		        _traNorthButton.setEnabled(true);
				_traSouthButton.setEnabled(false);
				
				//ClearList			
				_traSouthList.clear();
		        
		        SetTRAListView(_traSouthList, SOUTH);
			}			
		});
		
		//SetTitle
	    _stopLogoImageView.setImageDrawable(getResources().getDrawable(R.drawable.tra_state_button));
	    _stopNameTextView.setText(_traStop);
	    
	    _refreshImageView.setOnClickListener(new ImageView.OnClickListener()
	    {
			@Override
			public void onClick(View v) 
			{
				if(pd!=null)
					if(pd.isShowing())
						pd.dismiss();
				pd = new ProgressDialog(TraAR.this);
				pd.setMessage("Loading...");
				pd.show();
				new Thread(refreshRunnable).start();
			}	    	
	    });

	    _dialogIsShowing = false;
	    builder.setView(view)
	           .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() 
	           {
	               @Override
	               public void onClick(DialogInterface dialog, int id){
	            	   dialog.dismiss();
	               }
	           })      
	           .show();
	}
	
	private void SetTRAListView(ArrayList<HashMap<String,String>> _arrayList, String upOrDown)
	{
		//Set TRA List
        for(int i=0; i<_traNumList.size(); i++)
		{
		    HashMap<String, String> item = new HashMap<String,String>();
		    if(_traUpOrDownList.get(i).equalsIgnoreCase(upOrDown)){
			    item.put("tra_type", _traTypeList.get(i));   // key, value (車種)
			    item.put("tra_num", _traNumList.get(i));   // key, value (車次)
			    item.put("tra_departure", _traDepartureTimeList.get(i));   // key, value (發車時間)
			    item.put("tra_destination", _traDestinationList.get(i));   // key, value (往)
			    item.put("tra_state", _traStateList.get(i));   // key, value (狀態)
			    _arrayList.add(item);
		    }
		}
        
		_traAdapter = new SimpleAdapter
		    (
			    TraAR.this,
			    _arrayList,
			    R.layout.tra_list,
			    new String[]{"tra_type", "tra_num", "tra_departure", "tra_destination", "tra_state"},  // key的名字
			    new int[]{R.id._traTypeTextView, R.id._traNumTextView, R.id._traDepartureTextView, R.id._traDestinationTextView, R.id._traStateTextView}  // show在哪個textView id
            );
		_traListView.setAdapter(_traAdapter);
		
		//If List.size > 10, set ListView Height
		if(_traNorthList.size()>10 || _traSouthList.size()>10)
		{	
			int _listHeight = 0;//it is the ListView Height
			View _listItem = _traAdapter.getView(0, null, _traListView);
	        _listItem.measure(0, 0);
	        _listHeight = _listItem.getMeasuredHeight() + _traListView.getDividerHeight();//item height
	        
			LinearLayout.LayoutParams _listViewParam = (LinearLayout.LayoutParams) _traListView.getLayoutParams();
			_listViewParam.height = (_listHeight*10);
			_traListView.setLayoutParams(_listViewParam);
		}
	}
}
