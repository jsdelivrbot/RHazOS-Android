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

import java.io.File;
import java.util.function.BiConsumer;

import dalvik.system.DexClassLoader;
import fr.rhaz.os.OS;
import fr.rhaz.os.plugins.Plugin;
import fr.rhaz.os.plugins.PluginDescription;


public class ConsoleService extends Service {
    private OS os;
    private BigStringOutput output;

    public class ConsoleBinder extends Binder {
        public ConsoleService getService() {
            return ConsoleService.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent iopen = new Intent(this, ConsoleActivity.class);
        iopen.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP + Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent istop = new Intent(this, NotificationReceiver.class);
        Action stop = new Action(
                R.drawable.ic_clear_black_24dp,
                "Stop",
                pending(istop)
        );

        Notification note = new Builder(this)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("RHaz OS")
                .setContentIntent(pending(iopen))
                .setVisibility(Notification.VISIBILITY_SECRET)
                .addAction(stop)
                .build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(1337, note);

        this.output = new BigStringOutput();
        start();

        return Service.START_NOT_STICKY;
    }

    public PendingIntent pending(Intent intent){
        return PendingIntent.getBroadcast(this, 0, intent, 0);
    }

    public void start() {

        if(this.os != null)
            if (this.os.getThread().isAlive())
                this.os.exit();

        this.os = new OS();

        os.getConsole().getLogger().getOutputs().add(output);

        BiConsumer<PluginDescription, String> loader = (desc, main) -> injectDexClass(desc, main);
        os.getPluginManager().setFolder(new File(Environment.getExternalStorageDirectory(), "RHazOS/plugins"));
        os.getPluginManager().loadAll(loader);
        os.getPluginManager().enableAll();
        os.started();
    }

    public void injectDexClass(PluginDescription desc, String main){
        try {

            DexClassLoader loader = new DexClassLoader(desc.getFile().getPath(), getFilesDir().getPath(), null, getClass().getClassLoader());
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

    public class NotificationReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            System.exit(0);
        }
    }
}
