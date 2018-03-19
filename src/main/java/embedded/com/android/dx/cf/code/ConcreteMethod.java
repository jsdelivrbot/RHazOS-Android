package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.cf.attrib.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;

public final class ConcreteMethod implements Method
{
    private final Method method;
    private final CstString sourceFile;
    private final AttCode attCode;
    private final LineNumberList lineNumbers;
    private final LocalVariableList localVariables;
    
    public ConcreteMethod(final Method method, final ClassFile cf, final boolean keepLines, final boolean keepLocals) {
        this(method, cf.getSourceFile(), keepLines, keepLocals);
    }
    
    public ConcreteMethod(final Method method, final CstString sourceFile, final boolean keepLines, final boolean keepLocals) {
        this.method = method;
        this.sourceFile = sourceFile;
        final AttributeList attribs = method.getAttributes();
        this.attCode = (AttCode)attribs.findFirst("Code");
        final AttributeList codeAttribs = this.attCode.getAttributes();
        LineNumberList lineNumbers = LineNumberList.EMPTY;
        if (keepLines) {
            for (AttLineNumberTable lnt = (AttLineNumberTable)codeAttribs.findFirst("LineNumberTable"); lnt != null; lnt = (AttLineNumberTable)codeAttribs.findNext(lnt)) {
                lineNumbers = LineNumberList.concat(lineNumbers, lnt.getLineNumbers());
            }
        }
        this.lineNumbers = lineNumbers;
        LocalVariableList localVariables = LocalVariableList.EMPTY;
        if (keepLocals) {
            for (AttLocalVariableTable lvt = (AttLocalVariableTable)codeAttribs.findFirst("LocalVariableTable"); lvt != null; lvt = (AttLocalVariableTable)codeAttribs.findNext(lvt)) {
                localVariables = LocalVariableList.concat(localVariables, lvt.getLocalVariables());
            }
            LocalVariableList typeList = LocalVariableList.EMPTY;
            for (AttLocalVariableTypeTable lvtt = (AttLocalVariableTypeTable)codeAttribs.findFirst("LocalVariableTypeTable"); lvtt != null; lvtt = (AttLocalVariableTypeTable)codeAttribs.findNext(lvtt)) {
                typeList = LocalVariableList.concat(typeList, lvtt.getLocalVariables());
            }
            if (typeList.size() != 0) {
                localVariables = LocalVariableList.mergeDescriptorsAndSignatures(localVariables, typeList);
            }
        }
        this.localVariables = localVariables;
    }
    
    @Override
    public CstNat getNat() {
        return this.method.getNat();
    }
    
    @Override
    public CstString getName() {
        return this.method.getName();
    }
    
    @Override
    public CstString getDescriptor() {
        return this.method.getDescriptor();
    }
    
    @Override
    public int getAccessFlags() {
        return this.method.getAccessFlags();
    }
    
    @Override
    public AttributeList getAttributes() {
        return this.method.getAttributes();
    }
    
    @Override
    public CstType getDefiningClass() {
        return this.method.getDefiningClass();
    }
    
    @Override
    public Prototype getEffectiveDescriptor() {
        return this.method.getEffectiveDescriptor();
    }
    
    public int getMaxStack() {
        return this.attCode.getMaxStack();
    }
    
    public int getMaxLocals() {
        return this.attCode.getMaxLocals();
    }
    
    public BytecodeArray getCode() {
        return this.attCode.getCode();
    }
    
    public ByteCatchList getCatches() {
        return this.attCode.getCatches();
    }
    
    public LineNumberList getLineNumbers() {
        return this.lineNumbers;
    }
    
    public LocalVariableList getLocalVariables() {
        return this.localVariables;
    }
    
    public SourcePosition makeSourcePosistion(final int offset) {
        return new SourcePosition(this.sourceFile, offset, this.lineNumbers.pcToLine(offset));
    }
}
