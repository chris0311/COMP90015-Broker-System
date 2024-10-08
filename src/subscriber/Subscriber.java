package subscriber;

import common.NodeType;
import message.Request;
import message.Topic;
import remote.IRemoteBroker;
import remote.IRemoteDirectory;
import remote.IRemoteSubscriber;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Subscriber {
    private static String subscriberName;
    private static final Scanner scanner = new Scanner(System.in);
    private static IRemoteBroker remoteBroker;
    private static Registry registry;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            // perform remove publisher
            try {
                remoteBroker.removeSubscriber(subscriberName);
                registry.unbind("subscriber/" + subscriberName);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                System.out.println("Subscriber not bound, skipping unbinding.");
            }
        }));

        initConnection(args);
        while(true) {
            System.out.println("Please select command: list, sub, current, unsub.");
            String command = scanner.nextLine();
            String[] commandParts = command.split(" ");
            long topicId;

            switch (commandParts[0]) {
                case "list":
                    try {
                        ArrayList<Topic> topics = remoteBroker.listTopics();
                        for (Topic topic : topics) {
                            System.out.println(topic);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "sub":
                    topicId = validateTopicId(commandParts[1]);
                    if (topicId == -1) {
                        continue;
                    }

                    try {
                        remoteBroker.subscribe(topicId, subscriberName);
                        System.out.println("Subscribed to topic " + topicId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "current":
                    try {
                        ArrayList<Topic> res = remoteBroker.listSubscribedTopics(subscriberName);
                        for (Topic topic : res) {
                            System.out.println(topic);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "unsub":
                    topicId = validateTopicId(commandParts[1]);
                    if (topicId == -1) {
                        continue;
                    }

                    try {
                        remoteBroker.unsubscribe(topicId, subscriberName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    System.out.println("Invalid command");
            }
        }
    }

    private static void initConnection(String[] args) {
        try {
            subscriberName = args[0];
        } catch (Exception e) {
            System.err.println("Invalid Name: " + e.toString());
            System.exit(1);
        }

        // list all brokers and connect to one of them randomly
        ArrayList<HashMap<String, Integer>> brokers = new ArrayList<>();
        try {
            registry = LocateRegistry.getRegistry("localhost", 1099);
            IRemoteDirectory remoteDirectory = (IRemoteDirectory) registry.lookup("RemoteDirectory");
            brokers = remoteDirectory.listBrokers();

            // randomly select a broker
            int randomBroker = (int) (Math.random() * brokers.size());
            String address = brokers.get(randomBroker).keySet().iterator().next();
            int port = brokers.get(randomBroker).get(address);
            remoteBroker = (IRemoteBroker) registry.lookup(address + ":" + port);
            remoteBroker.addSubscriber(subscriberName);

            // Start a separate thread for pinging the server
            Thread pingThread = new Thread(() -> {
                try {
                    while (true) {
                        remoteBroker.ping(subscriberName, NodeType.SUBSCRIBER);
                        Thread.sleep(1000); // Ping every second
                    }
                } catch (Exception e) {
                    System.err.println("Ping thread error: " + e.getMessage());
                }
            });

            pingThread.setDaemon(true); // Set as daemon so it terminates when the main thread ends
            pingThread.start();

            // register subscriber
            RemoteSubscriber remoteSubscriber = new RemoteSubscriber();
            registry.rebind("subscriber/" + subscriberName, remoteSubscriber);
        } catch (Exception e) {
            System.err.println("Subscriber exception: " + e.toString());
            System.exit(1);
        }
    }

    private static long validateTopicId(String topicId) {
        try {
            return Long.parseLong(topicId);
        } catch (NumberFormatException e) {
            System.err.println("Invalid Topic ID: " + e.toString());
        }
        return -1;
    }
}
