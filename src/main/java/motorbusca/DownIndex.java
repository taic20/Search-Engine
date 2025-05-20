package motorbusca;

import java.rmi.Remote;

public interface DownIndex extends Remote {
    public String get_url() throws java.rmi.RemoteException;
    public void put_url(String url) throws java.rmi.RemoteException;
    public void save_words(String word, String url) throws java.rmi.RemoteException;
}
