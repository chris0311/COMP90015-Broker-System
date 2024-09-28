package message;

public class Message {
    private long messageId;
    private long topicId;
    private String message;
    private long timestamp;

    public Message(long messageId, long topicId, String message) {
        this.messageId = messageId;
        this.topicId = topicId;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public long getMessageId() {
        return messageId;
    }

    public long getTopicId() {
        return topicId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
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
}
