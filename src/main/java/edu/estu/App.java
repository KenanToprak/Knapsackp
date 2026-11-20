package edu.estu;

import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

/**
 * CLI entry point for loading and solving 0-1 Knapsack problem instances.
 */
public class App {
    static final Path DEFAULT_SAMPLE = Paths.get("samples", "test.in");

    public static void main(String[] args) {
        run(args, System.out, System.err);
    }

    /**
     * Executes the CLI workflow using the provided output streams.
     *
     * @param args CLI arguments (optional single path to a KP01 instance file)
     * @param out  standard output stream
     * @param err  standard error stream
     * @return {@code 0} on success, {@code 1} on invalid arguments, missing file, or parse error
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(err, "err must not be null");

        if (args != null && args.length > 1) {
            err.println("Usage: java -jar Knapsackp-1.0-SNAPSHOT.jar [path/to/instance.in]");
            return 1;
        }

        final Path inputPath;
        try {
            inputPath = (args == null || args.length == 0)
                    ? DEFAULT_SAMPLE
                    : Paths.get(args[0]);
        } catch (InvalidPathException e) {
            err.println("Invalid file path: " + e.getMessage());
            return 1;
        }

        if (!Files.isRegularFile(inputPath)) {
            err.println("Input file not found or not a regular file: " + inputPath);
            return 1;
        }

        try {
            KP01 problem = KP01.fromFile(inputPath);
            Solution greedy = problem.greedySolution();
            Solution exact = problem.solveExact();
            Solution parallelRandom = problem.oneMillionParallel();

            out.printf(Locale.ROOT, "Instance           : %s (%s)%n", inputPath, problem);
            out.printf(Locale.ROOT, "Trivial            : %s%n", problem.trivial());
            out.printf(Locale.ROOT, "Greedy solution    : %s%n", greedy);
            out.printf(Locale.ROOT, "Exact solution     : %s%n", exact);
            out.printf(Locale.ROOT, "1M Parallel random : %s%n", parallelRandom);
            return 0;
        } catch (IllegalArgumentException | UncheckedIOException | ArithmeticException e) {
            err.println("Failed to process knapsack instance '" + inputPath + "': " + e.getMessage());
            return 1;
        }
    }
}
