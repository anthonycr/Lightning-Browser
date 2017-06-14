package acr.browser.lightning.utils;

import org.junit.Test;

/**
 * Unit tests for {@link Preconditions}.
 */
public class PreconditionsTest {

    @Test(expected = RuntimeException.class)
    public void checkNonNull_Null_ThrowsException() {
        Preconditions.checkNonNull(null);
    }

    @Test
    public void checkNonNull_NonNull_Succeeds() {
        Preconditions.checkNonNull(new Object());
    }
}