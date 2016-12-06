package edu.ntut.csie.s003.trafficstation;

import java.util.List;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

public class HelpPagerAdapter extends PagerAdapter
{
    private Context _context;
	private List<View> _listViews;

	public HelpPagerAdapter(Context cx, List<View> lv)
	{
	    super();
	    // 1
	    _context = cx.getApplicationContext();
	    this._listViews = lv;
	}

	@Override
	public int getCount()
	{
	    return _listViews.size();
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
	    // 2
	    container.addView(_listViews.get(position), 0); 
	    return _listViews.get(position); 
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object)
	{
		((ViewPager) container).removeView((View)object);
		container = null;
		object = null;
	    //container.removeView(_listViews.get(position));
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
	    return view == object;
	}
}
