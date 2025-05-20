package googolweb.websocket;

import googolweb.rmi.IndexClient;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class StatsWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final IndexClient indexClient = new IndexClient();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    // Método chamado externamente para enviar estatísticas
    public void broadcastEstatisticas(List<String> topTerms, Map<String, Map<String, Object>> barrels) {
        String topMsg = "Top pesquisas: " + String.join(", ", topTerms);

        StringBuilder barrelMsg = new StringBuilder("Barrels ativos:\n");
        for (Map.Entry<String, Map<String, Object>> entry : barrels.entrySet()) {
            String barrelId = entry.getKey();
            Map<String, Object> info = entry.getValue();
            barrelMsg.append(String.format(
                "Barrel %s - Índice: %s páginas, Tempo médio: %.2f seg\n",
                barrelId,
                info.get("indexSize"),
                info.get("avgResponseTime")
            ));
        }

        String finalMessage = topMsg + "\n" + barrelMsg;

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(finalMessage));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
