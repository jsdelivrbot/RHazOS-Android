package embedded.com.android.dx.util;

public interface AnnotatedOutput extends Output
{
    boolean annotates();
    
    boolean isVerbose();
    
    void annotate(final String p0);
    
    void annotate(final int p0, final String p1);
    
    void endAnnotation();
    
    int getAnnotationWidth();
}
