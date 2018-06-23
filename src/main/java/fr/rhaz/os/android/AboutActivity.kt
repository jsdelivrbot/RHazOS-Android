package fr.rhaz.os.android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val konfettiView = findViewById<View>(R.id.konfettiView) as KonfettiView
        konfettiView.setOnTouchListener(View.OnTouchListener { view, e ->
            konfettiView.build()
                    .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
                    .setFadeOutEnabled(true)
                    .setDirection(0.0, 359.0)
                    .setTimeToLive(2000L)
                    .addShapes(Shape.RECT, Shape.CIRCLE)
                    .addSizes(Size(12))
                    .setPosition(e.x-100, e.x+100, e.y-100, e.y+100)
                    .stream(10, 1000L);
            true;
        });
    }

    fun intent(cls: Class<*>): Intent {
        return Intent(this, cls)
    }
}
