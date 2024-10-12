/**
 * @Author: Chris Liang 1159696
 */

import remote.IRemoteSubscriber;
import subscriber.RemoteSubscriber;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            RemoteSubscriber remoteSubscriber = new RemoteSubscriber();
            registry.rebind("RemoteSubscriber", remoteSubscriber);
            System.out.println("RemoteSubscriber bound to registry");

            IRemoteSubscriber remoteSubscriberStub = (IRemoteSubscriber) registry.lookup("RemoteSubscriber");
            remoteSubscriberStub.receiveMessage("Hello, World!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}