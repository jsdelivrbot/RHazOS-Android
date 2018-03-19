package fr.rhaz.os.android;

import android.Manifest;
import android.Manifest.permission;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.File;

import fr.rhaz.os.android.plugins.PluginsActivity;

public class Menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        int permissions_code = 42;
        String[] permissions = {permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE};

        ActivityCompat.requestPermissions(this, permissions, permissions_code);
    }

    public Intent intent(Class<?> cls){
        return new Intent(this, cls);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        for(int r:grantResults)
            if(r != PackageManager.PERMISSION_GRANTED) return;

        File folder = new File(Environment.getExternalStorageDirectory(), "RHazOS");
        try {
            if(!folder.exists())
                if(!folder.mkdirs())
                    Log.w("RHazOS", "Could not create folder");
            folder.setWritable(true);
        } catch(Exception e){
            e.printStackTrace();
            return;
        }

        if (!Utils.isServiceRunning(this, ConsoleService.class))
            startService(intent(ConsoleService.class));

        findViewById(R.id.startbutton).setOnClickListener(v -> startActivity(intent(ConsoleActivity.class)));
        findViewById(R.id.pluginsbutton).setOnClickListener(v -> startActivity(intent(PluginsActivity.class)));
        findViewById(R.id.optionsbutton).setOnClickListener(v -> Utils.openFolder(Menu.this, new File(Environment.getExternalStorageDirectory(), "RHazOS")));
        findViewById(R.id.aboutbutton).setOnClickListener(v -> startActivity(intent(AboutActivity.class)));
    }
}
