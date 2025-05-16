package test;

public class ParallelAgent implements Agent {
    private final Agent agent;
    private final Thread thread;
    private volatile boolean running = true;

    // Static counter to track active threads
    private static volatile int activeThreadCount = 0;

    // Increment counter when a thread starts
    private static synchronized void incrementThreadCount() {
        activeThreadCount++;
    }

    // Decrement counter when a thread ends
    private static synchronized void decrementThreadCount() {
        activeThreadCount--;
    }

    public ParallelAgent(Agent agent, int capacity) {
        this.agent = agent;

        thread = new Thread(() -> {
            try {
                incrementThreadCount(); // Increment thread counter
                agent.reset(); // start agent logic

                while (running) {
                    try {
                        Thread.sleep(10); // or do something useful
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } finally {
                agent.close(); // cleanup when thread ends
                decrementThreadCount(); // Decrement thread counter
            }
        });

        thread.start();
    }

    @Override
    public String getName() {
        return agent.getName();
    }

    @Override
    public void reset() {
        agent.reset();
    }

    @Override
    public void callback(String topic, Message msg) {
        agent.callback(topic, msg);
    }

    @Override
    public void close() {
        running = false;
        thread.interrupt();
        try {
            thread.join();  // wait for thread to finish cleanly
        } catch (InterruptedException e) {
            // ignore
        }
    }
}