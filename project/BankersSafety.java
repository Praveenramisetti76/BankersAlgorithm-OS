import java.util.*;

/**
 * BankersSafety.java
 *
 * Reads Available, Allocation, and Max matrices.
 * Computes Need = Max - Allocation.
 * Runs the Banker's safety algorithm and prints a step-by-step trace.
 *
 * Color Legend:
 *  - INFO/STATE  = BLUE
 *  - PROGRESS    = GREEN
 *  - UNSAFE/ERR  = RED
 */
public class BankersSafety {

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String BLUE  = "\u001B[34m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED   = "\u001B[31m";

    // -------------------- MAIN METHOD --------------------
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of processes (n): ");
        int n = sc.nextInt();
        System.out.print("Enter number of resource types (m): ");
        int m = sc.nextInt();

        int[] available = new int[m];
        System.out.println("\nEnter Available vector (" + m + " integers):");
        for (int j = 0; j < m; j++) available[j] = sc.nextInt();

        int[][] allocation = new int[n][m];
        System.out.println("\nEnter Allocation matrix (n rows, each " + m + " ints):");
        for (int i = 0; i < n; i++) {
            System.out.print("P" + i + " Allocation: ");
            for (int j = 0; j < m; j++) allocation[i][j] = sc.nextInt();
        }

        int[][] max = new int[n][m];
        System.out.println("\nEnter Max (maximum demand) matrix (n rows, each " + m + " ints):");
        for (int i = 0; i < n; i++) {
            System.out.print("P" + i + " Max: ");
            for (int j = 0; j < m; j++) max[i][j] = sc.nextInt();
        }

        // Compute Need = Max - Allocation
        int[][] need = new int[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                need[i][j] = max[i][j] - allocation[i][j];

        // Print initial state
        printState(n, m, available, allocation, max, need);

        // Run the Safety Algorithm
        runSafetyTrace(n, m, available, allocation, need);

        sc.close();
    }

    // -------------------- PRINT INITIAL STATE --------------------
    private static void printState(int n, int m, int[] available, int[][] allocation, int[][] max, int[][] need) {
        System.out.println(BLUE + "\nInitial State:" + RESET);

        System.out.println(BLUE + "Available: " + Arrays.toString(available) + RESET);

        System.out.println(BLUE + "Allocation Matrix:" + RESET);
        for (int i = 0; i < n; i++)
            System.out.println(" P" + i + ": " + Arrays.toString(allocation[i]));

        System.out.println(BLUE + "Max Matrix:" + RESET);
        for (int i = 0; i < n; i++)
            System.out.println(" P" + i + ": " + Arrays.toString(max[i]));

        System.out.println(BLUE + "Need Matrix (= Max - Allocation):" + RESET);
        for (int i = 0; i < n; i++)
            System.out.println(" P" + i + ": " + Arrays.toString(need[i]));

        System.out.println();
    }

    // -------------------- SAFETY ALGORITHM TRACE --------------------
    private static void runSafetyTrace(int n, int m, int[] available, int[][] allocation, int[][] need) {
        int[] work = Arrays.copyOf(available, m);
        boolean[] finish = new boolean[n];
        List<Integer> safeSequence = new ArrayList<>();

        System.out.println(BLUE + "Starting Safety Algorithm Trace..." + RESET);
        printWork(work);

        boolean progressMade = true;
        int step = 0;

        while (progressMade) {
            progressMade = false;

            for (int i = 0; i < n; i++) {
                if (!finish[i]) {
                    boolean canFinish = true;
                    for (int j = 0; j < m; j++) {
                        if (need[i][j] > work[j]) {
                            canFinish = false;
                            break;
                        }
                    }

                    if (canFinish) {
                        System.out.printf(GREEN + "Step %d: P%d can finish (Need <= Work)%n" + RESET, step++, i);
                        System.out.printf(GREEN + "  Before finishing, Work = %s%n" + RESET, Arrays.toString(work));

                        for (int j = 0; j < m; j++)
                            work[j] += allocation[i][j];

                        System.out.printf(GREEN + "  After P%d finishes, Work = %s%n" + RESET, i, Arrays.toString(work));

                        finish[i] = true;
                        safeSequence.add(i);
                        progressMade = true;

                        printFinishVector(finish);
                        System.out.println();
                    }
                }
            }
        }

        // -------------------- FINAL RESULT --------------------
        boolean allFinished = true;
        for (boolean f : finish)
            if (!f) allFinished = false;

        if (allFinished) {
            System.out.println(GREEN + "✅ System is in a SAFE state!" + RESET);
            System.out.print(GREEN + "Safe sequence: " + RESET);
            for (int i = 0; i < safeSequence.size(); i++) {
                System.out.print("P" + safeSequence.get(i));
                if (i < safeSequence.size() - 1) System.out.print(" -> ");
            }
            System.out.println();
        } else {
            System.out.println(RED + "❌ System is in an UNSAFE state. No safe sequence exists." + RESET);
            System.out.print(RED + "Could not finish: " + RESET);
            for (int i = 0; i < n; i++)
                if (!finish[i]) System.out.print("P" + i + " ");
            System.out.println();
        }
    }

    // -------------------- HELPERS --------------------
    private static void printWork(int[] work) {
        System.out.println(BLUE + "Initial Work = Available = " + Arrays.toString(work) + RESET);
    }

    private static void printFinishVector(boolean[] finish) {
        System.out.print(BLUE + "  Finish: [");
        for (int i = 0; i < finish.length; i++) {
            System.out.print(finish[i] ? "T" : "F");
            if (i != finish.length - 1) System.out.print(", ");
        }
        System.out.println("]" + RESET);
    }
}
