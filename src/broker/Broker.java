package broker;

import remote.IRemoteBroker;
import remote.IRemoteDirectory;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;

public class Broker {
    private static Registry registry;
    private static IRemoteDirectory remoteDirectory;
    private static int port;

    public static void main(String[] args) {
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.err.println("Invalid Port: " + e.toString());
            System.exit(1);
        }

        try {
            registry = LocateRegistry.getRegistry("localhost", 1099);
            remoteDirectory = (IRemoteDirectory) registry.lookup("RemoteDirectory");
            // list all brokers
            System.out.println(remoteDirectory.listBrokers());

            // create a list of remote brokers
            ArrayList<IRemoteBroker> brokers = new ArrayList<>();
            for (HashMap<String, Integer> broker : remoteDirectory.listBrokers()) {
                String address = broker.keySet().iterator().next();
                int port = broker.get(address);
                brokers.add((IRemoteBroker) registry.lookup("localhost" + ":" + port));
            }

            // create a new remote broker
            RemoteBroker remoteBroker = new RemoteBroker(brokers, port);
            registry.rebind("localhost" + ":" + port, remoteBroker);
            remoteDirectory.addBroker("localhost", port);
            System.out.println("Broker bound to registry");
            remoteBroker.connectAllBrokers();

        } catch (Exception e) {
            System.err.println("Broker exception: " + e.toString());
            System.exit(1);
        }
    }

}
