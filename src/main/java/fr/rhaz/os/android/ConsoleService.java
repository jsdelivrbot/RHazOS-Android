package fr.rhaz.os.android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v7.app.NotificationCompat.Builder;
import android.util.Log;

import java.io.File;
import java.util.function.BiConsumer;

import dalvik.system.DexClassLoader;
import fr.rhaz.os.*;
import fr.rhaz.os.OS.OSEnvironment;
import fr.rhaz.os.plugins.Plugin;
import fr.rhaz.os.plugins.PluginDescription;

public class ConsoleService extends Service {
    private OS os;
    private BigStringOutput output;
    private Notification note;

    public class ConsoleBinder extends Binder {
        public ConsoleService getService() {
            return ConsoleService.this;
        }
    }

    @Override
    public void onCreate(){
        Intent iopen = new Intent(this, ConsoleActivity.class);
        iopen.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP + Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent istop = new Intent(this, NotificationReceiver.class);
        Action stop = new Action(
                R.drawable.ic_clear_black_24dp,
                "Stop",
                pending(istop)
        );

        note = new Builder(this)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("RHaz OS")
                .setContentIntent(pending(iopen))
                .setVisibility(Notification.VISIBILITY_SECRET)
                .addAction(stop)
                .build();

        note.flags |= Notification.FLAG_NO_CLEAR;

        this.output = new BigStringOutput();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent.getAction() != null && intent.getAction().equals("stop")){
            this.stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(1337, note);
        start();

        return Service.START_NOT_STICKY;
    }

    public PendingIntent pending(Intent intent){
        return PendingIntent.getBroadcast(this, 0, intent, 0);
    }

    public void start() {
        try {

            if(this.os != null)
                if (this.os.getThread().isAlive())
                    return;


            this.os = new OS(OSEnvironment.ANDROID);

            os.getConsole().defaultStart();
            os.getConsole().getLogger().getOutputs().add(output);

            BiConsumer<PluginDescription, String> loader = (desc, main) -> injectDexClass(desc, main);

            os.getPluginManager().setFolder(new File(Environment.getExternalStorageDirectory(), "RHazOS/plugins"));



            os.getPluginManager().loadAll(loader);
            Log.w("RHazOS", "Loaded plugins");

            Thread.sleep(1000);

            os.getPluginManager().enableAll();
            Log.w("RHazOS", "Enabled plugins");

            os.started();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void injectDexClass(PluginDescription desc, String main){
        try {

            DexClassLoader loader = new DexClassLoader(desc.getFile().getPath(), getFilesDir().getPath(), null, this.getClassLoader());
            Class<? extends Plugin> pluginclass = Class.forName(main, true, loader).asSubclass(Plugin.class);
            desc.setPluginClass(pluginclass);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BigStringOutput getOutput(){
        return output;
    }

    public OS getOS() {
        return this.os;
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return new ConsoleBinder();
    }

    public static class NotificationReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Intent i = new Intent(context, ConsoleService.class);
            i.setAction("stop");
            context.startService(i);
        }
    }
}
