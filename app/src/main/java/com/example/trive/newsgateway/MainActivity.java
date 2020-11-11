package com.example.trive.newsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class MainActivity extends AppCompatActivity {

    static final String ACTION_MSG_TO_SERVICE = "ACTION_MSG_TO_SERVICE";
    static final String ACTION_NEWS_STORY = "ACTION_NEWS_STORY";


    private List<Fragment> fragments;
    private MyPageAdapter pageAdapter;
    private ViewPager pager;

    private NewsReceiver newsReceiver;

    private SourceDownloader sourceDownloader;
    private ArrayList<String> categoryList = new ArrayList<>();

    private Set<String> categorySetList = new TreeSet<>();
    private ArrayList<String> sourceList = new ArrayList<>();
    private List<Source> sourceObjectsList = new ArrayList<>();
    private HashMap<String, List<Source>> mapSourceCategory = new HashMap<>();
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerAdapter drawerAdapter;


    private String currentSourceName="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Intent intent = new Intent(MainActivity.this, NewsService.class);
        startService(intent);


        newsReceiver = new NewsReceiver();
        IntentFilter filter2 = new IntentFilter(ACTION_NEWS_STORY);
        registerReceiver(newsReceiver, filter2);


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        drawerAdapter = new DrawerAdapter(this, R.layout.drawer_list_item, sourceObjectsList);

        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        );

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);




        fragments = getFragments();
        pageAdapter = new MyPageAdapter(getSupportFragmentManager());
        pager = (ViewPager) findViewById(R.id.viewpager);
        pager.setAdapter(pageAdapter);



        sourceDownloader=new SourceDownloader(this,"");
        sourceDownloader.execute();

    }

    public void setSources(HashMap<String, List<Source>> catSourceList) {
        mapSourceCategory.putAll(catSourceList);
        categorySetList.addAll(catSourceList.keySet());
        categoryList.addAll(categorySetList);


        List<Source> temp=new ArrayList<>();
        temp=catSourceList.get("all");
        for(Source s:temp){
            Source source=new Source(s.getId(),s.getName(),s.getUrl(),s.getCategory());
            sourceObjectsList.add(source);
        }

        drawerAdapter = new DrawerAdapter(this, R.layout.drawer_list_item, sourceObjectsList);
        mDrawerList.setAdapter(drawerAdapter);
        invalidateOptionsMenu();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int catSize = categoryList.size();
        if (catSize > 0) {
            for (int i = 0; i < catSize; i++) {
                menu.add(menu.NONE, i, menu.NONE, categoryList.get(i));
            }
        }
        return true;
    }



    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        String str = categoryList.get(item.getItemId());
        sourceObjectsList.removeAll(sourceObjectsList);
        List<Source> tempList = mapSourceCategory.get(str);
        sourceObjectsList.addAll(tempList);
        drawerAdapter.notifyDataSetChanged();

        return super.onOptionsItemSelected(item);
    }

    private void selectItem(int position) {
        Toast.makeText(this, "You have choosen " + sourceObjectsList.get(position).getName(), Toast.LENGTH_SHORT).show();

        pager.setBackgroundDrawable(null);

        String currentSourceId = sourceObjectsList.get(position).getId();
        currentSourceName=sourceObjectsList.get(position).getName();

        Intent intent = new Intent();
        intent.setAction(ACTION_MSG_TO_SERVICE);

        intent.putExtra("Source", currentSourceId);

        sendBroadcast(intent);

        mDrawerLayout.closeDrawer(mDrawerList);

    }



    private List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        return fList;
    }

    private void reDoFragments(ArrayList<Story> passedList) {


        setTitle(currentSourceName);

        int pageCount=pageAdapter.getCount();
        for (int i = 0; i < pageCount; i++)
            pageAdapter.notifyChangeInPosition(i);

        fragments.clear();

        int sCount=0;
        int sSize=passedList.size();
        for(Story s:passedList){
            fragments.add(MyFragment.newInstance(s,sCount+1,sSize));
            sCount++;
        }

        pageAdapter.notifyDataSetChanged();
        pager.setCurrentItem(0);



    }


    private class MyPageAdapter extends FragmentPagerAdapter {
        private long baseId = 0;

        public MyPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public long getItemId(int position) {

            return baseId + position;
        }

        public void notifyChangeInPosition(int n) {

            baseId += getCount() + n;
        }


    }


    class NewsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case ACTION_NEWS_STORY:

                    ArrayList<Story> tempList=intent.getParcelableArrayListExtra("storylist");

                    reDoFragments(tempList);
                    break;
            }
        }
    }




}


