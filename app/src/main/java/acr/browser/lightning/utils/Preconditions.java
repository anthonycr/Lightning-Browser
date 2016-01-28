package acr.browser.lightning.utils;

public class Preconditions {
    /**
     * Ensure that an object is not null
     * and throw a RuntimeException if it
     * is null.
     *
     * @param object check nullness on this object.
     */
    public static void checkNonNull(Object object) {
        if (object == null) {
            throw new RuntimeException("Object must not be null");
        }
    }
}
