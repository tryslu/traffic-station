package edu.ntut.csie.s003.trafficstation;

import java.io.IOException;

import com.metaio.sdk.MetaioDebug;
import com.metaio.tools.io.AssetsManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Main extends ActionBarActivity {
	private AssetsExtracter mTask;
	private Button busButton;
	private Button ubikeButton;
	private Button mrtButton;
	private Button traButton;
	private Button helpButton;
	private LocationManager _mLocationManager;
	private boolean _isGPSEnable = false;
	private boolean _isNetworkEnable = false;
	private Location _currentLocation;
	private SharedPreferences _sp;
	public static String PREF = "pref";
	
	public void initFindViewById(){
		busButton = (Button) findViewById(R.id.main_busButton);
		ubikeButton = (Button) findViewById(R.id.main_ubikeButton);
		mrtButton = (Button) findViewById(R.id.main_mrtButton);
		traButton = (Button) findViewById(R.id.main_traButton);
		helpButton = (Button) findViewById(R.id.main_helpButton);
	}
	
	private void initLocation(){
		_mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		_isGPSEnable = _mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		_isNetworkEnable = _mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String best = _mLocationManager.getBestProvider(criteria, true);
		
//		if(!_isGPSEnable && !_isNetworkEnable){
//			//no network provider
//			Toast.makeText(getApplicationContext(), "Please turn on your Network or GPS.", Toast.LENGTH_SHORT).show();
//		}else{
//			//first get location from network provider
//			if(_mLocationManager!=null){
//				if(_isNetworkEnable){
//					_currentLocation = _mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//				}else if(_isGPSEnable){
//					_currentLocation = _mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//				}
//				//_currentLocation = _mLocationManager.getLastKnownLocation(bestProvider);
//			}
//		}
		
		if(_mLocationManager.getLastKnownLocation("gps") != null){
		    	_currentLocation = _mLocationManager.getLastKnownLocation(best);
		    	Log.i("debug", "1 my location lat:" + _currentLocation.getLatitude() + " lng:" + _currentLocation.getLongitude() + " provider is:"+best);
		    }else if (_mLocationManager.getLastKnownLocation("network") != null){
		    	_currentLocation = _mLocationManager.getLastKnownLocation("network");
		    	Log.i("debug", "2 my location lat:" + _currentLocation.getLatitude() + " lng:" + _currentLocation.getLongitude() + " provider is:"+best);
		    }else{
		    	Toast.makeText(getApplicationContext(), "Please turn on your Network or GPS.", Toast.LENGTH_SHORT).show();
		    	_currentLocation = null;
		    }
	}
	
	public void setButtonClickEvent(){
		busButton.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				try{
					Intent intent = new Intent(Main.this, BusAR.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.putExtras(getLocationBundle());
					startActivity(intent);
				}catch(Exception e){
					
				}
			}}
		);
		
		ubikeButton.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				try{
					Intent intent = new Intent(Main.this, UbikeAR.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.putExtras(getLocationBundle());
					startActivity(intent);
				}catch(Exception e){
					
				}
			}}
		);
		
		mrtButton.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				try{
					Intent intent = new Intent(Main.this, MrtAR.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.putExtras(getLocationBundle());
					startActivity(intent);
				}catch(Exception e){
					
				}
			}}
		);
		
		traButton.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				try{
					Intent intent = new Intent(Main.this, TraAR.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.putExtras(getLocationBundle());
					startActivity(intent);
				}catch(Exception e){
					
				}
			}}
		);
		
		helpButton.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				try{
					Intent intent = new Intent(Main.this, Settings.class);
					startActivity(intent);
				}catch(Exception e){
					
				}
			}}
		);
	}
	
	Bundle getLocationBundle(){
		Bundle extras = new Bundle();
		if(_currentLocation!=null){
			extras.putDouble("my_lat", _currentLocation.getLatitude());
			extras.putDouble("my_lng", _currentLocation.getLongitude());
			extras.putDouble("my_alt", 0.0);
			extras.putDouble("my_acc", 0.0);
		}else{
			extras.putDouble("my_lat", 0.0);
			extras.putDouble("my_lng", 0.0);
			extras.putDouble("my_alt", 0.0);
			extras.putDouble("my_acc", 0.0);
		}
		return extras;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		initLocation();
	    initFindViewById();
	    setButtonClickEvent();
	    
		mTask = new AssetsExtracter();
		mTask.execute(0);
	}
	
	/**
	 * This task extracts all the assets to an external or internal location
	 * to make them accessible to metaio SDK
	 */
	private class AssetsExtracter extends AsyncTask<Integer, Integer, Boolean>
	{
		@Override
		protected Boolean doInBackground(Integer... params) 
		{
			try 
			{
				// Extract all assets and overwrite existing files if debug build
				AssetsManager.extractAllAssets(getApplicationContext(), BuildConfig.DEBUG);
			} 
			catch (IOException e) 
			{
				MetaioDebug.printStackTrace(Log.ERROR, e);
				return false;
			}
			return true;
		}
	}
}
