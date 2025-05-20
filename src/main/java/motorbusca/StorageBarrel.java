package motorbusca;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface StorageBarrel extends Remote {
    void saveIndex(String word, PageInfo pageInfo) throws RemoteException;
    void saveReplica(String word, PageInfo pageInfo) throws RemoteException;
    void registerReplica(StorageBarrel replica) throws RemoteException;
    int getIndexSize() throws RemoteException;
    String getBarrelId() throws RemoteException;
    List<PageInfo> getPagesByWord(String word) throws RemoteException;
    Map<String, List<PageInfo>> getCompleteIndex() throws RemoteException;
}
