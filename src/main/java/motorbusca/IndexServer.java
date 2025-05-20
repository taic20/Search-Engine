package motorbusca;

import org.jsoup.Jsoup;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.concurrent.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jsoup.nodes.Document;
import java.net.*;




public class IndexServer extends UnicastRemoteObject implements Index, DownIndex {
    private ArrayList<String> listaParaFazerCrawl;
    private HashMap<String, ArrayList<PageInfo>> indiceParaPesquisas;
    private ClientFuncs client;
    private HashMap<String, Set<String>> linksRecebidos;
    private int roundRobinCounter = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<String, Set<String>> backlinks = new HashMap<>();

    

    // Nova estrutura para rastrear estatísticas de pesquisa
    private HashMap<String, Integer> estatisticasPesquisa;
    private boolean estatisticasAtualizadas;

    // Estruturas para Barrels e Gateway
    private Map<String, Integer> barrelsAtivos; // Nome do Barrel -> Tamanho do índice
    private Map<String, List<Double>> temposRespostaBarrels; // Nome do Barrel -> Lista de tempos de resposta

    public IndexServer() throws RemoteException {
        super();
        listaParaFazerCrawl = new ArrayList<>();
        indiceParaPesquisas = new HashMap<String, ArrayList<PageInfo>>();
        linksRecebidos = new HashMap<>();
        estatisticasPesquisa = new HashMap<>();
        estatisticasAtualizadas = false;

        // Inicializar estruturas para Barrels e Gateway
        barrelsAtivos = new HashMap<>();
        temposRespostaBarrels = new HashMap<>();


    }

public static String getLocalIP() {
    try {
        for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
            for (InetAddress address : java.util.Collections.list(ni.getInetAddresses())) {
                if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                    return address.getHostAddress();
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return "127.0.0.1"; // fallback
}


    public static void main(String args[]) {
        try {
            // Define dinamicamente o IP público para o RMI
            System.setProperty("java.rmi.server.hostname", getLocalIP());
            System.out.println("IP RMI definido para: " + System.getProperty("java.rmi.server.hostname"));

            IndexServer server = new IndexServer();
            Registry registry = LocateRegistry.createRegistry(8183);
            registry.rebind("index", server);
            System.out.println("Server ready. Waiting for input...");

            Scanner scanner = new Scanner(System.in);
            while (true){
                System.out.println("Choose an option:");
                System.out.println("1. Add URL for indexing");
                System.out.println("2. Search indexed URLs");
                System.out.println("3. View search statistics");
                System.out.println("4. Teste conexao barrels");
                System.out.println("5. Consultar páginas que apontam para um URL");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice){
                    case 1:
                        System.out.print("Enter URL to add: ");
                        String url = scanner.nextLine();
                        server.putNew(url);
                        System.out.println("URL added for indexing.");
                        break;
                    case 2:
                        System.out.print("Enter words for search (separated by spaces): ");
                        String searchInput = scanner.nextLine();
                        List<String> searchTerms = Arrays.asList(searchInput.toLowerCase().trim().split("\\s+"));
                        List<PageInfo> results = server.searchTerms(searchTerms);

                        if (results.isEmpty()) {
                            System.out.println("No results found for the terms: " + searchInput);
                        } else {
                            System.out.println("Search results for: " + searchInput);

                            int totalResults = results.size();
                            int index = 0; // Índice inicial
                            boolean showMore = true;

                            while (showMore && index < totalResults) {
                                int end = Math.min(index + 10, totalResults); // Máximo de 10 por página

                                for (int i = index; i < end; i++) {
                                    PageInfo result = results.get(i);
                                    System.out.println((i + 1) + ". Title: " + result.getTitle());
                                    System.out.println("   URL: " + result.getUrl());
                                    System.out.println("   Snippet: " + result.getSnippet());
                                    System.out.println();
                                }

                                index += 10;

                                if (index < totalResults) {
                                    System.out.print("Press ENTER to see more results or type a number to select a page: ");
                                    String inputNext = scanner.nextLine();

                                    if (!inputNext.isEmpty()) {
                                        try {
                                            int choiceNumber = Integer.parseInt(inputNext);
                                            if (choiceNumber > 0 && choiceNumber <= totalResults) {
                                                String selectedUrl = results.get(choiceNumber - 1).getUrl();
                                                List<String> ligacoes = server.getLigacoesRecebidas(selectedUrl);

                                                if (ligacoes.isEmpty()) {
                                                    System.out.println("No incoming links found for: " + selectedUrl);
                                                } else {
                                                    System.out.println("Incoming links for " + selectedUrl + ":");
                                                    for (String link : ligacoes) {
                                                        System.out.println(" - " + link);
                                                    }
                                                }
                                            }
                                            showMore = false;
                                        } catch (NumberFormatException e) {
                                            // Continua
                                        }
                                    }
                                } else {
                                    System.out.print("Enter the number of the result to see incoming links (or 0 to go back): ");
                                    String inputFinal = scanner.nextLine();
                                    try {
                                        int choiceNumber = Integer.parseInt(inputFinal);
                                        if (choiceNumber > 0 && choiceNumber <= totalResults) {
                                            String selectedUrl = results.get(choiceNumber - 1).getUrl();
                                            List<String> ligacoes = server.getLigacoesRecebidas(selectedUrl);

                                            if (ligacoes.isEmpty()) {
                                                System.out.println("No incoming links found for: " + selectedUrl);
                                            } else {
                                                System.out.println("Incoming links for " + selectedUrl + ":");
                                                for (String link : ligacoes) {
                                                    System.out.println(" - " + link);
                                                }
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        // Ignora e sai
                                    }
                                    showMore = false;
                                }
                            }
                        }
                        break;

                    case 3:
                        // Exibir estatísticas de pesquisa
                        List<Map.Entry<String, Integer>> topSearches = server.getTopSearches(10);
                        if (topSearches.isEmpty()) {
                            System.out.println("No search statistics available yet.");
                        } else {
                            System.out.println("Top 10 most common searches:");
                            for (int i = 0; i < topSearches.size(); i++) {
                                Map.Entry<String, Integer> entry = topSearches.get(i);
                                System.out.println((i + 1) + ". \"" + entry.getKey() + "\" - " + entry.getValue() + " searches");
                            }
                        }

                        // Exibir informações sobre Barrels ativos
                        System.out.println("\n===== Storage Barrels Information =====");
                        Map<String, Map<String, Object>> barrelDetails = server.getBarrelDetails();

                        if (barrelDetails.isEmpty()) {
                            System.out.println("No active Storage Barrels connected.");
                        } else {
                            System.out.println("Active Storage Barrels:");
                            for (Map.Entry<String, Map<String, Object>> entry : barrelDetails.entrySet()) {
                                String barrelId = entry.getKey();
                                Map<String, Object> info = entry.getValue();

                                int indexSize = (Integer) info.get("indexSize");
                                double avgTime = (Double) info.get("avgResponseTime");

                                System.out.println(" - Barrel " + barrelId + ":");
                                System.out.println("   * Index size: " + indexSize + " entries");
                                System.out.println("   * Avg response time: " + String.format("%.3f", avgTime) + " seconds");
                            }
                        }

                        // Exibir informações de status do gateway
                        System.out.println("\n===== Gateway Status =====");
                        Map<String, Object> gatewayStatus = server.getGatewayStatus();

                        int activeBarrels = (Integer) gatewayStatus.get("activeBarrels");
                        int totalIndexSize = (Integer) gatewayStatus.get("totalIndexSize");
                        double globalAvgResponseTime = (Double) gatewayStatus.get("globalAvgResponseTime");

                        System.out.println("Active barrels: " + activeBarrels);
                        System.out.println("Total index size: " + totalIndexSize + " entries");
                        System.out.println("Global average response time: " + String.format("%.3f", globalAvgResponseTime) + " seconds");

                        break;
                    case 4: // Nova opção para teste
                        server.verifyBarrelsConnection();
                        break;
                    case 5:
                        System.out.print("Introduz o URL para ver quem aponta para ele: ");
                        String destino = scanner.nextLine();
                        List<String> ligacoes = server.getLigacoesRecebidas(destino);

                        if (ligacoes.isEmpty()) {
                            System.out.println("Nenhuma página aponta para: " + destino);
                        } else {
                            System.out.println("Páginas que apontam para " + destino + ":");
                            for (String origem : ligacoes) {
                                System.out.println(" - " + origem);
                            }
                        }
                        break;
                    case 6:
                        System.out.println("Exiting...");
                        scanner.close();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Métodos para obter informações sobre Barrels
    public synchronized Map<String, Integer> getBarrelsAtivos() {
        return new HashMap<>(barrelsAtivos);
    }

    public synchronized Map<String, Double> getTemposMediaRespostaBarrels() {
        Map<String, Double> medias = new HashMap<>();

        for (Map.Entry<String, List<Double>> entry : temposRespostaBarrels.entrySet()) {
            String barrel = entry.getKey();
            List<Double> tempos = entry.getValue();

            if (!tempos.isEmpty()) {
                double soma = 0;
                for (Double tempo : tempos) {
                    soma += tempo;
                }
                double media = soma / tempos.size();
                medias.put(barrel, media);
            } else {
                medias.put(barrel, 0.0);
            }
        }

        return medias;
    }

    // Método para registrar um novo Barrel (para ser chamado por Barrels remotos)
    public synchronized void registerBarrel(String barrelId, int indexSize) throws RemoteException {
        barrelsAtivos.put(barrelId, indexSize);
        if (!temposRespostaBarrels.containsKey(barrelId)) {
            temposRespostaBarrels.put(barrelId, new ArrayList<>());
        }
    }

    // Método para atualizar o tamanho do índice de um Barrel
    public synchronized void updateBarrelIndexSize(String barrelId, int newSize) throws RemoteException {
        if (barrelsAtivos.containsKey(barrelId)) {
            barrelsAtivos.put(barrelId, newSize);
        }
    }

    // Método para registrar um novo tempo de resposta para um Barrel
    public synchronized void registerResponseTime(String barrelId, double time) throws RemoteException {
        if (temposRespostaBarrels.containsKey(barrelId)) {
            temposRespostaBarrels.get(barrelId).add(time);
        } else {
            List<Double> tempos = new ArrayList<>();
            tempos.add(time);
            temposRespostaBarrels.put(barrelId, tempos);
        }
        System.out.printf("⏱️  Registrado tempo %.6f segundos para barrel %s\n", time, barrelId);

    }

    private long counter = 0, timestamp = System.currentTimeMillis();;

    public void printOnClient(){
        if(client!=null){
            try {
                client.printOnClient();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String takeNext() throws RemoteException {
        lock.writeLock().lock(); // ainda é escrita, pois remove da lista
        try {
            if (!listaParaFazerCrawl.isEmpty()) {
                return listaParaFazerCrawl.remove(0);
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }


    synchronized public void registarLigacao(String urlDestino, String urlOrigem) throws RemoteException {
        if (!linksRecebidos.containsKey(urlDestino)) {
            linksRecebidos.put(urlDestino, new HashSet<>());
        }
        linksRecebidos.get(urlDestino).add(urlOrigem);
    }

    @Override
    public void putNew(String url) throws RemoteException {
        lock.writeLock().lock();
        try {
            if (!listaParaFazerCrawl.contains(url)) {
                listaParaFazerCrawl.add(url);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    public void addToIndex(String word, String url, String title, String snippet) throws RemoteException {
        lock.writeLock().lock();
        try {
            PageInfo pageInfo = new PageInfo(url, title, snippet);
            indiceParaPesquisas.computeIfAbsent(word, k -> new ArrayList<>());

            if (!indiceParaPesquisas.get(word).contains(pageInfo)) {
                indiceParaPesquisas.get(word).add(pageInfo);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    
    // Modify searchWord() to query both partitions
    @Override
    public List<PageInfo> searchWord(String word) throws RemoteException {
        lock.readLock().lock();
        try {
            System.out.println("Iniciando pesquisa. Barrels ativos: " + barrelsAtivos.keySet());

            String searchTerm = word.toLowerCase().trim();
            atualizarEstatisticasPesquisa(searchTerm); // pode mover para writeLock se quiser mais rigidez

            Set<PageInfo> uniqueResults = new HashSet<>();
            for (String barrelId : barrelsAtivos.keySet()) {
                try {
                    int port = 8184 + Integer.parseInt(barrelId);
                    Registry registry = LocateRegistry.getRegistry("localhost", port);
                    StorageBarrel barrel = (StorageBarrel) registry.lookup("barrel" + barrelId);

                    long startTime = System.nanoTime();
                    List<PageInfo> pages = barrel.getPagesByWord(searchTerm);
                    long endTime = System.nanoTime();

                    double durationMs = (endTime - startTime) / 1_000_000.0;

                    uniqueResults.addAll(pages);
                    registerResponseTime(barrelId, durationMs);

                } catch (Exception e) {
                    System.err.println("Erro ao contactar barrel " + barrelId + ": " + e.getMessage());
                }
            }


            List<PageInfo> sortedResults = new ArrayList<>(uniqueResults);
            sortedResults.sort((p1, p2) -> {
                int l1 = linksRecebidos.getOrDefault(p1.getUrl(), Set.of()).size();
                int l2 = linksRecebidos.getOrDefault(p2.getUrl(), Set.of()).size();
                return Integer.compare(l2, l1);
            });

            return sortedResults;

        } finally {
            lock.readLock().unlock();
        }
    }




    public synchronized void verifyBarrelsConnection() throws RemoteException {
        System.out.println("\n=== Verificação de Barrels ===");
        for (String barrelId : barrelsAtivos.keySet()) {
            try {
                int port = 8184 + Integer.parseInt(barrelId);
                Registry registry = LocateRegistry.getRegistry("localhost", port);
                StorageBarrel barrel = (StorageBarrel) registry.lookup("barrel" + barrelId);

                System.out.println("Barrel " + barrelId + " (porta " + port + "): OK");
                System.out.println("  Palavras indexadas: " + barrel.getIndexSize());

                // Teste de pesquisa simples
                List<PageInfo> testResults = barrel.getPagesByWord("page");
                System.out.println("  Teste 'page': " + testResults.size() + " resultados");

            } catch (Exception e) {
                System.out.println("Barrel " + barrelId + ": ERRO - " + e.getMessage());
            }
        }
        System.out.println("=============================\n");
    }

    // Método para atualizar estatísticas de pesquisa
    private synchronized void atualizarEstatisticasPesquisa(String palavra) {
        palavra = palavra.toLowerCase().trim();
        if (!palavra.isEmpty()) {
            int count = estatisticasPesquisa.getOrDefault(palavra, 0);
            estatisticasPesquisa.put(palavra, count + 1);
            estatisticasAtualizadas = true;
            System.out.println("Atualizado: " + palavra);
        }
    }

    // Método para obter as pesquisas mais comuns
    public synchronized List<Map.Entry<String, Integer>> getTopSearches(int limit) throws RemoteException {
        List<Map.Entry<String, Integer>> sortedSearches = estatisticasPesquisa.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
                System.out.println("Top searches: " + sortedSearches);


        estatisticasAtualizadas = false;
        return sortedSearches;
    }
    @Override
    public synchronized List<String> getTopSearchesFormatted(int limit) throws RemoteException {
        return estatisticasPesquisa.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)  // ESSENCIAL
            .map(entry -> entry.getKey() + " (" + entry.getValue() + " pesquisas)")
            .collect(Collectors.toList());
    }

    // Método para verificar se as estatísticas foram atualizadas
    public synchronized boolean estatisticasForamAtualizadas() throws RemoteException {
        return estatisticasAtualizadas;
    }

    @Override
    public List<PageInfo> searchTerms(List<String> terms) throws RemoteException {
        if (terms.isEmpty()) return new ArrayList<>();

        Set<PageInfo> resultFinal = null;

        for (String term : terms) {
            String searchTerm = term.toLowerCase().trim();
            atualizarEstatisticasPesquisa(searchTerm); // <--- ESSENCIAL!

            Set<PageInfo> combined = new HashSet<>();

            for (String barrelId : barrelsAtivos.keySet()) {
                try {
                    int port = 8184 + Integer.parseInt(barrelId);
                    Registry registry = LocateRegistry.getRegistry("localhost", port);
                    StorageBarrel barrel = (StorageBarrel) registry.lookup("barrel" + barrelId);

                    long start = System.nanoTime();
                    List<PageInfo> pages = barrel.getPagesByWord(searchTerm);
                    long end = System.nanoTime();

                    double duration = (end - start) / 1_000_000.0;
                    registerResponseTime(barrelId, duration);

                    combined.addAll(pages);
                } catch (Exception e) {
                    System.err.println("Erro ao contactar barrel " + barrelId + ": " + e.getMessage());
                }
            }

            if (resultFinal == null) {
                resultFinal = combined;
            } else {
                resultFinal.retainAll(combined);
            }
        }


        if (resultFinal == null) return new ArrayList<>();

        List<PageInfo> sorted = new ArrayList<>(resultFinal);
        sorted.sort((p1, p2) -> {
            int l1 = linksRecebidos.getOrDefault(p1.getUrl(), Set.of()).size();
            int l2 = linksRecebidos.getOrDefault(p2.getUrl(), Set.of()).size();
            return Integer.compare(l2, l1);
        });

        return sorted;
    }


    synchronized public List<String> getLigacoesRecebidas(String urlDestino) throws RemoteException {
        if (linksRecebidos.containsKey(urlDestino)) {
            return new ArrayList<>(linksRecebidos.get(urlDestino)); // Retorna a lista de origens
        }
        return new ArrayList<>(); // Retorna lista vazia se não houver ligações
    }

    @Override
    public boolean registerClient(ClientFuncs c) throws RemoteException {
        this.client = c;
        return true;
    }

    @Override
    public String get_url() throws RemoteException {
        if (!listaParaFazerCrawl.isEmpty()) {
            String url = listaParaFazerCrawl.get(0);
            listaParaFazerCrawl.remove(0);
            return url;
        }
        return null;
    }

    @Override
    public void put_url(String url) throws RemoteException {
        listaParaFazerCrawl.add(url);
    }

    public void save_words(String word, String url) throws RemoteException {
        lock.writeLock().lock();
        try {
            try {
                Document doc = Jsoup.connect(url).get();
                String title = doc.title();
                String snippet = doc.body().text();
                if (snippet.length() > 100) {
                    snippet = snippet.substring(0, 100) + "...";
                }
                addToIndex(word, url, title, snippet);
            } catch (Exception e) {
                System.out.println("Erro ao extrair dados de " + url + ": " + e.getMessage());
                addToIndex(word, url, "", "");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public synchronized Map<String, Map<String, Object>> getBarrelDetails() throws RemoteException {
        Map<String, Map<String, Object>> details = new HashMap<>();

        for (String barrelId : barrelsAtivos.keySet()) {
            Map<String, Object> barrelInfo = new HashMap<>();
            barrelInfo.put("indexSize", barrelsAtivos.get(barrelId));

            List<Double> tempos = temposRespostaBarrels.getOrDefault(barrelId, new ArrayList<>());
            String formattedAvg = "0.00 ms";

            if (!tempos.isEmpty()) {
                double avgTime = tempos.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                formattedAvg = String.format("%.2f ms", avgTime);
            }

            barrelInfo.put("avgResponseTime", formattedAvg);
            details.put(barrelId, barrelInfo);
        }

        return details;
    }


    // Método para obter informações resumidas sobre status do gateway
    public synchronized Map<String, Object> getGatewayStatus() throws RemoteException {
        Map<String, Object> status = new HashMap<>();

        // Número total de barrels ativos
        status.put("activeBarrels", barrelsAtivos.size());

        // Tamanho total do índice em todos os barrels
        int totalSize = 0;
        for (Integer size : barrelsAtivos.values()) {
            totalSize += size;
        }
        status.put("totalIndexSize", totalSize);

        // Tempo médio de resposta global
        double globalAvgTime = 0.0;
        int totalTimes = 0;

        for (List<Double> tempos : temposRespostaBarrels.values()) {
            for (double tempo : tempos) {
                globalAvgTime += tempo;
                totalTimes++;
            }
        }

        if (totalTimes > 0) {
            globalAvgTime /= totalTimes;
        }

        status.put("globalAvgResponseTime", globalAvgTime);

        return status;
    }
    private String escolherBarrelPorCarga() {
        if (barrelsAtivos.isEmpty()) return null;
    
        return barrelsAtivos.entrySet()
            .stream()
            .min(Comparator.comparingInt(Map.Entry::getValue)) // Menor índice
            .map(Map.Entry::getKey)
            .orElse(null);
    }   
}