package embedded.com.android.dx.dex.cf;

import embedded.com.android.dx.command.dexer.*;
import embedded.com.android.dx.cf.direct.*;
import embedded.com.android.dx.dex.*;
import embedded.com.android.dex.util.*;
import java.util.*;

import embedded.com.android.dx.rop.code.LocalVariableExtractor;
import embedded.com.android.dx.rop.code.LocalVariableInfo;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.ssa.*;
import embedded.com.android.dx.dex.file.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.annotation.*;

public class CfTranslator
{
    private static final boolean DEBUG = false;
    
    public static ClassDefItem translate(final DxContext context, final DirectClassFile cf, final byte[] bytes, final CfOptions cfOptions, final DexOptions dexOptions, final DexFile dexFile) {
        try {
            return translate0(context, cf, bytes, cfOptions, dexOptions, dexFile);
        }
        catch (RuntimeException ex) {
            final String msg = "...while processing " + cf.getFilePath();
            throw ExceptionWithContext.withContext(ex, msg);
        }
    }
    
    private static ClassDefItem translate0(final DxContext context, final DirectClassFile cf, final byte[] bytes, final CfOptions cfOptions, final DexOptions dexOptions, final DexFile dexFile) {
        context.optimizerOptions.loadOptimizeLists(cfOptions.optimizeListFile, cfOptions.dontOptimizeListFile);
        final CstType thisClass = cf.getThisClass();
        final int classAccessFlags = cf.getAccessFlags() & 0xFFFFFFDF;
        final CstString sourceFile = (cfOptions.positionInfo == 1) ? null : cf.getSourceFile();
        final ClassDefItem out = new ClassDefItem(thisClass, classAccessFlags, cf.getSuperclass(), cf.getInterfaces(), sourceFile);
        final Annotations classAnnotations = AttributeTranslator.getClassAnnotations(cf, cfOptions);
        if (classAnnotations.size() != 0) {
            out.setClassAnnotations(classAnnotations, dexFile);
        }
        final FieldIdsSection fieldIdsSection = dexFile.getFieldIds();
        final MethodIdsSection methodIdsSection = dexFile.getMethodIds();
        final MethodHandlesSection methodHandlesSection = dexFile.getMethodHandles();
        final CallSiteIdsSection callSiteIds = dexFile.getCallSiteIds();
        processFields(cf, out, dexFile);
        processMethods(context, cf, cfOptions, dexOptions, out, dexFile);
        final ConstantPool constantPool = cf.getConstantPool();
        for (int constantPoolSize = constantPool.size(), i = 0; i < constantPoolSize; ++i) {
            final Constant constant = constantPool.getOrNull(i);
            if (constant instanceof CstMethodRef) {
                methodIdsSection.intern((CstBaseMethodRef)constant);
            }
            else if (constant instanceof CstInterfaceMethodRef) {
                methodIdsSection.intern(((CstInterfaceMethodRef)constant).toMethodRef());
            }
            else if (constant instanceof CstFieldRef) {
                fieldIdsSection.intern((CstFieldRef)constant);
            }
            else if (constant instanceof CstEnumRef) {
                fieldIdsSection.intern(((CstEnumRef)constant).getFieldRef());
            }
            else if (constant instanceof CstMethodHandle) {
                methodHandlesSection.intern((CstMethodHandle)constant);
            }
            else if (constant instanceof CstInvokeDynamic) {
                final CstInvokeDynamic cstInvokeDynamic = (CstInvokeDynamic)constant;
                final int index = cstInvokeDynamic.getBootstrapMethodIndex();
                final BootstrapMethodsList.Item bootstrapMethod = cf.getBootstrapMethods().get(index);
                final CstCallSite callSite = CstCallSite.make(bootstrapMethod.getBootstrapMethodHandle(), cstInvokeDynamic.getNat(), bootstrapMethod.getBootstrapMethodArguments());
                cstInvokeDynamic.setDeclaringClass(cf.getThisClass());
                cstInvokeDynamic.setCallSite(callSite);
                for (final CstCallSiteRef ref : cstInvokeDynamic.getReferences()) {
                    callSiteIds.intern(ref);
                }
            }
        }
        return out;
    }
    
    private static void processFields(final DirectClassFile cf, final ClassDefItem out, final DexFile dexFile) {
        final CstType thisClass = cf.getThisClass();
        final FieldList fields = cf.getFields();
        for (int sz = fields.size(), i = 0; i < sz; ++i) {
            final Field one = fields.get(i);
            try {
                final CstFieldRef field = new CstFieldRef(thisClass, one.getNat());
                final int accessFlags = one.getAccessFlags();
                if (AccessFlags.isStatic(accessFlags)) {
                    TypedConstant constVal = one.getConstantValue();
                    final EncodedField fi = new EncodedField(field, accessFlags);
                    if (constVal != null) {
                        constVal = coerceConstant(constVal, field.getType());
                    }
                    out.addStaticField(fi, constVal);
                }
                else {
                    final EncodedField fi2 = new EncodedField(field, accessFlags);
                    out.addInstanceField(fi2);
                }
                final Annotations annotations = AttributeTranslator.getAnnotations(one.getAttributes());
                if (annotations.size() != 0) {
                    out.addFieldAnnotations(field, annotations, dexFile);
                }
                dexFile.getFieldIds().intern(field);
            }
            catch (RuntimeException ex) {
                final String msg = "...while processing " + one.getName().toHuman() + " " + one.getDescriptor().toHuman();
                throw ExceptionWithContext.withContext(ex, msg);
            }
        }
    }
    
    private static TypedConstant coerceConstant(final TypedConstant constant, final Type type) {
        final Type constantType = constant.getType();
        if (constantType.equals(type)) {
            return constant;
        }
        switch (type.getBasicType()) {
            case 1: {
                return CstBoolean.make(((CstInteger)constant).getValue());
            }
            case 2: {
                return CstByte.make(((CstInteger)constant).getValue());
            }
            case 3: {
                return CstChar.make(((CstInteger)constant).getValue());
            }
            case 8: {
                return CstShort.make(((CstInteger)constant).getValue());
            }
            default: {
                throw new UnsupportedOperationException("can't coerce " + constant + " to " + type);
            }
        }
    }
    
    private static void processMethods(final DxContext context, final DirectClassFile cf, final CfOptions cfOptions, final DexOptions dexOptions, final ClassDefItem out, final DexFile dexFile) {
        final CstType thisClass = cf.getThisClass();
        final MethodList methods = cf.getMethods();
        for (int sz = methods.size(), i = 0; i < sz; ++i) {
            final Method one = methods.get(i);
            try {
                final CstMethodRef meth = new CstMethodRef(thisClass, one.getNat());
                int accessFlags = one.getAccessFlags();
                final boolean isStatic = AccessFlags.isStatic(accessFlags);
                final boolean isPrivate = AccessFlags.isPrivate(accessFlags);
                final boolean isNative = AccessFlags.isNative(accessFlags);
                final boolean isAbstract = AccessFlags.isAbstract(accessFlags);
                final boolean isConstructor = meth.isInstanceInit() || meth.isClassInit();
                DalvCode code;
                if (isNative || isAbstract) {
                    code = null;
                }
                else {
                    final ConcreteMethod concrete = new ConcreteMethod(one, cf, cfOptions.positionInfo != 1, cfOptions.localInfo);
                    final TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
                    RopMethod rmeth = Ropper.convert(concrete, advice, methods, dexOptions);
                    RopMethod nonOptRmeth = null;
                    final int paramSize = meth.getParameterWordCount(isStatic);
                    final String canonicalName = thisClass.getClassType().getDescriptor() + "." + one.getName().getString();
                    if (cfOptions.optimize && context.optimizerOptions.shouldOptimize(canonicalName)) {
                        nonOptRmeth = rmeth;
                        rmeth = Optimizer.optimize(rmeth, paramSize, isStatic, cfOptions.localInfo, advice);
                        if (cfOptions.statistics) {
                            context.codeStatistics.updateRopStatistics(nonOptRmeth, rmeth);
                        }
                    }
                    LocalVariableInfo locals = null;
                    if (cfOptions.localInfo) {
                        locals = LocalVariableExtractor.extract(rmeth);
                    }
                    code = RopTranslator.translate(rmeth, cfOptions.positionInfo, locals, paramSize, dexOptions);
                    if (cfOptions.statistics && nonOptRmeth != null) {
                        updateDexStatistics(context, cfOptions, dexOptions, rmeth, nonOptRmeth, locals, paramSize, concrete.getCode().size());
                    }
                }
                if (AccessFlags.isSynchronized(accessFlags)) {
                    accessFlags |= 0x20000;
                    if (!isNative) {
                        accessFlags &= 0xFFFFFFDF;
                    }
                }
                if (isConstructor) {
                    accessFlags |= 0x10000;
                }
                final TypeList exceptions = AttributeTranslator.getExceptions(one);
                final EncodedMethod mi = new EncodedMethod(meth, accessFlags, code, exceptions);
                if (meth.isInstanceInit() || meth.isClassInit() || isStatic || isPrivate) {
                    out.addDirectMethod(mi);
                }
                else {
                    out.addVirtualMethod(mi);
                }
                final Annotations annotations = AttributeTranslator.getMethodAnnotations(one);
                if (annotations.size() != 0) {
                    out.addMethodAnnotations(meth, annotations, dexFile);
                }
                final AnnotationsList list = AttributeTranslator.getParameterAnnotations(one);
                if (list.size() != 0) {
                    out.addParameterAnnotations(meth, list, dexFile);
                }
                dexFile.getMethodIds().intern(meth);
            }
            catch (RuntimeException ex) {
                final String msg = "...while processing " + one.getName().toHuman() + " " + one.getDescriptor().toHuman();
                throw ExceptionWithContext.withContext(ex, msg);
            }
        }
    }
    
    private static void updateDexStatistics(final DxContext context, final CfOptions cfOptions, final DexOptions dexOptions, final RopMethod optRmeth, final RopMethod nonOptRmeth, final LocalVariableInfo locals, final int paramSize, final int originalByteCount) {
        final DalvCode optCode = RopTranslator.translate(optRmeth, cfOptions.positionInfo, locals, paramSize, dexOptions);
        final DalvCode nonOptCode = RopTranslator.translate(nonOptRmeth, cfOptions.positionInfo, locals, paramSize, dexOptions);
        final DalvCode.AssignIndicesCallback callback = new DalvCode.AssignIndicesCallback() {
            @Override
            public int getIndex(final Constant cst) {
                return 0;
            }
        };
        optCode.assignIndices(callback);
        nonOptCode.assignIndices(callback);
        context.codeStatistics.updateDexStatistics(nonOptCode, optCode);
        context.codeStatistics.updateOriginalByteCount(originalByteCount);
    }
}
