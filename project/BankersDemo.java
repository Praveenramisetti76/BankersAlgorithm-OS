import java.util.Arrays;
import java.util.Random;

class Bank {
    private final int[] available;
    private final int[][] max;
    private final int[][] allocation;
    public final int[][] need;
    private final int n, m;

    public Bank(int[] available, int[][] max, int[][] allocation) {
        this.n = max.length;
        this.m = available.length;
        this.available = Arrays.copyOf(available, m);
        this.max = new int[n][m];
        this.allocation = new int[n][m];
        this.need = new int[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++) {
                this.max[i][j] = max[i][j];
                this.allocation[i][j] = allocation[i][j];
                this.need[i][j] = max[i][j] - allocation[i][j];
            }
    }

    public synchronized boolean requestResource(int pid, int[] req) {
        // Check request validity
        for (int i = 0; i < m; i++)
            if (req[i] > need[pid][i] || req[i] > available[i])
                return false;

        // Tentatively allocate
        for (int i = 0; i < m; i++) {
            available[i] -= req[i];
            allocation[pid][i] += req[i];
            need[pid][i] -= req[i];
        }

        // Safety check
        boolean safe = checkSafety();

        if (!safe) {
            // Rollback
            for (int i = 0; i < m; i++) {
                available[i] += req[i];
                allocation[pid][i] -= req[i];
                need[pid][i] += req[i];
            }
        }

        return safe;
    }

    public synchronized void releaseResource(int pid, int[] rel) {
        for (int i = 0; i < m; i++) {
            allocation[pid][i] -= rel[i];
            available[i] += rel[i];
            need[pid][i] += rel[i];
            if (allocation[pid][i] < 0)
                allocation[pid][i] = 0;
            if (need[pid][i] > max[pid][i])
                need[pid][i] = max[pid][i];
        }
    }

    // Safety check
    private boolean checkSafety() {
        int[] work = Arrays.copyOf(available, m);
        boolean[] finish = new boolean[n];

        while (true) {
            boolean progress = false;
            for (int i = 0; i < n; i++) {
                if (!finish[i]) {
                    boolean canFinish = true;
                    for (int j = 0; j < m; j++)
                        if (need[i][j] > work[j])
                            canFinish = false;
                    if (canFinish) {
                        for (int j = 0; j < m; j++)
                            work[j] += allocation[i][j];
                        finish[i] = true;
                        progress = true;
                    }
                }
            }
            if (!progress) break;
        }
        for (boolean b : finish)
            if (!b) return false;
        return true;
    }

    public synchronized void printState() {
        System.out.println("Available: " + Arrays.toString(available));
        System.out.println("Allocation:");
        for (int i = 0; i < n; i++)
            System.out.println("P" + i + ": " + Arrays.toString(allocation[i]));
        System.out.println("Need:");
        for (int i = 0; i < n; i++)
            System.out.println("P" + i + ": " + Arrays.toString(need[i]));
    }
}

class Customer extends Thread {
    private final int pid;
    private final Bank bank;
    private final Random rand = new Random();

    public Customer(int pid, Bank bank) {
        this.pid = pid;
        this.bank = bank;
    }

    public void run() {
        while (true) {
            // Try to request a random valid amount within "need"
            int[] req = new int[bank.need[pid].length];
            boolean empty = true;
            for (int i = 0; i < req.length; i++) {
                if (bank.need[pid][i] > 0) {
                    req[i] = rand.nextInt(bank.need[pid][i] + 1);
                    if (req[i] > 0) empty = false;
                }
            }
            if (empty) break; // All done
            boolean granted = bank.requestResource(pid, req);
            if (granted) {
                System.out.println("P" + pid + " granted " + Arrays.toString(req));
                // Simulate doing work
                try { Thread.sleep(rand.nextInt(500) + 200); } catch (Exception ignored) {}
                // Release same resources
                bank.releaseResource(pid, req);
                System.out.println("P" + pid + " released " + Arrays.toString(req));
            } else {
                System.out.println("P" + pid + " request " + Arrays.toString(req) + " denied.");
            }
            try { Thread.sleep(rand.nextInt(300) + 100); } catch (Exception ignored) {}
        }
        System.out.println("P" + pid + " finished.");
    }
}

public class BankersDemo {
    public static void main(String[] args) {
        int[] available = {3, 3, 2};
        int[][] max = { {7, 5, 3}, {3, 2, 2}, {9, 0, 2} };
        int[][] allocation = { {0, 1, 0}, {2, 0, 0}, {3, 0, 2} };

        Bank bank = new Bank(available, max, allocation);

        // Print initial state
        bank.printState();

        Customer[] customers = new Customer[max.length];
        for (int i = 0; i < max.length; i++) {
            customers[i] = new Customer(i, bank);
            customers[i].start();
        }
        for (Customer c : customers) {
            try { c.join(); } catch (Exception ignored) {}
        }
        System.out.println("All processes finished.");
        bank.printState();
    }
}
