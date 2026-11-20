package edu.estu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

    @Test
    void runWithDefaultSampleSucceedsAndPrintsSolutions() {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exit = App.run(
                new String[0],
                new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8)
        );

        String stdout = outBytes.toString(StandardCharsets.UTF_8);
        String stderr = errBytes.toString(StandardCharsets.UTF_8);

        assertEquals(0, exit, "Expected zero status, stderr was: " + stderr);
        assertEquals("", stderr);
        assertTrue(stdout.contains("Trivial            : false"));
        assertTrue(stdout.contains("Greedy solution    : Solution{items=2, totalProfit=12, totalWeight=9"));
        assertTrue(stdout.contains("Exact solution     : Solution{items=2, totalProfit=12, totalWeight=9"));
        assertTrue(stdout.contains("1M Parallel random : Solution{"));
    }

    @Test
    void runWithExplicitTempFileSucceeds(@TempDir Path tempDir) throws IOException {
        Path instance = tempDir.resolve("counterexample.in");
        Files.writeString(instance, """
                3
                1 60 6
                2 50 5
                3 50 5
                10
                """, StandardCharsets.UTF_8);

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exit = App.run(
                new String[]{instance.toString()},
                new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8)
        );

        String stdout = outBytes.toString(StandardCharsets.UTF_8);
        assertEquals(0, exit);
        assertTrue(stdout.contains("Greedy solution    : Solution{items=1, totalProfit=60, totalWeight=6"));
        assertTrue(stdout.contains("Exact solution     : Solution{items=2, totalProfit=100, totalWeight=10"));
    }

    @Test
    void runWithMissingFileReturnsErrorWithoutCrashing(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does_not_exist.in");
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exit = App.run(
                new String[]{missing.toString()},
                new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8)
        );

        assertEquals(1, exit);
        assertTrue(errBytes.toString(StandardCharsets.UTF_8).contains("Input file not found"));
    }

    @Test
    void runWithMalformedFileReturnsError(@TempDir Path tempDir) throws IOException {
        Path badFile = tempDir.resolve("bad.in");
        Files.writeString(badFile, "not_a_number\n", StandardCharsets.UTF_8);

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exit = App.run(
                new String[]{badFile.toString()},
                new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8)
        );

        assertEquals(1, exit);
        assertTrue(errBytes.toString(StandardCharsets.UTF_8).contains("Failed to process knapsack instance"));
    }

    @Test
    void runWithTooManyArgumentsPrintsUsageAndReturnsError() {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        int exit = App.run(
                new String[]{"a.in", "b.in"},
                new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8)
        );

        assertEquals(1, exit);
        assertTrue(errBytes.toString(StandardCharsets.UTF_8).contains("Usage:"));
    }

    @Test
    void mainExecutesCleanlyWithSampleFile() {
        assertDoesNotThrow(() -> App.main(new String[]{"samples/test.in"}));
    }
}
