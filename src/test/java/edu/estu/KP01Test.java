package edu.estu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KP01Test {

    @Test
    void itemRejectsNegativeProfitOrWeight() {
        assertThrows(IllegalArgumentException.class, () -> new Item(1, -1, 5));
        assertThrows(IllegalArgumentException.class, () -> new Item(1, 5, -1));
        Item zeroItem = new Item(0, 0, 0);
        assertEquals(0, zeroItem.profit());
        assertEquals(0, zeroItem.weight());
    }

    @Test
    void constructorValidatesInputsAndDefensivelyCopiesItems() {
        assertThrows(IllegalArgumentException.class, () -> new KP01(-1, new Item(1, 1, 1)));
        assertThrows(NullPointerException.class, () -> new KP01(10, (Set<Item>) null));
        assertThrows(NullPointerException.class, () -> new KP01(10, (Item[]) null));
        assertThrows(NullPointerException.class, () -> new KP01(10, new Item[]{null}));

        Item a = new Item(1, 10, 4);
        Item b = new Item(2, 20, 6);
        Set<Item> mutable = new LinkedHashSet<>(List.of(a, b));
        KP01 kp = new KP01(10, mutable);

        mutable.clear();
        assertEquals(2, kp.getItems().size());
        assertEquals(List.of(a, b), List.copyOf(kp.getItems()));
        assertThrows(UnsupportedOperationException.class, () -> kp.getItems().add(new Item(3, 1, 1)));
    }

    @Test
    void trivialHandlesBoundaryAndPreventsLongOverflow() {
        KP01 fits = new KP01(10, new Item(1, 5, 4), new Item(2, 6, 6));
        assertTrue(fits.trivial());
        assertTrue(fits.isTrivial());

        KP01 exceeds = new KP01(9, new Item(1, 5, 4), new Item(2, 6, 6));
        assertFalse(exceeds.trivial());

        KP01 emptyZero = new KP01(0);
        assertTrue(emptyZero.trivial());

        KP01 zeroWeightOnly = new KP01(0, new Item(1, 10, 0));
        assertTrue(zeroWeightOnly.trivial());

        // Long.MAX_VALUE overflow test: two weights summing > Long.MAX_VALUE must NOT wrap negative!
        long halfPlusTen = (Long.MAX_VALUE / 2) + 10;
        KP01 overflowNonTrivial = new KP01(100,
                new Item(1, 1, halfPlusTen),
                new Item(2, 1, halfPlusTen));
        assertFalse(overflowNonTrivial.trivial());

        KP01 maxCapExact = new KP01(Long.MAX_VALUE,
                new Item(1, 1, Long.MAX_VALUE / 2),
                new Item(2, 1, Long.MAX_VALUE - (Long.MAX_VALUE / 2)));
        assertTrue(maxCapExact.trivial());

        KP01 maxCapExceededByOne = new KP01(Long.MAX_VALUE,
                new Item(1, 1, Long.MAX_VALUE),
                new Item(2, 1, 1));
        assertFalse(maxCapExceededByOne.trivial());
    }

    @Test
    void fromFileParsesValidSamplesAndWhitespace(@TempDir Path tempDir) throws IOException {
        KP01 sample = KP01.fromFile(Paths.get("samples", "test.in"));
        assertEquals(10L, sample.getCapacity());
        assertEquals(3, sample.getItems().size());
        assertFalse(sample.trivial());

        Path spaced = tempDir.resolve("spaced.in");
        Files.writeString(spaced, "\n   2  \n  10  5   3  \n\n  20\t7\t4 \n   7  \n\n", StandardCharsets.UTF_8);
        KP01 parsedSpaced = KP01.fromFile(spaced);
        assertEquals(7L, parsedSpaced.capacity());
        assertEquals(2, parsedSpaced.items().size());
        assertTrue(parsedSpaced.trivial());

        Path zeroItems = tempDir.resolve("zero.in");
        Files.writeString(zeroItems, "0\n15\n", StandardCharsets.UTF_8);
        KP01 parsedZero = KP01.fromFile(zeroItems);
        assertEquals(15L, parsedZero.getCapacity());
        assertTrue(parsedZero.getItems().isEmpty());
        assertTrue(parsedZero.trivial());
    }

    @Test
    void fromFileRejectsInvalidInputs(@TempDir Path tempDir) throws IOException {
        assertThrows(NullPointerException.class, () -> KP01.fromFile(null));
        assertThrows(UncheckedIOException.class, () -> KP01.fromFile(tempDir.resolve("missing.in")));

        Path empty = tempDir.resolve("empty.in");
        Files.writeString(empty, "   \n ", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> KP01.fromFile(empty));

        Path negativeN = tempDir.resolve("neg_n.in");
        Files.writeString(negativeN, "-3\n10\n", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> KP01.fromFile(negativeN));

        Path truncated = tempDir.resolve("truncated.in");
        Files.writeString(truncated, "2\n1 5 3\n10\n", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> KP01.fromFile(truncated));

        Path malformed = tempDir.resolve("malformed.in");
        Files.writeString(malformed, "1\n1 abc 3\n10\n", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> KP01.fromFile(malformed));

        Path trailing = tempDir.resolve("trailing.in");
        Files.writeString(trailing, "1\n1 5 3\n10\nextra\n", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> KP01.fromFile(trailing));

        Path negCapacity = tempDir.resolve("neg_cap.in");
        Files.writeString(negCapacity, "1\n1 5 3\n-1\n", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> KP01.fromFile(negCapacity));
    }

    @Test
    void solveExactBeatsGreedyOnClassicCounterexample() {
        KP01 kp = new KP01(10,
                new Item(1, 60, 6),
                new Item(2, 50, 5),
                new Item(3, 50, 5));

        Solution greedy = kp.greedySolution();
        assertTrue(greedy.isValid(kp));
        assertEquals(60L, greedy.totalProfit());
        assertEquals(6L, greedy.totalWeight());

        Solution exact = kp.solveExact();
        assertTrue(exact.isValid(kp));
        assertEquals(100L, exact.totalProfit());
        assertEquals(10L, exact.totalWeight());
        assertEquals(Set.of(new Item(2, 50, 5), new Item(3, 50, 5)), Set.copyOf(exact.selectedItems()));
    }

    @Test
    void solveExactHandlesLargeLongCapacityViaBranchAndBoundWithoutOom() {
        long c = 1_000_000_000_000L;
        KP01 kp = new KP01(c,
                new Item(1, 600_000_000_000L, 600_000_000_000L),
                new Item(2, 500_000_000_000L, 500_000_000_000L),
                new Item(3, 500_000_000_000L, 500_000_000_000L),
                new Item(4, 100_000_000_000L, 200_000_000_000L));

        Solution greedy = kp.greedySolution();
        assertTrue(greedy.isValid(kp));
        assertEquals(700_000_000_000L, greedy.totalProfit());

        Solution exact = kp.solveExact();
        assertTrue(exact.isValid(kp));
        assertEquals(1_000_000_000_000L, exact.totalProfit());
        assertEquals(1_000_000_000_000L, exact.totalWeight());
    }

    @Test
    void ratioComparisonDoesNotOverflowLargeLongMultiplications() {
        long w1 = 3_000_000_000L;
        long p1 = 6_000_000_001L; // ratio slightly > 2
        long w2 = 3_000_000_000L;
        long p2 = 6_000_000_000L; // ratio == 2

        KP01 kp = new KP01(w1,
                new Item(1, p2, w2),
                new Item(2, p1, w1));

        Solution greedy = kp.greedySolution();
        assertEquals(p1, greedy.totalProfit());
        assertTrue(greedy.isValid(kp));
    }

    @Test
    void handlesZeroWeightAndAllTooHeavyItems() {
        KP01 zeroCapWithFreeProfit = new KP01(0,
                new Item(1, 42, 0),
                new Item(2, 100, 5),
                new Item(3, 0, 0));

        Solution exact = zeroCapWithFreeProfit.solveExact();
        assertTrue(exact.isValid(zeroCapWithFreeProfit));
        assertEquals(42L, exact.totalProfit());
        assertEquals(0L, exact.totalWeight());

        Solution greedy = zeroCapWithFreeProfit.greedySolution();
        assertTrue(greedy.isValid(zeroCapWithFreeProfit));
        assertEquals(42L, greedy.totalProfit());

        KP01 allHeavy = new KP01(3,
                new Item(1, 10, 4),
                new Item(2, 20, 5));
        Solution emptyExact = allHeavy.solveExact();
        assertTrue(emptyExact.isValid(allHeavy));
        assertEquals(0L, emptyExact.totalProfit());
        assertEquals(0L, emptyExact.totalWeight());
        assertTrue(emptyExact.selectedItems().isEmpty());
    }

    @Test
    void randomAndParallelSolversProduceValidFeasibleSolutions() {
        KP01 kp = KP01.fromFile(Paths.get("samples", "test.in"));

        Solution s1 = kp.generateRandomSolution(new Random(42L));
        Solution s2 = kp.generateRandomSolution(new Random(42L));
        assertEquals(s1, s2);
        assertTrue(s1.isValid(kp));

        Solution seqBest = kp.oneMillionRandom(1_000);
        assertNotNull(seqBest);
        assertTrue(seqBest.isValid(kp));
        assertTrue(seqBest.totalProfit() <= kp.solveExact().totalProfit());

        Solution parBest = kp.oneMillionParallel(1_000);
        assertNotNull(parBest);
        assertTrue(parBest.isValid(kp));
        assertTrue(parBest.totalProfit() <= kp.solveExact().totalProfit());

        assertThrows(IllegalArgumentException.class, () -> kp.oneMillionRandom(0));
        assertThrows(IllegalArgumentException.class, () -> kp.oneMillionParallel(-5));
    }

    @Test
    void solutionValidationDetectsTamperingAndInfeasibility() {
        Item i1 = new Item(1, 10, 6);
        Item i2 = new Item(2, 12, 6);
        KP01 kp = new KP01(10, i1, i2);

        Solution overweight = new Solution(List.of(i1, i2), 22, 12);
        assertFalse(overweight.isValid(kp));

        Solution duplicate = new Solution(List.of(i1, i1), 20, 12);
        assertFalse(duplicate.isValid(kp));

        Solution foreign = new Solution(List.of(new Item(99, 5, 5)), 5, 5);
        assertFalse(foreign.isValid(kp));

        Solution tamperedProfit = new Solution(List.of(i1), 999, 6);
        assertFalse(tamperedProfit.isValid(kp));

        assertThrows(IllegalArgumentException.class, () -> new Solution(List.of(), -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Solution(List.of(), 0, -1));
        assertThrows(NullPointerException.class, () -> new Solution(null, 0, 0));
    }
}
