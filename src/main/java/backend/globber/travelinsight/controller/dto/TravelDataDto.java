package backend.globber.travelinsight.controller.dto;

import java.util.List;

public record TravelDataDto(
    String country,
    List<String> cities
) {

}
