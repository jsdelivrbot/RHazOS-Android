package fr.rhaz.os.android.plugins;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import fr.rhaz.os.android.R;

public class PluginsActivity extends AppCompatActivity implements ActionBar.TabListener{
    ViewPager viewpager;
    PluginsFragmentPageAdapter ft;
    ActionBar actionbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugins);
        this.viewpager = (ViewPager) findViewById(R.id.pager);
        this.ft = new PluginsFragmentPageAdapter(getSupportFragmentManager());
        this.actionbar = getSupportActionBar();
        this.viewpager.setAdapter(this.ft);
        this.actionbar.setNavigationMode(2);
        this.actionbar.addTab(this.actionbar.newTab().setText("Installed").setTabListener(this));
        this.actionbar.addTab(this.actionbar.newTab().setText("Download").setTabListener(this));
        this.viewpager.setOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                PluginsActivity.this.actionbar.setSelectedNavigationItem(position);
            }

            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if(viewpager == null) return;
        viewpager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    public static class PluginsFragmentPageAdapter extends FragmentPagerAdapter {

        public PluginsFragmentPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position){
                case 0: return new FirstFragment();
                case 1: return new SecondFragment();
                default: break;
            }

            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
