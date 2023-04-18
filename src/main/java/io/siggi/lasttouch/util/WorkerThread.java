package io.siggi.lasttouch.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

public class WorkerThread implements Executor {

    private final Thread thread;
    private final List<Runnable> queue = new LinkedList<>();
    private boolean closed = false;

    public WorkerThread() {
        (this.thread = new Thread(this::run)).start();
    }

    public void close() {
        synchronized (queue) {
            closed = true;
            queue.notify();
        }
    }

    public void closeAndWait() throws InterruptedException {
        close();
        thread.join();
    }

    private void run() {
        outerLoop:
        while (true) {
            Runnable runnable;
            synchronized (queue) {
                while (queue.isEmpty()) {
                    if (closed) break outerLoop;
                    try {
                        queue.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                runnable = queue.remove(0);
            }
            try {
                runnable.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        synchronized (queue) {
            if (closed) {
                throw new IllegalStateException("Executor is closed, not accepting any new tasks.");
            }
            queue.add(command);
            queue.notify();
        }
    }
}
