package test;

import java.util.Date;
import java.nio.charset.StandardCharsets;

public class Message {
    public final byte[] data;
    public final String asText;
    public final double asDouble;
    public final Date date;

    public Message(byte[] data) {
        this(new String(data, StandardCharsets.UTF_8));
    }

    public Message(String text) {
        this.data = text.getBytes(StandardCharsets.UTF_8);
        this.asText = text;
        double val;
        try {
            val = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            val = Double.NaN;
        }
        this.asDouble = val;
        this.date = new Date();
    }

    public Message(double val) {
        this(Double.toString(val));
    }
}
