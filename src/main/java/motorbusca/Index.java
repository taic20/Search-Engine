package motorbusca;

import java.rmi.*;
import java.util.*;

public interface Index extends Remote {
    public String takeNext() throws RemoteException;
    public void putNew(String url) throws RemoteException;
    public void addToIndex(String word, String url, String title, String snippet) throws RemoteException;
    public List<PageInfo> searchWord(String word) throws RemoteException;
    public boolean registerClient(ClientFuncs c) throws RemoteException;
    public void registarLigacao(String urlDestino, String urlOrigem) throws RemoteException;
    public List<String> getLigacoesRecebidas(String urlDestino) throws RemoteException;
    

    // Novos métodos para estatísticas
    public List<Map.Entry<String, Integer>> getTopSearches(int limit) throws RemoteException;
    public boolean estatisticasForamAtualizadas() throws RemoteException;
    public List<String> getTopSearchesFormatted(int limit) throws RemoteException;
    public Map<String, Map<String, Object>> getBarrelDetails() throws RemoteException;



    // Novos métodos para Barrels e Gateway
    public void registerBarrel(String barrelId, int indexSize) throws RemoteException;
    public void updateBarrelIndexSize(String barrelId, int newSize) throws RemoteException;
    public void registerResponseTime(String barrelId, double time) throws RemoteException;
    public List<PageInfo> searchTerms(List<String> terms) throws RemoteException;

}