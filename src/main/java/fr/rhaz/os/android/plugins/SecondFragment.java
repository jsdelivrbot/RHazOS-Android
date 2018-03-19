package fr.rhaz.os.android.plugins;

import android.graphics.Color;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
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

public class SecondFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private View v;
    private ListView list;
    private List<Map<String, String>> data;
    private SimpleAdapter adapter;
    private SwipeRefreshLayout swipe;
    private Configuration config;
    private List<String> repositories;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.second_layout, container, false);
        config = Utils.loadConfig(container.getContext());
        repositories = config.getStringList("repositories");
        list = (ListView) v.findViewById(R.id.listView);
        swipe = (SwipeRefreshLayout) v.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(this);
        data = new ArrayList<>();
        adapter = new SimpleAdapter(getActivity(), data, android.R.layout.simple_list_item_2,
                new String[]{"title", "description"},
                new int[]{android.R.id.text1, android.R.id.text2}) {
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                        text2.setText(Html.fromHtml(text2.getText().toString()));
                        text2.setTextColor(Color.GRAY);
                        return view;
                    }
                };
        list.setAdapter(adapter);
        swipe.post(() -> {
            swipe.setRefreshing(true);
            onRefresh();
        }
        );

        return v;
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
                data.clear();
                for (PluginRSS plugin:plugins) {
                    Map<String, String> datum = new HashMap<>();
                    datum.put("title", plugin.getTitle());
                    datum.put("description", plugin.getDescription());
                    data.add(datum);
                }
                list.setOnItemClickListener(new ListListener(plugins, getActivity()));
                adapter.notifyDataSetChanged();
                swipe.setRefreshing(false);
            }
        }.execute(Utils.toStringArray(repositories));
    }
}
