package fr.rhaz.os.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import fr.rhaz.os.android.ConsoleService.ConsoleBinder;

public class ConsoleActivity extends AppCompatActivity {
    public static final String RECEIVER = "fr.rhaz.os.RECEIVER";
    private ServiceConnection connection;
    private ConsoleService service;
    private EditText textView;
    private EditText inputView;
    private Timer timer;
    private Handler handler;
    private int linecount;
    private ScrollView scrollview;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);

        linecount = 0;

        scrollview = (ScrollView) findViewById(R.id.scroll);

        textView = (EditText) findViewById(R.id.text);
        //textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setFocusable(true);
        textView.setTextIsSelectable(true);
        textView.setOnKeyListener((v, keyCode, event) -> true);

        inputView = (EditText) findViewById(R.id.input);
        inputView.requestFocus();
        inputView.setOnEditorActionListener((v, actionId, event) -> {

            String input = v.getText().toString();
            v.setText("");

            if (service.getOS().getThread().isAlive())
                service.getOS().getConsole().process(input);
            else finish();

            return true;
        });

        handler = new Handler();

        setServiceConnection();
        setTimer();
    }

    public void setTimer(){
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
            handler.post(() -> {
                if (!textView.isFocused())
                    updateLogs();
            });
            }
        }, 0, 1000);
    }

    public void setServiceConnection(){

        this.connection = new ServiceConnection() {

            public void onServiceConnected(ComponentName name, IBinder ibinder) {
                service = ((ConsoleBinder) ibinder).getService();
                startService(new Intent(ConsoleActivity.this, ConsoleService.class));
            }

            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };
    }

    public void updateLogs() {
        if (!ready()) return;

        // Max lines showed
        if(textView.getLineCount() > 500) // If max lines is reached
            textView.getEditableText().delete(0, 400); // Delete first 400 lines

        List<String> logs = new ArrayList<>(service.getOutput().get());

        // Max lines processed per second (100 lines per second)
        List<String> diff;
        if(logs.size() - linecount > 100) // If missing lines is over 100
            diff = logs.subList(logs.size()-100, logs.size()); // Get 100 last lines
        else diff = logs.subList(linecount, logs.size()); // Get all missing lines

        for(String log:diff)
            textView.append("\n" + log);

        linecount = logs.size();
    }

    public boolean ready() {
        return (this.service == null || this.service.getOS() == null) ? false : true;
    }

    protected void onResume() {
        super.onResume();
        bindService(intent(ConsoleService.class), this.connection, Context.BIND_AUTO_CREATE);
    }

    protected void onPause() {
        super.onPause();
        unbindService(this.connection);
    }

    public void onStart() {
        super.onStart();
    }

    public void onStop() {
        super.onStop();
    }

    public Intent intent(Class<?> cls){
        return new Intent(this, cls);
    }

}
