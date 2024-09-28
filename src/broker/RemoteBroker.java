package broker;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import common.MessageType;
import common.NodeType;
import message.Topic;
import remote.IRemoteBroker;
import remote.IRemoteSubscriber;

import java.rmi.RemoteException;  // Required for handling remote communication errors.
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoteBroker extends UnicastRemoteObject implements IRemoteBroker {
    private ArrayList<Topic> topics = new ArrayList<>();
    // topic id and list of subscribers
    private ConcurrentHashMap<Long, ArrayList<String>> subscriberTopics = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<String> publishers = new ConcurrentLinkedQueue<>();
    private Registry registry;
    private HashSet<IRemoteBroker> brokers;
    private HashSet<Integer> brokerPorts = new HashSet<>();
    private Cache<String, MessageType> cache;
    private int port;

    protected RemoteBroker(ArrayList<IRemoteBroker> brokers, int port) throws RemoteException {
        super();

        registry = LocateRegistry.getRegistry("localhost", 1099);
        this.brokers = new HashSet<>(brokers);
        this.cache = CacheBuilder.newBuilder().maximumSize(100).build();
        this.port = port;
    }

    @Override
    public void addTopic(long topicId, String topicName, String publisherName) throws RemoteException {
        // add topics to all brokers
        if (cache.getIfPresent(String.valueOf(topicId)) == null || cache.getIfPresent(String.valueOf(topicId)) != MessageType.ADD_TOPIC) {
            cache.put(String.valueOf(topicId), MessageType.ADD_TOPIC);
            Topic newTopic = new Topic(topicId, topicName, publisherName);
            topics.add(newTopic);
            for (IRemoteBroker broker : brokers) {
                broker.addTopic(topicId, topicName, publisherName);
            }

            System.out.println("Topic: " + topicName + " added by " + publisherName);
        }

    }

    @Override
    public void removeTopic(long topicId) throws RemoteException {
        // remove topics from all brokers
        if (cache.getIfPresent(String.valueOf(topicId)) == null || cache.getIfPresent(String.valueOf(topicId)) != MessageType.REMOVE_TOPIC) {
            cache.put(String.valueOf(topicId), MessageType.REMOVE_TOPIC);

            topics.removeIf(topic -> topic.getTopicId() == topicId);
            System.out.println("Topic removed: " + topicId);
            for (IRemoteBroker broker : brokers) {
                broker.removeTopic(topicId);
            }
        }

    }

    @Override
    public void subscribe(long topicId, String subscriberName) throws RemoteException {
        if (emptyTopic(topicId)) {
            throw new RemoteException("Topic does not exist.");
        }

        if (subscriberTopics.containsKey(topicId)) {
            ArrayList<String> topicSubscribers = subscriberTopics.get(topicId);
            topicSubscribers.add(subscriberName);
        } else {
            ArrayList<String> topicSubscribers = new ArrayList<>();
            topicSubscribers.add(subscriberName);
            subscriberTopics.put(topicId, topicSubscribers);
        }
    }

    @Override
    public void unsubscribe(long topicId, String subscriberName) throws RemoteException {
        if (emptyTopic(topicId)) {
            throw new RemoteException("Topic does not exist.");
        }

        if (subscriberTopics.containsKey(topicId)) {
            ArrayList<String> topicSubscribers = subscriberTopics.get(topicId);
            topicSubscribers.remove(subscriberName);
        } else {
            throw new RemoteException("Subscriber does not exist.");
        }
    }

    @Override
    public void publishMessage(long topicId, String message) throws RemoteException {
        // send message to all local subscribers
        // TODO: Use message Class
        if (subscriberTopics.containsKey(topicId)) {
            ArrayList<String> topicSubscribers = subscriberTopics.get(topicId);
            for (String subscriber : topicSubscribers) {
                try {
                    IRemoteSubscriber remoteSubscriber = (IRemoteSubscriber) registry.lookup("subscriber/"+subscriber);
                    remoteSubscriber.receiveMessage(message);
                } catch (Exception e) {
                    System.err.println("Broker exception: " + e.toString());
                }
            }
        }

        // send message to all brokers if not in cache
        if (cache.getIfPresent(message) == null || cache.getIfPresent(message) != MessageType.MESSAGE) {
            cache.put(message, MessageType.MESSAGE);
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
        return subscriberTopics.get(topicId);
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
        System.out.println("Publisher " + publisherName + " added.");
    }

    @Override
    public void removePublisher(String publisherName) throws RemoteException {
        publishers.remove(publisherName);
    }

    @Override
    public String getSubscribersCount(String publisherName) throws RemoteException {
        StringBuilder res = new StringBuilder();
        for (Topic topic : topics) {
            if (topic.getPublisherName().equals(publisherName)) {
                String topicName = topic.getTopicName();
                long topicId = topic.getTopicId();
                int subscriberCount = 0;
                if (subscriberTopics.containsKey(topicId)) {
                    subscriberCount = subscriberTopics.get(topicId).size();
                }
                res.append(topicId).append(" ").append(topicName).append(" ").append(subscriberCount).append("\n");
            }
        }
        if (res.isEmpty()) {
            res = new StringBuilder("No topics found for publisher: " + publisherName);
        }
        return res.toString();
    }

    @Override
    public ArrayList<Topic> listSubscribedTopics(String subscriberName) throws RemoteException {
        ArrayList<Topic> subscribedTopics = new ArrayList<>();
        for (Topic topic : topics) {
            long topicId = topic.getTopicId();
            if (subscriberTopics.containsKey(topicId)) {
                ArrayList<String> topicSubscribers = subscriberTopics.get(topicId);
                if (topicSubscribers.contains(subscriberName)) {
                    subscribedTopics.add(topic);
                }
            }
        }
        return subscribedTopics;
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

//    public static <T> void flood(
//            Cache<String, MessageType> cache,
//            T key,
//            MessageType expectedType,
//            Supplier<Boolean> cacheCheck,
//            Runnable brokerAction
//    ) {
//        // Check if the cache contains the expected value
//        if (cacheCheck.get() == null || !cacheCheck.get().equals(expectedType)) {
//            // Perform the broker action
//            brokerAction.run();
//            // Update the cache with the new value
//            cache.put(key, expectedType);
//        }
//    }
}
