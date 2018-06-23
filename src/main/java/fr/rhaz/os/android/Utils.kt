package fr.rhaz.os.android

import android.app.Activity
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.View
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import org.jetbrains.anko.actionMenuView
import org.jetbrains.anko.browse
import org.jetbrains.anko.design.longSnackbar
import java.io.*
import java.net.URL

object Utils {

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (info in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (info.service.className != serviceClass.name) continue
            return true
        }

        return false
    }

    fun openFolder(view: View, context: Context, path: File) {
        val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", path)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "resource/folder")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        longSnackbar(view, "Please install a file manager", "Play Store"){context.browse("https://play.google.com/store/apps/details?id=com.estrongs.android.pop")};
        /*try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {

        }*/

    }

    fun toString(list: List<String>): String {
        var str = ""
        for (line in list) {
            str = str + line + "\n"
        }
        return str
    }

    fun toStringArray(list: List<String>): Array<String> {
        return list.toTypedArray()
    }

    fun loadConfig(context: Context): Configuration? {
        try {
            val folder = File(Environment.getExternalStorageDirectory(), "RHazOS")
            folder.mkdirs()
            val file = File(folder, "config.yml")
            if (!file.exists())
                copyInputStreamToFile(context.resources.openRawResource(R.raw.config), file)
            return ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(file)
        } catch (e: IOException) {
            Log.e("RHazOS", Log.getStackTraceString(e))
            return null
        }

    }

    fun getHTML(link: String): String {
        val content = StringBuilder()
        val bufferedReader = BufferedReader(InputStreamReader(URL(link).openConnection().getInputStream()))
        while (true) {
            val line = bufferedReader.readLine()
            if (line != null) {
                content.append(line + "\n")
            } else {
                bufferedReader.close()
                return content.toString()
            }
        }
    }

    private fun copyInputStreamToFile(`in`: InputStream, file: File) {
        try {
            val out = FileOutputStream(file)
            val buf = ByteArray(1024)
            while (true) {
                val len = `in`.read(buf)
                if (len > 0) {
                    out.write(buf, 0, len)
                } else {
                    out.close()
                    `in`.close()
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}

class GenericFileProvider : FileProvider()
