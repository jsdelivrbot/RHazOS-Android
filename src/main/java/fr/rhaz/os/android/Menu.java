package fr.rhaz.os.android;

import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

        ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 1);
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 2);

        if (!Utils.isServiceRunning(this, ConsoleService.class))
            startService(intent(ConsoleService.class));

        findViewById(R.id.startbutton).setOnClickListener(v -> startActivity(intent(ConsoleActivity.class)));
        findViewById(R.id.pluginsbutton).setOnClickListener(v -> startActivity(intent(PluginsActivity.class)));
        findViewById(R.id.optionsbutton).setOnClickListener(v -> Utils.openFolder(Menu.this, new File(Environment.getExternalStorageDirectory(), "RHazOS")));
        findViewById(R.id.aboutbutton).setOnClickListener(v -> startActivity(intent(AboutActivity.class)));
    }

    public Intent intent(Class<?> cls){
        return new Intent(this, cls);
    }
}
