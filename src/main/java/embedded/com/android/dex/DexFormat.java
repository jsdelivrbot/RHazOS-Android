package embedded.com.android.dex;

public final class DexFormat
{
    public static final int API_INVOKE_POLYMORPHIC = 26;
    public static final int API_DEFAULT_INTERFACE_METHODS = 24;
    public static final int API_NO_EXTENDED_OPCODES = 13;
    public static final int API_CURRENT = 26;
    public static final String VERSION_FOR_API_26 = "038";
    public static final String VERSION_FOR_API_24 = "037";
    public static final String VERSION_FOR_API_13 = "035";
    public static final String VERSION_CURRENT = "038";
    public static final String DEX_IN_JAR_NAME = "classes.dex";
    public static final String MAGIC_PREFIX = "dex\n";
    public static final String MAGIC_SUFFIX = "\u0000";
    public static final int ENDIAN_TAG = 305419896;
    public static final int MAX_MEMBER_IDX = 65535;
    public static final int MAX_TYPE_IDX = 65535;
    
    public static int magicToApi(final byte[] magic) {
        if (magic.length != 8) {
            return -1;
        }
        if (magic[0] != 100 || magic[1] != 101 || magic[2] != 120 || magic[3] != 10 || magic[7] != 0) {
            return -1;
        }
        final String version = "" + (char)magic[4] + (char)magic[5] + (char)magic[6];
        if (version.equals("035")) {
            return 13;
        }
        if (version.equals("037")) {
            return 24;
        }
        if (version.equals("038")) {
            return 26;
        }
        if (version.equals("038")) {
            return 26;
        }
        return -1;
    }
    
    public static String apiToMagic(final int targetApiLevel) {
        String version;
        if (targetApiLevel >= 26) {
            version = "038";
        }
        else if (targetApiLevel >= 26) {
            version = "038";
        }
        else if (targetApiLevel >= 24) {
            version = "037";
        }
        else {
            version = "035";
        }
        return "dex\n" + version + "\u0000";
    }
    
    public static boolean isSupportedDexMagic(final byte[] magic) {
        final int api = magicToApi(magic);
        return api > 0;
    }
}
