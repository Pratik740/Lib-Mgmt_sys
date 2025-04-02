import cli.LibraryCLI;
import utils.config;

public class Main {
    public static void main(String[] args) {
        config.preConfiguration();

        System.out.println("Console will be cleared in 3 seconds...");

        try {
            Thread.sleep(3000); // Wait 3 seconds
        } catch (InterruptedException ignored) {}
        for (int i = 0; i < 100; i++) { System.out.println("\n"); }

        LibraryCLI.cliMaster();
        config.del();
    }
}
