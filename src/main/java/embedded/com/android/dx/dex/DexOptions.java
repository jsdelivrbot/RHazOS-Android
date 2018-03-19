package embedded.com.android.dx.dex;

import embedded.com.android.dex.*;

public class DexOptions
{
    public static final boolean ALIGN_64BIT_REGS_SUPPORT = true;
    public boolean ALIGN_64BIT_REGS_IN_OUTPUT_FINISHER;
    public int minSdkVersion;
    public boolean forceJumbo;
    
    public DexOptions() {
        this.ALIGN_64BIT_REGS_IN_OUTPUT_FINISHER = true;
        this.minSdkVersion = 13;
        this.forceJumbo = false;
    }
    
    public String getMagic() {
        return DexFormat.apiToMagic(this.minSdkVersion);
    }
    
    public boolean canUseDefaultInterfaceMethods() {
        return this.minSdkVersion >= 24;
    }
    
    public boolean canUseInvokePolymorphic() {
        return this.minSdkVersion >= 26;
    }
    
    public boolean canUseInvokeCustom() {
        return this.minSdkVersion >= 26;
    }
}
