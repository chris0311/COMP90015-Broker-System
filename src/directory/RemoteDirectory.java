/**
 * @Author: Chris Liang 1159696
 */

package directory;

import remote.IRemoteDirectory;

import java.rmi.RemoteException;  // Required for handling remote communication errors.
import java.rmi.server.UnicastRemoteObject;  // Used to export the remote object and allow the JVM to create a stub for the remote object.
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoteDirectory extends UnicastRemoteObject implements IRemoteDirectory {
    Set<HashMap<String, Integer>> brokers;

    public RemoteDirectory() throws RemoteException {
        brokers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    @Override
    public void addBroker(String address, int port) {
        System.out.println("Adding broker: " + address + ":" + port);
        HashMap<String, Integer> broker = new HashMap<>();
        broker.put(address, port);
        brokers.add(broker);
    }

    public void removeBroker(String address, int port) {
        HashMap<String, Integer> broker = new HashMap<>();
        broker.put(address, port);
        brokers.remove(broker);
    }

    @Override
    public ArrayList<HashMap<String, Integer>> listBrokers() throws RemoteException {
        return new ArrayList<>(brokers);
    }
}
