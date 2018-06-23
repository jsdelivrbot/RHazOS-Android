package fr.rhaz.os.android.plugins

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast

import fr.rhaz.os.android.R

class PluginsActivity : AppCompatActivity(), ActionBar.TabListener {
    internal var viewpager: ViewPager? = null
    internal var ft: PluginsFragmentPageAdapter? = null;
    internal var actionbar: ActionBar? = null
    private var searchview: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plugins)
        this.viewpager = findViewById<View>(R.id.pager) as ViewPager
        this.ft = PluginsFragmentPageAdapter(supportFragmentManager)
        this.actionbar = supportActionBar
        this.viewpager!!.adapter = this.ft
        this.actionbar!!.navigationMode = 2
        this.actionbar!!.addTab(this.actionbar!!.newTab().setText("Installed").setTabListener(this))
        this.actionbar!!.addTab(this.actionbar!!.newTab().setText("Download").setTabListener(this))
        this.viewpager!!.setOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                this@PluginsActivity.actionbar!!.setSelectedNavigationItem(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.plugins_search, menu)
        val searchItem = menu.findItem(R.id.search)
        searchview = MenuItemCompat.getActionView(searchItem) as SearchView
        val searchPlate = searchview!!.findViewById<View>(android.support.v7.appcompat.R.id.search_src_text) as EditText
        searchPlate.hint = "Search"
        val searchPlateView = searchview!!.findViewById<View>(android.support.v7.appcompat.R.id.search_plate)
        searchview!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // use this method when query submitted
                Toast.makeText(applicationContext, query, Toast.LENGTH_SHORT).show()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (viewpager!!.currentItem != 1) return false
                val downloadFragment = ft!!.getItem(viewpager!!.currentItem) as DownloadFragment?
                val adapter = downloadFragment!!.adapter
                adapter.filter.filter(newText)
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onTabSelected(tab: ActionBar.Tab, ft: FragmentTransaction) {
        if (viewpager == null) return
        viewpager!!.currentItem = tab.position
    }

    override fun onTabUnselected(tab: ActionBar.Tab, ft: FragmentTransaction) {

    }

    override fun onTabReselected(tab: ActionBar.Tab, ft: FragmentTransaction) {

    }

    class PluginsFragmentPageAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        private val download: DownloadFragment
        private val installed: InstalledFragment

        init {
            installed = InstalledFragment()
            download = DownloadFragment()
        }

        override fun getItem(position: Int): Fragment? {
            when (position) {
                0 -> return installed
                1 -> return download
                else -> {
                }
            }

            return null
        }

        override fun getCount(): Int {
            return 2
        }
    }
}
