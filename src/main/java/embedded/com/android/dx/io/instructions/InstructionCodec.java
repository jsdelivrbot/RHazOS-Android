package embedded.com.android.dx.io.instructions;

import java.io.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dex.*;
import embedded.com.android.dx.io.*;
import java.util.*;

public enum InstructionCodec
{
    FORMAT_00X {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            return new ZeroRegisterDecodedInstruction(this, opcodeUnit, 0, null, 0, 0L);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(insn.getOpcodeUnit());
        }
    }, 
    FORMAT_10X {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int literal = byte1(opcodeUnit);
            return new ZeroRegisterDecodedInstruction(this, opcode, 0, null, 0, literal);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(insn.getOpcodeUnit());
        }
    }, 
    FORMAT_12X {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = nibble2(opcodeUnit);
            final int b = nibble3(opcodeUnit);
            return new TwoRegisterDecodedInstruction(this, opcode, 0, null, 0, 0L, a, b);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcodeUnit(), makeByte(insn.getA(), insn.getB())));
        }
    }, 
    FORMAT_11N {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = nibble2(opcodeUnit);
            final int literal = nibble3(opcodeUnit) << 28 >> 28;
            return new OneRegisterDecodedInstruction(this, opcode, 0, null, 0, literal, a);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcodeUnit(), makeByte(insn.getA(), insn.getLiteralNibble())));
        }
    }, 
    FORMAT_11X {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            return new OneRegisterDecodedInstruction(this, opcode, 0, null, 0, 0L, a);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), insn.getA()));
        }
    }, 
    FORMAT_10T {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int baseAddress = in.cursor() - 1;
            final int opcode = byte0(opcodeUnit);
            final int target = (byte)byte1(opcodeUnit);
            return new ZeroRegisterDecodedInstruction(this, opcode, 0, null, baseAddress + target, 0L);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final int relativeTarget = insn.getTargetByte(out.cursor());
            out.write(codeUnit(insn.getOpcode(), relativeTarget));
        }
    }, 
    FORMAT_20T {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int baseAddress = in.cursor() - 1;
            final int opcode = byte0(opcodeUnit);
            final int literal = byte1(opcodeUnit);
            final int target = (short)in.read();
            return new ZeroRegisterDecodedInstruction(this, opcode, 0, null, baseAddress + target, literal);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final short relativeTarget = insn.getTargetUnit(out.cursor());
            out.write(insn.getOpcodeUnit(), relativeTarget);
        }
    }, 
    FORMAT_20BC {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int literal = byte1(opcodeUnit);
            final int index = in.read();
            return new ZeroRegisterDecodedInstruction(this, opcode, index, IndexType.VARIES, 0, literal);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), insn.getLiteralByte()), insn.getIndexUnit());
        }
    }, 
    FORMAT_22X {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            final int b = in.read();
            return new TwoRegisterDecodedInstruction(this, opcode, 0, null, 0, 0L, a, b);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), insn.getA()), insn.getBUnit());
        }
    }, 
    FORMAT_21T {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int baseAddress = in.cursor() - 1;
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            final int target = (short)in.read();
            return new OneRegisterDecodedInstruction(this, opcode, 0, null, baseAddress + target, 0L, a);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final short relativeTarget = insn.getTargetUnit(out.cursor());
            out.write(codeUnit(insn.getOpcode(), insn.getA()), relativeTarget);
        }
    }, 
    FORMAT_21S {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            final int literal = (short)in.read();
            return new OneRegisterDecodedInstruction(this, opcode, 0, null, 0, literal, a);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), insn.getA()), insn.getLiteralUnit());
        }
    }, 
    FORMAT_21H {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            long literal = (short)in.read();
            literal <<= ((opcode == 21) ? 16 : 48);
            return new OneRegisterDecodedInstruction(this, opcode, 0, null, 0, literal, a);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final int opcode = insn.getOpcode();
            final int shift = (opcode == 21) ? 16 : 48;
            final short literal = (short)(insn.getLiteral() >> shift);
            out.write(codeUnit(opcode, insn.getA()), literal);
        }
    }, 
    FORMAT_21C {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            final int index = in.read();
            final IndexType indexType = OpcodeInfo.getIndexType(opcode);
            return new OneRegisterDecodedInstruction(this, opcode, index, indexType, 0, 0L, a);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), insn.getA()), insn.getIndexUnit());
        }
    }, 
    FORMAT_23X {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            final int bc = in.read();
            final int b = byte0(bc);
            final int c = byte1(bc);
            return new ThreeRegisterDecodedInstruction(this, opcode, 0, null, 0, 0L, a, b, c);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), insn.getA()), codeUnit(insn.getB(), insn.getC()));
        }
    }, 
    FORMAT_22B {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            final int bc = in.read();
            final int b = byte0(bc);
            final int literal = (byte)byte1(bc);
            return new TwoRegisterDecodedInstruction(this, opcode, 0, null, 0, literal, a, b);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), insn.getA()), codeUnit(insn.getB(), insn.getLiteralByte()));
        }
    }, 
    FORMAT_22T {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int baseAddress = in.cursor() - 1;
            final int opcode = byte0(opcodeUnit);
            final int a = nibble2(opcodeUnit);
            final int b = nibble3(opcodeUnit);
            final int target = (short)in.read();
            return new TwoRegisterDecodedInstruction(this, opcode, 0, null, baseAddress + target, 0L, a, b);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final short relativeTarget = insn.getTargetUnit(out.cursor());
            out.write(codeUnit(insn.getOpcode(), makeByte(insn.getA(), insn.getB())), relativeTarget);
        }
    }, 
    FORMAT_22S {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = nibble2(opcodeUnit);
            final int b = nibble3(opcodeUnit);
            final int literal = (short)in.read();
            return new TwoRegisterDecodedInstruction(this, opcode, 0, null, 0, literal, a, b);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), makeByte(insn.getA(), insn.getB())), insn.getLiteralUnit());
        }
    }, 
    FORMAT_22C {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = nibble2(opcodeUnit);
            final int b = nibble3(opcodeUnit);
            final int index = in.read();
            final IndexType indexType = OpcodeInfo.getIndexType(opcode);
            return new TwoRegisterDecodedInstruction(this, opcode, index, indexType, 0, 0L, a, b);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), makeByte(insn.getA(), insn.getB())), insn.getIndexUnit());
        }
    }, 
    FORMAT_22CS {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = nibble2(opcodeUnit);
            final int b = nibble3(opcodeUnit);
            final int index = in.read();
            return new TwoRegisterDecodedInstruction(this, opcode, index, IndexType.FIELD_OFFSET, 0, 0L, a, b);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), makeByte(insn.getA(), insn.getB())), insn.getIndexUnit());
        }
    }, 
    FORMAT_30T {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int baseAddress = in.cursor() - 1;
            final int opcode = byte0(opcodeUnit);
            final int literal = byte1(opcodeUnit);
            final int target = in.readInt();
            return new ZeroRegisterDecodedInstruction(this, opcode, 0, null, baseAddress + target, literal);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final int relativeTarget = insn.getTarget(out.cursor());
            out.write(insn.getOpcodeUnit(), unit0(relativeTarget), unit1(relativeTarget));
        }
    }, 
    FORMAT_32X {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int literal = byte1(opcodeUnit);
            final int a = in.read();
            final int b = in.read();
            return new TwoRegisterDecodedInstruction(this, opcode, 0, null, 0, literal, a, b);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(insn.getOpcodeUnit(), insn.getAUnit(), insn.getBUnit());
        }
    }, 
    FORMAT_31I {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            final int literal = in.readInt();
            return new OneRegisterDecodedInstruction(this, opcode, 0, null, 0, literal, a);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final int literal = insn.getLiteralInt();
            out.write(codeUnit(insn.getOpcode(), insn.getA()), unit0(literal), unit1(literal));
        }
    }, 
    FORMAT_31T {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int baseAddress = in.cursor() - 1;
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            final int target = baseAddress + in.readInt();
            switch (opcode) {
                case 43:
                case 44: {
                    in.setBaseAddress(target, baseAddress);
                    break;
                }
            }
            return new OneRegisterDecodedInstruction(this, opcode, 0, null, target, 0L, a);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final int relativeTarget = insn.getTarget(out.cursor());
            out.write(codeUnit(insn.getOpcode(), insn.getA()), unit0(relativeTarget), unit1(relativeTarget));
        }
    }, 
    FORMAT_31C {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            final int index = in.readInt();
            final IndexType indexType = OpcodeInfo.getIndexType(opcode);
            return new OneRegisterDecodedInstruction(this, opcode, index, indexType, 0, 0L, a);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final int index = insn.getIndex();
            out.write(codeUnit(insn.getOpcode(), insn.getA()), unit0(index), unit1(index));
        }
    }, 
    FORMAT_35C {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            return decodeRegisterList(this, opcodeUnit, in);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            encodeRegisterList(insn, out);
        }
    }, 
    FORMAT_35MS {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            return decodeRegisterList(this, opcodeUnit, in);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            encodeRegisterList(insn, out);
        }
    }, 
    FORMAT_35MI {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            return decodeRegisterList(this, opcodeUnit, in);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            encodeRegisterList(insn, out);
        }
    }, 
    FORMAT_3RC {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            return decodeRegisterRange(this, opcodeUnit, in);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            encodeRegisterRange(insn, out);
        }
    }, 
    FORMAT_3RMS {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            return decodeRegisterRange(this, opcodeUnit, in);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            encodeRegisterRange(insn, out);
        }
    }, 
    FORMAT_3RMI {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            return decodeRegisterRange(this, opcodeUnit, in);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            encodeRegisterRange(insn, out);
        }
    }, 
    FORMAT_51L {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            final int a = byte1(opcodeUnit);
            final long literal = in.readLong();
            return new OneRegisterDecodedInstruction(this, opcode, 0, null, 0, literal, a);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final long literal = insn.getLiteral();
            out.write(codeUnit(insn.getOpcode(), insn.getA()), unit0(literal), unit1(literal), unit2(literal), unit3(literal));
        }
    }, 
    FORMAT_45CC {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            if (opcode != 250) {
                throw new UnsupportedOperationException(String.valueOf(opcode));
            }
            final int g = nibble2(opcodeUnit);
            final int registerCount = nibble3(opcodeUnit);
            final int methodIndex = in.read();
            final int cdef = in.read();
            final int c = nibble0(cdef);
            final int d = nibble1(cdef);
            final int e = nibble2(cdef);
            final int f = nibble3(cdef);
            final int protoIndex = in.read();
            final IndexType indexType = OpcodeInfo.getIndexType(opcode);
            if (registerCount < 1 || registerCount > 5) {
                throw new DexException("bogus registerCount: " + Hex.uNibble(registerCount));
            }
            int[] registers = { c, d, e, f, g };
            registers = Arrays.copyOfRange(registers, 0, registerCount);
            return new InvokePolymorphicDecodedInstruction(this, opcode, methodIndex, indexType, protoIndex, registers);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final InvokePolymorphicDecodedInstruction polyInsn = (InvokePolymorphicDecodedInstruction)insn;
            out.write(codeUnit(polyInsn.getOpcode(), makeByte(polyInsn.getG(), polyInsn.getRegisterCount())), polyInsn.getIndexUnit(), codeUnit(polyInsn.getC(), polyInsn.getD(), polyInsn.getE(), polyInsn.getF()), polyInsn.getProtoIndex());
        }
    }, 
    FORMAT_4RCC {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int opcode = byte0(opcodeUnit);
            if (opcode != 251) {
                throw new UnsupportedOperationException(String.valueOf(opcode));
            }
            final int registerCount = byte1(opcodeUnit);
            final int methodIndex = in.read();
            final int c = in.read();
            final int protoIndex = in.read();
            final IndexType indexType = OpcodeInfo.getIndexType(opcode);
            return new InvokePolymorphicRangeDecodedInstruction(this, opcode, methodIndex, indexType, c, registerCount, protoIndex);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), insn.getRegisterCount()), insn.getIndexUnit(), insn.getCUnit(), insn.getProtoIndex());
        }
    }, 
    FORMAT_PACKED_SWITCH_PAYLOAD {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int baseAddress = in.baseAddressForCursor() - 1;
            final int size = in.read();
            final int firstKey = in.readInt();
            final int[] targets = new int[size];
            for (int i = 0; i < size; ++i) {
                targets[i] = baseAddress + in.readInt();
            }
            return new PackedSwitchPayloadDecodedInstruction(this, opcodeUnit, firstKey, targets);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final PackedSwitchPayloadDecodedInstruction payload = (PackedSwitchPayloadDecodedInstruction)insn;
            final int[] targets = payload.getTargets();
            final int baseAddress = out.baseAddressForCursor();
            out.write(payload.getOpcodeUnit());
            out.write(asUnsignedUnit(targets.length));
            out.writeInt(payload.getFirstKey());
            for (final int target : targets) {
                out.writeInt(target - baseAddress);
            }
        }
    }, 
    FORMAT_SPARSE_SWITCH_PAYLOAD {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int baseAddress = in.baseAddressForCursor() - 1;
            final int size = in.read();
            final int[] keys = new int[size];
            final int[] targets = new int[size];
            for (int i = 0; i < size; ++i) {
                keys[i] = in.readInt();
            }
            for (int i = 0; i < size; ++i) {
                targets[i] = baseAddress + in.readInt();
            }
            return new SparseSwitchPayloadDecodedInstruction(this, opcodeUnit, keys, targets);
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final SparseSwitchPayloadDecodedInstruction payload = (SparseSwitchPayloadDecodedInstruction)insn;
            final int[] keys = payload.getKeys();
            final int[] targets = payload.getTargets();
            final int baseAddress = out.baseAddressForCursor();
            out.write(payload.getOpcodeUnit());
            out.write(asUnsignedUnit(targets.length));
            for (final int key : keys) {
                out.writeInt(key);
            }
            for (final int target : targets) {
                out.writeInt(target - baseAddress);
            }
        }
    }, 
    FORMAT_FILL_ARRAY_DATA_PAYLOAD {
        @Override
        public DecodedInstruction decode(final int opcodeUnit, final CodeInput in) throws EOFException {
            final int elementWidth = in.read();
            final int size = in.readInt();
            switch (elementWidth) {
                case 1: {
                    final byte[] array = new byte[size];
                    boolean even = true;
                    int i = 0;
                    int value = 0;
                    while (i < size) {
                        if (even) {
                            value = in.read();
                        }
                        array[i] = (byte)(value & 0xFF);
                        value >>= 8;
                        ++i;
                        even = !even;
                    }
                    return new FillArrayDataPayloadDecodedInstruction(this, opcodeUnit, array);
                }
                case 2: {
                    final short[] array2 = new short[size];
                    for (int j = 0; j < size; ++j) {
                        array2[j] = (short)in.read();
                    }
                    return new FillArrayDataPayloadDecodedInstruction(this, opcodeUnit, array2);
                }
                case 4: {
                    final int[] array3 = new int[size];
                    for (int j = 0; j < size; ++j) {
                        array3[j] = in.readInt();
                    }
                    return new FillArrayDataPayloadDecodedInstruction(this, opcodeUnit, array3);
                }
                case 8: {
                    final long[] array4 = new long[size];
                    for (int j = 0; j < size; ++j) {
                        array4[j] = in.readLong();
                    }
                    return new FillArrayDataPayloadDecodedInstruction(this, opcodeUnit, array4);
                }
                default: {
                    throw new DexException("bogus element_width: " + Hex.u2(elementWidth));
                }
            }
        }
        
        @Override
        public void encode(final DecodedInstruction insn, final CodeOutput out) {
            final FillArrayDataPayloadDecodedInstruction payload = (FillArrayDataPayloadDecodedInstruction)insn;
            final short elementWidth = payload.getElementWidthUnit();
            final Object data = payload.getData();
            out.write(payload.getOpcodeUnit());
            out.write(elementWidth);
            out.writeInt(payload.getSize());
            switch (elementWidth) {
                case 1: {
                    out.write((byte[])data);
                    break;
                }
                case 2: {
                    out.write((short[])data);
                    break;
                }
                case 4: {
                    out.write((int[])data);
                    break;
                }
                case 8: {
                    out.write((long[])data);
                    break;
                }
                default: {
                    throw new DexException("bogus element_width: " + Hex.u2(elementWidth));
                }
            }
        }
    };
    
    public abstract DecodedInstruction decode(final int p0, final CodeInput p1) throws EOFException;
    
    public abstract void encode(final DecodedInstruction p0, final CodeOutput p1);
    
    private static DecodedInstruction decodeRegisterList(final InstructionCodec format, final int opcodeUnit, final CodeInput in) throws EOFException {
        final int opcode = byte0(opcodeUnit);
        final int e = nibble2(opcodeUnit);
        final int registerCount = nibble3(opcodeUnit);
        final int index = in.read();
        final int abcd = in.read();
        final int a = nibble0(abcd);
        final int b = nibble1(abcd);
        final int c = nibble2(abcd);
        final int d = nibble3(abcd);
        final IndexType indexType = OpcodeInfo.getIndexType(opcode);
        switch (registerCount) {
            case 0: {
                return new ZeroRegisterDecodedInstruction(format, opcode, index, indexType, 0, 0L);
            }
            case 1: {
                return new OneRegisterDecodedInstruction(format, opcode, index, indexType, 0, 0L, a);
            }
            case 2: {
                return new TwoRegisterDecodedInstruction(format, opcode, index, indexType, 0, 0L, a, b);
            }
            case 3: {
                return new ThreeRegisterDecodedInstruction(format, opcode, index, indexType, 0, 0L, a, b, c);
            }
            case 4: {
                return new FourRegisterDecodedInstruction(format, opcode, index, indexType, 0, 0L, a, b, c, d);
            }
            case 5: {
                return new FiveRegisterDecodedInstruction(format, opcode, index, indexType, 0, 0L, a, b, c, d, e);
            }
            default: {
                throw new DexException("bogus registerCount: " + Hex.uNibble(registerCount));
            }
        }
    }
    
    private static void encodeRegisterList(final DecodedInstruction insn, final CodeOutput out) {
        out.write(codeUnit(insn.getOpcode(), makeByte(insn.getE(), insn.getRegisterCount())), insn.getIndexUnit(), codeUnit(insn.getA(), insn.getB(), insn.getC(), insn.getD()));
    }
    
    private static DecodedInstruction decodeRegisterRange(final InstructionCodec format, final int opcodeUnit, final CodeInput in) throws EOFException {
        final int opcode = byte0(opcodeUnit);
        final int registerCount = byte1(opcodeUnit);
        final int index = in.read();
        final int a = in.read();
        final IndexType indexType = OpcodeInfo.getIndexType(opcode);
        return new RegisterRangeDecodedInstruction(format, opcode, index, indexType, 0, 0L, a, registerCount);
    }
    
    private static void encodeRegisterRange(final DecodedInstruction insn, final CodeOutput out) {
        out.write(codeUnit(insn.getOpcode(), insn.getRegisterCount()), insn.getIndexUnit(), insn.getAUnit());
    }
    
    private static short codeUnit(final int lowByte, final int highByte) {
        if ((lowByte & 0xFFFFFF00) != 0x0) {
            throw new IllegalArgumentException("bogus lowByte");
        }
        if ((highByte & 0xFFFFFF00) != 0x0) {
            throw new IllegalArgumentException("bogus highByte");
        }
        return (short)(lowByte | highByte << 8);
    }
    
    private static short codeUnit(final int nibble0, final int nibble1, final int nibble2, final int nibble3) {
        if ((nibble0 & 0xFFFFFFF0) != 0x0) {
            throw new IllegalArgumentException("bogus nibble0");
        }
        if ((nibble1 & 0xFFFFFFF0) != 0x0) {
            throw new IllegalArgumentException("bogus nibble1");
        }
        if ((nibble2 & 0xFFFFFFF0) != 0x0) {
            throw new IllegalArgumentException("bogus nibble2");
        }
        if ((nibble3 & 0xFFFFFFF0) != 0x0) {
            throw new IllegalArgumentException("bogus nibble3");
        }
        return (short)(nibble0 | nibble1 << 4 | nibble2 << 8 | nibble3 << 12);
    }
    
    private static int makeByte(final int lowNibble, final int highNibble) {
        if ((lowNibble & 0xFFFFFFF0) != 0x0) {
            throw new IllegalArgumentException("bogus lowNibble");
        }
        if ((highNibble & 0xFFFFFFF0) != 0x0) {
            throw new IllegalArgumentException("bogus highNibble");
        }
        return lowNibble | highNibble << 4;
    }
    
    private static short asUnsignedUnit(final int value) {
        if ((value & 0xFFFF0000) != 0x0) {
            throw new IllegalArgumentException("bogus unsigned code unit");
        }
        return (short)value;
    }
    
    private static short unit0(final int value) {
        return (short)value;
    }
    
    private static short unit1(final int value) {
        return (short)(value >> 16);
    }
    
    private static short unit0(final long value) {
        return (short)value;
    }
    
    private static short unit1(final long value) {
        return (short)(value >> 16);
    }
    
    private static short unit2(final long value) {
        return (short)(value >> 32);
    }
    
    private static short unit3(final long value) {
        return (short)(value >> 48);
    }
    
    private static int byte0(final int value) {
        return value & 0xFF;
    }
    
    private static int byte1(final int value) {
        return value >> 8 & 0xFF;
    }
    
    private static int byte2(final int value) {
        return value >> 16 & 0xFF;
    }
    
    private static int byte3(final int value) {
        return value >>> 24;
    }
    
    private static int nibble0(final int value) {
        return value & 0xF;
    }
    
    private static int nibble1(final int value) {
        return value >> 4 & 0xF;
    }
    
    private static int nibble2(final int value) {
        return value >> 8 & 0xF;
    }
    
    private static int nibble3(final int value) {
        return value >> 12 & 0xF;
    }
}
