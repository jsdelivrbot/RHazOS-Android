package fr.rhaz.os.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if (!Utils.isServiceRunning(this, ConsoleService.class))
            startService(intent(ConsoleService.class));
    }

    public Intent intent(Class<?> cls){
        return new Intent(this, cls);
    }
}
