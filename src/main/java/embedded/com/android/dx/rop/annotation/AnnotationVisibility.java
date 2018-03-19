package embedded.com.android.dx.rop.annotation;

import embedded.com.android.dx.util.*;

public enum AnnotationVisibility implements ToHuman
{
    RUNTIME("runtime"), 
    BUILD("build"), 
    SYSTEM("system"), 
    EMBEDDED("embedded");
    
    private final String human;
    
    private AnnotationVisibility(final String human) {
        this.human = human;
    }
    
    @Override
    public String toHuman() {
        return this.human;
    }
}
