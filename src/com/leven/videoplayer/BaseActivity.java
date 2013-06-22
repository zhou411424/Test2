package com.leven.videoplayer;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.actionbarsherlock.app.ActionBar;
import com.leven.videoplayer.fragment.MenuListFragment;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class BaseActivity extends SlidingFragmentActivity {

	protected MenuListFragment mFrag;
    private ActionBar actionBar;
    private ImageButton ibTitleIcon;
    private ImageButton ibScan;
    private ImageButton ibSort;
    private LayoutInflater inflater;
    private View titlebar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set the Behind View
		setBehindContentView(R.layout.menu_frame);
		if (savedInstanceState == null) {
			FragmentTransaction t = this.getSupportFragmentManager().beginTransaction();
			mFrag = new MenuListFragment();
			t.replace(R.id.menu_frame, mFrag);
			t.commit();
		} else {
			mFrag = (MenuListFragment)this.getSupportFragmentManager().findFragmentById(R.id.menu_frame);
		}

		// customize the SlidingMenu
		SlidingMenu sm = getSlidingMenu();
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.shadow);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setFadeDegree(0.35f);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);

		inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        titlebar = inflater.inflate(R.layout.title_bar, null);
        ibTitleIcon = (ImageButton) titlebar.findViewById(R.id.ib_title_icon);
        ibScan = (ImageButton) titlebar.findViewById(R.id.ib_scan);
        ibSort = (ImageButton) titlebar.findViewById(R.id.ib_sort);
        
        ibTitleIcon.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        ibSort.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });
        
        ibScan.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });
		actionBar = getSupportActionBar();
		actionBar.setCustomView(titlebar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
	}
}
