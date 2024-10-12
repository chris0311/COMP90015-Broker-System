/**
 * @Author: Chris Liang 1159696
 */

package publisher;

import common.NodeType;
import error.DuplicateRequestException;
import error.ResourceNotFoundException;
import message.Message;
import message.Request;
import message.Topic;
import remote.IRemoteBroker;
import remote.IRemoteDirectory;
import subscriber.AccessDeniedException;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Publisher {
    private static final Scanner scanner = new Scanner(System.in);
    private static Registry registry;
    private static String publisherName;
    private static IRemoteBroker remoteBroker;

    public static void main(String[] args) {
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Request request = new Request(publisherName);
                remoteBroker.removePublisher(request);
            } catch (ConnectException | UnmarshalException e) {
                System.out.println("Broker disconnected.");
            } catch (RemoteException | DuplicateRequestException e) {
                System.err.println("Remote error, check broker connection: " + e.getMessage());
            } catch (Exception ignored) {
            }
        }));

        initConnection(args);

        while (true) {
            // show cli commands
            System.out.println("""
                    Please select command: create, publish, show, delete.
                    1. create {topic_id} {topic_name} # create a new topic
                    2. publish {topic_id} {message} # publish a message to an existing topic
                    3. show # show all subscriber count for current publisher
                    4. delete {topic_id} #delete a topic""");
            String command = scanner.nextLine();
            String[] commandParts = command.split(" ");
            long topicId;
            switch (commandParts[0]) {
                case "create":
                    if (commandParts.length < 3) {
                        System.out.println("ERROR: Invalid command.");
                        continue;
                    }
                    topicId = validateTopicId(commandParts[1]);
                    if (topicId == -1) {
                        continue;
                    }

                    try {
                        Topic topic = new Topic(topicId, commandParts[2], publisherName);
                        Request request = new Request(topic);
                        remoteBroker.addTopic(request);
                        System.out.println("SUCCESS: Topic created.");
                    } catch (RemoteException e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }
                    break;
                case "publish":
                    if (commandParts.length < 3) {
                        System.out.println("ERROR: Invalid command.");
                        continue;
                    }
                    topicId = validateTopicId(commandParts[1]);
                    if (topicId == -1) {
                        continue;
                    }

                    try {
                        Message message = new Message(topicId, commandParts[2], publisherName);
                        remoteBroker.publishMessage(message);
                        System.out.println("SUCCESS: Message published.");
                    } catch (RemoteException | ResourceNotFoundException | AccessDeniedException e) {
                        System.out.println(e.getMessage());
                    }
                    break;
                case "show":
                    try {
                        String res = remoteBroker.getSubscribersCount(publisherName);
                        System.out.println(res);
                    } catch (RemoteException e) {
                        System.out.println(e.getMessage());
                    }
                    break;
                case "delete":
                    if (commandParts.length < 2) {
                        System.out.println("ERROR: Invalid command.");
                        continue;
                    }
                    topicId = validateTopicId(commandParts[1]);
                    if (topicId == -1) {
                        continue;
                    }

                    try {
                        Request request = new Request(publisherName, topicId);
                        remoteBroker.removeTopic(request);
                        System.out.println("SUCCESS: Topic deleted.");
                    } catch (RemoteException | DuplicateRequestException | AccessDeniedException e) {
                        System.out.println(e.getMessage());
                    }
                    break;
                default:
                    System.out.println("Invalid command.");
            }
        }

    }

    private static void initConnection(String[] args) {
        try {
            publisherName = args[0];
        } catch (Exception e) {
            System.err.println("ERROR: Invalid Name: " + e.toString());
            System.exit(1);
        }

        try {
            registry = LocateRegistry.getRegistry("localhost", 1099);
        } catch (RemoteException e) {
            System.err.println("ERROR: Registry error: " + e.toString());
        }

        // list all brokers and connect to one of them randomly
        ArrayList<HashMap<String, Integer>> brokers = new ArrayList<>();
        try {
            IRemoteDirectory remoteDirectory = (IRemoteDirectory) registry.lookup("RemoteDirectory");
            brokers = remoteDirectory.listBrokers();
            if (brokers.isEmpty()) {
                System.err.println("ERROR: No brokers available.");
                System.exit(1);
            }

            // randomly select a broker
            int randomBroker = (int) (Math.random() * brokers.size());
            String address = brokers.get(randomBroker).keySet().iterator().next();
            int port = brokers.get(randomBroker).get(address);
            remoteBroker = (IRemoteBroker) registry.lookup(address + ":" + port);
            remoteBroker.addPublisher(publisherName);

            // Start a separate thread for pinging the server
            Thread pingThread = new Thread(() -> {
                try {
                    while (true) {
                        remoteBroker.ping(publisherName, NodeType.PUBLISHER);
                        Thread.sleep(1000); // Ping every second
                    }
                } catch (Exception e) {
                    System.err.println("ERROR: Ping thread error: " + e.getMessage());
                }
            });

            pingThread.setDaemon(true); // Set as daemon so it terminates when the main thread ends
            pingThread.start();

        } catch (RemoteException e) {
            System.err.println("ERROR: Registry error: " + e.toString());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("ERROR: Publisher exception: " + e.toString());
            System.exit(1);
        }
    }

    private static long validateTopicId(String topicId) {
        try {
            return Long.parseLong(topicId);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Invalid Topic ID: " + e.toString());
        }
        return -1;
    }
}
