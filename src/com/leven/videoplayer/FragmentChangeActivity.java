package com.leven.videoplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.leven.videoplayer.fragment.LocalVideoFragment;
import com.leven.videoplayer.fragment.MenuListFragment;
import com.leven.videoplayer.fragment.OfflineVideoFragment;
import com.slidingmenu.lib.SlidingMenu;

public class FragmentChangeActivity extends BaseActivity {

	private Fragment mContent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// set the above view
		if(savedInstanceState != null) {
			mContent = getSupportFragmentManager().getFragment(savedInstanceState, "mContent");
		}
		if(mContent == null) {
			mContent = new LocalVideoFragment();
		}
		
		setContentView(R.layout.content_frame);
		getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.content_frame, mContent)
		.commit();
		
		// set the behind view
		setBehindContentView(R.layout.menu_frame);
		getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.menu_frame, new MenuListFragment())
		.commit();
		
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.titlebar_bg));
		
		getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getSupportFragmentManager().putFragment(outState, "mContent", mContent);
	}

	public void switchContent(Fragment fragment) {
		mContent = fragment;
		getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.content_frame, fragment)
		.commit();
		getSlidingMenu().showContent();
	}
	
}
