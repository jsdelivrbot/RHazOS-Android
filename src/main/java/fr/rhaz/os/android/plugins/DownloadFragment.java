package fr.rhaz.os.android.plugins;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import fr.rhaz.os.android.R;
import fr.rhaz.os.android.Utils;
import fr.rhaz.os.android.rss.Handler;
import fr.rhaz.os.android.rss.ListListener;
import fr.rhaz.os.android.rss.PluginRSS;

public class DownloadFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private View v;
    private ListView list;
    private List<Map<String, String>> data;
    private PluginRSSAdapter adapter;
    private SwipeRefreshLayout swipe;
    private Configuration config;
    private List<String> repositories;
    private SearchView searchview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.second_layout, container, false);
        config = Utils.loadConfig(container.getContext());
        repositories = config.getStringList("repositories");
        list = (ListView) v.findViewById(R.id.listView);
        swipe = (SwipeRefreshLayout) v.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(this);
        data = new ArrayList<>();
        adapter = PluginRSSAdapter.create(getContext());
        list.setAdapter(adapter);
        swipe.post(() -> {
            swipe.setRefreshing(true);
            onRefresh();
        });
        return v;
    }

    public static class PluginRSSAdapter extends ArrayAdapter<PluginRSS>{

        private static Context mContext;
        private static int mResource;
        private final LayoutInflater mInflater;
        private static ArrayList<PluginRSS> mOriginalValues;

        public PluginRSSAdapter() {
            super(mContext, mResource, mOriginalValues);
            mInflater = LayoutInflater.from(mContext);
        }

        public static PluginRSSAdapter create(Context context){
            mOriginalValues = new ArrayList<>();
            mContext = context;
            mResource = android.R.layout.simple_list_item_2;
            return new PluginRSSAdapter();
        }

        public ArrayList<PluginRSS> getValues(){
            return mOriginalValues;
        }

        public void setValues(ArrayList<PluginRSS> values){
            mOriginalValues = values;
            clear();
            addAll(mOriginalValues);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mInflater.inflate(mResource, parent, false);
            } else {
                view = convertView;
            }

            TextView text1;
            TextView text2;
            try {
                //  Otherwise, find the TextView field within the layout
                TwoLineListItem twolineview = (TwoLineListItem) view;
                text1 = (TextView) twolineview.getChildAt(0);
                text2 = (TextView) twolineview.getChildAt(1);
            } catch (ClassCastException e) {
                Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
                throw new IllegalStateException(
                        "ArrayAdapter requires the resource ID to be a TextView", e);
            }

            PluginRSS item = getItem(position);
            text1.setText(item.getTitle());
            text1.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            text2.setText(Html.fromHtml(item.getDescription()));
            text2.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            return view;
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint,FilterResults results) {
                    clear();
                    addAll((Collection<PluginRSS>) results.values); // has the filtered values
                    notifyDataSetChanged();  // notifies the data with new filtered values
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
                    ArrayList<PluginRSS> FilteredArrList = new ArrayList<>();

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
                            PluginRSS rss = mOriginalValues.get(i);

                            if (rss.getTitle().toLowerCase().contains(constraint.toString()))
                                FilteredArrList.add(rss);
                            else if (rss.getDescription().toLowerCase().contains(constraint.toString()))
                                FilteredArrList.add(rss);
                        }
                        // set the Filtered result to return
                        results.count = FilteredArrList.size();
                        results.values = FilteredArrList;
                    }
                    return results;
                }
            };
            return filter;
        }
    }

    public ArrayAdapter getAdapter(){
        return adapter;
    }

    @Override
    public void onRefresh() {
        new AsyncTask<String, Void, List<PluginRSS>>(){
            @Override
            protected List<PluginRSS> doInBackground(String... urls) {
                ArrayList<PluginRSS> plugins = new ArrayList<PluginRSS>();
                for(String url: urls) {
                    try {
                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        SAXParser parser = parser = factory.newSAXParser();

                        Handler handler = new Handler();
                        parser.parse(url, handler);

                        plugins.addAll(handler.getPlugins());
                    } catch(Exception e){
                        Log.e("PluginDownload", e.getMessage());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "An internal error occured while downloading plugins list", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
                return plugins;
            }

            @Override
            protected void onPostExecute(List<PluginRSS> plugins){
                adapter.setValues(new ArrayList<>(plugins));
                list.setOnItemClickListener(new ListListener(plugins, getActivity()));
                swipe.setRefreshing(false);
            }
        }.execute(Utils.toStringArray(repositories));
    }
}
