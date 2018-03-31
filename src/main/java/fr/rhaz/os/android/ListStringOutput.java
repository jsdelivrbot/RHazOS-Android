package fr.rhaz.os.android;

import java.util.ArrayList;

import fr.rhaz.os.logging.Output;

public class ListStringOutput extends Output<ArrayList<String>> {

    public ListStringOutput() {
        super(new ArrayList<>());
    }

    @Override
    public void write(String msg) {
        out.add(msg);
    }
}
