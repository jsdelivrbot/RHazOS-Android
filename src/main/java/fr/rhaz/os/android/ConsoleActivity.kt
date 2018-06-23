package fr.rhaz.os.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ScrollView

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Timer
import java.util.TimerTask

import fr.rhaz.os.android.ConsoleService.ConsoleBinder

class ConsoleActivity : AppCompatActivity() {
    private var connection: ServiceConnection? = null
    private var service: ConsoleService? = null
    private var textView: EditText? = null
    private var inputView: EditText? = null
    private var timer: Timer? = null
    private var handler: Handler? = null
    private var linecount: Int = 0
    private var scrollview: ScrollView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_console)

        linecount = 0

        //scrollview = findViewById<View>(R.id.scroll) as ScrollView

        textView = findViewById<View>(R.id.text) as EditText
        //textView.setMovementMethod(new ScrollingMovementMethod());
        textView!!.isFocusable = true
        textView!!.setTextIsSelectable(true)
        textView!!.setOnKeyListener { v, keyCode, event -> true }

        inputView = findViewById<View>(R.id.input) as EditText
        inputView!!.requestFocus()
        inputView!!.setOnEditorActionListener { v, actionId, event ->

            val input = v.text.toString()
            v.text = ""

            if (service!!.os!!.thread.isAlive)
                service!!.os!!.console.process(input)
            else
                finish()

            true
        }

        handler = Handler()

        setServiceConnection()
        setTimer()
    }

    fun setTimer() {
        this.timer = Timer()
        this.timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler!!.post {
                    if (!textView!!.isFocused)
                        updateLogs()
                }
            }
        }, 0, 1000)
    }

    fun setServiceConnection() {

        this.connection = object : ServiceConnection {

            override fun onServiceConnected(name: ComponentName, ibinder: IBinder) {
                service = (ibinder as ConsoleBinder).service
                startService(Intent(this@ConsoleActivity, ConsoleService::class.java))
            }

            override fun onServiceDisconnected(name: ComponentName) {
                service = null
            }
        }
    }

    fun updateLogs() {
        if (!ready()) return

        // Max lines showed
        if (textView!!.lineCount > 500)
        // If max lines is reached
            textView!!.editableText.delete(0, 400) // Delete first 400 lines

        val logs = ArrayList(service!!.output!!.get())

        // Max lines processed per second (100 lines per second)
        val diff: List<String>
        if (logs.size - linecount > 100)
        // If missing lines is over 100
            diff = logs.subList(logs.size - 100, logs.size) // Get 100 last lines
        else
            diff = logs.subList(linecount, logs.size) // Get all missing lines

        for (log in diff)
            textView!!.append("\n" + log)

        linecount = logs.size
    }

    fun ready(): Boolean {
        return if (this.service == null || this.service!!.os == null) false else true
    }

    override fun onResume() {
        super.onResume()
        bindService(intent(ConsoleService::class.java), this.connection, Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        unbindService(this.connection)
    }

    public override fun onStart() {
        super.onStart()
    }

    public override fun onStop() {
        super.onStop()
    }

    fun intent(cls: Class<*>): Intent {
        return Intent(this, cls)
    }

    companion object {
        val RECEIVER = "fr.rhaz.os.RECEIVER"
    }

}
