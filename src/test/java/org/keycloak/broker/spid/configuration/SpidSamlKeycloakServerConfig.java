package org.keycloak.broker.spid.configuration;

import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class SpidSamlKeycloakServerConfig extends DefaultKeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return super.configure(config).dependencyCurrentProject();
    }

}
