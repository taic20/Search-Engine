package motorbusca;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.*;
import java.net.*;

public class StorageBarrelImpl extends UnicastRemoteObject implements StorageBarrel {

    private String barrelId;
    private HashMap<String, ArrayList<PageInfo>> indice;
    private String filename;
    private Index indexServer;
    private List<StorageBarrel> replicas;

    public StorageBarrelImpl(String barrelId) throws RemoteException {
        super();
        this.barrelId = barrelId;
        this.filename = "barrel_" + barrelId + ".dat";
        this.indice = new HashMap<>();
        this.replicas = new ArrayList<>();

        try {
            loadFromFile();
        } catch (Exception e) {
            System.out.println("Arquivo de índice não encontrado. Criando novo índice.");
        }
    }

    public void connectToServer() {
        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) {
                props.load(fis);
            }

            String host = props.getProperty("server.host", "localhost");
            int port = Integer.parseInt(props.getProperty("server.port", "8183"));

            Registry registry = LocateRegistry.getRegistry(host, port);
            indexServer = (Index) registry.lookup("index");
            indexServer.registerBarrel(barrelId, getIndexSize());

            System.out.println("Barrel " + barrelId + " conectado ao servidor com sucesso.");
        } catch (Exception e) {
            System.out.println("Erro ao conectar ao servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void saveIndex(String word, PageInfo pageInfo) throws RemoteException {
        if (word == null || word.isEmpty() || pageInfo == null)
            return;

        word = word.toLowerCase();
        boolean isNew = false;

        indice.computeIfAbsent(word, k -> new ArrayList<>());

        if (!indice.get(word).contains(pageInfo)) {
            indice.get(word).add(pageInfo);
            isNew = true;
        }

        // Multicast para réplicas mesmo que a palavra já exista, mas só se for novo pageInfo
        if (isNew) {
            for (StorageBarrel replica : replicas) {
                try {
                    replica.saveReplica(word, pageInfo);
                } catch (RemoteException e) {
                    System.err.println("Erro multicast para réplica: " + e.getMessage());
                }
            }

            saveToFile();

            if (indexServer != null) {
                indexServer.updateBarrelIndexSize(barrelId, getIndexSize());
            }
        }
    }


    @Override
    public synchronized void saveReplica(String word, PageInfo pageInfo) throws RemoteException {
        word = word.toLowerCase();
        indice.computeIfAbsent(word, k -> new ArrayList<>());

        if (!indice.get(word).contains(pageInfo)) {
            indice.get(word).add(pageInfo);
            saveToFile();

            // Atualiza o servidor com o novo tamanho
            if (indexServer != null) {
                try {
                    indexServer.updateBarrelIndexSize(barrelId, getIndexSize());
                } catch (Exception e) {
                    System.err.println("Erro ao atualizar tamanho do índice (réplica): " + e.getMessage());
                }
            }
        }
    }


    @Override
    public synchronized void registerReplica(StorageBarrel replica) throws RemoteException {
        replicas.add(replica);
        System.out.println("Nova réplica registada com sucesso.");
    }

    @Override
    public synchronized Map<String, List<PageInfo>> getCompleteIndex() throws RemoteException {
        Map<String, List<PageInfo>> result = new HashMap<>();
        for (Map.Entry<String, ArrayList<PageInfo>> entry : indice.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }


    @Override
    public synchronized List<PageInfo> getPagesByWord(String word) throws RemoteException {
        String search = word.toLowerCase().trim();
        List<PageInfo> result = indice.getOrDefault(search, new ArrayList<>());
        return new ArrayList<>(result);
    }



    @Override
    public synchronized int getIndexSize() throws RemoteException {
        return indice.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public String getBarrelId() throws RemoteException {
        return barrelId;
    }

    public synchronized void saveToFile() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(indice);
            System.out.println("Índice salvo em " + filename);
        } catch (IOException e) {
            System.out.println("Erro ao salvar índice: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadFromFile() throws RemoteException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            indice = (HashMap<String, ArrayList<PageInfo>>) in.readObject();
            System.out.println("Índice carregado com sucesso.");
        } catch (IOException | ClassNotFoundException e) {
            throw new RemoteException("Erro ao carregar índice", e);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java StorageBarrelImpl <barrelId> [primaryHost primaryPort]");
            return;
        }

        try {
            // Define o IP RMI dinamicamente
            System.setProperty("java.rmi.server.hostname", getLocalIP());
            System.out.println("IP local definido para RMI: " + System.getProperty("java.rmi.server.hostname"));

            StorageBarrelImpl barrel = new StorageBarrelImpl(args[0]);

            Registry registry = LocateRegistry.createRegistry(8184 + Integer.parseInt(args[0]));
            registry.rebind("barrel" + args[0], barrel);
            barrel.connectToServer();

            if (args.length == 3) {
                String primaryHost = args[1];
                int primaryPort = Integer.parseInt(args[2]);
                Registry primaryRegistry = LocateRegistry.getRegistry(primaryHost, primaryPort);
                StorageBarrel primary = (StorageBarrel) primaryRegistry.lookup("barrel0");
                primary.registerReplica(barrel);
                System.out.println("Registrado como réplica do barrel primário.");
            }

            System.out.println("Barrel " + args[0] + " operacional.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String getLocalIP() {
        try {
            for (java.net.NetworkInterface ni : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                for (java.net.InetAddress address : java.util.Collections.list(ni.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1"; // fallback
    }

}
