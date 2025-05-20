package googolweb.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Component
public class OpenRouterService {

    @Value("${openrouter.api.key}")
    private String apiKey;

    public String gerarAnaliseContextual(String termo, List<String> snippets) {
        try {
            String prompt = "Analisar o seguinte termo de pesquisa: '" + termo + "' com base nestes excertos:\n\n" +
                    String.join("\n", snippets);

            // Cria o payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "openai/gpt-3.5-turbo");
            payload.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));
            payload.put("temperature", 0.7);

            // Serializa o JSON
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(payload);

            // Cria o request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://yourapp.com") // opcional
                    .header("X-Title", "GoogolWeb")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Envia o pedido
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Extrai conteúdo
            String resposta = response.body();
            Map<String, Object> respostaMap = mapper.readValue(resposta, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respostaMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            return (String) message.get("content");

        } catch (Exception e) {
            return "Erro ao gerar análise com IA: " + e.getMessage();
        }
    }
}
