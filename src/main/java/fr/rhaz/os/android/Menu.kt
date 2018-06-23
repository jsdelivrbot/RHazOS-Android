package fr.rhaz.os.android

import android.Manifest.permission
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View

import java.io.File

import fr.rhaz.os.android.plugins.PluginsActivity
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.startActivity

class Menu : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val requestCode = 42
        val permissions = arrayOf(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE)

        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        for (r in grantResults)
            if (r != PackageManager.PERMISSION_GRANTED) return

        val folder = File(Environment.getExternalStorageDirectory(), "RHazOS")
        try {
            if (!folder.exists())
                if (!folder.mkdirs())
                    Log.w("RHazOS", "Could not create folder")
            folder.setWritable(true)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        if (!Utils.isServiceRunning(this, ConsoleService::class.java))
            startService(intentFor<ConsoleService>())

        findViewById<View>(R.id.startbutton).setOnClickListener { startActivity<ConsoleActivity>() }
        findViewById<View>(R.id.pluginsbutton).setOnClickListener { startActivity<PluginsActivity>() }
        findViewById<View>(R.id.optionsbutton).setOnClickListener { Utils.openFolder(it,this@Menu, File(Environment.getExternalStorageDirectory(), "RHazOS")) }
        findViewById<View>(R.id.aboutbutton).setOnClickListener { startActivity<AboutActivity>() }
    }
}
