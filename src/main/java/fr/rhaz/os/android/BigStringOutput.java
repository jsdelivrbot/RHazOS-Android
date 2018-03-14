package fr.rhaz.os.android;

import fr.rhaz.os.logging.Output;

public class BigStringOutput extends Output<String> {

    public BigStringOutput() {
        super(new String());
    }

    @Override
    public void write(String msg) {
        out += msg + "\n";
    }
}
