// import java.util.*;

// /**
//  * BankersAlgorithmMultithreaded.java
//  *
//  * Multithreaded simulation of the Banker's Algorithm.
//  * Each customer runs as a thread, making random requests and releases.
//  * Banker grants requests only if they keep the system in a safe state.
//  *
//  * Demonstrates:
//  *  (1) Multithreading
//  *  (2) Preventing Race Conditions
//  *  (3) Deadlock Avoidance (via Safe State Check)
//  */

// public class BankersAlgorithmMultithreaded {

//     private static final String RESET = "\u001B[0m";
//     private static final String BLUE  = "\u001B[34m";
//     private static final String GREEN = "\u001B[32m";
//     private static final String RED   = "\u001B[31m";

//     private final int n; // processes
//     private final int m; // resource types

//     private final int[] available;
//     private final int[][] max;
//     private final int[][] allocation;
//     private final int[][] need;

//     public BankersAlgorithmMultithreaded(int n, int m, int[] available, int[][] max, int[][] allocation) {
//         this.n = n;
//         this.m = m;
//         this.available = available;
//         this.max = max;
//         this.allocation = allocation;
//         this.need = new int[n][m];

//         // Compute Need matrix
//         for (int i = 0; i < n; i++)
//             for (int j = 0; j < m; j++)
//                 need[i][j] = max[i][j] - allocation[i][j];
//     }

//     // -------------------- REQUEST HANDLER --------------------
//     public synchronized boolean requestResources(int customerID, int[] request) {
//         System.out.println(BLUE + "\nCustomer P" + customerID + " making request: " + Arrays.toString(request) + RESET);

//         // 1. Check if request <= need
//         for (int j = 0; j < m; j++) {
//             if (request[j] > need[customerID][j]) {
//                 System.out.println(RED + "Request exceeds need. Denied." + RESET);
//                 return false;
//             }
//         }

//         // 2. Check if request <= available
//         for (int j = 0; j < m; j++) {
//             if (request[j] > available[j]) {
//                 System.out.println(RED + "Not enough resources available. Customer must wait." + RESET);
//                 return false;
//             }
//         }

//         // 3. Pretend to allocate temporarily
//         for (int j = 0; j < m; j++) {
//             available[j] -= request[j];
//             allocation[customerID][j] += request[j];
//             need[customerID][j] -= request[j];
//         }

//         // 4. Check if safe
//         boolean safe = checkSafeState();

//         if (safe) {
//             System.out.println(GREEN + "Request granted. System remains in SAFE state." + RESET);
//             return true;
//         } else {
//             // Rollback
//             for (int j = 0; j < m; j++) {
//                 available[j] += request[j];
//                 allocation[customerID][j] -= request[j];
//                 need[customerID][j] += request[j];
//             }
//             System.out.println(RED + "Request would lead to UNSAFE state. Denied." + RESET);
//             return false;
//         }
//     }

//     // -------------------- RELEASE HANDLER --------------------
//     public synchronized void releaseResources(int customerID, int[] release) {
//         System.out.println(BLUE + "Customer P" + customerID + " releasing: " + Arrays.toString(release) + RESET);

//         for (int j = 0; j < m; j++) {
//             allocation[customerID][j] -= release[j];
//             available[j] += release[j];
//             need[customerID][j] += release[j];
//         }

//         System.out.println(GREEN + "Resources released successfully. Available now: "
//                 + Arrays.toString(available) + RESET);
//     }

//     // -------------------- SAFETY CHECK --------------------
//     private boolean checkSafeState() {
//         int[] work = Arrays.copyOf(available, m);
//         boolean[] finish = new boolean[n];
//         List<Integer> safeSequence = new ArrayList<>();

//         boolean progress = true;
//         while (progress) {
//             progress = false;
//             for (int i = 0; i < n; i++) {
//                 if (!finish[i]) {
//                     boolean canFinish = true;
//                     for (int j = 0; j < m; j++) {
//                         if (need[i][j] > work[j]) {
//                             canFinish = false;
//                             break;
//                         }
//                     }

//                     if (canFinish) {
//                         for (int j = 0; j < m; j++) {
//                             work[j] += allocation[i][j];
//                         }
//                         finish[i] = true;
//                         safeSequence.add(i);
//                         progress = true;
//                     }
//                 }
//             }
//         }

//         for (boolean f : finish)
//             if (!f) return false;

//         System.out.println(GREEN + "Safe sequence: " + safeSequence + RESET);
//         return true;
//     }

//     // -------------------- CUSTOMER THREAD CLASS --------------------
//     class Customer extends Thread {
//         private final int id;
//         private final Random rand = new Random();

//         public Customer(int id) {
//             this.id = id;
//         }

//         @Override
//         public void run() {
//             try {
//                 while (true) {
//                     Thread.sleep(rand.nextInt(2000) + 500); // simulate thinking/waiting

//                     int[] req = new int[m];
//                     boolean hasNeed = false;
//                     for (int j = 0; j < m; j++) {
//                         if (need[id][j] > 0) {
//                             req[j] = rand.nextInt(need[id][j] + 1);
//                             if (req[j] > 0) hasNeed = true;
//                         }
//                     }

//                     if (!hasNeed) break; // process done

//                     requestResources(id, req);

//                     Thread.sleep(rand.nextInt(1500) + 500);

//                     // Release some resources randomly
//                     int[] rel = new int[m];
//                     for (int j = 0; j < m; j++) {
//                         rel[j] = rand.nextInt(allocation[id][j] + 1);
//                     }
//                     releaseResources(id, rel);
//                 }

//                 System.out.println(GREEN + "Customer P" + id + " has completed." + RESET);

//             } catch (InterruptedException e) {
//                 Thread.currentThread().interrupt();
//             }
//         }
//     }

//     // -------------------- MAIN METHOD --------------------
//     public static void main(String[] args) {
//         Scanner sc = new Scanner(System.in);

//         System.out.print("Enter number of processes (n): ");
//         int n = sc.nextInt();
//         System.out.print("Enter number of resource types (m): ");
//         int m = sc.nextInt();

//         int[] available = new int[m];
//         System.out.println("Enter Available vector:");
//         for (int j = 0; j < m; j++) available[j] = sc.nextInt();

//         int[][] max = new int[n][m];
//         System.out.println("Enter Max matrix:");
//         for (int i = 0; i < n; i++) {
//             System.out.print("P" + i + " Max: ");
//             for (int j = 0; j < m; j++) max[i][j] = sc.nextInt();
//         }

//         int[][] allocation = new int[n][m];
//         System.out.println("Enter Allocation matrix:");
//         for (int i = 0; i < n; i++) {
//             System.out.print("P" + i + " Allocation: ");
//             for (int j = 0; j < m; j++) allocation[i][j] = sc.nextInt();
//         }

//         BankersAlgorithmMultithreaded banker = new BankersAlgorithmMultithreaded(n, m, available, max, allocation);

//         // Start each process as a separate thread
//         for (int i = 0; i < n; i++) {
//             banker.new Customer(i).start();
//         }

//         sc.close();
//     }
// }


import java.util.*;

/**
 * BankersAlgorithmMultithreaded.java
 *
 * Multithreaded simulation of the Banker's Algorithm with SAFE SEQUENCE output.
 * Each process runs as a thread making random requests and releases.
 * Demonstrates:
 *  (1) Multithreading
 *  (2) Race condition prevention via synchronization
 *  (3) Deadlock avoidance using safe-state checking
 */

public class BankersAlgorithmMultithreaded {

    private static final String RESET = "\u001B[0m";
    private static final String BLUE  = "\u001B[34m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED   = "\u001B[31m";

    private final int n, m;
    private final int[] available;
    private final int[][] max, allocation, need;
    private final List<Integer> safeSequence = Collections.synchronizedList(new ArrayList<>());

    public BankersAlgorithmMultithreaded(int n, int m, int[] available, int[][] max, int[][] allocation) {
        this.n = n;
        this.m = m;
        this.available = available;
        this.max = max;
        this.allocation = allocation;
        this.need = new int[n][m];

        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                need[i][j] = max[i][j] - allocation[i][j];
    }

    // -------------------- REQUEST --------------------
    public synchronized boolean requestResources(int pid, int[] request) {
        System.out.println(BLUE + "\nP" + pid + " requests " + Arrays.toString(request) + RESET);

        for (int j = 0; j < m; j++)
            if (request[j] > need[pid][j]) {
                System.out.println(RED + "Request exceeds need. Denied." + RESET);
                return false;
            }

        for (int j = 0; j < m; j++)
            if (request[j] > available[j]) {
                System.out.println(RED + "Not enough available. P" + pid + " must wait." + RESET);
                return false;
            }

        // Pretend allocation
        for (int j = 0; j < m; j++) {
            available[j] -= request[j];
            allocation[pid][j] += request[j];
            need[pid][j] -= request[j];
        }

        if (isSafeState()) {
            System.out.println(GREEN + "Request granted. System remains SAFE." + RESET);
            return true;
        } else {
            // Rollback
            for (int j = 0; j < m; j++) {
                available[j] += request[j];
                allocation[pid][j] -= request[j];
                need[pid][j] += request[j];
            }
            System.out.println(RED + "Request leads to UNSAFE state. Rolled back." + RESET);
            return false;
        }
    }

    // -------------------- RELEASE --------------------
    public synchronized void releaseResources(int pid, int[] release) {
        for (int j = 0; j < m; j++) {
            allocation[pid][j] -= release[j];
            available[j] += release[j];
            need[pid][j] += release[j];
        }
        System.out.println(GREEN + "P" + pid + " released " + Arrays.toString(release) +
                           ". Available: " + Arrays.toString(available) + RESET);
    }

    // -------------------- SAFETY CHECK --------------------
    private boolean isSafeState() {
        int[] work = Arrays.copyOf(available, m);
        boolean[] finish = new boolean[n];
        List<Integer> seq = new ArrayList<>();

        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < n; i++) {
                if (!finish[i]) {
                    boolean canFinish = true;
                    for (int j = 0; j < m; j++)
                        if (need[i][j] > work[j]) canFinish = false;

                    if (canFinish) {
                        for (int j = 0; j < m; j++)
                            work[j] += allocation[i][j];
                        finish[i] = true;
                        seq.add(i);
                        progress = true;
                    }
                }
            }
        }

        for (boolean f : finish)
            if (!f) return false;

        System.out.println(GREEN + "Safe Sequence (so far): " + seq + RESET);
        return true;
    }

    // -------------------- CUSTOMER THREAD --------------------
    class Customer extends Thread {
        private final int id;
        private final Random rand = new Random();

        public Customer(int id) { this.id = id; }

        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(rand.nextInt(2000) + 500);

                    int[] req = new int[m];
                    boolean needLeft = false;
                    for (int j = 0; j < m; j++) {
                        if (need[id][j] > 0) {
                            req[j] = rand.nextInt(need[id][j] + 1);
                            if (req[j] > 0) needLeft = true;
                        }
                    }

                    if (!needLeft) break;

                    requestResources(id, req);

                    Thread.sleep(rand.nextInt(1500) + 500);

                    int[] rel = new int[m];
                    for (int j = 0; j < m; j++)
                        rel[j] = rand.nextInt(allocation[id][j] + 1);

                    releaseResources(id, rel);
                }

                synchronized (safeSequence) {
                    safeSequence.add(id);
                }

                System.out.println(GREEN + "✅ P" + id + " has completed execution." + RESET);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // -------------------- MAIN --------------------
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of processes (n): ");
        int n = sc.nextInt();
        System.out.print("Enter number of resource types (m): ");
        int m = sc.nextInt();

        int[] available = new int[m];
        System.out.println("Enter Available vector:");
        for (int j = 0; j < m; j++) available[j] = sc.nextInt();

        int[][] max = new int[n][m];
        System.out.println("Enter Max matrix:");
        for (int i = 0; i < n; i++) {
            System.out.print("P" + i + " Max: ");
            for (int j = 0; j < m; j++) max[i][j] = sc.nextInt();
        }

        int[][] allocation = new int[n][m];
        System.out.println("Enter Allocation matrix:");
        for (int i = 0; i < n; i++) {
            System.out.print("P" + i + " Allocation: ");
            for (int j = 0; j < m; j++) allocation[i][j] = sc.nextInt();
        }

        BankersAlgorithmMultithreaded banker =
            new BankersAlgorithmMultithreaded(n, m, available, max, allocation);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Thread t = banker.new Customer(i);
            threads.add(t);
            t.start();
        }

        // Wait for all customers to finish
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        System.out.println("\n" + BLUE + "==============================" + RESET);
        System.out.println(GREEN + "Final SAFE SEQUENCE: " + banker.safeSequence + RESET);
        System.out.println(BLUE + "==============================" + RESET);

        sc.close();
    }
}
