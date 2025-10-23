package rege.syntax.model;

/**
 * The epsilon terminal (ε).
 * This is a singleton representing the empty string - a language that accepts only the empty string.
 */
public final class Epsilon implements Terminal {
    
    private static final Epsilon INSTANCE = new Epsilon();
    
    private Epsilon() {
        // Private constructor to enforce singleton
    }
    
    /**
     * Get the singleton instance of Epsilon.
     * 
     * @return the Epsilon instance
     */
    public static Epsilon instance() {
        return INSTANCE;
    }
    
    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T input) {
        return visitor.visitEpsilon(this, input);
    }
    
    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
    
    @Override
    public int hashCode() {
        return 31;  // Unique hash
    }
    
    @Override
    public String toString() {
        return "ε";
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
