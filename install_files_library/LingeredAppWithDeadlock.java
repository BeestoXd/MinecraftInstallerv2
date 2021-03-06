package jdk.test.lib.apps;

import java.util.concurrent.Phaser;

public class LingeredAppWithDeadlock extends LingeredApp {

    private static final Object Lock1 = new Object();
    private static final Object Lock2 = new Object();

    private static volatile int reachCount = 0;

    private static final Phaser p = new Phaser(2);

    private static class ThreadOne extends Thread {
        public void run() {
            // wait Lock2 is locked
            p.arriveAndAwaitAdvance();
            synchronized (Lock1) {
                // signal Lock1 is locked
                p.arriveAndAwaitAdvance();
                synchronized (Lock2) {
                    reachCount += 1;
                }
            }
        }
    }

    private static class ThreadTwo extends Thread {
        public void run() {
            synchronized (Lock2) {
                // signal Lock2 is locked
                p.arriveAndAwaitAdvance();
                // wait Lock1 is locked
                p.arriveAndAwaitAdvance();
                synchronized (Lock1) {
                    reachCount += 1;
                }
            }
        }
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            System.err.println("Lock file name is not specified");
            System.exit(7);
        }

        // Run two theads that should come to deadlock
        new ThreadOne().start();
        new ThreadTwo().start();

        if (reachCount > 0) {
            // Not able to deadlock, exiting
            System.exit(3);
        }

        LingeredApp.main(args);
    }
 }
