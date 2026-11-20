# Knapsackp — 0-1 Knapsack Problem Solver & Parallel Randomization

> **Fork & Remediation Note:** This repository (`KenanToprak/Knapsackp`) is forked from an Eskişehir Technical University (ESTU) Java/Maven assignment (`salihberke/Knapsackp`). Upstream `main` is preserved untouched; audit fixes, complete solvers, and JUnit 6 tests live on branch `audit/fixes`.

## Problem Description

The [0-1 knapsack](https://www.sciencedirect.com/science/article/pii/S0925231223007531) problem (`KP01`) is a classical NP-hard combinatorial optimization problem. Given a knapsack of integer capacity $c$ and a set of $n$ items—each with non-negative integer profit $p_i$ and weight $w_i$—the goal is to select a subset of items maximizing total profit such that total weight does not exceed $c$.

### Input Format ([Jooken, Leyman, and De Causmaecker, 2022](https://www.sciencedirect.com/science/article/pii/S037722172101016X))

- Line 1: number of items $n$
- Next $n$ lines: `<id> <profit> <weight>` (64-bit `long` integers to support large instances)
- Final line: knapsack capacity $c$

Example (`samples/test.in`):

```text
3
1 3 8
2 2 8
3 9 1
10
```

## Implemented Components

- **`edu.estu.Item`**: Immutable Java 21 `record Item(long id, long profit, long weight)` validating non-negative profit and weight.
- **`edu.estu.Solution`**: Immutable `record Solution(List<Item> selectedItems, long totalProfit, long totalWeight)` with overflow-safe totals and `isValid(KP01)` feasibility checking.
- **`edu.estu.KP01`**:
  - **Phase 1 — Model & Parser**: `KP01.fromFile(Path)` using UTF-8, `Locale.ROOT`, strict validation, and defensive unmodifiable copying (`Set<Item>`).
  - **Phase 2 — Trivial Case Check**: Overflow-safe `KP01.trivial()` / `isTrivial()` checking whether all items fit simultaneously.
  - **Phase 3 — Random & Greedy Solvers**:
    - `generateRandomSolution()` / `generateRandomSolution(RandomGenerator)` for unbiased feasible random subsets.
    - `greedySolution()` sorting descending by `profit / weight` using exact `BigInteger` cross-multiplication (no `long` multiplication overflow).
  - **Exact 0-1 Solver**: `solveExact()` combining 0-1 dynamic programming (for bounded capacities) and iterative Branch-and-Bound with an overflow-safe fractional upper bound (for large `long` capacities).
  - **Phase 4 & 5 — 1M Random Search (Sequential & Parallel)**: `oneMillionRandom()` and `oneMillionParallel()` using `ThreadLocalRandom` and `AtomicReference` without retaining all candidate solutions in memory.
- **`edu.estu.App`**: CLI runner accepting an optional instance path (`args[0]`, defaulting to `samples/test.in`).
- **`edu.estu.OneMillionRandom`**: Sequential, synchronized, and atomic (`AtomicInteger` / `AtomicReference`) parallel maximum-integer demonstrations with optional directory scan (`args[0]`).

## Prerequisites

- **Java**: JDK 21 or newer (`pom.xml` targets Java 21 via `<maven.compiler.release>21</maven.compiler.release>`)
- **Maven**: Apache Maven 3.9+ (or a locally cached Maven distribution)

## Build, Test & Run

### Using Maven

```powershell
# Compile and run all JUnit 6 unit & integration tests
mvn clean test

# Build executable JAR
mvn package

# Run CLI with default sample (samples/test.in)
java -jar target/Knapsackp-1.0-SNAPSHOT.jar

# Run CLI with a custom instance file
java -jar target/Knapsackp-1.0-SNAPSHOT.jar samples/test.in

# Run OneMillionRandom benchmark demo (optional directory argument to list .in files)
java -cp target/classes edu.estu.OneMillionRandom
```

### Compiling Directly with `javac` (Main Sources)

```powershell
javac --release 21 -encoding UTF-8 -d target/classes src/main/java/edu/estu/*.java
java -cp target/classes edu.estu.App samples/test.in
java -cp target/classes edu.estu.OneMillionRandom
```
