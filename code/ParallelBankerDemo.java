import java.util.Random;
import java.util.Scanner;

class Banker {
    private int[][] max, alloc, need;
    private int[] available;
    private int n, m;

    public Banker(int[][] max, int[][] alloc, int[][] need, int[] available, int n, int m) {
        this.max = max;
        this.alloc = alloc;
        this.need = need;
        this.available = available;
        this.n = n;
        this.m = m;
    }

    public synchronized boolean requestResources(int pid, StringBuilder safeSeq) {

        int[] req = need[pid].clone();

        print("\nP" + pid + " requesting resources...");
        print("Available: " + vectorToString(available));
        print("P" + pid + " Need:      " + vectorToString(req));
        print("Comparing Need <= Available ...");

        for (int j = 0; j < m; j++) {
            if (req[j] > available[j]) {
                print("=> Request DENIED (Need > Available)");
                return false;
            }
        }

        for (int j = 0; j < m; j++) {
            available[j] -= req[j];
            alloc[pid][j] += req[j];
            need[pid][j] -= req[j];
        }

        if (isSafe()) {
            print("=> Request GRANTED (System remains SAFE)");

            // store sequence but DO NOT print yet
            safeSeq.append("P").append(pid).append(" ");

            return true;
        } else {
            for (int j = 0; j < m; j++) {
                available[j] += req[j];
                alloc[pid][j] -= req[j];
                need[pid][j] += req[j];
            }
            print("=> Request DENIED (Would cause UNSAFE STATE)");
            return false;
        }
    }

    private boolean isSafe() {
        int[] work = available.clone();
        boolean[] finish = new boolean[n];
        int completed = 0;

        while (completed < n) {
            boolean progress = false;
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
                        for (int j = 0; j < m; j++)
                            work[j] += alloc[i][j];
                        finish[i] = true;
                        completed++;
                        progress = true;
                    }
                }
            }
            if (!progress)
                return false;
        }
        return true;
    }

    public synchronized void releaseResources(int pid) {
        print("P" + pid + " releasing resources...");
        for (int j = 0; j < m; j++) {
            available[j] += alloc[pid][j];
            alloc[pid][j] = 0;
        }
        print("Available now: " + vectorToString(available));
    }

    private String vectorToString(int[] v) {
        StringBuilder sb = new StringBuilder("[ ");
        for (int x : v)
            sb.append(x).append(" ");
        sb.append("]");
        return sb.toString();
    }

    private synchronized void print(String s) {
        System.out.println(s);
    }
}

class ProcessThread extends Thread {
    private int pid;
    private Banker banker;
    private StringBuilder safeSeqRef;
    private Random rand = new Random();

    public ProcessThread(int pid, Banker banker, StringBuilder safeSeqRef) {
        this.pid = pid;
        this.banker = banker;
        this.safeSeqRef = safeSeqRef;
    }

    @Override
    public void run() {
        int attempts = 0;

        while (attempts < 25) {
            if (banker.requestResources(pid, safeSeqRef)) {
                System.out.println("P" + pid + " executing...");
                try {
                    Thread.sleep(600 + rand.nextInt(400));
                } catch (Exception e) {
                }
                banker.releaseResources(pid);
                System.out.println("P" + pid + " finished.");
                return;
            } else {
                System.out.println("P" + pid + " waiting and retrying...");
                try {
                    Thread.sleep(400 + rand.nextInt(300));
                } catch (Exception e) {
                }
            }
            attempts++;
        }

        System.out.println("\nSYSTEM UNSAFE! All Processes cannot be satisfied.");
        System.exit(0);
    }
}

public class ParallelBankerDemo {

    public static StringBuilder safeSeqRef = new StringBuilder();

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of processes: ");
        int n = sc.nextInt();
        if(n<=0){
            System.out.println("\nERROR: Number of processes must be positive!");
            System.out.println("Please re-run program with valid values.");
            System.exit(0); 
        }

        System.out.print("Enter number of resources: ");
        int m = sc.nextInt();
        if(m<=0){
            System.out.println("\nERROR: Number of resources must be positive!");
            System.out.println("Please re-run program with valid values.");
            System.exit(0); 
        }

        int[][] max = new int[n][m];
        int[][] alloc = new int[n][m];
        int[][] need = new int[n][m];
        int[] available = new int[m];

        System.out.println("\nEnter ALLOCATION matrix:");
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++) {
                alloc[i][j] = sc.nextInt();
                if(alloc[i][j]<0){
                    System.out.println("\nERROR: ALLOCATION values cannot be negative!");
                    System.out.println("Please re-run program with valid values.");
                    System.exit(0); 
                }
        }
            
        System.out.println("\nEnter MAX matrix:");
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++) {
                max[i][j] = sc.nextInt();
                if(max[i][j]<0){
                    System.out.println("\nERROR: MAX values cannot be negative!");
                    System.out.println("Please re-run program with valid values.");
                    System.exit(0); 
                }
            }

        System.out.println("\nChoose how to provide Available Resources:");
        System.out.println("1) Enter AVAILABLE vector directly");
        System.out.println("2) Enter TOTAL system resources (AVAILABLE will be computed)");

        int choice;
        do {
            System.out.print("Enter choice (1 or 2): ");
            choice = sc.nextInt();
        } while (choice != 1 && choice != 2);

        if (choice == 1) {

            System.out.println("\nEnter AVAILABLE vector:");
            for (int j = 0; j < m; j++) {
                available[j] = sc.nextInt();
                if(available[j]<0){
                    System.out.println("\nERROR: AVAILABLE resources cannot be negative!");
                    System.out.println("Please re-run program with valid values.");
                    System.exit(0);
                }
            }
        } else {

            int[] total = new int[m];

            System.out.println("\nEnter TOTAL system resources:");
            for (int j = 0; j < m; j++)
                total[j] = sc.nextInt();

            for (int j = 0; j < m; j++) {
                int sumAlloc = 0;
                for (int i = 0; i < n; i++)
                    sumAlloc += alloc[i][j];
                available[j] = total[j] - sumAlloc;
            }

            // ✅ Validation added here
            for (int j = 0; j < m; j++) {
                if (available[j] < 0) {
                    System.out.println("\nERROR: Total resources less than allocated resources!");
                    System.out.println("Please re-run program with valid values.");
                    System.exit(0);
                }
            }

            System.out.println("\nComputed AVAILABLE vector:");
            for (int j = 0; j < m; j++)
                System.out.print(available[j] + " ");
            System.out.println();
        }

        System.out.println("\nNEED Matrix:");
        for (int i = 0; i < n; i++) {
            System.out.print("P" + i + ": ");
            for (int j = 0; j < m; j++) {
                need[i][j] = max[i][j] - alloc[i][j];
                System.out.print(need[i][j] + " ");
            }
            System.out.println();
        }

        Banker banker = new Banker(max, alloc, need, available, n, m);

        System.out.println("\nStarting all processes in parallel...\n");

        ProcessThread[] threads = new ProcessThread[n];
        for (int i = 0; i < n; i++) {
            threads[i] = new ProcessThread(i, banker, safeSeqRef);
            threads[i].start();
        }

        for (int i = 0; i < n; i++) {
            try {
                threads[i].join();
            } catch (Exception e) {
            }
        }

        String finalSeq = safeSeqRef.toString().trim();

        System.out.println("\nALL PROCESSES COMPLETED SAFELY.");
        System.out.println("FINAL SAFE SEQUENCE: " + finalSeq);

        sc.close();
    }
}