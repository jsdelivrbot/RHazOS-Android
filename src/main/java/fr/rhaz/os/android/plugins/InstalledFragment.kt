package fr.rhaz.os.android.plugins

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context

import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.media.midi.MidiOutputPort
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.Filter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import java.io.File
import java.util.ArrayList

import fr.rhaz.os.OS
import fr.rhaz.os.android.ConsoleService
import fr.rhaz.os.android.ConsoleService.ConsoleBinder
import fr.rhaz.os.android.R
import fr.rhaz.os.plugins.PluginDescription
import fr.rhaz.os.plugins.PluginRunnable

class InstalledFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var adapter: PluginAdapter? = null
    private var listview: ListView? = null
    private var swipe: SwipeRefreshLayout? = null
    private var connection: ServiceConnection? = null
    private var service: ConsoleService? = null

    val os: OS
        @Throws(NullPointerException::class)
        get() {
            if (service == null)
                throw NullPointerException()

            if (service!!.os == null)
                throw NullPointerException()

            return service!!.os!!
        }
    //private EditText searchview;

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.first_layout, container, false)

        setServiceConnection()
        listview = v.findViewById<View>(R.id.list) as ListView
        swipe = v.findViewById<View>(R.id.swipe) as SwipeRefreshLayout
        swipe!!.setOnRefreshListener(this)
        adapter = PluginAdapter(activity!!, android.R.layout.simple_list_item_1)
        listview!!.adapter = adapter
        registerForContextMenu(listview!!)
        listview!!.isLongClickable = activity!!.isRestricted
        swipe!!.post { onRefresh() }
        listview!!.setOnItemClickListener { parent, view, position, id -> parent.showContextMenuForChild(view) }
        /*searchview = (EditText) v.findViewById(R.id.search);
        searchview.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Call back the Adapter with current character to Filter
                adapter.getFilter().filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });*/
        return v
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val i = activity!!.menuInflater
        i.inflate(R.menu.plugins_context, menu)
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        val info = item!!.menuInfo as AdapterView.AdapterContextMenuInfo
        val desc = adapter!!.getItem(info.position)
        when (item.itemId) {
            R.id.config -> {
                val folder = File(os.pluginManager.folder, desc!!.name)
                if (!folder.exists()) folder.mkdir()
                openFolder(folder)
                return true
            }
            R.id.delete -> {
                AlertDialog.Builder(activity)
                        .setMessage("Are you sure you want to delete this plugin?")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { dialog, id ->
                            desc!!.file.delete()
                            adapter!!.remove(desc)
                            adapter!!.notifyDataSetChanged()
                        }
                        .setNegativeButton("No", null)
                        .show()
                return true
            }
            R.id.share -> {
                share(desc!!.file)
                return true
            }
            else -> return super.onContextItemSelected(item)
        }
    }

    fun openFolder(path: File) {
        val uri = Uri.fromFile(path)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "resource/folder")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, "Please install a file manager", Toast.LENGTH_LONG).show()
        }

    }

    override fun onRefresh() {
        adapter!!.clear()
        try {
            os
        } catch (e: NullPointerException) {
            return
        }

        swipe!!.isRefreshing = true
        for (runnable in os.pluginManager.plugins) {
            val desc = runnable.plugin.description
            adapter!!.add(desc)
        }
        adapter!!.notifyDataSetChanged()
        swipe!!.isRefreshing = false
    }

    fun setServiceConnection() {

        this.connection = object : ServiceConnection {

            override fun onServiceConnected(name: ComponentName, ibinder: IBinder) {
                service = (ibinder as ConsoleBinder).service
                this@InstalledFragment.context!!.startService(Intent(this@InstalledFragment.context, ConsoleService::class.java))
            }

            override fun onServiceDisconnected(name: ComponentName) {
                service = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        context!!.bindService(intent(ConsoleService::class.java), connection!!, Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        context!!.unbindService(connection!!)
    }

    fun intent(cls: Class<*>): Intent {
        return Intent(context, cls)
    }

    fun share(file: File) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/*"
        sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.absolutePath))
        startActivity(Intent.createChooser(sharingIntent, "share file with"))
    }

    class PluginAdapter(context: Context, resource: Int) : ArrayAdapter<PluginDescription>(context, resource) {
        private var mOriginalValues: ArrayList<PluginDescription>? = null
        private var mDisplayedValues: ArrayList<PluginDescription>? = null

        init {
            mInflater = LayoutInflater.from(context)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val text: TextView

            if (convertView == null) {
                view = mInflater.inflate(mResource, parent, false)
            } else {
                view = convertView
            }

            try {
                //  Otherwise, find the TextView field within the layout
                text = view as TextView
            } catch (e: ClassCastException) {
                Log.e("ArrayAdapter", "You must supply a resource ID for a TextView")
                throw IllegalStateException(
                        "ArrayAdapter requires the resource ID to be a TextView", e)
            }

            val item = getItem(position)
            text.text = item!!.name + " (v" + item.version + ")"
            text.setTextColor(ContextCompat.getColor(mContext!!, R.color.colorAccent))
            return view
        }

        override fun getFilter(): Filter {
            return PluginFilter()
        }

        private inner class PluginFilter : Filter() {
            override fun publishResults(constraint: CharSequence, results: Filter.FilterResults) {

                mDisplayedValues = results.values as ArrayList<PluginDescription> // has the filtered values
                notifyDataSetChanged()  // notifies the data with new filtered values
            }

            override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
                var constraint = constraint
                val results = Filter.FilterResults()        // Holds the results of a filtering operation in values
                val FilteredArrList = ArrayList<PluginDescription>()

                if (mOriginalValues == null) {
                    mOriginalValues = ArrayList(mDisplayedValues!!) // saves the original data in mOriginalValues
                }

                /********
                 *
                 * If constraint(CharSequence that is received) is null returns the mOriginalValues(Original) values
                 * else does the Filtering and returns FilteredArrList(Filtered)
                 *
                 */
                if (constraint == null || constraint.length == 0) {

                    // set the Original result to return
                    results.count = mOriginalValues!!.size
                    results.values = mOriginalValues
                } else {
                    constraint = constraint.toString().toLowerCase()
                    for (i in mOriginalValues!!.indices) {
                        val desc = mOriginalValues!![i]

                        if (desc.name.toLowerCase().contains(constraint.toString()))
                            FilteredArrList.add(desc)

                        if (desc.author.toLowerCase().contains(constraint.toString()))
                            FilteredArrList.add(desc)

                    }
                    // set the Filtered result to return
                    results.count = FilteredArrList.size
                    results.values = FilteredArrList
                }
                return results
            }
        }

        companion object {

            private var mResource: Int = 0
            private var mContext: Context? = null
            private lateinit var mInflater: LayoutInflater
        }
    }
}
