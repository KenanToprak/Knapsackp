package edu.estu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generate one million random integers and find the maximum of them.
 * Implements both sequential and parallel versions using synchronized blocks and atomic variables.
 */
public class OneMillionRandom {
    private static final int SAMPLE_COUNT = 1_000_000;

    static void printInts(int... args) {
        for (int a : args) {
            System.out.println(a);
        }
    }

    public static void main(String[] args) throws IOException {
        printInts(
                sequential(),
                sequentialAccum(),
                synch(),
                synchAccum(),
                atomic(),
                atomicAccum(),
                atomic2(),
                atomicAccum2()
        );

        if (args != null && args.length > 0) {
            Path root = Paths.get(args[0]);
            if (Files.isDirectory(root)) {
                try (Stream<Path> stream = Files.find(root, 5,
                        (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith(".in"))) {
                    stream.parallel().forEach(System.out::println);
                }
            } else {
                System.err.println("Provided directory does not exist or is not a directory: " + root);
            }
        }
    }

    public static int sequential() {
        Random rand = new Random();
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            int aRandomInt = rand.nextInt();
            if (aRandomInt > max) {
                max = aRandomInt;
            }
        }
        return max;
    }

    public static int sequentialAccum() {
        Random rand = new Random();
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            max = Math.max(max, rand.nextInt());
        }
        return max;
    }

    public static int atomic2() {
        AtomicReference<Integer> max = new AtomicReference<>(Integer.MIN_VALUE);

        IntStream.range(0, SAMPLE_COUNT).parallel().forEach(i -> {
            int candidate = ThreadLocalRandom.current().nextInt();
            max.getAndUpdate(z -> Math.max(z, candidate));
        });

        return max.get();
    }

    public static int atomicAccum2() {
        AtomicReference<Integer> max = new AtomicReference<>(Integer.MIN_VALUE);

        IntStream.range(0, SAMPLE_COUNT).parallel().forEach(i -> {
            max.accumulateAndGet(ThreadLocalRandom.current().nextInt(), Math::max);
        });

        return max.get();
    }

    public static int synch() {
        final Object lock = new Object();
        final int[] max = {Integer.MIN_VALUE};

        IntStream.range(0, SAMPLE_COUNT).parallel().forEach(i -> {
            int aRandomInt = ThreadLocalRandom.current().nextInt();
            synchronized (lock) {
                if (aRandomInt > max[0]) {
                    max[0] = aRandomInt;
                }
            }
        });

        return max[0];
    }

    public static int synchAccum() {
        final Object lock = new Object();
        final int[] max = {Integer.MIN_VALUE};

        IntStream.range(0, SAMPLE_COUNT).parallel().forEach(i -> {
            int aRandomInt = ThreadLocalRandom.current().nextInt();
            synchronized (lock) {
                max[0] = Math.max(max[0], aRandomInt);
            }
        });

        return max[0];
    }

    public static int atomic() {
        AtomicInteger max = new AtomicInteger(Integer.MIN_VALUE);

        IntStream.range(0, SAMPLE_COUNT).parallel().forEach(i -> {
            int candidate = ThreadLocalRandom.current().nextInt();
            max.updateAndGet(z -> Math.max(z, candidate));
        });

        return max.get();
    }

    public static int atomicAccum() {
        AtomicInteger max = new AtomicInteger(Integer.MIN_VALUE);

        IntStream.range(0, SAMPLE_COUNT).parallel().forEach(i -> {
            max.accumulateAndGet(ThreadLocalRandom.current().nextInt(), Math::max);
        });

        return max.get();
    }
}
