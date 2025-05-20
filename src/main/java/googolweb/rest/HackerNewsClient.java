package googolweb.rest;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
public class HackerNewsClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public String[] getTopStoryIds() {
        String url = "https://hacker-news.firebaseio.com/v0/topstories.json";
        ResponseEntity<String[]> response = restTemplate.getForEntity(url, String[].class);
        return response.getBody();
    }
}