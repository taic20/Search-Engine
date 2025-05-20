package motorbusca;

import java.io.Serializable;
import java.util.Objects;

public class PageInfo implements Serializable {
    private String url;
    private String title;
    private String snippet;

    public PageInfo(String url, String title, String snippet) {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    @Override
    public String toString() {
        return "Title: " + title + "\nURL: " + url + "\nSnippet: " + snippet + "\n";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PageInfo pageInfo = (PageInfo) obj;
        return url.equals(pageInfo.url) &&
                title.equals(pageInfo.title) &&
                snippet.equals(pageInfo.snippet);
    }
    @Override
    public int hashCode() {
        return Objects.hash(url, title, snippet);
    }

}