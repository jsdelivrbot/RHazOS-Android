package embedded.com.android.dx.io;

import java.io.*;
import java.util.*;
import embedded.com.android.dex.*;

public final class DexIndexPrinter
{
    private final Dex dex;
    private final TableOfContents tableOfContents;
    
    public DexIndexPrinter(final File file) throws IOException {
        this.dex = new Dex(file);
        this.tableOfContents = this.dex.getTableOfContents();
    }
    
    private void printMap() {
        for (final TableOfContents.Section section : this.tableOfContents.sections) {
            if (section.off != -1) {
                System.out.println("section " + Integer.toHexString(section.type) + " off=" + Integer.toHexString(section.off) + " size=" + Integer.toHexString(section.size) + " byteCount=" + Integer.toHexString(section.byteCount));
            }
        }
    }
    
    private void printStrings() throws IOException {
        int index = 0;
        for (final String string : this.dex.strings()) {
            System.out.println("string " + index + ": " + string);
            ++index;
        }
    }
    
    private void printTypeIds() throws IOException {
        int index = 0;
        for (final Integer type : this.dex.typeIds()) {
            System.out.println("type " + index + ": " + this.dex.strings().get(type));
            ++index;
        }
    }
    
    private void printProtoIds() throws IOException {
        int index = 0;
        for (final ProtoId protoId : this.dex.protoIds()) {
            System.out.println("proto " + index + ": " + protoId);
            ++index;
        }
    }
    
    private void printFieldIds() throws IOException {
        int index = 0;
        for (final FieldId fieldId : this.dex.fieldIds()) {
            System.out.println("field " + index + ": " + fieldId);
            ++index;
        }
    }
    
    private void printMethodIds() throws IOException {
        int index = 0;
        for (final MethodId methodId : this.dex.methodIds()) {
            System.out.println("methodId " + index + ": " + methodId);
            ++index;
        }
    }
    
    private void printTypeLists() throws IOException {
        if (this.tableOfContents.typeLists.off == -1) {
            System.out.println("No type lists");
            return;
        }
        final Dex.Section in = this.dex.open(this.tableOfContents.typeLists.off);
        for (int i = 0; i < this.tableOfContents.typeLists.size; ++i) {
            final int size = in.readInt();
            System.out.print("Type list i=" + i + ", size=" + size + ", elements=");
            for (int t = 0; t < size; ++t) {
                System.out.print(" " + this.dex.typeNames().get(in.readShort()));
            }
            if (size % 2 == 1) {
                in.readShort();
            }
            System.out.println();
        }
    }
    
    private void printClassDefs() {
        int index = 0;
        for (final ClassDef classDef : this.dex.classDefs()) {
            System.out.println("class def " + index + ": " + classDef);
            ++index;
        }
    }
    
    public static void main(final String[] args) throws IOException {
        final DexIndexPrinter indexPrinter = new DexIndexPrinter(new File(args[0]));
        indexPrinter.printMap();
        indexPrinter.printStrings();
        indexPrinter.printTypeIds();
        indexPrinter.printProtoIds();
        indexPrinter.printFieldIds();
        indexPrinter.printMethodIds();
        indexPrinter.printTypeLists();
        indexPrinter.printClassDefs();
    }
}
