/**
 * @Author: Chris Liang 1159696
 */

package message;

import java.io.Serializable;

public class Topic extends Request implements Serializable {
    private long topicId;
    private String topicName;
    private String publisherName;

    public Topic(long topicId, String topicName, String publisherName) {
        super();
        this.topicId = topicId;
        this.topicName = topicName;
        this.publisherName = publisherName;
    }

    public long getTopicId() {
        return topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicId(long topicId) {
        this.topicId = topicId;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    @Override
    public String toString() {
        return topicId + " " + topicName + " " + publisherName;
    }
}
