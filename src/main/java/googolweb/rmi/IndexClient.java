package googolweb.rmi;

import motorbusca.Index;
import motorbusca.PageInfo;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IndexClient {

    private Index indexStub;

    public IndexClient() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 8183);
            indexStub = (Index) registry.lookup("index");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void putNew(String url) {
        try {
            indexStub.putNew(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<PageInfo> search(String word) {
        try {
            return indexStub.searchWord(word);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    public List<String> getTop10Pesquisas() {
        try {
            List<String> top = indexStub.getTopSearchesFormatted(10);
            System.out.println("Recebido do servidor: " + top);
            return top;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
    public Map<String, Map<String, Object>> getBarrelDetails() {
        try {
            return indexStub.getBarrelDetails();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
}
