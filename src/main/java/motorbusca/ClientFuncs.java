package motorbusca;
import java.rmi.*;
import java.util.*;

public interface ClientFuncs extends Remote {
    public void printOnClient() throws RemoteException;
}
