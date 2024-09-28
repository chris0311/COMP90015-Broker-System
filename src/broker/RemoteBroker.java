package broker;

import common.CacheList;
import common.NodeType;
import message.Topic;
import remote.IRemoteBroker;
import remote.IRemoteDirectory;

import java.rmi.RemoteException;  // Required for handling remote communication errors.
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoteBroker extends UnicastRemoteObject implements IRemoteBroker {
    private ArrayList<Topic> topics = new ArrayList<>();
    // topic id and list of subscribers
    private ConcurrentHashMap<Long, ArrayList<String>> subscribers = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<String> publishers = new ConcurrentLinkedQueue<>();
    private Registry registry;
    private ArrayList<IRemoteBroker> brokers;
    private CacheList cacheList;
    private int port;

    protected RemoteBroker(ArrayList<IRemoteBroker> brokers, int port) throws RemoteException {
        super();

        registry = LocateRegistry.getRegistry("localhost", 1099);
        this.brokers = brokers;
        cacheList = new CacheList(10);
        this.port = port;
    }

    @Override
    public void addTopic(long topicId, String topicName, String publisherName) throws RemoteException {
        Topic newTopic = new Topic(topicId, topicName, publisherName);
        topics.add(newTopic);
    }

    @Override
    public void removeTopic(long topicId) throws RemoteException {
        for (Topic topic : topics) {
            if (topic.getTopicId() == topicId) {
                topics.remove(topic);
                break;
            }
        }
    }

    @Override
    public void addSubscriber(long topicId, String subscriberName) throws RemoteException {
        if (emptyTopic(topicId)) {
            throw new RemoteException("Topic does not exist.");
        }

        if (subscribers.containsKey(topicId)) {
            ArrayList<String> topicSubscribers = subscribers.get(topicId);
            topicSubscribers.add(subscriberName);
        } else {
            ArrayList<String> topicSubscribers = new ArrayList<>();
            topicSubscribers.add(subscriberName);
            subscribers.put(topicId, topicSubscribers);
        }
    }

    @Override
    public void removeSubscriber(long topicId, String subscriberName) throws RemoteException {
        if (emptyTopic(topicId)) {
            throw new RemoteException("Topic does not exist.");
        }

        if (subscribers.containsKey(topicId)) {
            ArrayList<String> topicSubscribers = subscribers.get(topicId);
            topicSubscribers.remove(subscriberName);
        } else {
            throw new RemoteException("Subscriber does not exist.");
        }
    }

    @Override
    public void publishMessage(long topicId, String message) throws RemoteException {
        // send message to all local subscribers
        if (subscribers.containsKey(topicId)) {
            ArrayList<String> topicSubscribers = subscribers.get(topicId);
            for (String subscriber : topicSubscribers) {
                System.out.println("Message: " + message + " sent to " + subscriber);
            }
        }

        // send message to all brokers if not in cache
        if (!cacheList.contains(message)) {
            cacheList.add(message);
            for (IRemoteBroker broker : brokers) {
                broker.publishMessage(topicId, message);
            }
        }

    }

    @Override
    public ArrayList<Topic> listTopics() throws RemoteException {
        return topics;
    }

    @Override
    public ArrayList<String> listSubscribers(long topicId) throws RemoteException {
        return subscribers.get(topicId);
    }

    @Override
    public void connect(String fromAddress, int fromPort) throws RemoteException {
        // query new broker
        try {
            String newBrokerRegisterName = fromAddress + ":" + fromPort;
            IRemoteBroker newBroker = (IRemoteBroker) registry.lookup(newBrokerRegisterName);
            brokers.add(newBroker);
        } catch (Exception e) {
            System.err.println("Broker exception: " + e.toString());
        }

        System.out.println("Broker connected from: " + fromAddress + ":" + fromPort);
        System.out.println("Current brokers: " + brokers.size());
    }

    @Override
    public void ping(String name, NodeType type) throws RemoteException {
        System.out.println("Ping from " + type + ": " + name);
    }

    @Override
    public void addPublisher(String publisherName) throws RemoteException {
        publishers.add(publisherName);
    }

    @Override
    public void removePublisher(String publisherName) throws RemoteException {
        publishers.remove(publisherName);
    }

    private boolean emptyTopic(long topicId) throws RemoteException {
        for (Topic topic : topics) {
            if (topic.getTopicId() == topicId) {
                return false;
            }
        }
        return true;
    }

    protected void connectAllBrokers() {
        for (IRemoteBroker broker : brokers) {
            try {
                broker.connect("localhost", port);
            } catch (RemoteException e) {
                System.err.println("Broker exception: " + e.toString());
            }
        }
    }
}
