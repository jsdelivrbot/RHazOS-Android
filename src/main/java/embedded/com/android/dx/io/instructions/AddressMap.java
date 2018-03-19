package embedded.com.android.dx.io.instructions;

import java.util.*;

public final class AddressMap
{
    private final HashMap<Integer, Integer> map;
    
    public AddressMap() {
        this.map = new HashMap<Integer, Integer>();
    }
    
    public int get(final int keyAddress) {
        final Integer value = this.map.get(keyAddress);
        return (value == null) ? -1 : value;
    }
    
    public void put(final int keyAddress, final int valueAddress) {
        this.map.put(keyAddress, valueAddress);
    }
}
