import java.util.*;

public class BankersSafetyStepByStep {

    // ANSI color codes for clarity (optional)
    private static final String RESET = "\u001B[0m";
    private static final String BLUE  = "\u001B[34m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED   = "\u001B[31m";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of processes (n): ");
        int n = sc.nextInt();
        System.out.print("Enter number of resource types (m): ");
        int m = sc.nextInt();

        int[] available = new int[m];
        System.out.println("\nEnter Available vector:");
        for (int j = 0; j < m; j++) available[j] = sc.nextInt();

        int[][] max = new int[n][m];
        System.out.println("\nEnter Max matrix:");
        for (int i = 0; i < n; i++) {
            System.out.print("P" + i + " Max: ");
            for (int j = 0; j < m; j++) max[i][j] = sc.nextInt();
        }

        int[][] allocation = new int[n][m];
        System.out.println("\nEnter Allocation matrix:");
        for (int i = 0; i < n; i++) {
            System.out.print("P" + i + " Allocation: ");
            for (int j = 0; j < m; j++) allocation[i][j] = sc.nextInt();
        }

        // Compute Need matrix
        int[][] need = new int[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                need[i][j] = max[i][j] - allocation[i][j];

        // Print all matrices
        System.out.println(BLUE + "\n=== Initial State ===" + RESET);
        System.out.println(BLUE + "Available: " + Arrays.toString(available) + RESET);

        System.out.println(BLUE + "\nMax Matrix:" + RESET);
        for (int i = 0; i < n; i++)
            System.out.println("P" + i + ": " + Arrays.toString(max[i]));

        System.out.println(BLUE + "\nAllocation Matrix:" + RESET);
        for (int i = 0; i < n; i++)
            System.out.println("P" + i + ": " + Arrays.toString(allocation[i]));

        System.out.println(BLUE + "\nNeed Matrix (= Max - Allocation):" + RESET);
        for (int i = 0; i < n; i++)
            System.out.println("P" + i + ": " + Arrays.toString(need[i]));

        // Run safety algorithm
        System.out.println(BLUE + "\n=== Running Safety Algorithm Step-by-Step ===" + RESET);
        findSafeSequence(n, m, available, allocation, need);
        sc.close();
    }

    private static void findSafeSequence(int n, int m, int[] available, int[][] allocation, int[][] need) {
        int[] work = Arrays.copyOf(available, m);
        boolean[] finish = new boolean[n];
        List<Integer> safeSequence = new ArrayList<>();

        System.out.println(BLUE + "\nInitial Work: " + Arrays.toString(work) + RESET);
        System.out.println(BLUE + "Initial Finish: " + Arrays.toString(finish) + RESET);
        System.out.println();

        int step = 1;
        boolean progress;

        do {
            progress = false;

            for (int i = 0; i < n; i++) {
                if (!finish[i]) {
                    boolean canAllocate = true;

                    // Check Need <= Work
                    System.out.println("Checking P" + i + " => Need: " + Arrays.toString(need[i]) +
                            " | Work: " + Arrays.toString(work));

                    for (int j = 0; j < m; j++) {
                        if (need[i][j] > work[j]) {
                            canAllocate = false;
                            break;
                        }
                    }

                    if (canAllocate) {
                        System.out.println(GREEN + "Step " + step + ": P" + i + " can be satisfied (Need ≤ Work)." + RESET);
                        System.out.println(GREEN + "Allocating resources to P" + i + "..." + RESET);
                        System.out.println(GREEN + "Before completion, Work = " + Arrays.toString(work) + RESET);

                        // Simulate process completion → release its allocation
                        for (int j = 0; j < m; j++)
                            work[j] += allocation[i][j];

                        finish[i] = true;
                        safeSequence.add(i);
                        System.out.println(GREEN + "After P" + i + " finishes, Work = " + Arrays.toString(work) + RESET);
                        System.out.println(GREEN + "Updated Finish: " + Arrays.toString(finish) + RESET);
                        System.out.println();

                        step++;
                        progress = true;
                    } else {
                        System.out.println(RED + "P" + i + " cannot be satisfied now (Need > Work)." + RESET);
                    }
                }
            }

        } while (progress);

        // Final Result
        boolean allFinished = true;
        for (boolean f : finish)
            if (!f) allFinished = false;

        System.out.println(BLUE + "======================================" + RESET);
        if (allFinished) {
            System.out.println(GREEN + "✅ System is in a SAFE state." + RESET);
            System.out.print(GREEN + "Safe Sequence: " + RESET);
            for (int i = 0; i < safeSequence.size(); i++) {
                System.out.print("P" + safeSequence.get(i));
                if (i < safeSequence.size() - 1) System.out.print(" → ");
            }
            System.out.println();
        } else {
            System.out.println(RED + "❌ System is in an UNSAFE state! No safe sequence found." + RESET);
            System.out.print(RED + "Unfinished processes: " + RESET);
            for (int i = 0; i < n; i++)
                if (!finish[i]) System.out.print("P" + i + " ");
            System.out.println();
        }
        System.out.println(BLUE + "======================================" + RESET);
    }
}
