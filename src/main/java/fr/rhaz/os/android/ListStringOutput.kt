package fr.rhaz.os.android

import java.util.ArrayList

import fr.rhaz.os.logging.Output

class ListStringOutput : Output<ArrayList<String>>(ArrayList()) {

    override fun write(msg: String) {
        out.add(msg)
    }
}
