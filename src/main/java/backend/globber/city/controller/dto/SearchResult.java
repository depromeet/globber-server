package backend.globber.city.controller.dto;

import java.util.List;

public record SearchResult(List<SearchResponse> cities) {
}
