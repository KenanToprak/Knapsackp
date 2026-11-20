package edu.estu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OneMillionRandomTest {

    @Test
    void allRandomMaxVariantsReturnValidIntegers() {
        assertTrue(OneMillionRandom.sequential() >= Integer.MIN_VALUE);
        assertTrue(OneMillionRandom.sequentialAccum() >= Integer.MIN_VALUE);
        assertTrue(OneMillionRandom.atomic() >= Integer.MIN_VALUE);
        assertTrue(OneMillionRandom.atomicAccum() >= Integer.MIN_VALUE);
        assertTrue(OneMillionRandom.atomic2() >= Integer.MIN_VALUE);
        assertTrue(OneMillionRandom.atomicAccum2() >= Integer.MIN_VALUE);
        assertTrue(OneMillionRandom.synch() >= Integer.MIN_VALUE);
        assertTrue(OneMillionRandom.synchAccum() >= Integer.MIN_VALUE);
    }

    @Test
    void mainRunsCleanlyWithoutHardcodedPathAndWithOptionalDir(@TempDir Path tempDir) throws IOException {
        assertDoesNotThrow(() -> OneMillionRandom.main(new String[0]));

        Path sample = tempDir.resolve("sample.in");
        Files.writeString(sample, "0\n5\n", StandardCharsets.UTF_8);
        assertDoesNotThrow(() -> OneMillionRandom.main(new String[]{tempDir.toString()}));
    }
}
