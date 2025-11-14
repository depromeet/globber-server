package backend.globber.travelinsight.domain;

import backend.globber.travelinsight.domain.constant.TravelLevel;
import backend.globber.travelinsight.domain.constant.TravelScope;
import backend.globber.travelinsight.domain.constant.TravelType;
import org.springframework.stereotype.Component;

@Component
public class TravelTitleComposer {

    public String compose(TravelLevel level, TravelScope scope, TravelType type) {
        return String.format("%s %s %s",
            level.getAdjective(),
            scope.getScopeName(),
            type.getTypeName());
    }
}

