package edu.ntut.csie.s003.trafficstation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class Settings extends Activity{
	private SharedPreferences _sp;
	public static String PREF = "pref";
	public static String KEY_BUS_SEARCH_RANGE = "KEY_BUS_SEARCH_RANGE";
	public static String KEY_UBIKE_SEARCH_RANGE = "KEY_UBIKE_SEARCH_RANGE";
	public static String KEY_MRT_SEARCH_RANGE = "KEY_MRT_SEARCH_RANGE";
	public static String KEY_TRA_SEARCH_RANGE = "KEY_TRA_SEARCH_RANGE";
	public static String KEY_IS_DEFAULT = "KEY_IS_DEFAULT";
	private TextView _busMeterTextView;
	private TextView _ubikeMeterTextView;
	private TextView _mrtMeterTextView;
	private TextView _traMeterTextView;
	private SeekBar _busSeekBar;
	private SeekBar _ubikeSeekBar;
	private SeekBar _mrtSeekBar;
	private SeekBar _traSeekBar;
	private TextView _stopNameTextView;
	private ImageView _stopLogoImageView;
	private ImageView _refreshImageView;
	private Bitmap _stopLogo;
	private Button _helpButton;
	private CheckBox _defaultCheckBox;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		_sp = getSharedPreferences(PREF, Context.MODE_PRIVATE);
		initFindViewById();
		initSeekBar();
		initMeterTextView();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(_stopLogo!=null){
			_stopLogo.recycle();
		}
		System.gc();
	}

	private void initFindViewById(){
		_stopNameTextView = (TextView) findViewById(R.id._dialogTitleTextView);
		_stopLogoImageView = (ImageView) findViewById(R.id._dialogTitleImageView);	
		_refreshImageView = (ImageView) findViewById(R.id._dialogRefreshImageView);
		_busMeterTextView = (TextView) findViewById(R.id.settings_busMeterTextView);
		_ubikeMeterTextView = (TextView) findViewById(R.id.settings_ubikeMeterTextView);
		_mrtMeterTextView = (TextView) findViewById(R.id.settings_mrtMeterTextView);
		_traMeterTextView = (TextView) findViewById(R.id.settings_traMeterTextView);
		_busSeekBar = (SeekBar) findViewById(R.id.settings_busSeekBar);
		_ubikeSeekBar = (SeekBar) findViewById(R.id.settings_ubikeSeekBar);
		_mrtSeekBar = (SeekBar) findViewById(R.id.settings_mrtSeekBar);
		_traSeekBar = (SeekBar) findViewById(R.id.settings_traSeekBar);
		_helpButton = (Button) findViewById(R.id.settings_HelpButton);
		_defaultCheckBox = (CheckBox) findViewById(R.id.settings_defaultCheckBox);
		_defaultCheckBox.setChecked(_sp.getBoolean(KEY_IS_DEFAULT, true));
		_busSeekBar.setEnabled(!_defaultCheckBox.isChecked());
		_ubikeSeekBar.setEnabled(!_defaultCheckBox.isChecked());
		_mrtSeekBar.setEnabled(!_defaultCheckBox.isChecked());
		_traSeekBar.setEnabled(!_defaultCheckBox.isChecked());
		
		_stopLogo = BitmapFactory.decodeResource(getResources(), R.drawable.setting);
		_stopNameTextView.setText(getResources().getString(R.string.str_settings_title));
		_stopLogoImageView.setImageBitmap(_stopLogo);
		_refreshImageView.setVisibility(View.GONE);
		
		_busSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				SharedPreferences.Editor se = _sp.edit();
				se.putInt(Settings.KEY_BUS_SEARCH_RANGE, seekBar.getProgress());
				se.commit();
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				if(fromUser){
					_busMeterTextView.setText(progress + "m");
				}
			}
		});
		
		_ubikeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				SharedPreferences.Editor se = _sp.edit();
				se.putInt(Settings.KEY_UBIKE_SEARCH_RANGE, seekBar.getProgress());
				se.commit();
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				if(fromUser){
					_ubikeMeterTextView.setText(progress + "m");
				}
			}
		});
		
		_mrtSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				SharedPreferences.Editor se = _sp.edit();
				se.putInt(Settings.KEY_MRT_SEARCH_RANGE, seekBar.getProgress());
				se.commit();
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				if(fromUser){
					_mrtMeterTextView.setText(progress + "m");
				}
			}
		});
		
		_traSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				SharedPreferences.Editor se = _sp.edit();
				se.putInt(Settings.KEY_TRA_SEARCH_RANGE, seekBar.getProgress());
				se.commit();
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				if(fromUser){
					_traMeterTextView.setText(progress + "m");
				}
			}
		});
		
		_helpButton.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Bundle extras = new Bundle();
				extras.putBoolean("FROM_SETTINGS", true);
				try {
					Intent intent = new Intent(Settings.this, Help.class);
					intent.putExtras(extras);
					startActivity(intent);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		});
		
		_defaultCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked){
					_busSeekBar.setEnabled(false);
					_ubikeSeekBar.setEnabled(false);
					_mrtSeekBar.setEnabled(false);
					_traSeekBar.setEnabled(false);
					_busSeekBar.setProgress(500);
					_ubikeSeekBar.setProgress(1000);
					_mrtSeekBar.setProgress(1000);
					_traSeekBar.setProgress(5000);
					_busMeterTextView.setText(_busSeekBar.getProgress() + "m");
					_ubikeMeterTextView.setText(_ubikeSeekBar.getProgress() + "m");
					_mrtMeterTextView.setText(_mrtSeekBar.getProgress() + "m");
					_traMeterTextView.setText(_traSeekBar.getProgress() + "m");
					SharedPreferences.Editor se = _sp.edit();
					se.putBoolean(KEY_IS_DEFAULT, true);
					se.putInt(Settings.KEY_BUS_SEARCH_RANGE, 500);
					se.putInt(Settings.KEY_UBIKE_SEARCH_RANGE, 1000);
					se.putInt(Settings.KEY_MRT_SEARCH_RANGE, 1000);
					se.putInt(Settings.KEY_TRA_SEARCH_RANGE, 5000);
					se.commit();
				}else if(!isChecked){
					SharedPreferences.Editor se = _sp.edit();
					se.putBoolean(KEY_IS_DEFAULT, false);
					se.commit();
					_busSeekBar.setEnabled(true);
					_ubikeSeekBar.setEnabled(true);
					_mrtSeekBar.setEnabled(true);
					_traSeekBar.setEnabled(true);
				}
			}
		});
	}
	
	private void initSeekBar(){
		_busSeekBar.setMax(1000);//1000m
		_ubikeSeekBar.setMax(1500);//1500m
		_mrtSeekBar.setMax(2000);//2000m
		_traSeekBar.setMax(10000);//10km
		_busSeekBar.setProgress(_sp.getInt(KEY_BUS_SEARCH_RANGE, 500));
		_ubikeSeekBar.setProgress(_sp.getInt(KEY_UBIKE_SEARCH_RANGE, 1000));
		_mrtSeekBar.setProgress(_sp.getInt(KEY_MRT_SEARCH_RANGE, 1000));
		_traSeekBar.setProgress(_sp.getInt(KEY_TRA_SEARCH_RANGE, 5000));
	}
	
	private void initMeterTextView(){
		_busMeterTextView.setText(_busSeekBar.getProgress() + "m");
		_ubikeMeterTextView.setText(_ubikeSeekBar.getProgress() + "m");
		_mrtMeterTextView.setText(_mrtSeekBar.getProgress() + "m");
		_traMeterTextView.setText(_traSeekBar.getProgress() + "m");
	}
}
