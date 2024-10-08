package message;

import java.io.Serializable;

public class Message extends Request implements Serializable {
    private long topicId;
    private String message;
    private long timestamp;
    private String publisherName;

    public Message(long topicId, String message, String publisherName) {
        super();
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

    public String getPublisherName() {
        return publisherName;
    }
}
