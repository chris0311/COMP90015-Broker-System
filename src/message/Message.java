package message;

import java.io.Serializable;
import java.util.Objects;

public class Message implements Serializable {
    private long topicId;
    private String message;
    private long timestamp;
    private String publisherName;

    public Message(long topicId, String message, String publisherName) {
        this.topicId = topicId;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.publisherName = publisherName;
    }

    public long getTopicId() {
        return topicId;
    }

    public String getMessage() {
        return message;
    }

    public void setTopicId(long topicId) {
        this.topicId = topicId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int hashCode() {
        // create hashcode based on topicId, publisherName, timestamp, and message
        return Objects.hash(topicId, publisherName, timestamp, message);
    }

    public String getPublisherName() {
        return publisherName;
    }
}
