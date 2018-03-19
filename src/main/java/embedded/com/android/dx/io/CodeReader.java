package embedded.com.android.dx.io;

import embedded.com.android.dx.io.instructions.*;
import embedded.com.android.dex.*;

public final class CodeReader
{
    private Visitor fallbackVisitor;
    private Visitor stringVisitor;
    private Visitor typeVisitor;
    private Visitor fieldVisitor;
    private Visitor methodVisitor;
    private Visitor methodAndProtoVisitor;
    private Visitor callSiteVisitor;
    
    public CodeReader() {
        this.fallbackVisitor = null;
        this.stringVisitor = null;
        this.typeVisitor = null;
        this.fieldVisitor = null;
        this.methodVisitor = null;
        this.methodAndProtoVisitor = null;
        this.callSiteVisitor = null;
    }
    
    public void setAllVisitors(final Visitor visitor) {
        this.fallbackVisitor = visitor;
        this.stringVisitor = visitor;
        this.typeVisitor = visitor;
        this.fieldVisitor = visitor;
        this.methodVisitor = visitor;
        this.methodAndProtoVisitor = visitor;
        this.callSiteVisitor = visitor;
    }
    
    public void setFallbackVisitor(final Visitor visitor) {
        this.fallbackVisitor = visitor;
    }
    
    public void setStringVisitor(final Visitor visitor) {
        this.stringVisitor = visitor;
    }
    
    public void setTypeVisitor(final Visitor visitor) {
        this.typeVisitor = visitor;
    }
    
    public void setFieldVisitor(final Visitor visitor) {
        this.fieldVisitor = visitor;
    }
    
    public void setMethodVisitor(final Visitor visitor) {
        this.methodVisitor = visitor;
    }
    
    public void setMethodAndProtoVisitor(final Visitor visitor) {
        this.methodAndProtoVisitor = visitor;
    }
    
    public void setCallSiteVisitor(final Visitor visitor) {
        this.callSiteVisitor = visitor;
    }
    
    public void visitAll(final DecodedInstruction[] decodedInstructions) throws DexException {
        for (final DecodedInstruction one : decodedInstructions) {
            if (one != null) {
                this.callVisit(decodedInstructions, one);
            }
        }
    }
    
    public void visitAll(final short[] encodedInstructions) throws DexException {
        final DecodedInstruction[] decodedInstructions = DecodedInstruction.decodeAll(encodedInstructions);
        this.visitAll(decodedInstructions);
    }
    
    private void callVisit(final DecodedInstruction[] all, final DecodedInstruction one) {
        Visitor visitor = null;
        switch (OpcodeInfo.getIndexType(one.getOpcode())) {
            case STRING_REF: {
                visitor = this.stringVisitor;
                break;
            }
            case TYPE_REF: {
                visitor = this.typeVisitor;
                break;
            }
            case FIELD_REF: {
                visitor = this.fieldVisitor;
                break;
            }
            case METHOD_REF: {
                visitor = this.methodVisitor;
                break;
            }
            case METHOD_AND_PROTO_REF: {
                visitor = this.methodAndProtoVisitor;
                break;
            }
            case CALL_SITE_REF: {
                visitor = this.callSiteVisitor;
                break;
            }
        }
        if (visitor == null) {
            visitor = this.fallbackVisitor;
        }
        if (visitor != null) {
            visitor.visit(all, one);
        }
    }
    
    public interface Visitor
    {
        void visit(final DecodedInstruction[] p0, final DecodedInstruction p1);
    }
}
