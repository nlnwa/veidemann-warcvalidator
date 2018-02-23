package no.nb.warcvalidator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${warcs:/defaultWarcs}")
    private String warcsLocation;
    @Value("${validwarcs:/defaultValidWarcs}")
    private String validWarcsLocation;

    public String getJhoveConfigFilePath() {
        return jhoveConfigFilePath;
    }

    public void setJhoveConfigFilePath(String jhoveConfigFilePath) {
        this.jhoveConfigFilePath = jhoveConfigFilePath;
    }

    @Value("${configFileFullPath}")
    private String jhoveConfigFilePath;

    public String getWarcsLocation() {
        return warcsLocation;
    }

    public void setWarcsLocation(String warcsLocation) {
        this.warcsLocation = warcsLocation;
    }

    public String getValidWarcsLocation() {
        return validWarcsLocation;
    }

    public void setValidWarcsLocation(String validWarcsLocation) {
        this.validWarcsLocation = validWarcsLocation;
    }
}
