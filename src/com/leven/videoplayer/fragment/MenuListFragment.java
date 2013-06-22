package com.leven.videoplayer.fragment;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.leven.videoplayer.FragmentChangeActivity;
import com.leven.videoplayer.R;
import com.leven.videoplayer.persistance.SlidingMenuItem;

public class MenuListFragment extends Fragment implements OnItemClickListener {
	private View view;
    private ListView menuListView;
	private int menuIcons[] = { R.drawable.navigation_download_ico,
            R.drawable.navigation_local_videos_ico, R.drawable.navigation_collect_ico,
            R.drawable.navigation_history_ico, R.drawable.navigation_recommend_ico,
            R.drawable.navigation_movie_ico, R.drawable.navigation_tvplay_ico,
            R.drawable.navigation_tvshow_ico, R.drawable.navigation_comic_ico,
            R.drawable.navigation_fuli_ico, R.drawable.navigation_info_ico,
            R.drawable.navigation_woman_ico, R.drawable.navigation_music_ico,
            R.drawable.navigation_amuse_ico, R.drawable.navigation_sport_ico,
            R.drawable.navigation_ranking_ico, R.drawable.navigation_app_recommend_ico};
    private int menuTitles[] = { R.string.navigation_download_title,
            R.string.navigation_local_videos_title, R.string.navigation_collect_title,
            R.string.navigation_history_title, R.string.navigation_recommend_title,
            R.string.navigation_movie_title, R.string.navigation_tvplay_title,
            R.string.navigation_tvshow_title, R.string.navigation_comic_title,
            R.string.navigation_fuli_title, R.string.navigation_info_title,
            R.string.navigation_woman_title, R.string.navigation_music_title,
            R.string.navigation_amuse_title, R.string.navigation_sport_title,
            R.string.navigation_ranking_title, R.string.navigation_app_recommend_title };

    private ArrayList<SlidingMenuItem> menuList = null;
	private SlidingMenuItem item;

    public ArrayList<SlidingMenuItem>  getMenuList() {
        menuList = new ArrayList<SlidingMenuItem>();

		for (int i = 0; i < menuIcons.length; i++) {
			item = new SlidingMenuItem();
			item.setMenuIcon(menuIcons[i]);
			item.setMenuTitle(menuTitles[i]);
			menuList.add(item);
		}
        return menuList;
    }

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.menu_frame, null);
		return view;
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		menuListView = (ListView) view.findViewById(R.id.menu_list);
		MenuListAdapter adapter = new MenuListAdapter(getActivity(), getMenuList());
		menuListView.setAdapter(adapter);
		menuListView.setOnItemClickListener(this);
	}

	private class MenuListAdapter extends BaseAdapter {

	    private Context context;
	    private ArrayList<SlidingMenuItem> menuList;

        public MenuListAdapter(Context context, ArrayList<SlidingMenuItem> menuList) {
            this.context = context;
            this.menuList = menuList;
        }

        @Override
        public int getCount() {
            return menuList.size();
        }

        @Override
        public Object getItem(int position) {
            return menuList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if(convertView == null) {
                viewHolder = new ViewHolder();
                convertView = View.inflate(context, R.layout.menu_list_item, null);
                viewHolder.menuIcon = (ImageView) convertView.findViewById(R.id.menuIcon);
                viewHolder.menuTitle = (TextView) convertView.findViewById(R.id.menuTitle);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.menuIcon.setImageResource(menuList.get(position).getMenuIcon());
            viewHolder.menuTitle.setText(menuList.get(position).getMenuTitle());
            return convertView;
        }

	}

	private static class ViewHolder {
	    public ImageView menuIcon;
	    public TextView menuTitle;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Fragment contentFragment = null;
		switch (position) {
		case 0:
			contentFragment = new OfflineVideoFragment();
			break;
		case 1:
			contentFragment = new LocalVideoFragment();
			break;

		default:
			break;
		}
		if(contentFragment != null) {
			switchFragment(contentFragment);
		}
	}

	private void switchFragment(Fragment fragment) {
		if(getActivity() == null) {
			return;
		}
		if(getActivity() instanceof FragmentChangeActivity) {
			FragmentChangeActivity fragmentChangeActivity = (FragmentChangeActivity) getActivity();
			fragmentChangeActivity.switchContent(fragment);
		}
	}

}

