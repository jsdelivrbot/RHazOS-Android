package fr.rhaz.os.android

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.Action
import android.util.Log

import java.io.File
import java.util.function.Consumer

import dalvik.system.DexClassLoader
import fr.rhaz.os.OS
import fr.rhaz.os.commands.users.Root
import fr.rhaz.os.plugins.Plugin
import fr.rhaz.os.plugins.PluginDescription

class ConsoleService : Service() {
    var os: OS? = null
    var output: ListStringOutput? = null
    private var note: Notification? = null

    inner class ConsoleBinder : Binder() {
        val service: ConsoleService
            get() = this@ConsoleService
    }

    override fun onCreate() {
        val imenu = Intent(this, Menu::class.java)

        val iconsole = Intent(this, ConsoleActivity::class.java)
        val console = Action(
                R.drawable.ic_keyboard_black_24dp,
                "Open",
                activity(iconsole)
        )

        val istop = Intent(this, NotificationReceiver::class.java)
        val stop = Action(
                R.drawable.ic_clear_black_24dp,
                "Stop",
                broadcast(istop)
        )

        note = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("RHaz OS")
                .setContentIntent(activity(imenu))
                .setVisibility(Notification.VISIBILITY_SECRET)
                .addAction(console)
                .addAction(stop)
                .build()

        note!!.flags = note!!.flags or Notification.FLAG_NO_CLEAR

        this.output = ListStringOutput()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.action != null && intent.action == "stop") {
            this.stopSelf()
            return Service.START_NOT_STICKY
        }

        startForeground(1337, note)
        start()

        return Service.START_NOT_STICKY
    }

    fun broadcast(intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(this, 0, intent, 0)
    }

    fun activity(intent: Intent): PendingIntent {
        return PendingIntent.getActivity(this, 0, intent, 0)
    }

    fun start() {
        try {

            if (this.os != null)
                if (this.os!!.thread.isAlive)
                    return

            val folder = File(Environment.getExternalStorageDirectory(), "RHazOS")
            folder.mkdirs()
            folder.setWritable(true)


            val pfolder = File(folder, "plugins")
            pfolder.mkdirs()
            pfolder.setWritable(true)

            Log.w("RHazOS", "Created plugins dir")

            Utils.loadConfig(this)

            this.os = OS(OS.Environment.ANDROID)

            os!!.folder = folder
            os!!.add(Root(os!!))
            os!!.console.defaultStart()
            os!!.console.logger.outputs.clear()
            os!!.console.logger.outputs.add(output)

            val loader = this::injectDexClass

            os!!.pluginManager.folder = pfolder

            for (file in os!!.pluginManager.all)
                try {
                    Deodexer.deodex(pfolder, file)
                } catch (e: Exception) {
                    if (e.message == "too big")
                        os!!.write("File " + file.name + " is too big, please dex it using a computer.")
                }

            os!!.pluginManager.loadAll(loader)
            Log.w("RHazOS", "Loaded all plugins")

            Thread.sleep(1000)

            os!!.pluginManager.enableAll()
            Log.w("RHazOS", "Enabled plugins")

            os!!.started()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    fun injectDexClass(desc: PluginDescription) {
        try {
            val loader = DexClassLoader(desc.file.path, filesDir.path, null, this.classLoader)
            val pluginclass = Class.forName(desc.pluginClassName, true, loader).asSubclass(Plugin::class.java)
            desc.pluginClass = pluginclass
            Log.w("RHazOS", "Loaded plugin " + desc.name)

        } catch (e: Exception) {
        }

    }

    override fun onBind(intent: Intent): IBinder? {
        return ConsoleBinder()
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val i = Intent(context, ConsoleService::class.java)
            i.action = "stop"
            context.startService(i)
        }
    }
}
