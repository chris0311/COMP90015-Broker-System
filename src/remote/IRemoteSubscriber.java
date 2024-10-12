/**
 * @Author: Chris Liang 1159696
 */

package remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRemoteSubscriber extends Remote {
    void receiveMessage(String message) throws RemoteException;
}
