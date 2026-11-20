package edu.estu;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

/**
 * The 0-1 knapsack problem is a classical NP-hard optimization problem.
 * In this problem, one is given a knapsack with an integer capacity c and a set of n items,
 * which each have an integer profit p_i and an integer weight w_i.
 * The goal is to select a subset of items to put into the knapsack such
 * that the total value is maximized and the total weight does not
 * exceed the knapsack capacity.
 */
public class KP01 {
    private static final int DEFAULT_RANDOM_ITERATIONS = 1_000_000;
    private static final long MAX_DP_CAPACITY = 200_000L;
    private static final long MAX_DP_CELLS = 10_000_000L;

    private static final Comparator<Item> RATIO_DESCENDING = (a, b) -> {
        if (a.weight() == 0 && b.weight() == 0) {
            int pCmp = Long.compare(b.profit(), a.profit());
            return pCmp != 0 ? pCmp : Long.compare(a.id(), b.id());
        }
        if (a.weight() == 0) {
            return a.profit() > 0 ? -1 : 1;
        }
        if (b.weight() == 0) {
            return b.profit() > 0 ? 1 : -1;
        }
        BigInteger lhs = BigInteger.valueOf(b.profit()).multiply(BigInteger.valueOf(a.weight()));
        BigInteger rhs = BigInteger.valueOf(a.profit()).multiply(BigInteger.valueOf(b.weight()));
        int ratioCmp = lhs.compareTo(rhs);
        if (ratioCmp != 0) {
            return ratioCmp;
        }
        int profitCmp = Long.compare(b.profit(), a.profit());
        if (profitCmp != 0) {
            return profitCmp;
        }
        int weightCmp = Long.compare(a.weight(), b.weight());
        if (weightCmp != 0) {
            return weightCmp;
        }
        return Long.compare(a.id(), b.id());
    };

    private final long capacity;
    private final Set<Item> items;

    public KP01(long capacity, Item... items) {
        this(capacity, validateArray(items));
    }

    public KP01(long capacity, Set<Item> items) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Knapsack capacity must be non-negative, got: " + capacity);
        }
        Objects.requireNonNull(items, "items must not be null");
        Set<Item> copy = new LinkedHashSet<>(Math.max(items.size() * 2, 16));
        for (Item item : items) {
            copy.add(Objects.requireNonNull(item, "items must not contain null elements"));
        }
        this.capacity = capacity;
        this.items = Collections.unmodifiableSet(copy);
    }

    private static Set<Item> validateArray(Item[] items) {
        Objects.requireNonNull(items, "items must not be null");
        Set<Item> copy = new LinkedHashSet<>(Math.max(items.length * 2, 16));
        for (Item item : items) {
            copy.add(Objects.requireNonNull(item, "items must not contain null elements"));
        }
        return copy;
    }

    public long getCapacity() {
        return capacity;
    }

    public long capacity() {
        return capacity;
    }

    public Set<Item> getItems() {
        return items;
    }

    public Set<Item> items() {
        return items;
    }

    /**
     * Parses a 0-1 knapsack instance from a file in Jooken et al. format.
     *
     * @param path path to the input file
     * @return parsed immutable {@link KP01} instance
     */
    public static KP01 fromFile(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        try (InputStream input = Files.newInputStream(path);
             Scanner scanner = new Scanner(input, StandardCharsets.UTF_8)) {
            scanner.useLocale(Locale.ROOT);

            if (!scanner.hasNext()) {
                throw new IllegalArgumentException("Input file is empty: " + path);
            }
            if (!scanner.hasNextInt()) {
                throw new IllegalArgumentException(
                        "Invalid item count token in " + path + ": " + scanner.next());
            }

            int numberOfItems = scanner.nextInt();
            if (numberOfItems < 0) {
                throw new IllegalArgumentException(
                        "numberOfItems must be non-negative in " + path + ", got: " + numberOfItems);
            }

            Set<Item> parsedItems = new LinkedHashSet<>(Math.min(numberOfItems, 4096));
            for (int i = 0; i < numberOfItems; i++) {
                long id = nextLongToken(scanner, path, "id", i);
                long profit = nextLongToken(scanner, path, "profit", i);
                long weight = nextLongToken(scanner, path, "weight", i);
                parsedItems.add(new Item(id, profit, weight));
            }

            if (!scanner.hasNextLong()) {
                String found = scanner.hasNext() ? scanner.next() : "<EOF>";
                throw new IllegalArgumentException(
                        "Missing or invalid knapsack capacity in " + path + ", found: " + found);
            }
            long capacity = scanner.nextLong();

            if (scanner.hasNext()) {
                throw new IllegalArgumentException(
                        "Unexpected trailing token in " + path + ": " + scanner.next());
            }

            return new KP01(capacity, parsedItems);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read KP01 file: " + path, e);
        }
    }

    private static long nextLongToken(Scanner scanner, Path path, String field, int itemIndex) {
        if (!scanner.hasNextLong()) {
            String found = scanner.hasNext() ? scanner.next() : "<EOF>";
            throw new IllegalArgumentException(
                    "Missing or invalid " + field + " for item #" + itemIndex + " in " + path + ", found: " + found);
        }
        return scanner.nextLong();
    }

    /**
     * Returns {@code true} if all items fit simultaneously into the knapsack without exceeding capacity.
     * Overflow-safe for all non-negative {@code long} capacities and item weights.
     *
     * @return {@code true} when this instance is trivial, {@code false} otherwise
     */
    public boolean trivial() {
        long totalWeight = 0L;
        for (Item item : items) {
            long w = item.weight();
            if (w > capacity - totalWeight) {
                return false;
            }
            totalWeight += w;
        }
        return true;
    }

    /**
     * Alias for {@link #trivial()}.
     *
     * @return {@code true} when all items fit within capacity
     */
    public boolean isTrivial() {
        return trivial();
    }

    /**
     * Solves the 0-1 knapsack problem to proven optimality.
     * Uses exact 0-1 dynamic programming for bounded capacity/item counts and
     * iterative Branch-and-Bound with an overflow-safe fractional upper bound for large capacities.
     *
     * @return an optimal feasible {@link Solution}
     */
    public Solution solveExact() {
        if (items.isEmpty()) {
            return new Solution(List.of(), 0L, 0L);
        }
        if (trivial()) {
            long totalProfit = 0L;
            long totalWeight = 0L;
            for (Item item : items) {
                totalProfit = Math.addExact(totalProfit, item.profit());
                totalWeight += item.weight();
            }
            return new Solution(List.copyOf(items), totalProfit, totalWeight);
        }

        List<Item> zeroWeightOptimal = new ArrayList<>();
        long baseProfit = 0L;
        List<Item> candidates = new ArrayList<>();
        for (Item item : items) {
            if (item.weight() == 0) {
                if (item.profit() > 0) {
                    zeroWeightOptimal.add(item);
                    baseProfit = Math.addExact(baseProfit, item.profit());
                }
            } else if (item.weight() <= capacity) {
                candidates.add(item);
            }
        }

        if (candidates.isEmpty()) {
            return new Solution(zeroWeightOptimal, baseProfit, 0L);
        }

        int m = candidates.size();
        Solution candidateOpt;
        if (capacity <= MAX_DP_CAPACITY && (long) m * (capacity + 1) <= MAX_DP_CELLS) {
            candidateOpt = solveExactDp(candidates, (int) capacity);
        } else {
            candidateOpt = solveExactBranchAndBound(candidates);
        }

        List<Item> combined = new ArrayList<>(zeroWeightOptimal.size() + candidateOpt.selectedItems().size());
        combined.addAll(zeroWeightOptimal);
        combined.addAll(candidateOpt.selectedItems());
        long totalProfit = Math.addExact(baseProfit, candidateOpt.totalProfit());
        return new Solution(combined, totalProfit, candidateOpt.totalWeight());
    }

    private Solution solveExactDp(List<Item> candidates, int cap) {
        int m = candidates.size();
        long[] dp = new long[cap + 1];
        boolean[][] keep = new boolean[m][cap + 1];

        for (int i = 0; i < m; i++) {
            Item item = candidates.get(i);
            int wi = (int) item.weight();
            long pi = item.profit();
            for (int w = cap; w >= wi; w--) {
                long withItem = Math.addExact(dp[w - wi], pi);
                if (withItem > dp[w]) {
                    dp[w] = withItem;
                    keep[i][w] = true;
                }
            }
        }

        List<Item> selected = new ArrayList<>();
        int rem = cap;
        long totalWeight = 0L;
        for (int i = m - 1; i >= 0; i--) {
            if (keep[i][rem]) {
                Item item = candidates.get(i);
                selected.add(item);
                rem -= (int) item.weight();
                totalWeight += item.weight();
            }
        }
        Collections.reverse(selected);
        return new Solution(selected, dp[cap], totalWeight);
    }

    private Solution solveExactBranchAndBound(List<Item> candidates) {
        List<Item> sorted = new ArrayList<>(candidates);
        sorted.sort(RATIO_DESCENDING);
        int m = sorted.size();

        long[] weights = new long[m];
        long[] profits = new long[m];
        for (int i = 0; i < m; i++) {
            weights[i] = sorted.get(i).weight();
            profits[i] = sorted.get(i).profit();
        }

        // Seed primal bound with greedy solution over sorted candidates.
        boolean[] bestChosen = new boolean[m];
        long bestProfit = 0L;
        long bestWeight = 0L;
        long gWeight = 0L;
        long gProfit = 0L;
        for (int i = 0; i < m; i++) {
            if (weights[i] <= capacity - gWeight) {
                gWeight += weights[i];
                gProfit = Math.addExact(gProfit, profits[i]);
                bestChosen[i] = true;
            }
        }
        bestProfit = gProfit;
        bestWeight = gWeight;

        boolean[] chosen = new boolean[m];
        byte[] phase = new byte[m + 1];
        int d = 0;
        long curW = 0L;
        long curP = 0L;
        phase[0] = 0;

        while (d >= 0) {
            if (d == m) {
                if (curP > bestProfit) {
                    bestProfit = curP;
                    bestWeight = curW;
                    System.arraycopy(chosen, 0, bestChosen, 0, m);
                }
                d--;
                continue;
            }

            if (phase[d] == 0) {
                phase[d] = 1;
                if (weights[d] <= capacity - curW) {
                    long nextW = curW + weights[d];
                    long nextP = Math.addExact(curP, profits[d]);
                    if (nextP > bestProfit) {
                        bestProfit = nextP;
                        bestWeight = nextW;
                        chosen[d] = true;
                        System.arraycopy(chosen, 0, bestChosen, 0, d + 1);
                        Arrays.fill(bestChosen, d + 1, m, false);
                    }
                    BigInteger ub = fractionalUpperBound(d + 1, nextW, nextP, m, weights, profits, capacity);
                    if (ub.compareTo(BigInteger.valueOf(bestProfit)) > 0) {
                        chosen[d] = true;
                        curW = nextW;
                        curP = nextP;
                        d++;
                        phase[d] = 0;
                    } else {
                        chosen[d] = false;
                    }
                }
            } else if (phase[d] == 1) {
                if (chosen[d]) {
                    chosen[d] = false;
                    curW -= weights[d];
                    curP -= profits[d];
                }
                phase[d] = 2;
                BigInteger ub = fractionalUpperBound(d + 1, curW, curP, m, weights, profits, capacity);
                if (ub.compareTo(BigInteger.valueOf(bestProfit)) > 0) {
                    d++;
                    phase[d] = 0;
                }
            } else {
                d--;
            }
        }

        List<Item> selected = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            if (bestChosen[i]) {
                selected.add(sorted.get(i));
            }
        }
        return new Solution(selected, bestProfit, bestWeight);
    }

    private static BigInteger fractionalUpperBound(
            int idx, long curW, long curP, int m, long[] weights, long[] profits, long capacity) {
        BigInteger bound = BigInteger.valueOf(curP);
        long rem = capacity - curW;
        for (int j = idx; j < m && rem > 0; j++) {
            long wj = weights[j];
            if (wj <= rem) {
                bound = bound.add(BigInteger.valueOf(profits[j]));
                rem -= wj;
            } else {
                bound = bound.add(
                        BigInteger.valueOf(profits[j])
                                .multiply(BigInteger.valueOf(rem))
                                .divide(BigInteger.valueOf(wj))
                );
                break;
            }
        }
        return bound;
    }

    /**
     * Phase 3(ii): Deterministic greedy heuristic sorting items descending by profit/weight ratio.
     *
     * @return feasible greedy {@link Solution}
     */
    public Solution greedySolution() {
        List<Item> sorted = new ArrayList<>(items);
        sorted.sort(RATIO_DESCENDING);

        List<Item> selected = new ArrayList<>();
        long totalProfit = 0L;
        long totalWeight = 0L;
        for (Item item : sorted) {
            if (item.weight() <= capacity - totalWeight) {
                selected.add(item);
                totalWeight += item.weight();
                totalProfit = Math.addExact(totalProfit, item.profit());
            }
        }
        return new Solution(selected, totalProfit, totalWeight);
    }

    /**
     * Phase 3(i): Generates a feasible random solution using {@link ThreadLocalRandom}.
     *
     * @return feasible random {@link Solution}
     */
    public Solution generateRandomSolution() {
        return generateRandomSolution(ThreadLocalRandom.current());
    }

    /**
     * Phase 3(i): Generates a feasible random solution using the supplied {@link RandomGenerator}.
     *
     * @param rng random number generator
     * @return feasible random {@link Solution}
     */
    public Solution generateRandomSolution(RandomGenerator rng) {
        Objects.requireNonNull(rng, "rng must not be null");
        Item[] pool = items.toArray(Item[]::new);
        return generateRandomSolutionFromArray(pool, rng);
    }

    private Solution generateRandomSolutionFromArray(Item[] pool, RandomGenerator rng) {
        Item[] shuffled = pool.clone();
        for (int i = shuffled.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Item tmp = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = tmp;
        }

        List<Item> selected = new ArrayList<>();
        long totalProfit = 0L;
        long totalWeight = 0L;
        for (Item item : shuffled) {
            if (item.weight() <= capacity - totalWeight && rng.nextBoolean()) {
                selected.add(item);
                totalWeight += item.weight();
                totalProfit = Math.addExact(totalProfit, item.profit());
            }
        }
        return new Solution(selected, totalProfit, totalWeight);
    }

    /**
     * Phase 4: Evaluates 1,000,000 random feasible solutions sequentially without storing all of them,
     * returning the one with the highest profit.
     *
     * @return best feasible {@link Solution} found
     */
    public Solution oneMillionRandom() {
        return oneMillionRandom(DEFAULT_RANDOM_ITERATIONS);
    }

    /**
     * Phase 4: Evaluates {@code iterations} random feasible solutions sequentially without storing all of them.
     *
     * @param iterations number of random solutions to sample (must be &gt; 0)
     * @return best feasible {@link Solution} found
     */
    public Solution oneMillionRandom(int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive, got: " + iterations);
        }
        Item[] pool = items.toArray(Item[]::new);
        RandomGenerator rng = ThreadLocalRandom.current();
        Solution best = null;
        for (int i = 0; i < iterations; i++) {
            Solution candidate = generateRandomSolutionFromArray(pool, rng);
            if (best == null || candidate.totalProfit() > best.totalProfit()) {
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Phase 5: Evaluates 1,000,000 random feasible solutions in parallel using {@link ThreadLocalRandom}
     * and {@link AtomicReference}, returning the one with the highest profit.
     *
     * @return best feasible {@link Solution} found
     */
    public Solution oneMillionParallel() {
        return oneMillionParallel(DEFAULT_RANDOM_ITERATIONS);
    }

    /**
     * Phase 5: Evaluates {@code iterations} random feasible solutions in parallel using {@link ThreadLocalRandom}
     * and {@link AtomicReference}, returning the one with the highest profit.
     *
     * @param iterations number of random solutions to sample (must be &gt; 0)
     * @return best feasible {@link Solution} found
     */
    public Solution oneMillionParallel(int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive, got: " + iterations);
        }
        Item[] pool = items.toArray(Item[]::new);
        AtomicReference<Solution> bestRef = new AtomicReference<>();

        IntStream.range(0, iterations).parallel().forEach(i -> {
            Solution candidate = generateRandomSolutionFromArray(pool, ThreadLocalRandom.current());
            bestRef.accumulateAndGet(candidate, (current, next) ->
                    (current == null || next.totalProfit() > current.totalProfit()) ? next : current);
        });

        return bestRef.get();
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "KP01{capacity=%d, # items=%d}",
                capacity,
                items.size()
        );
    }
}
