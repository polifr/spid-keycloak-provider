package org.keycloak.broker.spid.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.broker.spid.SpidIdentityProviderFactory;
import org.keycloak.broker.spid.configuration.SpidSamlKeycloakServerConfig;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

@KeycloakIntegrationTest(config = SpidSamlKeycloakServerConfig.class)
public class SpidKeycloakProviderTest {

    @InjectAdminClient
    Keycloak adminClient;

    @Test
    final void adminClientShouldBeInjected() {
        assertNotNull(adminClient, "adminClient not injected");
    }

    @Test
    final void spidIdentityProviderShouldBeRegistered() throws Exception {
        List<Map<String, String>> idps = adminClient.serverInfo().getInfo().getIdentityProviders();
        List<String> providers = idps.stream()
                .map(m -> m.get("id"))
                .sorted()
                .toList();

        assertTrue(
            providers.contains(SpidIdentityProviderFactory.PROVIDER_ID),
            () -> "Registered providers: " + providers
        );
    }
}
