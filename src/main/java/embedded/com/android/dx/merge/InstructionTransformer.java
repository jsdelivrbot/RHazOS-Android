package embedded.com.android.dx.merge;

import embedded.com.android.dx.io.*;
import embedded.com.android.dx.io.instructions.*;
import embedded.com.android.dex.*;

final class InstructionTransformer
{
    private final CodeReader reader;
    private DecodedInstruction[] mappedInstructions;
    private int mappedAt;
    private IndexMap indexMap;
    
    public InstructionTransformer() {
        (this.reader = new CodeReader()).setAllVisitors(new GenericVisitor());
        this.reader.setStringVisitor(new StringVisitor());
        this.reader.setTypeVisitor(new TypeVisitor());
        this.reader.setFieldVisitor(new FieldVisitor());
        this.reader.setMethodVisitor(new MethodVisitor());
        this.reader.setMethodAndProtoVisitor(new MethodAndProtoVisitor());
        this.reader.setCallSiteVisitor(new CallSiteVisitor());
    }
    
    public short[] transform(final IndexMap indexMap, final short[] encodedInstructions) throws DexException {
        final DecodedInstruction[] decodedInstructions = DecodedInstruction.decodeAll(encodedInstructions);
        final int size = decodedInstructions.length;
        this.indexMap = indexMap;
        this.mappedInstructions = new DecodedInstruction[size];
        this.mappedAt = 0;
        this.reader.visitAll(decodedInstructions);
        final ShortArrayCodeOutput out = new ShortArrayCodeOutput(size);
        for (final DecodedInstruction instruction : this.mappedInstructions) {
            if (instruction != null) {
                instruction.encode(out);
            }
        }
        this.indexMap = null;
        return out.getArray();
    }
    
    private static void jumboCheck(final boolean isJumbo, final int newIndex) {
        if (!isJumbo && newIndex > 65535) {
            throw new DexIndexOverflowException("Cannot merge new index " + newIndex + " into a non-jumbo instruction!");
        }
    }
    
    private class GenericVisitor implements CodeReader.Visitor
    {
        @Override
        public void visit(final DecodedInstruction[] all, final DecodedInstruction one) {
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt++] = one;
        }
    }
    
    private class StringVisitor implements CodeReader.Visitor
    {
        @Override
        public void visit(final DecodedInstruction[] all, final DecodedInstruction one) {
            final int stringId = one.getIndex();
            final int mappedId = InstructionTransformer.this.indexMap.adjustString(stringId);
            final boolean isJumbo = one.getOpcode() == 27;
            jumboCheck(isJumbo, mappedId);
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt++] = one.withIndex(mappedId);
        }
    }
    
    private class FieldVisitor implements CodeReader.Visitor
    {
        @Override
        public void visit(final DecodedInstruction[] all, final DecodedInstruction one) {
            final int fieldId = one.getIndex();
            final int mappedId = InstructionTransformer.this.indexMap.adjustField(fieldId);
            final boolean isJumbo = one.getOpcode() == 27;
            jumboCheck(isJumbo, mappedId);
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt++] = one.withIndex(mappedId);
        }
    }
    
    private class TypeVisitor implements CodeReader.Visitor
    {
        @Override
        public void visit(final DecodedInstruction[] all, final DecodedInstruction one) {
            final int typeId = one.getIndex();
            final int mappedId = InstructionTransformer.this.indexMap.adjustType(typeId);
            final boolean isJumbo = one.getOpcode() == 27;
            jumboCheck(isJumbo, mappedId);
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt++] = one.withIndex(mappedId);
        }
    }
    
    private class MethodVisitor implements CodeReader.Visitor
    {
        @Override
        public void visit(final DecodedInstruction[] all, final DecodedInstruction one) {
            final int methodId = one.getIndex();
            final int mappedId = InstructionTransformer.this.indexMap.adjustMethod(methodId);
            final boolean isJumbo = one.getOpcode() == 27;
            jumboCheck(isJumbo, mappedId);
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt++] = one.withIndex(mappedId);
        }
    }
    
    private class MethodAndProtoVisitor implements CodeReader.Visitor
    {
        @Override
        public void visit(final DecodedInstruction[] all, final DecodedInstruction one) {
            final int methodId = one.getIndex();
            final int protoId = one.getProtoIndex();
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt++] = one.withProtoIndex(InstructionTransformer.this.indexMap.adjustMethod(methodId), InstructionTransformer.this.indexMap.adjustProto(protoId));
        }
    }
    
    private class CallSiteVisitor implements CodeReader.Visitor
    {
        @Override
        public void visit(final DecodedInstruction[] all, final DecodedInstruction one) {
            final int callSiteId = one.getIndex();
            final int mappedCallSiteId = InstructionTransformer.this.indexMap.adjustCallSite(callSiteId);
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt++] = one.withIndex(mappedCallSiteId);
        }
    }
}
