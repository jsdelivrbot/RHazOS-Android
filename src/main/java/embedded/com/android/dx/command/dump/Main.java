package embedded.com.android.dx.command.dump;

import embedded.com.android.dex.util.*;
import java.io.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.cf.iface.*;

public class Main
{
    private Args parsedArgs;
    
    private Main() {
        this.parsedArgs = new Args();
    }
    
    public static void main(final String[] args) {
        new Main().run(args);
    }
    
    private void run(final String[] args) {
        int at;
        for (at = 0; at < args.length; ++at) {
            String arg = args[at];
            if (arg.equals("--")) {
                break;
            }
            if (!arg.startsWith("--")) {
                break;
            }
            if (arg.equals("--bytes")) {
                this.parsedArgs.rawBytes = true;
            }
            else if (arg.equals("--basic-blocks")) {
                this.parsedArgs.basicBlocks = true;
            }
            else if (arg.equals("--rop-blocks")) {
                this.parsedArgs.ropBlocks = true;
            }
            else if (arg.equals("--optimize")) {
                this.parsedArgs.optimize = true;
            }
            else if (arg.equals("--ssa-blocks")) {
                this.parsedArgs.ssaBlocks = true;
            }
            else if (arg.startsWith("--ssa-step=")) {
                this.parsedArgs.ssaStep = arg.substring(arg.indexOf(61) + 1);
            }
            else if (arg.equals("--debug")) {
                this.parsedArgs.debug = true;
            }
            else if (arg.equals("--dot")) {
                this.parsedArgs.dotDump = true;
            }
            else if (arg.equals("--strict")) {
                this.parsedArgs.strictParse = true;
            }
            else if (arg.startsWith("--width=")) {
                arg = arg.substring(arg.indexOf(61) + 1);
                this.parsedArgs.width = Integer.parseInt(arg);
            }
            else {
                if (!arg.startsWith("--method=")) {
                    System.err.println("unknown option: " + arg);
                    throw new RuntimeException("usage");
                }
                arg = arg.substring(arg.indexOf(61) + 1);
                this.parsedArgs.method = arg;
            }
        }
        if (at == args.length) {
            System.err.println("no input files specified");
            throw new RuntimeException("usage");
        }
        while (at < args.length) {
            try {
                final String name = args[at];
                System.out.println("reading " + name + "...");
                byte[] bytes = FileUtils.readFile(name);
                if (!name.endsWith(".class")) {
                    String src;
                    try {
                        src = new String(bytes, "utf-8");
                    }
                    catch (UnsupportedEncodingException ex) {
                        throw new RuntimeException("shouldn't happen", ex);
                    }
                    bytes = HexParser.parse(src);
                }
                this.processOne(name, bytes);
            }
            catch (ParseException ex2) {
                System.err.println("\ntrouble parsing:");
                if (this.parsedArgs.debug) {
                    ex2.printStackTrace();
                }
                else {
                    ex2.printContext(System.err);
                }
            }
            ++at;
        }
    }
    
    private void processOne(final String name, final byte[] bytes) {
        if (this.parsedArgs.dotDump) {
            DotDumper.dump(bytes, name, this.parsedArgs);
        }
        else if (this.parsedArgs.basicBlocks) {
            BlockDumper.dump(bytes, System.out, name, false, this.parsedArgs);
        }
        else if (this.parsedArgs.ropBlocks) {
            BlockDumper.dump(bytes, System.out, name, true, this.parsedArgs);
        }
        else if (this.parsedArgs.ssaBlocks) {
            this.parsedArgs.optimize = false;
            SsaDumper.dump(bytes, System.out, name, this.parsedArgs);
        }
        else {
            ClassDumper.dump(bytes, System.out, name, this.parsedArgs);
        }
    }
}
