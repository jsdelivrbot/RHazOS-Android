package embedded.com.android.dx.io.instructions;

public abstract class BaseCodeCursor implements CodeCursor
{
    private final AddressMap baseAddressMap;
    private int cursor;
    
    public BaseCodeCursor() {
        this.baseAddressMap = new AddressMap();
        this.cursor = 0;
    }
    
    @Override
    public final int cursor() {
        return this.cursor;
    }
    
    @Override
    public final int baseAddressForCursor() {
        final int mapped = this.baseAddressMap.get(this.cursor);
        return (mapped >= 0) ? mapped : this.cursor;
    }
    
    @Override
    public final void setBaseAddress(final int targetAddress, final int baseAddress) {
        this.baseAddressMap.put(targetAddress, baseAddress);
    }
    
    protected final void advance(final int amount) {
        this.cursor += amount;
    }
}
