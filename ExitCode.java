package jdk.test.lib.process;

/**
 * Exit code values that could be returned by the JVM.
 */
public enum ExitCode {
    OK(0),
    FAIL(1),
    CRASH(134);

    public final int value;

    ExitCode(int value) {
        this.value = value;
    }
}

