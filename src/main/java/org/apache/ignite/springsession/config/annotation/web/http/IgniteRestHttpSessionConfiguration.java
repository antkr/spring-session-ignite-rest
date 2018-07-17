package org.apache.ignite.springsession.config.annotation.web.http;

import java.util.Map;
import org.apache.ignite.springsession.IgniteRestSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.MapSession;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.util.StringUtils;

@Configuration
public class IgniteRestHttpSessionConfiguration extends SpringHttpSessionConfiguration implements ImportAware {

    private String sessionCacheName = IgniteRestSessionRepository.DFLT_SESSION_STORAGE_NAME;

    private Integer maxInactiveIntervalInSeconds = MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

    private String igniteAddress = IgniteRestSessionRepository.DFLT_IGNITE_ADDRESS;

    private String ignitePort = IgniteRestSessionRepository.DFLT_IGNITE_PORT;

    private String igniteConnection;

    @Bean
    public IgniteRestSessionRepository repository(/*@Value("$(ignite.ip.url)") final String ip, @Value("$(ignite.port") final String port*/) {
        IgniteRestSessionRepository repository = new IgniteRestSessionRepository();

        if (StringUtils.hasText(igniteConnection)) {
            assert igniteConnection.contains(":");
            String[] connection = igniteConnection.split(":");
            assert connection.length == 2;
            String address = connection[0];
            String port = connection[1];

            repository.setIp(address);
            repository.setPort(port);
        }
        else {
            repository.setIp(this.igniteAddress);
            repository.setPort(this.ignitePort);
        }

        repository.setSessionCacheName(this.sessionCacheName);
        repository.setDefaultMaxInactiveInterval(maxInactiveIntervalInSeconds);
        return repository;
    }

    public void setSessionCacheName(String sessionCacheName) {
        this.sessionCacheName = sessionCacheName;
    }

    public void setMaxInactiveIntervalInSeconds(Integer maxInactiveIntervalInSeconds) {
        this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
    }

    public void setIgniteAddress(String igniteAddress) {
        this.igniteAddress = igniteAddress;
    }

    public void setIgnitePort(String ignitePort) {
        this.ignitePort = ignitePort;
    }

    public void setIgniteConnection(String igniteConnection) {
        this.igniteConnection = igniteConnection;
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> attributeMap = importMetadata
            .getAnnotationAttributes(EnableRestIgniteHttpSession.class.getName());
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(attributeMap);
        this.maxInactiveIntervalInSeconds =
            attributes.getNumber("maxInactiveIntervalInSeconds");
        String sessionCacheNameValue = attributes.getString("sessionCacheName");
        if (StringUtils.hasText(sessionCacheNameValue)) {
            this.sessionCacheName = sessionCacheNameValue;
        }
        String igniteAddr = attributes.getString("igniteAddress");
        if (StringUtils.hasText(igniteAddr)) {
            this.igniteAddress = igniteAddr;
        }
        String ignitePort = attributes.getString("ignitePort");
        if (StringUtils.hasText(ignitePort)) {
            this.ignitePort = ignitePort;
        }
    }
}
