package fr.rhaz.os.android.plugins;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;

import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import fr.rhaz.os.OS;
import fr.rhaz.os.android.ConsoleService;
import fr.rhaz.os.android.ConsoleService.ConsoleBinder;
import fr.rhaz.os.android.R;
import fr.rhaz.os.plugins.Plugin;
import fr.rhaz.os.plugins.PluginDescription;
import fr.rhaz.os.plugins.PluginRunnable;

public class FirstFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private ArrayList<String> list;
    private ArrayAdapter adapter;
    private ListView listview;
    private SwipeRefreshLayout swipe;
    private ServiceConnection connection;
    private ConsoleService service;
    private Plugin plugin;
    private HashMap<String, PluginDescription> plugins;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.first_layout, container, false);

        setServiceConnection();
        plugins = new HashMap<>();
        listview = (ListView) v.findViewById(R.id.list);
        swipe = (SwipeRefreshLayout) v.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(this);
        list = new ArrayList<>();
        adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_multiple_choice, list);
        listview.setAdapter(adapter);
        registerForContextMenu(listview);
        listview.setLongClickable(getActivity().isRestricted());
        swipe.post(() -> {
            onRefresh();
        });
        listview.setOnItemClickListener((parent, view, position, id) -> {
            parent.showContextMenuForChild(view);
            plugin = getOS().getPluginManager().getPlugin(((TextView)view).getText().toString());
        });
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
        switch(item.getItemId()){
            case R.id.config:{
                File folder = new File(getOS().getPluginManager().getFolder(), plugin.getDescription().getName());
                if(!folder.exists()) folder.mkdir();
                openFolder(folder);
                return true;
            }
            case R.id.delete:{
                new AlertDialog.Builder(getActivity())
                        .setMessage("Are you sure you want to delete this plugin?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", (dialog, id) -> {
                            plugin.getDescription().getFile().delete();
                            list.remove(info.position);
                            adapter.notifyDataSetChanged();
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            } default: return super.onContextItemSelected(item);
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
        list.clear();
        try{
            getOS();
        } catch (NullPointerException e){
            return;
        }
        swipe.setRefreshing(true);
        for(PluginRunnable runnable:getOS().getPluginManager().getPlugins()){
            PluginDescription desc = runnable.getPlugin().getDescription();
            plugins.put(desc.getName()+" (v"+desc.getVersion()+")", desc);
            list.add(desc.getName()+" (v"+desc.getVersion()+")");
        }
        adapter.notifyDataSetChanged();
        swipe.setRefreshing(false);
    }

    public void setServiceConnection(){

        this.connection = new ServiceConnection() {

            public void onServiceConnected(ComponentName name, IBinder ibinder) {
                service = ((ConsoleBinder) ibinder).getService();
                FirstFragment.this.getContext().startService(new Intent(FirstFragment.this.getContext(), ConsoleService.class));
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
}
