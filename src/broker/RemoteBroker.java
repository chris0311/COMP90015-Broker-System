package broker;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import common.MessageType;
import common.NodeType;
import message.Message;
import message.Request;
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

public class RemoteBroker extends UnicastRemoteObject implements IRemoteBroker {
    private ArrayList<Topic> topics = new ArrayList<>();
    // topic id and list of subscribers
    private ConcurrentHashMap<Long, ArrayList<String>> subscriberTopics = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, Integer> subscriberCount = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> publishers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> subscribers = new ConcurrentHashMap<>();
    private Registry registry;
    private HashSet<IRemoteBroker> brokers;
    private HashSet<Integer> brokerPorts = new HashSet<>();
    private Cache<String, MessageType> cache;
    private int port;

    protected RemoteBroker(ArrayList<IRemoteBroker> brokers, int port) throws RemoteException {
        super();

        // Start a thread to monitor client activity and clean up inactive clients
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Check every second
                    long currentTime = System.currentTimeMillis();
                    long currentTimeSeconds = currentTime / 1000;
                    subscribers.entrySet().removeIf(entry -> {
                        if (currentTime - entry.getValue() > 2000) {
                            System.out.println("Cleaning up client: " + entry.getKey() + " at " + currentTimeSeconds);
                            this.unsubscribeAll(entry.getKey());

                            return true;
                        }
                        return false;
                    });
                    for (String publisher : publishers.keySet()) {
                        if (currentTime - publishers.get(publisher) > 2000) {
                            System.out.println("Cleaning up publisher: " + publisher + " at " + currentTimeSeconds);
                            Request request = new Request(publisher);
                            try {
                                this.removePublisher(request);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        registry = LocateRegistry.getRegistry("localhost", 1099);
        this.brokers = new HashSet<>(brokers);
        this.cache = CacheBuilder.newBuilder().maximumSize(100).build();
        this.port = port;
    }

    @Override
    public void addTopic(Request request) throws RemoteException {
        String cacheKey = request.getIdentifier();
        // add topics to all brokers
        if (cache.getIfPresent(cacheKey) == null || cache.getIfPresent(cacheKey) != MessageType.ADD_TOPIC) {
            cache.put(cacheKey, MessageType.ADD_TOPIC);
            Topic newTopic = (Topic) request.getObject();
            topics.add(newTopic);
            for (IRemoteBroker broker : brokers) {
                broker.addTopic(request);
            }

            System.out.println("Topic: " + newTopic.getTopicName() + " added by " + newTopic.getPublisherName());
        }

    }

    @Override
    public void removeTopic(Request request) throws RemoteException {
        String cacheKey = request.getIdentifier();
        // remove topics from all brokers
        if (cache.getIfPresent(cacheKey) == null || cache.getIfPresent(cacheKey) != MessageType.REMOVE_TOPIC) {
            cache.put(cacheKey, MessageType.REMOVE_TOPIC);
            long topicId = (long) request.getObject();

            // send message to all local subscribers
            if (subscriberTopics.containsKey(topicId)) {
                ArrayList<String> topicSubscribers = subscriberTopics.get(topicId);
                for (String subscriber : topicSubscribers) {
                    messageSubscriber(subscriber, "Topic removed: " + topicId + "; " + "you are unsubscribed.");
                }
            }

            topics.removeIf(topic -> topic.getTopicId() == topicId);
            // remove from subscriberTopics and subscriberCount
            subscriberTopics.remove(topicId);
            subscriberCount.remove(topicId);
            System.out.println("Topic removed: " + topicId);

            // notify all brokers
            for (IRemoteBroker broker : brokers) {
                broker.removeTopic(request);
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

        // add to cache and flood
        Request request = new Request(topicId);
        String cacheKey = request.getIdentifier();
        if (cache.getIfPresent(cacheKey) == null || cache.getIfPresent(cacheKey) != MessageType.SUBSCRIBER_COUNT) {
            cache.put(cacheKey, MessageType.SUBSCRIBER_COUNT);
            this.subscriberCount.put(topicId, this.subscriberCount.getOrDefault(topicId, 0) + 1);

            System.out.println("Subscriber " + subscriberName + " subscribed to topic: " + topicId);

            for (IRemoteBroker broker : brokers) {
                broker.increaseSubscriberCount(request);
            }
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

            // send message to local subscriber
            messageSubscriber(subscriberName, "Unsubscribed from topic: " + topicId);
        }

        // add to cache and flood
        Request request = new Request(topicId);
        String cacheKey = request.getIdentifier();
        if (cache.getIfPresent(cacheKey) == null || cache.getIfPresent(cacheKey) != MessageType.UNSUBSCRIBE) {
            cache.put(cacheKey, MessageType.UNSUBSCRIBE);
            this.subscriberCount.put(topicId, this.subscriberCount.get(topicId) - 1);

            for (IRemoteBroker broker : brokers) {
                broker.decreaseSubscriberCount(request);
            }
        }
    }

    @Override
    public void increaseSubscriberCount(Request request) throws RemoteException {
        String cacheKey = request.getIdentifier();
        if (cache.getIfPresent(cacheKey) == null || cache.getIfPresent(cacheKey) != MessageType.SUBSCRIBER_COUNT) {
            cache.put(cacheKey, MessageType.SUBSCRIBER_COUNT);
            long topicId = (long) request.getObject();
            this.subscriberCount.put(topicId, this.subscriberCount.getOrDefault(topicId, 0) + 1);

            // Flood the message to all brokers
            for (IRemoteBroker broker : brokers) {
                broker.increaseSubscriberCount(request);
            }
        }
    }

    @Override
    public void decreaseSubscriberCount(Request request) throws RemoteException {
        String cacheKey = request.getIdentifier();
        if (cache.getIfPresent(cacheKey) == null || cache.getIfPresent(cacheKey) != MessageType.SUBSCRIBER_COUNT) {
            cache.put(cacheKey, MessageType.SUBSCRIBER_COUNT);
            long topicId = (long) request.getObject();
            this.subscriberCount.put(topicId, this.subscriberCount.get(topicId) - 1);

            // Flood the message to all brokers
            for (IRemoteBroker broker : brokers) {
                broker.decreaseSubscriberCount(request);
            }
        }
    }

    private void messageSubscriber(String subscriberName, String message) {
        try {
            IRemoteSubscriber remoteSubscriber = (IRemoteSubscriber) registry.lookup("subscriber/" + subscriberName);
            remoteSubscriber.receiveMessage(message);
        } catch (Exception e) {
            System.err.println("Broker exception: " + e.toString());
        }
    }

    @Override
    public void publishMessage(Message message) throws RemoteException {
        // send message to all brokers if not in cache
        if (cache.getIfPresent(message.getIdentifier()) == null || cache.getIfPresent(message.getIdentifier()) != MessageType.MESSAGE) {
            cache.put(String.valueOf(message.getIdentifier()), MessageType.MESSAGE);

            // send message to all local subscribers
            if (subscriberTopics.containsKey(message.getTopicId())) {
                ArrayList<String> topicSubscribers = subscriberTopics.get(message.getTopicId());
                for (String subscriber : topicSubscribers) {
                    messageSubscriber(subscriber, message.getMessage());
                }
            }

            for (IRemoteBroker broker : brokers) {
                broker.publishMessage(message);
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
        switch (type) {
            case PUBLISHER:
                publishers.put(name, System.currentTimeMillis());
                break;
            case SUBSCRIBER:
                subscribers.put(name, System.currentTimeMillis());
                break;
        }
    }

    @Override
    public void addPublisher(String publisherName) throws RemoteException {
        long currentTimeMillis = System.currentTimeMillis();
        long currentTimeSeconds = currentTimeMillis / 1000;
        publishers.put(publisherName, currentTimeMillis);
        System.out.println("Publisher " + publisherName + " added at " + currentTimeSeconds);
    }

    @Override
    public void removePublisher(Request request) throws RemoteException {
        String cacheKey = request.getIdentifier();
        if (cache.getIfPresent(cacheKey) == null || cache.getIfPresent(cacheKey) != MessageType.REMOVE_PUBLISHER) {
            cache.put(cacheKey, MessageType.REMOVE_PUBLISHER);
            String publisherName = (String) request.getObject();

            // remove all local topics
            this.removeAllLocalTopics(publisherName);
            publishers.remove(publisherName);
            System.out.println("Publisher " + publisherName + " removed.");

            for (IRemoteBroker broker : brokers) {
                broker.removePublisher(request);
            }
        }
    }

    @Override
    public String getSubscribersCount(String publisherName) throws RemoteException {
        StringBuilder res = new StringBuilder();
        for (Topic topic : topics) {
            if (topic.getPublisherName().equals(publisherName)) {
                String topicName = topic.getTopicName();
                long topicId = topic.getTopicId();
                int count = 0;
                if (this.subscriberCount.containsKey(topicId)) {
                    count = this.subscriberCount.get(topicId);
                }
                res.append(topicId).append(" ").append(topicName).append(" ").append(count).append("\n");
            }
        }
        if (res.isEmpty()) {
            res = new StringBuilder("No topics found for publisher: " + publisherName);
        }
        return res.toString();
    }

    @Override
    public int getTopicSubscribersCount(long topicId) throws RemoteException {
        if (subscriberTopics.containsKey(topicId)) {
            return subscriberTopics.get(topicId).size();
        }
        return 0;
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

    public void addSubscriber(String subscriberName) {
        long currentTimeMillis = System.currentTimeMillis();
        long currentTimeSeconds = currentTimeMillis / 1000;
        System.out.println("Subscriber " + subscriberName + " added at " + currentTimeSeconds);
        // Add the subscriber to the subscribers map
        subscribers.put(subscriberName, currentTimeMillis);
    }


    public void removeSubscriber(String subscriberName) {
        this.unsubscribeAll(subscriberName);
        subscribers.remove(subscriberName);
    }

    private void unsubscribeAll(String subscriberName) {
        for (Topic topic : topics) {
            long topicId = topic.getTopicId();
            if (subscriberTopics.containsKey(topicId)) {
                ArrayList<String> topicSubscribers = subscriberTopics.get(topicId);
                topicSubscribers.remove(subscriberName);

                // decrease subscriber count
                Request request = new Request(topicId);
                try {
                    this.decreaseSubscriberCount(request);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void removeAllLocalTopics(String publisherName) {
        ArrayList<Topic> topicsToRemove = new ArrayList<>();
        for (Topic topic : topics) {
            if (topic.getPublisherName().equals(publisherName)) {
                long topicId = topic.getTopicId();
                // notify all subscribers
                if (subscriberTopics.containsKey(topicId)) {
                    ArrayList<String> topicSubscribers = subscriberTopics.get(topicId);
                    for (String subscriber : topicSubscribers) {
                        messageSubscriber(subscriber, "publisher disconnected: " + publisherName + "; " + "you are unsubscribed from topic " + topicId);
                    }
                }

                subscriberTopics.remove(topicId);
                subscriberCount.remove(topicId);
                topicsToRemove.add(topic);
            }
        }
        topics.removeAll(topicsToRemove);
        System.out.println("All topics removed for publisher: " + publisherName);
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
