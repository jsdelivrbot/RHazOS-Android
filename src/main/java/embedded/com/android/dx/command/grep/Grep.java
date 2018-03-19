package embedded.com.android.dx.command.grep;

import embedded.com.android.dx.io.*;
import java.io.*;
import java.util.regex.*;
import embedded.com.android.dx.io.instructions.*;
import embedded.com.android.dex.*;
import embedded.com.android.dex.util.*;
import java.util.*;

public final class Grep
{
    private final Dex dex;
    private final CodeReader codeReader;
    private final Set<Integer> stringIds;
    private final PrintWriter out;
    private int count;
    private ClassDef currentClass;
    private ClassData.Method currentMethod;
    
    public Grep(final Dex dex, final Pattern pattern, final PrintWriter out) {
        this.codeReader = new CodeReader();
        this.count = 0;
        this.dex = dex;
        this.out = out;
        this.stringIds = this.getStringIds(dex, pattern);
        this.codeReader.setStringVisitor(new CodeReader.Visitor() {
            @Override
            public void visit(final DecodedInstruction[] all, final DecodedInstruction one) {
                Grep.this.encounterString(one.getIndex());
            }
        });
    }
    
    private void readArray(final EncodedValueReader reader) {
        for (int i = 0, size = reader.readArray(); i < size; ++i) {
            switch (reader.peek()) {
                case 23: {
                    this.encounterString(reader.readString());
                    break;
                }
                case 28: {
                    this.readArray(reader);
                    break;
                }
            }
        }
    }
    
    private void encounterString(final int index) {
        if (this.stringIds.contains(index)) {
            this.out.println(this.location() + " " + this.dex.strings().get(index));
            ++this.count;
        }
    }
    
    private String location() {
        final String className = this.dex.typeNames().get(this.currentClass.getTypeIndex());
        if (this.currentMethod != null) {
            final MethodId methodId = this.dex.methodIds().get(this.currentMethod.getMethodIndex());
            return className + "." + this.dex.strings().get(methodId.getNameIndex());
        }
        return className;
    }
    
    public int grep() {
        for (final ClassDef classDef : this.dex.classDefs()) {
            this.currentClass = classDef;
            this.currentMethod = null;
            if (classDef.getClassDataOffset() == 0) {
                continue;
            }
            final ClassData classData = this.dex.readClassData(classDef);
            final int staticValuesOffset = classDef.getStaticValuesOffset();
            if (staticValuesOffset != 0) {
                this.readArray(new EncodedValueReader(this.dex.open(staticValuesOffset)));
            }
            for (final ClassData.Method method : classData.allMethods()) {
                this.currentMethod = method;
                if (method.getCodeOffset() != 0) {
                    this.codeReader.visitAll(this.dex.readCode(method).getInstructions());
                }
            }
        }
        this.currentClass = null;
        this.currentMethod = null;
        return this.count;
    }
    
    private Set<Integer> getStringIds(final Dex dex, final Pattern pattern) {
        final Set<Integer> stringIds = new HashSet<Integer>();
        int stringIndex = 0;
        for (final String s : dex.strings()) {
            if (pattern.matcher(s).find()) {
                stringIds.add(stringIndex);
            }
            ++stringIndex;
        }
        return stringIds;
    }
}
