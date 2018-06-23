package fr.rhaz.os.android

import org.apache.commons.io.FileUtils

import java.io.File
import java.io.IOException
import java.util.jar.JarFile

object Deodexer {

    fun deodex(outdir: File, file: File) {
        try {
            val out = File(outdir, file.name)

            if (JarFile(file).getEntry("classes.dex") != null) {
                if (file.path != out.path) FileUtils.copyFile(file, out)
                return
            }

            if (file.length() > 100000)
                throw Exception("too big")

            embedded.com.android.dx.command.Main.main(arrayOf("--dex", "--core-library", "--keep-classes", "--output=" + out.path, file.path))
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}
