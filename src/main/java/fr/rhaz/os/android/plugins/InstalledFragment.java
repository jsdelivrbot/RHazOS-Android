package fr.rhaz.os.android.plugins;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;

import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.media.midi.MidiOutputPort;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.rhaz.os.OS;
import fr.rhaz.os.android.ConsoleService;
import fr.rhaz.os.android.ConsoleService.ConsoleBinder;
import fr.rhaz.os.android.R;
import fr.rhaz.os.plugins.PluginDescription;
import fr.rhaz.os.plugins.PluginRunnable;

public class InstalledFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private PluginAdapter adapter;
    private ListView listview;
    private SwipeRefreshLayout swipe;
    private ServiceConnection connection;
    private ConsoleService service;
    //private EditText searchview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.first_layout, container, false);

        setServiceConnection();
        listview = (ListView) v.findViewById(R.id.list);
        swipe = (SwipeRefreshLayout) v.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(this);
        adapter = new PluginAdapter(getActivity());
        listview.setAdapter(adapter);
        registerForContextMenu(listview);
        listview.setLongClickable(getActivity().isRestricted());
        swipe.post(() -> onRefresh());
        listview.setOnItemClickListener((parent, view, position, id) -> parent.showContextMenuForChild(view));
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
        return v;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater i = getActivity().getMenuInflater();
        i.inflate(R.menu.plugins_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        PluginDescription desc = adapter.getItem(info.position);
        switch(item.getItemId()){
            case R.id.config:{
                File folder = new File(getOS().getPluginManager().getFolder(), desc.getName());
                if(!folder.exists()) folder.mkdir();
                openFolder(folder);
                return true;
            }
            case R.id.delete:{
                new AlertDialog.Builder(getActivity())
                        .setMessage("Are you sure you want to delete this plugin?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", (dialog, id) -> {
                            desc.getFile().delete();
                            adapter.remove(desc);
                            adapter.notifyDataSetChanged();
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }
            case R.id.share:{
                share(desc.getFile());
                return true;
            }
            default: return super.onContextItemSelected(item);
        }
    }

    public void openFolder(File path) {
        Uri uri = Uri.fromFile(path);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        try {
            startActivity(intent);
        } catch(ActivityNotFoundException e){
            Toast.makeText(getActivity(), "Please install a file manager", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRefresh() {
        adapter.clear();
        try{
            getOS();
        } catch (NullPointerException e){
            return;
        }
        swipe.setRefreshing(true);
        for(PluginRunnable runnable:getOS().getPluginManager().getPlugins()){
            PluginDescription desc = runnable.getPlugin().getDescription();
            adapter.add(desc);
        }
        adapter.notifyDataSetChanged();
        swipe.setRefreshing(false);
    }

    public void setServiceConnection(){

        this.connection = new ServiceConnection() {

            public void onServiceConnected(ComponentName name, IBinder ibinder) {
                service = ((ConsoleBinder) ibinder).getService();
                InstalledFragment.this.getContext().startService(new Intent(InstalledFragment.this.getContext(), ConsoleService.class));
            }

            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };
    }

    public void onResume() {
        super.onResume();
        getContext().bindService(intent(ConsoleService.class), connection, Context.BIND_AUTO_CREATE);
    }

    public void onPause() {
        super.onPause();
        getContext().unbindService(connection);
    }

    public Intent intent(Class<?> cls){
        return new Intent(getContext(), cls);
    }

    public OS getOS() throws NullPointerException{
        if(service == null)
            throw new NullPointerException();

        if(service.getOS() == null)
            throw new NullPointerException();

        return service.getOS();
    }

    public void share(File file){
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/*");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getAbsolutePath()));
        startActivity(Intent.createChooser(sharingIntent, "share file with"));
    }

    public static class PluginAdapter extends ArrayAdapter<PluginDescription>{

        private static int mResource;
        private static Context mContext;
        private static LayoutInflater mInflater;
        private ArrayList<PluginDescription> mOriginalValues;
        private ArrayList<PluginDescription> mDisplayedValues;

        public PluginAdapter(@NonNull Context context) {
            super((mContext = context), (mResource = android.R.layout.simple_list_item_1));
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View view;
            TextView text;

            if (convertView == null) {
                view = mInflater.inflate(mResource, parent, false);
            } else {
                view = convertView;
            }

            try {
                //  Otherwise, find the TextView field within the layout
                text = (TextView) view;
            } catch (ClassCastException e) {
                Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
                throw new IllegalStateException(
                        "ArrayAdapter requires the resource ID to be a TextView", e);
            }

            PluginDescription item = getItem(position);
            text.setText(item.getName() + " (v" + item.getVersion() + ")");
            text.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            return view;
        }

        @Override
        public Filter getFilter() {
            return new PluginFilter();
        }

        private class PluginFilter extends Filter {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint,FilterResults results) {

                mDisplayedValues = (ArrayList<PluginDescription>) results.values; // has the filtered values
                notifyDataSetChanged();  // notifies the data with new filtered values
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
                ArrayList<PluginDescription> FilteredArrList = new ArrayList<>();

                if (mOriginalValues == null) {
                    mOriginalValues = new ArrayList<PluginDescription>(mDisplayedValues); // saves the original data in mOriginalValues
                }

                /********
                 *
                 *  If constraint(CharSequence that is received) is null returns the mOriginalValues(Original) values
                 *  else does the Filtering and returns FilteredArrList(Filtered)
                 *
                 ********/
                if (constraint == null || constraint.length() == 0) {

                    // set the Original result to return
                    results.count = mOriginalValues.size();
                    results.values = mOriginalValues;
                } else {
                    constraint = constraint.toString().toLowerCase();
                    for (int i = 0; i < mOriginalValues.size(); i++) {
                        PluginDescription desc = mOriginalValues.get(i);

                        if (desc.getName().toLowerCase().contains(constraint.toString()))
                            FilteredArrList.add(desc);

                        if (desc.getAuthor().toLowerCase().contains(constraint.toString()))
                            FilteredArrList.add(desc);

                    }
                    // set the Filtered result to return
                    results.count = FilteredArrList.size();
                    results.values = FilteredArrList;
                }
                return results;
            }
        }
    }
}
