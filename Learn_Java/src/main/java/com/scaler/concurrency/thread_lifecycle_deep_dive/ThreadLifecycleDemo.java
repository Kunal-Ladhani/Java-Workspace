package com.scaler.concurrency.thread_lifecycle_deep_dive;

public class ThreadLifecycleDemo {

    private static final Object monitor = new Object();

    public static void main(String[] args) throws InterruptedException {

        demoAllSixStates();
        demoDaemonThread();
        demoInterruption();
        demoRunVsStart();
    }

    // ==================== DEMO 1: All 6 Thread States ====================

    private static void demoAllSixStates() throws InterruptedException {
        printHeader("DEMO 1: All 6 Thread States");

        // ---- NEW ----
        Thread t1 = new Thread(() -> {
            System.out.println("  [t1] Running inside run(), state from within: "
                    + Thread.currentThread().getState());
        }, "StateDemo-Thread");

        System.out.println("[NEW] t1 state after construction: " + t1.getState());

        // ---- RUNNABLE ----
        t1.start();
        t1.join();
        System.out.println("[RUNNABLE] t1 reported RUNNABLE from inside run() (see above)");

        // ---- TERMINATED ----
        System.out.println("[TERMINATED] t1 state after run() completed: " + t1.getState());

        try {
            t1.start();
        } catch (IllegalThreadStateException e) {
            System.out.println("[TERMINATED] Calling start() again threw: " + e.getClass().getSimpleName());
        }

        // ---- TIMED_WAITING ----
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Sleeper-Thread");
        sleeper.start();
        Thread.sleep(100);
        System.out.println("[TIMED_WAITING] sleeper state during sleep(): " + sleeper.getState());
        sleeper.join();

        // ---- WAITING (using join()) ----
        Thread longRunner = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "LongRunner-Thread");

        Thread joiner = new Thread(() -> {
            try {
                longRunner.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Joiner-Thread");

        longRunner.start();
        joiner.start();
        Thread.sleep(100);
        System.out.println("[WAITING] joiner state while waiting on join(): " + joiner.getState());
        longRunner.join();
        joiner.join();

        // ---- BLOCKED ----
        synchronized (monitor) {
            Thread blocked = new Thread(() -> {
                synchronized (monitor) {
                    System.out.println("  [blocked] Acquired monitor lock");
                }
            }, "Blocked-Thread");

            blocked.start();
            Thread.sleep(100);
            System.out.println("[BLOCKED] blocked state while waiting for monitor: " + blocked.getState());
        }
        Thread.sleep(200);

        System.out.println();
    }

    // ==================== DEMO 2: Daemon Threads ====================

    private static void demoDaemonThread() throws InterruptedException {
        printHeader("DEMO 2: Daemon vs User Threads");

        Thread daemon = new Thread(() -> {
            int count = 0;
            while (true) {
                count++;
                if (count <= 3) {
                    System.out.println("  [daemon] Tick #" + count + " (isDaemon=" +
                            Thread.currentThread().isDaemon() + ")");
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Daemon-Thread");

        daemon.setDaemon(true);
        System.out.println("daemon.isDaemon() before start: " + daemon.isDaemon());
        daemon.start();

        Thread user = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("  [user] User thread finishing. " +
                    "If this were the last user thread, daemon would be killed.");
        }, "User-Thread");

        user.start();
        user.join();

        System.out.println("User thread done. Daemon is still alive: " + daemon.isAlive());
        System.out.println("(In a real scenario with no other user threads, JVM would exit now,");
        System.out.println(" killing the daemon abruptly — no finally, no cleanup.)");
        daemon.interrupt();
        daemon.join(500);
        System.out.println();
    }

    // ==================== DEMO 3: Thread Interruption ====================

    private static void demoInterruption() throws InterruptedException {
        printHeader("DEMO 3: Thread Interruption");

        // 3a: Interrupting a sleeping thread
        System.out.println("--- 3a: Interrupting a sleeping thread ---");
        Thread sleepy = new Thread(() -> {
            System.out.println("  [sleepy] Going to sleep for 10 seconds...");
            try {
                Thread.sleep(10_000);
                System.out.println("  [sleepy] Woke up naturally (you should NOT see this)");
            } catch (InterruptedException e) {
                System.out.println("  [sleepy] InterruptedException caught! Message: " + e.getMessage());
                System.out.println("  [sleepy] isInterrupted() after catch: "
                        + Thread.currentThread().isInterrupted());
                Thread.currentThread().interrupt();
                System.out.println("  [sleepy] isInterrupted() after restoring flag: "
                        + Thread.currentThread().isInterrupted());
            }
        }, "Sleepy-Thread");

        sleepy.start();
        Thread.sleep(500);
        System.out.println("[main] Interrupting sleepy thread...");
        sleepy.interrupt();
        sleepy.join();

        System.out.println();

        // 3b: Interrupting a running thread (flag check)
        System.out.println("--- 3b: Interrupting a running (non-blocking) thread ---");
        Thread worker = new Thread(() -> {
            long count = 0;
            while (!Thread.currentThread().isInterrupted()) {
                count++;
            }
            System.out.println("  [worker] Detected interrupt after " + count + " iterations");
            System.out.println("  [worker] isInterrupted(): " + Thread.currentThread().isInterrupted());
        }, "Worker-Thread");

        worker.start();
        Thread.sleep(100);
        System.out.println("[main] Interrupting worker thread...");
        worker.interrupt();
        worker.join();

        System.out.println();

        // 3c: Thread.interrupted() clears the flag
        System.out.println("--- 3c: Thread.interrupted() clears the flag ---");
        Thread flagDemo = new Thread(() -> {
            Thread.currentThread().interrupt();
            System.out.println("  [flagDemo] First  Thread.interrupted() call: " + Thread.interrupted());
            System.out.println("  [flagDemo] Second Thread.interrupted() call: " + Thread.interrupted());
        }, "FlagDemo-Thread");

        flagDemo.start();
        flagDemo.join();

        System.out.println();
    }

    // ==================== DEMO 4: run() vs start() ====================

    private static void demoRunVsStart() throws InterruptedException {
        printHeader("DEMO 4: Calling run() vs start()");

        Runnable task = () -> System.out.println("  Executing on thread: "
                + Thread.currentThread().getName());

        Thread t = new Thread(task, "MyNewThread");

        System.out.println("--- Calling t.run() directly (NO new thread) ---");
        t.run();

        System.out.println("--- Calling t.start() (creates a NEW thread) ---");
        t.start();
        t.join();

        System.out.println();
    }

    // ==================== Utility ====================

    private static void printHeader(String title) {
        System.out.println("=".repeat(65));
        System.out.println("  " + title);
        System.out.println("=".repeat(65));
    }
}
