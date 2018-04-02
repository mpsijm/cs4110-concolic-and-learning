package cs4110;

import java.io.InputStream;
import java.io.PrintStream;

public abstract class MockSystem {
    public static class System {
        public static final InputStream in = java.lang.System.in;
        public static final PrintStream err = java.lang.System.err;

        public static class out {
            static String res;

            public static void println(String s) {
                res = s;
            }
        }
    }

    public static class Errors {
        public static void __VERIFIER_error(int i) {
            throw new IllegalStateException("ERR:" + i);
        }
    }

    public String calculateOutputString(String input) {
        try {
            calculateOutput(input);
        } catch (IllegalArgumentException e) {
            return "-";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
        return System.out.res;
    }

    protected abstract void calculateOutput(String input);
}
