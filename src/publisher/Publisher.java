package publisher;

import remote.IRemoteBroker;
import remote.IRemoteDirectory;

import java.rmi.RemoteException;  // Required for handling remote communication errors.
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Publisher {
    private static Registry registry;
    private static String publisherName;
    private static final Scanner scanner = new Scanner(System.in);
    private static IRemoteBroker remoteBroker;

    public static void main(String[] args) {
        initConnection(args);

        while (true) {
            // show cli commands
            System.out.println("Please select command: create, publish, show, delete.");
            String command = scanner.nextLine();
            String[] commandParts = command.split(" ");
            long topicId;
            switch (commandParts[0]) {
                case "create":
                    topicId = validateTopicId(commandParts[1]);
                    if (topicId == -1) {
                        continue;
                    }

                    try {
                        remoteBroker.addTopic(topicId, commandParts[2], publisherName);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case "publish":
                    topicId = validateTopicId(commandParts[1]);
                    if (topicId == -1) {
                        continue;
                    }

                    try {
                        remoteBroker.publishMessage(topicId, commandParts[2]);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case "show":
                    try {
                        String res = remoteBroker.getSubscribersCount(publisherName);
                        System.out.println(res);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case "delete":
                    topicId = validateTopicId(commandParts[1]);
                    if (topicId == -1) {
                        continue;
                    }

                    try {
                        remoteBroker.removeTopic(topicId);
                    } catch (RemoteException e) {
                        e.printStackTrace();
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
            System.err.println("Invalid Name: " + e.toString());
            System.exit(1);
        }

        try {
            registry = LocateRegistry.getRegistry("localhost", 1099);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // list all brokers and connect to one of them randomly
        ArrayList<HashMap<String, Integer>> brokers = new ArrayList<>();
        try {
            IRemoteDirectory remoteDirectory = (IRemoteDirectory) registry.lookup("RemoteDirectory");
            brokers = remoteDirectory.listBrokers();

            // randomly select a broker
            int randomBroker = (int) (Math.random() * brokers.size());
            String address = brokers.get(randomBroker).keySet().iterator().next();
            int port = brokers.get(randomBroker).get(address);
            remoteBroker = (IRemoteBroker) registry.lookup(address + ":" + port);
            remoteBroker.addPublisher(publisherName);
        } catch (Exception e) {
            System.err.println("Publisher exception: " + e.toString());
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
