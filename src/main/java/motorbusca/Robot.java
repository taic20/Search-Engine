package motorbusca;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.io.IOException;


public class Robot extends UnicastRemoteObject implements ClientFuncs, Serializable {

    Index index;
    
    StorageBarrel barrel;

    public Robot() throws RemoteException {

    }

    private Set<String> urlsProcessados = new HashSet<>();
    

    public void executar() {
        try {
            index = (Index) LocateRegistry.getRegistry(8183).lookup("index");
            Registry barrelRegistry = LocateRegistry.getRegistry("localhost", 8184); // Porta do barrel 0
            barrel = (StorageBarrel) barrelRegistry.lookup("barrel0");
            System.out.println("Conectado ao barrel primário.");

            index.registerClient(this);
            while (true) {
                String url = index.takeNext();
                if (url == null) {
                    Thread.sleep(1000);
                    continue;
                }

                if (urlsProcessados.contains(url)) {
                    continue; // Ignora URLs já processados
                }

                urlsProcessados.add(url); // Marca o URL como processado
                System.out.println("Rastreando: " + url);

                try {
                    // Conecta ao URL e obtém o documento HTML
                    Document doc = Jsoup.connect(url).get();

                    // Extrai links e adiciona à fila de rastreamento
                    Elements anchors = doc.select("a");
                    for (Element anchor : anchors) {
                        String href = anchor.attr("href");
                        // Verifica se o link é absoluto (começa com http:// ou https://)
                        if (href.startsWith("http://") || href.startsWith("https://")) {
                            index.putNew(href); // Adiciona apenas URLs absolutos
                            index.registarLigacao(href, url);
                            System.out.println("Registar ligação: " + url + " -> " + href);
                        }
                    }

                    // Extrai o título da página
                    String title = doc.title();

                    // Extrai uma citação curta (por exemplo, as primeiras 100 caracteres do texto)
                    String snippet = doc.body().text();
                    if (snippet.length() > 100) {
                        snippet = snippet.substring(0, 100) + "...";
                    }

                    // Indexa as palavras do conteúdo da página
                    String[] palavras = doc.body().text().split("[\\s\\p{Punct}]+");
                    for (String palavra : palavras) {
                        palavra = palavra.toLowerCase();
                        if (!palavra.isEmpty()) {
                            PageInfo p = new PageInfo(url, title, snippet);
                            barrel.saveIndex(palavra, p);
                        }
                    }

                    // Indexa palavras do próprio URL
                    String[] palavrasURL = url.split("[\\W_]+");
                    for (String palavra : palavrasURL) {
                        palavra = palavra.toLowerCase();
                        if (!palavra.isEmpty()) {
                            barrel.saveIndex(palavra, new PageInfo(url, title, snippet));
                        }
                    }
                    //index.exibirLinksRecebidos();
                } catch (HttpStatusException e) {
                    // Trata erros de status HTTP (como 400, 404, 500, etc.)
                    System.out.println("Erro ao acessar o URL: " + url + " - Status: " + e.getStatusCode());
                } catch (IOException e) {
                    // Trata erros de I/O (como timeouts, URLs inválidos, etc.)
                    System.out.println("Erro de I/O ao acessar o URL: " + url);
                } catch (Exception e) {
                    // Trata outras exceções inesperadas
                    System.out.println("Erro inesperado ao acessar o URL: " + url);
                    e.printStackTrace();
                }

                Thread.sleep(1000); // Espera antes de processar o próximo URL
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Robot r;
        try {
            r = new Robot();
            r.executar();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void printOnClient() throws RemoteException {
        System.out.println("Print on client");
    }
}
