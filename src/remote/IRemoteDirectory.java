package remote;

import java.rmi.Remote;  // Importing the Remote interface from the java.rmi package.
import java.rmi.RemoteException;  // Importing the RemoteException, necessary for handling communication errors in RMI.
import java.util.ArrayList;
import java.util.HashMap;


public interface IRemoteDirectory extends Remote {
    public ArrayList<HashMap<String, Integer>> listBrokers() throws RemoteException;
    public void addBroker(String address, int port) throws RemoteException;
}
