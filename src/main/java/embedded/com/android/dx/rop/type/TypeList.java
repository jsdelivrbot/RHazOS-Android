package embedded.com.android.dx.rop.type;

public interface TypeList
{
    boolean isMutable();
    
    int size();
    
    Type getType(final int p0);
    
    int getWordCount();
    
    TypeList withAddedType(final Type p0);
}
