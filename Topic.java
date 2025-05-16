package test;

import java.util.List;
import java.util.ArrayList;

/**
 * This class represents a topic that agents can subscribe to or publish messages on.
 */
public class Topic {

    public final String name;                       // The name of the topic
    public List<Agent> subs = new ArrayList<>();    // List of subscribers (agents who listen)
    public List<Agent> pubs = new ArrayList<>();    // List of publishers (agents who send messages)
    private Message lastMessage = new Message(0.0); // Keeps the last message sent on this topic

    // Constructor - set the topic name and initialize with a default message
    public Topic(String name) {
        this.name = name;
        this.lastMessage = new Message(0); // Set some default message
    }

    // Add agent as subscriber (only if not already in the list)
    public void subscribe(Agent sub) {
        if (!subs.contains(sub)) {
            subs.add(sub);
        }
    }

    // Remove agent from subscriber list
    public void unsubscribe(Agent unSub) {
        subs.remove(unSub);
    }

    // Send a message to all subscribers
    public void publish(Message msg) {
        lastMessage = msg; // Save the message
        for (Agent agent : subs) {
            agent.callback(this.name, msg); // Call each subscriber's callback method
        }
    }

    // Add agent as a publisher
    public void addPublisher(Agent publisher) {
        if (!pubs.contains(publisher)) {
            pubs.add(publisher);
        }
    }

    // Remove agent from publishers
    public void removePublisher(Agent unPublisher) {
        pubs.remove(unPublisher);
    }

    // Returns the name of the topic
    public String getName() {
        return this.name;
    }

    // Returns the last message that was published on this topic
    public Message getLastMessage() {
        return lastMessage;
    }

    // Returns the list of subscribers
    public List<Agent> getSubscribers() {
        return subs;
    }

    // Returns the list of publishers
    public List<Agent> getPublishers() {
        return pubs;
    }

    // Just prints the name of the topic (optional method)
    public void print(String message) {
        System.out.println(this.name);
    }
}
