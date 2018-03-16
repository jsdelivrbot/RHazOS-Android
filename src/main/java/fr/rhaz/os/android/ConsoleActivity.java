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
import android.widget.EditText;

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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);

        textView = (EditText) findViewById(R.id.main);
        textView.setMovementMethod(new ScrollingMovementMethod());
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
                    if (!textView.isFocused()) {
                        updateLogs();
                        if (textView.getScrollY() == textView.getBottom()) return;
                        textView.requestFocus();
                        textView.scrollTo(0, textView.getBottom());
                        inputView.requestFocus();
                    }
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
        this.textView.setText(service.getOutput().get());
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
