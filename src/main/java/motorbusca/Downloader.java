package motorbusca;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Downloader {
    private DownIndex index;
    private List<StorageBarrel> barrels;
    private boolean running;

    public Downloader() throws RemoteException {
        barrels = new ArrayList<>();
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
            index = (DownIndex) registry.lookup("index");

            System.out.println("Connected to Server at " + host + ":" + port);
        } catch (Exception e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void broadcastToBarrels(String word, PageInfo pageInfo) {
        if (!barrels.isEmpty()) {
            try {
                System.out.println("A enviar para o barrel: " + word);
                barrels.get(0).saveIndex(word, pageInfo);
                System.out.println("Palavra enviada com sucesso: " + word);
            } catch (RemoteException e) {
                System.out.println("Erro ao enviar para o barrel primário: " + e.getMessage());
            }
        }
    }


    public void executeDownloader() {
        running = true;

        while (running) {
            try {
                String url = null;
                do {
                    url = index.get_url();
                    if (url == null) {
                        System.out.println("Nenhum URL. Aguardando...");
                        Thread.sleep(2000);
                    }
                } while (url == null);

                System.out.println("Processing URL: " + url);

                try {
                    // Conectar ao URL e obter o documento
                    Document doc = Jsoup.connect(url).get();

                    // Extrair o título da página
                    String title = doc.title();

                    // Extrair um snippet (primeiros 100 caracteres do texto)
                    String snippet = doc.body().text();
                    if (snippet.length() > 100) {
                        snippet = snippet.substring(0, 100) + "...";
                    }

                    // Extrair palavras do conteúdo
                    String[] words = doc.body().text().split("[\\s\\p{Punct}]+");
                    // Lista de stop words
                    Set<String> stopWords = Set.of("a", "an", "the", "be", "is", "are", "was", "were", "will", "this", "that", "for", "with", "your", "and", "or", "in", "on", "at", "to", "of", "it", "as", "by", "have", "has", "had", "not", "but", "if", "so", "up", "out", "about", "into", "over", "under", "again", "further", "then", "once");

                    // Modify the word processing loop to distribute words to appropriate barrels
                    int maxPalavras = 50; // ou o valor que quiseres
                    int count = 0;

                    PageInfo pageInfo = new PageInfo(url, title, snippet);

                    for (String word : words) {
                        word = word.toLowerCase();
                        if (!word.isEmpty() && !stopWords.contains(word)) {
                            index.save_words(word, url);

                            if (!barrels.isEmpty()) {
                                try {
                                    barrels.get(0).saveIndex(word, pageInfo);
                                    System.out.println("Palavra enviada com sucesso: " + word);
                                } catch (Exception e) {
                                    System.out.println("Erro ao enviar palavra ao barrel: " + e.getMessage());
                                }
                            }

                            count++;
                            if (count >= maxPalavras) break;
                        }
                    }


                    // Extrair links e adicionar à fila
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String href = link.attr("abs:href");
                        if (href != null && !href.isEmpty()) {
                            index.put_url(href);
                        }
                    }

                } catch (IOException e) {
                    System.out.println("Erro ao processar URL " + url + ": " + e.getMessage());
                }

                // Aguardar um pouco para não sobrecarregar os servidores
                Thread.sleep(1000);

            } catch (Exception e) {
                System.out.println("Erro no downloader: " + e.getMessage());
                try {
                    Thread.sleep(5000); // Esperar um pouco mais se ocorrer erro
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void stop() {
        running = false;
    }

    public static void main(String[] args) {
        Downloader downloader;
        try {
            downloader = new Downloader();

            // Conectar ao servidor principal
            downloader.connectToServer();

            // Configurar barrels para redundância
            Scanner scanner = new Scanner(System.in);

            downloader.connectToPrimaryBarrel();

            // Iniciar o processamento em thread separada
            Thread downloaderThread = new Thread(() -> {
                downloader.executeDownloader();
            });
            downloaderThread.start();

            System.out.println("Downloader iniciado. Digite 'exit' para encerrar.");
            while (true) {
                String command = scanner.nextLine();
                if ("exit".equalsIgnoreCase(command)) {
                    downloader.stop();
                    break;
                }
            }

            scanner.close();

        } catch (RemoteException e) {
            System.out.println("Erro ao iniciar downloader: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void connectToPrimaryBarrel() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 8184); // porta do barrel 0
            StorageBarrel primary = (StorageBarrel) registry.lookup("barrel0");
            barrels.clear();
            barrels.add(primary);
            System.out.println("Conectado ao barrel primário.");
        } catch (Exception e) {
            System.out.println("Erro ao conectar ao barrel primário: " + e.getMessage());
        }
    }

}