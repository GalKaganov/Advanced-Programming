package test;

public class PlusAgent implements Agent {
    private final String name;
    private final String[] subs;
    private final String[] pubs;
    private volatile double x;
    private volatile double y;
    private boolean gotX = false;
    private boolean gotY = false;


    public PlusAgent(String[] subs, String[] pubs) {
        this.name = "PlusAgent";
        this.subs = subs;
        this.pubs = pubs;
        this.x = 0.0;
        this.y = 0.0;

        if (subs.length >= 2) {
            TopicManagerSingleton.get().getTopic(subs[0]).subscribe(this);
            TopicManagerSingleton.get().getTopic(subs[1]).subscribe(this);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void reset() {
        x = 0.0;
        y = 0.0;
        gotX = false;
        gotY = false;
    }

    @Override
    public void callback(String topic, Message msg) {
        if (topic.equals(subs[0])) {
            x = msg.asDouble;
            gotX = true;
        } else if (topic.equals(subs[1])) {
            y = msg.asDouble;
            gotY = true;
        }

        if (gotX && gotY && pubs.length > 0) {
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(x + y));
            gotX = false;
            gotY = false;
        }
    }


    @Override
    public void close() {
        if (subs.length >= 2) {
            TopicManagerSingleton.get().getTopic(subs[0]).unsubscribe(this);
            TopicManagerSingleton.get().getTopic(subs[1]).unsubscribe(this);
        }
    }
}