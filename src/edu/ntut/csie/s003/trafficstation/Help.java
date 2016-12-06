package edu.ntut.csie.s003.trafficstation;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

public class Help extends Activity
{
	private HelpPagerAdapter _pagerAdapter;
	private LayoutInflater _layoutInflater;
	private List<View> _listViews;
	private ViewPager _viewPager;
	private boolean _isLastPage = false;
	private SharedPreferences _sp;
	private static String PREF = "pref";
	private static String KEY_IS_FIRST_TIME = "KEY_IS_FIRST_TIME";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		Bundle extras = getIntent().getExtras();
		
		_sp = getSharedPreferences(PREF, Context.MODE_PRIVATE);
		if(!(_sp.getBoolean(KEY_IS_FIRST_TIME, true)) && extras==null){
			try
			{
				Intent intent = new Intent(Help.this, Main.class);
				startActivity(intent);
				finish();
			}
			catch(Exception e){}
		}else if(extras!=null){
			if(extras.getBoolean("FROM_SETTINGS")){
				_viewPager = (ViewPager) findViewById(R.id._helpViewPager);
				
				_listViews = new ArrayList<View>();
				_layoutInflater = Help.this.getLayoutInflater();

				_listViews.add(_layoutInflater.inflate( R.layout.help_page1, null));
				_listViews.add(_layoutInflater.inflate( R.layout.help_page2, null));
				_listViews.add(_layoutInflater.inflate( R.layout.help_page3, null));
				_listViews.add(_layoutInflater.inflate( R.layout.help_page4, null));
				_listViews.add(_layoutInflater.inflate( R.layout.help_page5, null));
				_pagerAdapter = new HelpPagerAdapter(getApplicationContext(), _listViews);
				_viewPager.setAdapter(_pagerAdapter);
				_viewPager.setCurrentItem(0);
				_viewPager.setOffscreenPageLimit(3);

				_viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
				{
					@Override
					public void onPageScrollStateChanged(int position){}

					@Override
					public void onPageScrolled(int arg0, float arg1, int arg2){}

					@Override
					public void onPageSelected(int position)
					{
						Log.i("LYNN_DEBUG_Function", "Now Selected position is " + position);
						
						if(position==_listViews.size()-1)
							_isLastPage = true;
						else
							_isLastPage = false;	
					}
				});
				
				_viewPager.setOnTouchListener(new ViewPager.OnTouchListener()
				{
					@Override
					public boolean onTouch(View v, MotionEvent event)
					{	
						if(_isLastPage==true && event.getAction() == MotionEvent.ACTION_DOWN)
			            {
							finish();
						}
						return false;
					}						
				});
			}
		}else{
			SharedPreferences.Editor se = _sp.edit();
			se.putBoolean(KEY_IS_FIRST_TIME, false);
			se.putInt(Settings.KEY_BUS_SEARCH_RANGE, 500);
			se.putInt(Settings.KEY_UBIKE_SEARCH_RANGE, 1000);
			se.putInt(Settings.KEY_MRT_SEARCH_RANGE, 1000);
			se.putInt(Settings.KEY_TRA_SEARCH_RANGE, 5000);
			se.commit();
			
			_viewPager = (ViewPager) findViewById(R.id._helpViewPager);
			
			_listViews = new ArrayList<View>();
			_layoutInflater = Help.this.getLayoutInflater();
	
			_listViews.add(_layoutInflater.inflate( R.layout.help_welcome, null));
			_listViews.add(_layoutInflater.inflate( R.layout.help_page1, null));
			_listViews.add(_layoutInflater.inflate( R.layout.help_page2, null));
			_listViews.add(_layoutInflater.inflate( R.layout.help_page3, null));
			_listViews.add(_layoutInflater.inflate( R.layout.help_page4, null));
			_listViews.add(_layoutInflater.inflate( R.layout.help_page5, null));
			_listViews.add(_layoutInflater.inflate( R.layout.help_last, null));
			_pagerAdapter = new HelpPagerAdapter(getApplicationContext(), _listViews);
			_viewPager.setAdapter(_pagerAdapter);
			_viewPager.setCurrentItem(0);
			_viewPager.setOffscreenPageLimit(3);
	
			_viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
			{
				@Override
				public void onPageScrollStateChanged(int position){}
	
				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2){}
	
				@Override
				public void onPageSelected(int position)
				{
					Log.i("LYNN_DEBUG_Function", "Now Selected position is " + position);
					
					if(position==_listViews.size()-1)
						_isLastPage = true;
					else
						_isLastPage = false;	
				}
			});
			
			_viewPager.setOnTouchListener(new ViewPager.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{	
					if(_isLastPage==true && event.getAction() == MotionEvent.ACTION_DOWN)
		            {
						try
						{
							Intent intent = new Intent(Help.this, Main.class);
							startActivity(intent);
							finish();
						}
						catch(Exception e){}
					}
					return false;
				}						
			});
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(_listViews!=null && _listViews.size()>0){
			for(View v : _listViews){
				unbindDrawables(v);
			}
		}
	    System.gc();
	}
	
	private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
        	view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            	unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }
}
