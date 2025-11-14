package backend.globber.travelinsight.config;

import backend.globber.travelinsight.domain.constant.TitleStrategy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class TravelInsightSettings {

    private final TitleStrategy titleStrategy;

    public TravelInsightSettings(
        @Value("${travel.insight.title-strategy:SERVER}") TitleStrategy titleStrategy
    ) {
        this.titleStrategy = titleStrategy;
    }

}

