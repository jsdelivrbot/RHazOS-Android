package embedded.com.android.dex;

public final class ClassData
{
    private final Field[] staticFields;
    private final Field[] instanceFields;
    private final Method[] directMethods;
    private final Method[] virtualMethods;
    
    public ClassData(final Field[] staticFields, final Field[] instanceFields, final Method[] directMethods, final Method[] virtualMethods) {
        this.staticFields = staticFields;
        this.instanceFields = instanceFields;
        this.directMethods = directMethods;
        this.virtualMethods = virtualMethods;
    }
    
    public Field[] getStaticFields() {
        return this.staticFields;
    }
    
    public Field[] getInstanceFields() {
        return this.instanceFields;
    }
    
    public Method[] getDirectMethods() {
        return this.directMethods;
    }
    
    public Method[] getVirtualMethods() {
        return this.virtualMethods;
    }
    
    public Field[] allFields() {
        final Field[] result = new Field[this.staticFields.length + this.instanceFields.length];
        System.arraycopy(this.staticFields, 0, result, 0, this.staticFields.length);
        System.arraycopy(this.instanceFields, 0, result, this.staticFields.length, this.instanceFields.length);
        return result;
    }
    
    public Method[] allMethods() {
        final Method[] result = new Method[this.directMethods.length + this.virtualMethods.length];
        System.arraycopy(this.directMethods, 0, result, 0, this.directMethods.length);
        System.arraycopy(this.virtualMethods, 0, result, this.directMethods.length, this.virtualMethods.length);
        return result;
    }
    
    public static class Field
    {
        private final int fieldIndex;
        private final int accessFlags;
        
        public Field(final int fieldIndex, final int accessFlags) {
            this.fieldIndex = fieldIndex;
            this.accessFlags = accessFlags;
        }
        
        public int getFieldIndex() {
            return this.fieldIndex;
        }
        
        public int getAccessFlags() {
            return this.accessFlags;
        }
    }
    
    public static class Method
    {
        private final int methodIndex;
        private final int accessFlags;
        private final int codeOffset;
        
        public Method(final int methodIndex, final int accessFlags, final int codeOffset) {
            this.methodIndex = methodIndex;
            this.accessFlags = accessFlags;
            this.codeOffset = codeOffset;
        }
        
        public int getMethodIndex() {
            return this.methodIndex;
        }
        
        public int getAccessFlags() {
            return this.accessFlags;
        }
        
        public int getCodeOffset() {
            return this.codeOffset;
        }
    }
}
