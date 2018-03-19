package embedded.com.android.dx.command.dump;

class Args
{
    boolean debug;
    boolean rawBytes;
    boolean basicBlocks;
    boolean ropBlocks;
    boolean ssaBlocks;
    String ssaStep;
    boolean optimize;
    boolean strictParse;
    int width;
    boolean dotDump;
    String method;
    
    Args() {
        this.debug = false;
        this.rawBytes = false;
        this.basicBlocks = false;
        this.ropBlocks = false;
        this.ssaBlocks = false;
        this.ssaStep = null;
        this.optimize = false;
        this.strictParse = false;
        this.width = 0;
        this.dotDump = false;
    }
}
