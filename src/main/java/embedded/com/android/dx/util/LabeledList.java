package embedded.com.android.dx.util;

import java.util.*;

public class LabeledList extends FixedSizeList
{
    private final IntList labelToIndex;
    
    public LabeledList(final int size) {
        super(size);
        this.labelToIndex = new IntList(size);
    }
    
    public LabeledList(final LabeledList old) {
        super(old.size());
        this.labelToIndex = old.labelToIndex.mutableCopy();
        for (int sz = old.size(), i = 0; i < sz; ++i) {
            final Object one = old.get0(i);
            if (one != null) {
                this.set0(i, one);
            }
        }
    }
    
    public final int getMaxLabel() {
        final int sz = this.labelToIndex.size();
        int i;
        for (i = sz - 1; i >= 0 && this.labelToIndex.get(i) < 0; --i) {}
        final int newSize = i + 1;
        this.labelToIndex.shrink(newSize);
        return newSize;
    }
    
    private void removeLabel(final int oldLabel) {
        this.labelToIndex.set(oldLabel, -1);
    }
    
    private void addLabelIndex(final int label, final int index) {
        for (int origSz = this.labelToIndex.size(), i = 0; i <= label - origSz; ++i) {
            this.labelToIndex.add(-1);
        }
        this.labelToIndex.set(label, index);
    }
    
    public final int indexOfLabel(final int label) {
        if (label >= this.labelToIndex.size()) {
            return -1;
        }
        return this.labelToIndex.get(label);
    }
    
    public final int[] getLabelsInOrder() {
        final int sz = this.size();
        final int[] result = new int[sz];
        for (int i = 0; i < sz; ++i) {
            final LabeledItem li = (LabeledItem)this.get0(i);
            if (li == null) {
                throw new NullPointerException("null at index " + i);
            }
            result[i] = li.getLabel();
        }
        Arrays.sort(result);
        return result;
    }
    
    @Override
    public void shrinkToFit() {
        super.shrinkToFit();
        this.rebuildLabelToIndex();
    }
    
    private void rebuildLabelToIndex() {
        for (int szItems = this.size(), i = 0; i < szItems; ++i) {
            final LabeledItem li = (LabeledItem)this.get0(i);
            if (li != null) {
                this.labelToIndex.set(li.getLabel(), i);
            }
        }
    }
    
    protected void set(final int n, final LabeledItem item) {
        final LabeledItem old = (LabeledItem)this.getOrNull0(n);
        this.set0(n, item);
        if (old != null) {
            this.removeLabel(old.getLabel());
        }
        if (item != null) {
            this.addLabelIndex(item.getLabel(), n);
        }
    }
}
