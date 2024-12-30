package edu.estu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App {
   
    public static void main(String[] testFilePaths) {
        if (testFilePaths.length < 1) {
            System.out.println("Usage: java -cp target/Knapsackp-1.0-SNAPSHOT.jar edu.estu.App <test_file1> [test_file2 ...]");
            System.exit(1);
        }

        for (String testFilePath : testFilePaths) {
            Path path = Paths.get(testFilePath);
            
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                System.err.println("Test file not found: " + testFilePath);
                continue;
            }

            System.out.println("\nProcessing instance file: " + testFilePath);
            KP01 problem = KP01.fromFile(path);
            problem.compareAndPrintStats();
        }
    }
}
