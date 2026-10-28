/** Prints the Java home selected for this Bazel Java binary. */
public final class PrintJavaHome {
    private PrintJavaHome() {}

    public static void main(String[] args) {
        System.out.println(System.getProperty("java.home"));
    }
}
