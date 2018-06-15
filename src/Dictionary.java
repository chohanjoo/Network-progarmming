import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Dictionary extends Remote {

	public void setDictionary() throws RemoteException;

	public boolean checkWord(String word) throws RemoteException;
}
