package embedded.com.android.dx.command.findusages;

import java.io.*;
import java.util.regex.*;
import embedded.com.android.dx.io.instructions.*;
import embedded.com.android.dx.io.*;
import java.util.*;
import embedded.com.android.dex.*;

public final class FindUsages
{
    private final Dex dex;
    private final Set<Integer> methodIds;
    private final Set<Integer> fieldIds;
    private final CodeReader codeReader;
    private final PrintWriter out;
    private ClassDef currentClass;
    private ClassData.Method currentMethod;
    
    public FindUsages(final Dex dex, final String declaredBy, final String memberName, final PrintWriter out) {
        this.codeReader = new CodeReader();
        this.dex = dex;
        this.out = out;
        final Set<Integer> typeStringIndexes = new HashSet<Integer>();
        final Set<Integer> memberNameIndexes = new HashSet<Integer>();
        final Pattern declaredByPattern = Pattern.compile(declaredBy);
        final Pattern memberNamePattern = Pattern.compile(memberName);
        final List<String> strings = dex.strings();
        for (int i = 0; i < strings.size(); ++i) {
            final String string = strings.get(i);
            if (declaredByPattern.matcher(string).matches()) {
                typeStringIndexes.add(i);
            }
            if (memberNamePattern.matcher(string).matches()) {
                memberNameIndexes.add(i);
            }
        }
        if (typeStringIndexes.isEmpty() || memberNameIndexes.isEmpty()) {
            final Set<Integer> set = null;
            this.fieldIds = set;
            this.methodIds = set;
            return;
        }
        this.methodIds = new HashSet<Integer>();
        this.fieldIds = new HashSet<Integer>();
        for (final int typeStringIndex : typeStringIndexes) {
            final int typeIndex = Collections.binarySearch(dex.typeIds(), typeStringIndex);
            if (typeIndex < 0) {
                continue;
            }
            this.methodIds.addAll(this.getMethodIds(dex, memberNameIndexes, typeIndex));
            this.fieldIds.addAll(this.getFieldIds(dex, memberNameIndexes, typeIndex));
        }
        this.codeReader.setFieldVisitor(new CodeReader.Visitor() {
            @Override
            public void visit(final DecodedInstruction[] all, final DecodedInstruction one) {
                final int fieldId = one.getIndex();
                if (FindUsages.this.fieldIds.contains(fieldId)) {
                    out.println(FindUsages.this.location() + ": field reference " + dex.fieldIds().get(fieldId) + " (" + OpcodeInfo.getName(one.getOpcode()) + ")");
                }
            }
        });
        this.codeReader.setMethodVisitor(new CodeReader.Visitor() {
            @Override
            public void visit(final DecodedInstruction[] all, final DecodedInstruction one) {
                final int methodId = one.getIndex();
                if (FindUsages.this.methodIds.contains(methodId)) {
                    out.println(FindUsages.this.location() + ": method reference " + dex.methodIds().get(methodId) + " (" + OpcodeInfo.getName(one.getOpcode()) + ")");
                }
            }
        });
    }
    
    private String location() {
        final String className = this.dex.typeNames().get(this.currentClass.getTypeIndex());
        if (this.currentMethod != null) {
            final MethodId methodId = this.dex.methodIds().get(this.currentMethod.getMethodIndex());
            return className + "." + this.dex.strings().get(methodId.getNameIndex());
        }
        return className;
    }
    
    public void findUsages() {
        if (this.fieldIds == null || this.methodIds == null) {
            return;
        }
        for (final ClassDef classDef : this.dex.classDefs()) {
            this.currentClass = classDef;
            this.currentMethod = null;
            if (classDef.getClassDataOffset() == 0) {
                continue;
            }
            final ClassData classData = this.dex.readClassData(classDef);
            for (final ClassData.Field field : classData.allFields()) {
                final int fieldIndex = field.getFieldIndex();
                if (this.fieldIds.contains(fieldIndex)) {
                    this.out.println(this.location() + " field declared " + this.dex.fieldIds().get(fieldIndex));
                }
            }
            for (final ClassData.Method method : classData.allMethods()) {
                this.currentMethod = method;
                final int methodIndex = method.getMethodIndex();
                if (this.methodIds.contains(methodIndex)) {
                    this.out.println(this.location() + " method declared " + this.dex.methodIds().get(methodIndex));
                }
                if (method.getCodeOffset() != 0) {
                    this.codeReader.visitAll(this.dex.readCode(method).getInstructions());
                }
            }
        }
        this.currentClass = null;
        this.currentMethod = null;
    }
    
    private Set<Integer> getFieldIds(final Dex dex, final Set<Integer> memberNameIndexes, final int declaringType) {
        final Set<Integer> fields = new HashSet<Integer>();
        int fieldIndex = 0;
        for (final FieldId fieldId : dex.fieldIds()) {
            if (memberNameIndexes.contains(fieldId.getNameIndex()) && declaringType == fieldId.getDeclaringClassIndex()) {
                fields.add(fieldIndex);
            }
            ++fieldIndex;
        }
        return fields;
    }
    
    private Set<Integer> getMethodIds(final Dex dex, final Set<Integer> memberNameIndexes, final int declaringType) {
        final Set<Integer> subtypes = this.findAssignableTypes(dex, declaringType);
        final Set<Integer> methods = new HashSet<Integer>();
        int methodIndex = 0;
        for (final MethodId method : dex.methodIds()) {
            if (memberNameIndexes.contains(method.getNameIndex()) && subtypes.contains(method.getDeclaringClassIndex())) {
                methods.add(methodIndex);
            }
            ++methodIndex;
        }
        return methods;
    }
    
    private Set<Integer> findAssignableTypes(final Dex dex, final int typeIndex) {
        final Set<Integer> assignableTypes = new HashSet<Integer>();
        assignableTypes.add(typeIndex);
        for (final ClassDef classDef : dex.classDefs()) {
            if (assignableTypes.contains(classDef.getSupertypeIndex())) {
                assignableTypes.add(classDef.getTypeIndex());
            }
            else {
                for (final int implemented : classDef.getInterfaces()) {
                    if (assignableTypes.contains(implemented)) {
                        assignableTypes.add(classDef.getTypeIndex());
                        break;
                    }
                }
            }
        }
        return assignableTypes;
    }
}
