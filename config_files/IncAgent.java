package test;

public class IncAgent implements Agent {
    private final String name;
    private final String[] subs;
    private final String[] pubs;

    public IncAgent(String[] subs, String[] pubs) {
        this.name = "IncAgent";
        this.subs = subs;
        this.pubs = pubs;

        // Subscribe to the first subscription topic, if any
        if (subs.length > 0) {
            TopicManagerSingleton.get().getTopic(subs[0]).subscribe(this);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void reset() {
        // No internal state to reset
    }

    @Override
    public void callback(String topic, Message msg) {
        if (subs.length > 0 && topic.equals(subs[0])) {
            double value = msg.asDouble;
            if (pubs.length > 0) {
                // Publish incremented value to first publishing topic
                TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(value + 1));
            }
        }
    }

    @Override
    public void close() {
        // Unsubscribe from the first subscription topic, if any
        if (subs.length > 0) {
            TopicManagerSingleton.get().getTopic(subs[0]).unsubscribe(this);
        }
    }
}
