package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import java.util.*;

public final class Statistics
{
    private final HashMap<String, Data> dataMap;
    
    public Statistics() {
        this.dataMap = new HashMap<String, Data>(50);
    }
    
    public void add(final Item item) {
        final String typeName = item.typeName();
        final Data data = this.dataMap.get(typeName);
        if (data == null) {
            this.dataMap.put(typeName, new Data(item, typeName));
        }
        else {
            data.add(item);
        }
    }
    
    public void addAll(final Section list) {
        final Collection<? extends Item> items = list.items();
        for (final Item item : items) {
            this.add(item);
        }
    }
    
    public final void writeAnnotation(final AnnotatedOutput out) {
        if (this.dataMap.size() == 0) {
            return;
        }
        out.annotate(0, "\nstatistics:\n");
        final TreeMap<String, Data> sortedData = new TreeMap<String, Data>();
        for (final Data data : this.dataMap.values()) {
            sortedData.put(data.name, data);
        }
        for (final Data data : sortedData.values()) {
            data.writeAnnotation(out);
        }
    }
    
    public String toHuman() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Statistics:\n");
        final TreeMap<String, Data> sortedData = new TreeMap<String, Data>();
        for (final Data data : this.dataMap.values()) {
            sortedData.put(data.name, data);
        }
        for (final Data data : sortedData.values()) {
            sb.append(data.toHuman());
        }
        return sb.toString();
    }
    
    private static class Data
    {
        private final String name;
        private int count;
        private int totalSize;
        private int largestSize;
        private int smallestSize;
        
        public Data(final Item item, final String name) {
            final int size = item.writeSize();
            this.name = name;
            this.count = 1;
            this.totalSize = size;
            this.largestSize = size;
            this.smallestSize = size;
        }
        
        public void add(final Item item) {
            final int size = item.writeSize();
            ++this.count;
            this.totalSize += size;
            if (size > this.largestSize) {
                this.largestSize = size;
            }
            if (size < this.smallestSize) {
                this.smallestSize = size;
            }
        }
        
        public void writeAnnotation(final AnnotatedOutput out) {
            out.annotate(this.toHuman());
        }
        
        public String toHuman() {
            final StringBuilder sb = new StringBuilder();
            sb.append("  " + this.name + ": " + this.count + " item" + ((this.count == 1) ? "" : "s") + "; " + this.totalSize + " bytes total\n");
            if (this.smallestSize == this.largestSize) {
                sb.append("    " + this.smallestSize + " bytes/item\n");
            }
            else {
                final int average = this.totalSize / this.count;
                sb.append("    " + this.smallestSize + ".." + this.largestSize + " bytes/item; average " + average + "\n");
            }
            return sb.toString();
        }
    }
}
