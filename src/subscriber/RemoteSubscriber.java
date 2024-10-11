package subscriber;

import remote.IRemoteSubscriber;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RemoteSubscriber extends UnicastRemoteObject implements IRemoteSubscriber {

    public RemoteSubscriber() throws RemoteException {
    }

    @Override
    public void receiveMessage(String message) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM hh:mm:ss");
        String formattedDateTime = now.format(formatter);
        System.out.println("[" + formattedDateTime + "] " + message);
    }
}
