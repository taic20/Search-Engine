package googolweb.scheduler;

import googolweb.rmi.IndexClient;
import googolweb.websocket.StatsWebSocketHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StatsScheduler {

    private final IndexClient indexClient = new IndexClient();
    private final StatsWebSocketHandler statsHandler;

    public StatsScheduler(StatsWebSocketHandler statsHandler) {
        this.statsHandler = statsHandler;
    }

    @Scheduled(fixedRate = 5000)
    public void enviarEstatisticas() {
        List<String> top = indexClient.getTop10Pesquisas();
        Map<String, Map<String, Object>> barrels = indexClient.getBarrelDetails();
        statsHandler.broadcastEstatisticas(top, barrels);
    }
}
