package subscriber;

import remote.IRemoteSubscriber;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteSubscriber extends UnicastRemoteObject implements IRemoteSubscriber {

    public RemoteSubscriber() throws RemoteException {
    }

    @Override
    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);
    }
}
