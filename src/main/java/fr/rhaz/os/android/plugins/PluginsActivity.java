package fr.rhaz.os.android.plugins;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import fr.rhaz.os.android.R;

public class PluginsActivity extends AppCompatActivity implements ActionBar.TabListener{
    ViewPager viewpager;
    PluginsFragmentPageAdapter ft;
    ActionBar actionbar;
    private SearchView searchview;

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.plugins_search, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        searchview = (SearchView) MenuItemCompat.getActionView(searchItem);
        EditText searchPlate = (EditText) searchview.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchPlate.setHint("Search");
        View searchPlateView = searchview.findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // use this method when query submitted
                Toast.makeText(getApplicationContext(), query, Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(viewpager.getCurrentItem() != 1) return false;
                DownloadFragment downloadFragment = (DownloadFragment) ft.getItem(viewpager.getCurrentItem());
                ArrayAdapter adapter = downloadFragment.getAdapter();
                adapter.getFilter().filter(newText);
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
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

        private final DownloadFragment download;
        private final InstalledFragment installed;

        public PluginsFragmentPageAdapter(FragmentManager fm) {
            super(fm);
            installed = new InstalledFragment();
            download = new DownloadFragment();
        }

        @Override
        public Fragment getItem(int position) {
            switch(position){
                case 0: return installed;
                case 1: return download;
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
