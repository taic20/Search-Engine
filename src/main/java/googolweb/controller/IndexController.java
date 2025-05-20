package googolweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import googolweb.rmi.IndexClient;
import googolweb.rest.OpenRouterService;
import motorbusca.PageInfo;

import java.util.List;
import java.util.Objects;

@Controller
public class IndexController {

    private final IndexClient indexClient = new IndexClient();
    private final OpenRouterService openRouterService;

    public IndexController(OpenRouterService openRouterService) {
        this.openRouterService = openRouterService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/index-url")
    public String indexUrl(@RequestParam String url, Model model) {
        indexClient.putNew(url);
        model.addAttribute("message", "URL enviado para indexação: " + url);
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam String query,
                        @RequestParam(defaultValue = "1") int page,
                        Model model) {

        List<PageInfo> allResults = indexClient.search(query);

        // Paginação: 10 resultados por página
        int pageSize = 10;
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allResults.size());

        List<PageInfo> paginatedResults = allResults.subList(fromIndex, toIndex);

        // Adiciona à view apenas os resultados da página atual
        model.addAttribute("results", paginatedResults);
        model.addAttribute("query", query);
        model.addAttribute("page", page);
        model.addAttribute("hasNext", toIndex < allResults.size());
        model.addAttribute("hasPrev", page > 1);

        // IA continua baseada nos 5 primeiros snippets do total
        List<String> snippets = allResults.stream()
                .map(PageInfo::getSnippet)
                .filter(Objects::nonNull)
                .limit(5)
                .toList();

        String analiseIA = openRouterService.gerarAnaliseContextual(query, snippets);
        model.addAttribute("analise", analiseIA);

        return "results";
    }
}