package embedded.com.android.dx.command.annotool;

import java.lang.annotation.*;
import java.util.*;

public class Main
{
    public static void main(final String[] argArray) {
        final Arguments args = new Arguments();
        try {
            args.parse(argArray);
        }
        catch (InvalidArgumentException ex) {
            System.err.println(ex.getMessage());
            throw new RuntimeException("usage");
        }
        new AnnotationLister(args).process();
    }
    
    private static class InvalidArgumentException extends Exception
    {
        InvalidArgumentException() {
        }
        
        InvalidArgumentException(final String s) {
            super(s);
        }
    }
    
    enum PrintType
    {
        CLASS, 
        INNERCLASS, 
        METHOD, 
        PACKAGE;
    }
    
    static class Arguments
    {
        String aclass;
        EnumSet<ElementType> eTypes;
        EnumSet<PrintType> printTypes;
        String[] files;
        
        Arguments() {
            this.eTypes = EnumSet.noneOf(ElementType.class);
            this.printTypes = EnumSet.noneOf(PrintType.class);
        }
        
        void parse(final String[] argArray) throws InvalidArgumentException {
            for (int i = 0; i < argArray.length; ++i) {
                final String arg = argArray[i];
                if (arg.startsWith("--annotation=")) {
                    final String argParam = arg.substring(arg.indexOf(61) + 1);
                    if (this.aclass != null) {
                        throw new InvalidArgumentException("--annotation can only be specified once.");
                    }
                    this.aclass = argParam.replace('.', '/');
                }
                else if (arg.startsWith("--element=")) {
                    final String argParam = arg.substring(arg.indexOf(61) + 1);
                    try {
                        for (final String p : argParam.split(",")) {
                            this.eTypes.add(ElementType.valueOf(p.toUpperCase(Locale.ROOT)));
                        }
                    }
                    catch (IllegalArgumentException ex) {
                        throw new InvalidArgumentException("invalid --element");
                    }
                }
                else {
                    if (!arg.startsWith("--print=")) {
                        System.arraycopy(argArray, i, this.files = new String[argArray.length - i], 0, this.files.length);
                        break;
                    }
                    final String argParam = arg.substring(arg.indexOf(61) + 1);
                    try {
                        for (final String p : argParam.split(",")) {
                            this.printTypes.add(PrintType.valueOf(p.toUpperCase(Locale.ROOT)));
                        }
                    }
                    catch (IllegalArgumentException ex) {
                        throw new InvalidArgumentException("invalid --print");
                    }
                }
            }
            if (this.aclass == null) {
                throw new InvalidArgumentException("--annotation must be specified");
            }
            if (this.printTypes.isEmpty()) {
                this.printTypes.add(PrintType.CLASS);
            }
            if (this.eTypes.isEmpty()) {
                this.eTypes.add(ElementType.TYPE);
            }
            final EnumSet<ElementType> set = this.eTypes.clone();
            set.remove(ElementType.TYPE);
            set.remove(ElementType.PACKAGE);
            if (!set.isEmpty()) {
                throw new InvalidArgumentException("only --element parameters 'type' and 'package' supported");
            }
        }
    }
}
