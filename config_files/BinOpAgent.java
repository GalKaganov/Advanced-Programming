package test;

import java.util.function.BinaryOperator;

/**
 * A binary operation agent — waits for messages from two topics,
 * applies a math operation, and publishes the result to another topic.
 */
public class BinOpAgent implements Agent {

    private String name;
    private String inputTopic1;          // First input topic
    private String inputTopic2;          // Second input topic
    private String outputTopic;          // Output topic to publish to
    private BinaryOperator<Double> operation; // The math operation (e.g. +, -, *, /)

    private Message input1 = null;       // Last message from inputTopic1
    private Message input2 = null;       // Last message from inputTopic2

    // Constructor sets names and connects to the topics
    public BinOpAgent(String name, String inputTopic1, String inputTopic2,
                      String outputTopic, BinaryOperator<Double> operation) {
        this.name = name;
        this.inputTopic1 = inputTopic1;
        this.inputTopic2 = inputTopic2;
        this.outputTopic = outputTopic;
        this.operation = operation;

        // Subscribe to both input topics
        TopicManagerSingleton.get().getTopic(inputTopic1).subscribe(this);
        TopicManagerSingleton.get().getTopic(inputTopic2).subscribe(this);

        // Register as publisher for the output topic
        TopicManagerSingleton.get().getTopic(outputTopic).addPublisher(this);
    }

    @Override
    public String getName() {
        return name;
    }

    // Reset internal state (used between runs)
    @Override
    public void reset() {
        input1 = null;
        input2 = null;
    }

    // Called when a new message arrives on a topic
    @Override
    public void callback(String topic, Message msg) {
        if (topic.equals(inputTopic1)) {
            input1 = msg;
        } else if (topic.equals(inputTopic2)) {
            input2 = msg;
        }

        // If both messages exist and their asDouble values are not NaN, do the math and publish
        if (input1 != null && input2 != null &&
                !Double.isNaN(input1.asDouble) && !Double.isNaN(input2.asDouble)) {

            double result = operation.apply(input1.asDouble, input2.asDouble);
            TopicManagerSingleton.get().getTopic(outputTopic).publish(new Message(result));
        }
    }

    // Disconnect from topics when closing
    @Override
    public void close() {
        TopicManagerSingleton.get().getTopic(inputTopic1).unsubscribe(this);
        TopicManagerSingleton.get().getTopic(inputTopic2).unsubscribe(this);
        TopicManagerSingleton.get().getTopic(outputTopic).removePublisher(this);
    }
}
