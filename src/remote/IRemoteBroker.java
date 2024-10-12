package remote;

import common.NodeType;
import error.DuplicateRequestException;
import error.ResourceNotFoundException;
import message.Message;
import message.Request;
import message.Topic;
import subscriber.AccessDeniedException;

import java.rmi.Remote;  // Importing the Remote interface from the java.rmi package.
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface IRemoteBroker extends Remote {
    public void addTopic(Request<Topic> request) throws RemoteException;
    public void removeTopic(Request<Long> request) throws RemoteException, DuplicateRequestException, AccessDeniedException;
    public void subscribe(long topicId, String subscriberName) throws RemoteException, DuplicateRequestException, ResourceNotFoundException;
    public void unsubscribe(long topicId, String subscriberName) throws RemoteException, ResourceNotFoundException;
    void increaseSubscriberCount(Request<Long> request) throws RemoteException;
    void decreaseSubscriberCount(Request<Long> request) throws RemoteException;
    public void publishMessage(Message message) throws RemoteException, ResourceNotFoundException, AccessDeniedException;
    public ArrayList<Topic> listTopics() throws RemoteException;
    public ArrayList<String> listSubscribers(long topicId) throws RemoteException;
    public void connect(String fromAddress, int fromPort) throws RemoteException;
    public void ping(String name, NodeType type) throws RemoteException;
    public void addPublisher(String publisherName) throws RemoteException;
    public void removePublisher(Request<String> request) throws RemoteException, DuplicateRequestException;
    public String getSubscribersCount(String publisherName) throws RemoteException;
    int getTopicSubscribersCount(long topicId) throws RemoteException;
    ArrayList<Topic> listSubscribedTopics(String subscriberName) throws RemoteException;
    public void addSubscriber(String subscriberName) throws RemoteException;
    void removeSubscriber(String subscriberName) throws RemoteException;
}
