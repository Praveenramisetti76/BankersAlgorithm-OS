import java.util.*;

/**
 * BankerMultithreadedTrace.java
 *
 * Multithreaded Banker's Algorithm that:
 *  - protects against race conditions (synchronized)
 *  - avoids deadlock by performing the Safety Algorithm before granting a request
 *  - prints Need matrix and a step-by-step safety trace for each request
 *  - prints final safe sequence (order in which processes finished)
 *
 * Usage: run and enter n, m, Available, Max, Allocation as prompted.
 */

public class BankerMultithreadedTrace {

    private static final String RESET = "\u001B[0m";
    private static final String BLUE  = "\u001B[34m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED   = "\u001B[31m";

    private final int n;              // number of processes
    private final int m;              // number of resource types
    private final int[] available;    // global available vector
    private final int[][] max;        // Max matrix
    private final int[][] allocation; // Allocation matrix
    private final List<Integer> completedSequence = Collections.synchronizedList(new ArrayList<>());

    // Constructor - copies references (we assume caller provided arrays)
    public BankerMultithreadedTrace(int n, int m, int[] available, int[][] max, int[][] allocation) {
        this.n = n;
        this.m = m;
        this.available = available;
        this.max = max;
        this.allocation = allocation;
    }

    // Compute current Need matrix = Max - Allocation
    private int[][] computeNeed() {
        int[][] need = new int[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                need[i][j] = max[i][j] - allocation[i][j];
        return need;
    }

    // Print Need matrix (current)
    private void printNeedMatrix(int[][] need) {
        System.out.println(BLUE + "\nNeed Matrix (= Max - Allocation):" + RESET);
        for (int i = 0; i < n; i++)
            System.out.println(" P" + i + ": " + Arrays.toString(need[i]));
        System.out.println();
    }

    // Synchronized request handler: prints step-by-step safety trace using temp copies
    public synchronized boolean requestResources(int pid, int[] request) {
        System.out.println(BLUE + "\nP" + pid + " requests: " + Arrays.toString(request) + RESET);

        int[][] need = computeNeed();

        // 1. Check request <= need
        for (int j = 0; j < m; j++) {
            if (request[j] > need[pid][j]) {
                System.out.println(RED + "Request denied: exceeds declared Need." + RESET);
                return false;
            }
        }

        // 2. Check request <= available
        for (int j = 0; j < m; j++) {
            if (request[j] > available[j]) {
                System.out.println(RED + "Request denied: not enough Available (must wait)." + RESET);
                return false;
            }
        }

        // 3. Create temporary state (pretend allocation) to run safety check and print step-by-step
        int[] tempAvailable = Arrays.copyOf(available, m);
        int[][] tempAllocation = new int[n][m];
        int[][] tempNeed = new int[n][m];

        // copy allocation and need into temps
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++) {
                tempAllocation[i][j] = allocation[i][j];
                tempNeed[i][j] = max[i][j] - tempAllocation[i][j];
            }

        // pretend allocate
        for (int j = 0; j < m; j++) {
            tempAvailable[j] -= request[j];
            tempAllocation[pid][j] += request[j];
            tempNeed[pid][j] -= request[j];
        }

        // print Need matrix of the temporary state
        System.out.println(BLUE + "=== Safety Check (pretend state) ===" + RESET);
        System.out.println(BLUE + "Pretend Available (Work start): " + Arrays.toString(tempAvailable) + RESET);
        printTempNeed(tempNeed);

        // Run the step-by-step safety trace on the temporary state
        SafetyTraceResult result = safetyTraceStepByStep(tempAvailable, tempAllocation, tempNeed);

        if (result.isSafe) {
            // commit the allocation to the real state
            for (int j = 0; j < m; j++) {
                available[j] -= request[j];
                allocation[pid][j] += request[j];
            }
            System.out.println(GREEN + "Request granted: system remains SAFE. Committing allocation." + RESET);
            return true;
        } else {
            System.out.println(RED + "Request denied: would lead to UNSAFE state. Rolling back pretend allocation." + RESET);
            return false;
        }
    }

    // Synchronized release handler (used by processes to release resources)
    public synchronized void releaseResources(int pid, int[] release) {
        System.out.println(BLUE + "P" + pid + " releases: " + Arrays.toString(release) + RESET);

        for (int j = 0; j < m; j++) {
            // guard: do not release more than allocated
            int rel = Math.min(release[j], allocation[pid][j]);
            allocation[pid][j] -= rel;
            available[j] += rel;
        }

        System.out.println(GREEN + "After release, Available = " + Arrays.toString(available) + RESET);
    }

    // If process has finished (need all zero), finalRelease will give back all allocation and mark process completed
    public synchronized void finalReleaseAndComplete(int pid) {
        int[] rel = new int[m];
        for (int j = 0; j < m; j++) {
            rel[j] = allocation[pid][j];         // release all currently allocated
            available[j] += allocation[pid][j];
            allocation[pid][j] = 0;
        }
        completedSequence.add(pid);
        System.out.println(GREEN + "P" + pid + " has completed. Released: " + Arrays.toString(rel) +
                           ". Available now: " + Arrays.toString(available) + RESET);
    }

    // Helper: print a temp need matrix
    private void printTempNeed(int[][] tempNeed) {
        System.out.println(BLUE + "Temp Need Matrix:" + RESET);
        for (int i = 0; i < n; i++)
            System.out.println(" P" + i + ": " + Arrays.toString(tempNeed[i]));
        System.out.println();
    }

    // Step-by-step Safety Algorithm on provided temp state (does not modify real state)
    private SafetyTraceResult safetyTraceStepByStep(int[] tempAvailable, int[][] tempAllocation, int[][] tempNeed) {
        int[] work = Arrays.copyOf(tempAvailable, m);
        boolean[] finish = new boolean[n];
        List<Integer> seq = new ArrayList<>();
        int step = 1;
        boolean progress;

        System.out.println(BLUE + "Starting step-by-step safety simulation..." + RESET);
        System.out.println(BLUE + "Initial Work: " + Arrays.toString(work) + RESET);
        System.out.println(BLUE + "Initial Finish: " + Arrays.toString(finish) + RESET);
        System.out.println();

        do {
            progress = false;
            for (int i = 0; i < n; i++) {
                if (!finish[i]) {
                    System.out.println("Checking P" + i + " => Need: " + Arrays.toString(tempNeed[i]) +
                                       " | Work: " + Arrays.toString(work));

                    boolean canFinish = true;
                    for (int j = 0; j < m; j++) {
                        if (tempNeed[i][j] > work[j]) {
                            canFinish = false;
                            break;
                        }
                    }

                    if (canFinish) {
                        System.out.println(GREEN + "Step " + step + ": P" + i + " can be satisfied (Need ≤ Work)." + RESET);
                        System.out.println(GREEN + "  Before completion, Work = " + Arrays.toString(work) + RESET);

                        // Simulate P_i finishing: Work += allocation[i]
                        for (int j = 0; j < m; j++)
                            work[j] += tempAllocation[i][j];

                        finish[i] = true;
                        seq.add(i);

                        System.out.println(GREEN + "  After P" + i + " finishes, Work = " + Arrays.toString(work) + RESET);
                        System.out.println(GREEN + "  Updated Finish: " + Arrays.toString(finish) + RESET);
                        System.out.println();

                        step++;
                        progress = true;
                    } else {
                        System.out.println(RED + "  P" + i + " cannot be satisfied now (Need > Work)." + RESET);
                    }
                }
            }
        } while (progress);

        // Determine if safe
        boolean allFinished = true;
        for (boolean f : finish)
            if (!f) { allFinished = false; break; }

        if (allFinished) {
            System.out.println(GREEN + "Simulation result: SAFE. Candidate Safe Sequence: " + seq + RESET);
        } else {
            System.out.print(RED + "Simulation result: UNSAFE. Unfinished: " + RESET);
            for (int i = 0; i < n; i++)
                if (!finish[i]) System.out.print("P" + i + " ");
            System.out.println();
        }

        return new SafetyTraceResult(allFinished, seq);
    }

    // Small DTO for safety trace results
    private static class SafetyTraceResult {
        final boolean isSafe;
        final List<Integer> safeSequence;
        SafetyTraceResult(boolean isSafe, List<Integer> seq) {
            this.isSafe = isSafe;
            this.safeSequence = seq;
        }
    }

    // ------------------------- Customer Thread -------------------------
    class Customer extends Thread {
        private final int id;
        private final Random rand = new Random();

        Customer(int id) { this.id = id; }

        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(rand.nextInt(1500) + 200); // think/wait

                    // compute current need
                    int[] need = computeNeedForPid(id);
                    boolean hasNeed = false;
                    for (int j = 0; j < m; j++) if (need[j] > 0) { hasNeed = true; break; }

                    if (!hasNeed) {
                        // nothing more needed => release all and complete
                        finalReleaseAndComplete(id);
                        break;
                    }

                    // build a request vector (random sub-portion of need)
                    int[] request = new int[m];
                    boolean nonZero = false;
                    for (int j = 0; j < m; j++) {
                        if (need[j] > 0) {
                            request[j] = rand.nextInt(need[j] + 1); // may be 0..need
                            if (request[j] > 0) nonZero = true;
                        } else {
                            request[j] = 0;
                        }
                    }

                    if (!nonZero) {
                        // generated all zeros (rare) -> try again next loop
                        continue;
                    }

                    // attempt to request
                    boolean granted = requestResources(id, request);

                    // if not granted, wait a bit longer before retry
                    if (!granted) Thread.sleep(rand.nextInt(1000) + 200);
                    else {
                        // if granted, optionally sleep then release some random portion of allocation
                        Thread.sleep(rand.nextInt(1200) + 200);
                        // compute current allocation snapshot for this pid
                        int[] allocSnapshot = new int[m];
                        synchronized (BankerMultithreadedTrace.this) {
                            for (int j = 0; j < m; j++) allocSnapshot[j] = allocation[id][j];
                        }
                        // release a random part of the allocation (could be zero)
                        int[] release = new int[m];
                        for (int j = 0; j < m; j++)
                            release[j] = (allocSnapshot[j] == 0) ? 0 : rand.nextInt(allocSnapshot[j] + 1);

                        if (Arrays.stream(release).sum() > 0) {
                            releaseResources(id, release);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Helper to compute the current need of this pid (reads shared allocation/max)
        private int[] computeNeedForPid(int pid) {
            int[] need = new int[m];
            synchronized (BankerMultithreadedTrace.this) {
                for (int j = 0; j < m; j++)
                    need[j] = max[pid][j] - allocation[pid][j];
            }
            return need;
        }
    }

    // ------------------------- Main / Run -------------------------
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of processes (n): ");
        int n = sc.nextInt();
        System.out.print("Enter number of resource types (m): ");
        int m = sc.nextInt();

        int[] available = new int[m];
        System.out.println("\nEnter Available vector (" + m + " ints):");
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

        BankerMultithreadedTrace banker = new BankerMultithreadedTrace(n, m, available, max, allocation);

        // Print initial matrices (Need included)
        System.out.println(BLUE + "\n=== Initial State ===" + RESET);
        System.out.println(BLUE + "Available: " + Arrays.toString(available) + RESET);
        System.out.println(BLUE + "Max Matrix:" + RESET);
        for (int i = 0; i < n; i++) System.out.println(" P" + i + ": " + Arrays.toString(max[i]));
        System.out.println(BLUE + "Allocation Matrix:" + RESET);
        for (int i = 0; i < n; i++) System.out.println(" P" + i + ": " + Arrays.toString(allocation[i]));
        banker.printNeedMatrix(banker.computeNeed());

        // Start threads (customers)
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Thread t = banker.new Customer(i);
            threads.add(t);
            t.start();
        }

        // wait for all threads to complete
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // Print final completed safe sequence
        System.out.println(BLUE + "\n================ FINAL SAFE SEQUENCE ================\n" + RESET);
        System.out.println(GREEN + "Processes finished in order: " + banker.completedSequence + RESET);
        System.out.println(BLUE + "=====================================================" + RESET);

        sc.close();
    }
}
