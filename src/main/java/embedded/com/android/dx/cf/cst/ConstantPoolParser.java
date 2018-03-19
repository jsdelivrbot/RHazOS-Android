package embedded.com.android.dx.cf.cst;

import embedded.com.android.dx.util.*;
import java.util.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;

public final class ConstantPoolParser
{
    private final ByteArray bytes;
    private final StdConstantPool pool;
    private final int[] offsets;
    private int endOffset;
    private ParseObserver observer;
    
    public ConstantPoolParser(final ByteArray bytes) {
        final int size = bytes.getUnsignedShort(8);
        this.bytes = bytes;
        this.pool = new StdConstantPool(size);
        this.offsets = new int[size];
        this.endOffset = -1;
    }
    
    public void setObserver(final ParseObserver observer) {
        this.observer = observer;
    }
    
    public int getEndOffset() {
        this.parseIfNecessary();
        return this.endOffset;
    }
    
    public StdConstantPool getPool() {
        this.parseIfNecessary();
        return this.pool;
    }
    
    private void parseIfNecessary() {
        if (this.endOffset < 0) {
            this.parse();
        }
    }
    
    private void parse() {
        this.determineOffsets();
        if (this.observer != null) {
            this.observer.parsed(this.bytes, 8, 2, "constant_pool_count: " + Hex.u2(this.offsets.length));
            this.observer.parsed(this.bytes, 10, 0, "\nconstant_pool:");
            this.observer.changeIndent(1);
        }
        final BitSet wasUtf8 = new BitSet(this.offsets.length);
        for (int i = 1; i < this.offsets.length; ++i) {
            final int offset = this.offsets[i];
            if (offset != 0 && this.pool.getOrNull(i) == null) {
                this.parse0(i, wasUtf8);
            }
        }
        if (this.observer != null) {
            for (int i = 1; i < this.offsets.length; ++i) {
                final Constant cst = this.pool.getOrNull(i);
                if (cst != null) {
                    final int offset2 = this.offsets[i];
                    int nextOffset = this.endOffset;
                    for (int j = i + 1; j < this.offsets.length; ++j) {
                        final int off = this.offsets[j];
                        if (off != 0) {
                            nextOffset = off;
                            break;
                        }
                    }
                    final String human = wasUtf8.get(i) ? (Hex.u2(i) + ": utf8{\"" + cst.toHuman() + "\"}") : (Hex.u2(i) + ": " + cst.toString());
                    this.observer.parsed(this.bytes, offset2, nextOffset - offset2, human);
                }
            }
            this.observer.changeIndent(-1);
            this.observer.parsed(this.bytes, this.endOffset, 0, "end constant_pool");
        }
    }
    
    private void determineOffsets() {
        int at = 10;
        int lastCategory = 0;
        for (int i = 1; i < this.offsets.length; i += lastCategory) {
            this.offsets[i] = at;
            final int tag = this.bytes.getUnsignedByte(at);
            try {
                switch (tag) {
                    case 3:
                    case 4:
                    case 9:
                    case 10:
                    case 11:
                    case 12: {
                        lastCategory = 1;
                        at += 5;
                        break;
                    }
                    case 5:
                    case 6: {
                        lastCategory = 2;
                        at += 9;
                        break;
                    }
                    case 7:
                    case 8: {
                        lastCategory = 1;
                        at += 3;
                        break;
                    }
                    case 1: {
                        lastCategory = 1;
                        at += this.bytes.getUnsignedShort(at + 1) + 3;
                        break;
                    }
                    case 15: {
                        lastCategory = 1;
                        at += 4;
                        break;
                    }
                    case 16: {
                        lastCategory = 1;
                        at += 3;
                        break;
                    }
                    case 18: {
                        lastCategory = 1;
                        at += 5;
                        break;
                    }
                    default: {
                        throw new ParseException("unknown tag byte: " + Hex.u1(tag));
                    }
                }
            }
            catch (ParseException ex) {
                ex.addContext("...while preparsing cst " + Hex.u2(i) + " at offset " + Hex.u4(at));
                throw ex;
            }
        }
        this.endOffset = at;
    }
    
    private Constant parse0(final int idx, final BitSet wasUtf8) {
        Constant cst = this.pool.getOrNull(idx);
        if (cst != null) {
            return cst;
        }
        final int at = this.offsets[idx];
        try {
            final int tag = this.bytes.getUnsignedByte(at);
            switch (tag) {
                case 1: {
                    cst = this.parseUtf8(at);
                    wasUtf8.set(idx);
                    break;
                }
                case 3: {
                    final int value = this.bytes.getInt(at + 1);
                    cst = CstInteger.make(value);
                    break;
                }
                case 4: {
                    final int bits = this.bytes.getInt(at + 1);
                    cst = CstFloat.make(bits);
                    break;
                }
                case 5: {
                    final long value2 = this.bytes.getLong(at + 1);
                    cst = CstLong.make(value2);
                    break;
                }
                case 6: {
                    final long bits2 = this.bytes.getLong(at + 1);
                    cst = CstDouble.make(bits2);
                    break;
                }
                case 7: {
                    final int nameIndex = this.bytes.getUnsignedShort(at + 1);
                    final CstString name = (CstString)this.parse0(nameIndex, wasUtf8);
                    cst = new CstType(Type.internClassName(name.getString()));
                    break;
                }
                case 8: {
                    final int stringIndex = this.bytes.getUnsignedShort(at + 1);
                    cst = this.parse0(stringIndex, wasUtf8);
                    break;
                }
                case 9: {
                    final int classIndex = this.bytes.getUnsignedShort(at + 1);
                    final CstType type = (CstType)this.parse0(classIndex, wasUtf8);
                    final int natIndex = this.bytes.getUnsignedShort(at + 3);
                    final CstNat nat = (CstNat)this.parse0(natIndex, wasUtf8);
                    cst = new CstFieldRef(type, nat);
                    break;
                }
                case 10: {
                    final int classIndex = this.bytes.getUnsignedShort(at + 1);
                    final CstType type = (CstType)this.parse0(classIndex, wasUtf8);
                    final int natIndex = this.bytes.getUnsignedShort(at + 3);
                    final CstNat nat = (CstNat)this.parse0(natIndex, wasUtf8);
                    cst = new CstMethodRef(type, nat);
                    break;
                }
                case 11: {
                    final int classIndex = this.bytes.getUnsignedShort(at + 1);
                    final CstType type = (CstType)this.parse0(classIndex, wasUtf8);
                    final int natIndex = this.bytes.getUnsignedShort(at + 3);
                    final CstNat nat = (CstNat)this.parse0(natIndex, wasUtf8);
                    cst = new CstInterfaceMethodRef(type, nat);
                    break;
                }
                case 12: {
                    final int nameIndex = this.bytes.getUnsignedShort(at + 1);
                    final CstString name = (CstString)this.parse0(nameIndex, wasUtf8);
                    final int descriptorIndex = this.bytes.getUnsignedShort(at + 3);
                    final CstString descriptor = (CstString)this.parse0(descriptorIndex, wasUtf8);
                    cst = new CstNat(name, descriptor);
                    break;
                }
                case 15: {
                    final int kind = this.bytes.getUnsignedByte(at + 1);
                    final int constantIndex = this.bytes.getUnsignedShort(at + 2);
                    Constant ref = null;
                    switch (kind) {
                        case 1:
                        case 2:
                        case 3:
                        case 4: {
                            ref = this.parse0(constantIndex, wasUtf8);
                            break;
                        }
                        case 5:
                        case 8: {
                            ref = this.parse0(constantIndex, wasUtf8);
                            break;
                        }
                        case 6:
                        case 7: {
                            ref = this.parse0(constantIndex, wasUtf8);
                            if (!(ref instanceof CstMethodRef) && !(ref instanceof CstInterfaceMethodRef)) {
                                throw new ParseException("Unsupported ref constant type for MethodHandle " + ref.getClass());
                            }
                            break;
                        }
                        case 9: {
                            ref = this.parse0(constantIndex, wasUtf8);
                            break;
                        }
                        default: {
                            throw new ParseException("Unsupported MethodHandle kind: " + kind);
                        }
                    }
                    final int methodHandleType = getMethodHandleTypeForKind(kind);
                    cst = CstMethodHandle.make(methodHandleType, ref);
                    break;
                }
                case 16: {
                    final int descriptorIndex2 = this.bytes.getUnsignedShort(at + 1);
                    final CstString descriptor2 = (CstString)this.parse0(descriptorIndex2, wasUtf8);
                    cst = CstProtoRef.make(descriptor2);
                    break;
                }
                case 18: {
                    final int bootstrapMethodIndex = this.bytes.getUnsignedShort(at + 1);
                    final int natIndex2 = this.bytes.getUnsignedShort(at + 3);
                    final CstNat nat2 = (CstNat)this.parse0(natIndex2, wasUtf8);
                    cst = CstInvokeDynamic.make(bootstrapMethodIndex, nat2);
                    break;
                }
                default: {
                    throw new ParseException("unknown tag byte: " + Hex.u1(tag));
                }
            }
        }
        catch (ParseException ex) {
            ex.addContext("...while parsing cst " + Hex.u2(idx) + " at offset " + Hex.u4(at));
            throw ex;
        }
        catch (RuntimeException ex2) {
            final ParseException pe = new ParseException(ex2);
            pe.addContext("...while parsing cst " + Hex.u2(idx) + " at offset " + Hex.u4(at));
            throw pe;
        }
        this.pool.set(idx, cst);
        return cst;
    }
    
    private CstString parseUtf8(int at) {
        final int length = this.bytes.getUnsignedShort(at + 1);
        at += 3;
        final ByteArray ubytes = this.bytes.slice(at, at + length);
        try {
            return new CstString(ubytes);
        }
        catch (IllegalArgumentException ex) {
            throw new ParseException(ex);
        }
    }
    
    private static int getMethodHandleTypeForKind(final int kind) {
        switch (kind) {
            case 1: {
                return 3;
            }
            case 2: {
                return 1;
            }
            case 3: {
                return 2;
            }
            case 4: {
                return 0;
            }
            case 5: {
                return 5;
            }
            case 6: {
                return 4;
            }
            case 7: {
                return 7;
            }
            case 8: {
                return 6;
            }
            case 9: {
                return 8;
            }
            default: {
                throw new IllegalArgumentException("invalid kind: " + kind);
            }
        }
    }
}
