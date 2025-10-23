package rege.syntax.model;

/**
 * The empty language terminal (∅).
 * This is a singleton representing the empty set - a language that accepts no strings.
 */
public final class Empty implements Terminal {
    
    private static final Empty INSTANCE = new Empty();
    
    private Empty() {
        // Private constructor to enforce singleton
    }
    
    /**
     * Get the singleton instance of Empty.
     * 
     * @return the Empty instance
     */
    public static Empty instance() {
        return INSTANCE;
    }
    
    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T input) {
        return visitor.visitEmpty(this, input);
    }
    
    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
    
    @Override
    public int hashCode() {
        return 37;  // Unique hash
    }
    
    @Override
    public String toString() {
        return "∅";
    }

    /**
     * Ensures singleton property is maintained during deserialization.
     * [read more](https://docs.oracle.com/en/java/javase/23/docs/specs/serialization/input.html#the-readresolve-method)
     * @return the singleton instance
     */
    private Object readResolve() {
        return INSTANCE; // Always return the singleton instance
    }
}
