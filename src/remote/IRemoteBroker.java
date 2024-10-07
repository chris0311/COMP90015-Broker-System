package remote;

import common.NodeType;
import message.Message;
import message.Topic;

import java.rmi.Remote;  // Importing the Remote interface from the java.rmi package.
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface IRemoteBroker extends Remote {
    public void addTopic(long topicId, String topicName, String publisherName) throws RemoteException;
    public void removeTopic(long topicId) throws RemoteException;
    public void subscribe(long topicId, String subscriberName) throws RemoteException;
    public void unsubscribe(long topicId, String subscriberName) throws RemoteException;
    public void publishMessage(Message message) throws RemoteException;
    public ArrayList<Topic> listTopics() throws RemoteException;
    public ArrayList<String> listSubscribers(long topicId) throws RemoteException;
    public void connect(String fromAddress, int fromPort) throws RemoteException;
    public void ping(String name, NodeType type) throws RemoteException;
    public void addPublisher(String publisherName) throws RemoteException;
    public void removePublisher(String publisherName) throws RemoteException;
    public String getSubscribersCount(String publisherName) throws RemoteException;
    ArrayList<Topic> listSubscribedTopics(String subscriberName) throws RemoteException;
    public void addSubscriber(String subscriberName) throws RemoteException;
}
