import java.util.Arrays;
import java.util.Scanner;

class Banker {
    private int n, m;
    private int[][] max, allocation, need;
    private int[] available;

    public Banker(int n, int m, int[][] max, int[][] allocation, int[] available, int[][] need) {
        this.n = n;
        this.m = m;
        this.max = max;
        this.allocation = allocation;
        this.available = available;
        this.need = need;
    }

    public synchronized boolean requestResources(int processID) {
        int[] request = need[processID].clone();
        System.out.println("\nP" + processID + " requesting resources:");
        System.out.println("Available: " + Arrays.toString(available));
        System.out.println("P" + processID + " Need " + Arrays.toString(request));
        System.out.println("Checking availability...");
        for (int i = 0; i < m; i++) {
            if (request[i] > available[i]) {
                System.out.println("\nNeed > Available. Request denied for Process " + processID);
                return false;
            }
        }
        for (int i = 0; i < m; i++) {
            available[i] -= request[i];
            allocation[processID][i] += request[i];
            need[processID][i] -= request[i];
        }
        if (isSafeState()) {
            System.out.println("\nNeed <= Available. Request granted for P" + processID);
            System.out.println("\nResources allocated to P" + processID);
            return true;
        } else {
            for (int i = 0; i < m; i++) {
                available[i] += request[i];
                allocation[processID][i] -= request[i];
                need[processID][i] += request[i];
            }
            System.out.println("\nSystem would be unsafe! Request denied for P" + processID);
            return false;
        }
    }

    public synchronized boolean isSafeState() {
        boolean[] finish = new boolean[n];
        int[] work = available.clone();
        int finishedCount = 0;
        while (finishedCount < n) {
            boolean progress = false;
            for (int p = 0; p < n; p++) {
                if (!finish[p]) {
                    boolean canFinish = true;
                    for (int j = 0; j < m; j++) {
                        if (need[p][j] > work[j]) {
                            canFinish = false;
                            break;
                        }
                    }
                    if (canFinish) {
                        for (int j = 0; j < m; j++)
                            work[j] += allocation[p][j];

                        finish[p] = true;
                        finishedCount++;
                        progress = true;
                    }
                }
            }
            if (!progress) {
                return false;
            }
        }
        return true;
    }

    public synchronized void releaseResources(int processID) {
        System.out.println("\nP" + processID + " releasing resources:");
        for (int i = 0; i < m; i++) {
            available[i] += allocation[processID][i];
            allocation[processID][i] = 0;
            need[processID][i] = 0;
        }
        System.out.println("\nResources released by P" + processID);
        System.out.println("\nAvailable resources: " + Arrays.toString(available));
    }
}

class ProcessThread extends Thread {
    private int processID;
    private Banker banker;
    private boolean[] finish;
    private StringBuilder safeSeq;

    public ProcessThread(int processID, Banker banker, boolean[] finish, StringBuilder safeSeq) {
        this.processID = processID;
        this.banker = banker;
        this.finish = finish;
        this.safeSeq = safeSeq;
    }

    public void run() {
        int attempts = 0;
        while (attempts < 25) {
            if (banker.requestResources(processID)) {
                System.out.println("\nP" + processID + " executing.");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                banker.releaseResources(processID);
                System.out.println("\nP" + processID + " completed.");
                synchronized (safeSeq) {
                    safeSeq.append("P").append(processID).append(" ");
                    finish[processID] = true;
                }
                return;
            }
            else {
                System.out.println("\nP" + processID + " will retry after some time.");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            attempts++;
        }
    }
};

public class BankersAlgorithm {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter number of processes: ");
        int n = sc.nextInt();
        if (n <= 0) {
            System.out.println("Number of processes must be greater than zero.");
            return;
        }
        System.out.print("\nEnter number of resources: ");
        int m = sc.nextInt();
        if (m <= 0) {
            System.out.println("Number of resources must be greater than zero.");
            return;
        }
        int[][] max = new int[n][m];
        int[][] allocation = new int[n][m];
        int[][] need = new int[n][m];
        int[] available = new int[m];
        System.out.println("\nEnter the allocation matrix:");
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                allocation[i][j] = sc.nextInt();
                if (allocation[i][j] < 0) {
                    System.out.println("Allocation values must be non-negative.");
                    return;
                }
            }
        }
        System.out.println("\nEnter the maximum matrix:");
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                max[i][j] = sc.nextInt();
                if (max[i][j] < allocation[i][j]) {
                    System.out.println("Maximum values must be greater than or equal to allocation values.");
                    return;
                }
                need[i][j] = max[i][j] - allocation[i][j];
                if (need[i][j] < 0) {
                    System.out.println("Need values must be non-negative.");
                    return;
                }
            }
        }
        System.out.print(
                "\nSelect your choice among these options(1. Enter available resources, 2. Enter System max resources): ");
        int choice = sc.nextInt();
        if (choice == 1) {
            System.out.println("\nEnter the available resources:");
            for (int j = 0; j < m; j++) {
                available[j] = sc.nextInt();
                if (available[j] < 0) {
                    System.out.println("Available resources must be non-negative.");
                    return;
                }
            }
        } else if (choice == 2) {
            System.out.println("\nEnter the total system resources:");
            int[] totalResources = new int[m];
            for (int i = 0; i < m; i++) {
                totalResources[i] = sc.nextInt();
                if (totalResources[i] < 0) {
                    System.out.println("Total system resources must be non-negative.");
                    return;
                }
            }
            for (int i = 0; i < m; i++) {
                int sumAllocation = 0;
                for (int j = 0; j < n; j++) {
                    sumAllocation += allocation[j][i];
                }
                available[i] = totalResources[i] - sumAllocation;
                if (available[i] < 0) {
                    System.out.println("Calculated available resources cannot be negative.");
                    return;
                }
            }
        } else {
            System.out.println("Invalid choice.");
            return;
        }
        System.out.println("\nNeed Matrix:");
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                System.out.print(need[i][j] + " ");
            }
            System.out.println();
        }
        boolean[] finish = new boolean[n];
        StringBuilder safeSeq = new StringBuilder();
        Banker banker = new Banker(n, m, max, allocation, available, need);
        ProcessThread[] processes = new ProcessThread[n];
        for (int i = 0; i < n; i++) {
            processes[i] = new ProcessThread(i, banker, finish, safeSeq);
            processes[i].start();
        }
        for (int i = 0; i < n; i++) {
            try {
                processes[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        for (int i = 0; i < n; i++) {
            if (!finish[i]) {
                System.out.println("\nSystem is not in a safe state. No safe sequence exists.");
                sc.close();
                return;
            }
        }
        System.out.println("\nAll processes have completed execution.");
        System.out.println("\nSystem is in a safe state. Safe sequence is: " + safeSeq.toString());
        sc.close();
    }
}