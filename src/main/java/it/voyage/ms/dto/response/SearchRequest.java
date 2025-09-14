package it.voyage.ms.dto.response;

public class SearchRequest {
    private String query;

    // Costruttore vuoto per la deserializzazione JSON
    public SearchRequest() {}

    // Getter e Setter
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
