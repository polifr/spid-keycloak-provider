package org.keycloak.broker.spid.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.broker.spid.SpidIdentityProviderFactory;
import org.keycloak.broker.spid.configuration.SpidSamlKeycloakServerConfig;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

/**
 * Il test verifica che il provider "spid-saml" e' stato correttamente
 * letto ed e' configurabile tramite l'admin client di Keycloak. Prevede
 * l'uso della classe di configurazione {@link SpidSamlKeycloakServerConfig}
 * per indicare che nelle dipendenze del server c'e' anche il progetto
 * corrente.
 * 
 * @see SpidSamlKeycloakServerConfig
 */
@KeycloakIntegrationTest(config = SpidSamlKeycloakServerConfig.class)
public class SpidKeycloakProviderTest {

    private static final Logger logger = Logger.getLogger(SpidKeycloakProviderTest.class);

    /**
     * Riferimento all'admin client per la gestione del server Keycloak di test
     */
    @InjectAdminClient
    Keycloak adminClient;

    @Test
    final void adminClientShouldBeInjected() {
        assertNotNull(adminClient, "adminClient not injected");
    }

    /**
     * Verifica che tra gli identity providers disponibili nell'istanza di Keycloak
     * sia presente anche lo "spid-saml", associato alla
     * {@link SpidIdentityProviderFactory}.
     */
    @Test
    final void spidIdentityProviderShouldBeRegistered() {
        List<Map<String, String>> idps = adminClient.serverInfo().getInfo().getIdentityProviders();
        List<String> providers = idps.stream()
                .map(m -> m.get("id"))
                .sorted()
                .toList();

        logger.info("Providers: " + providers.toString());
        assertTrue(
            providers.contains(SpidIdentityProviderFactory.PROVIDER_ID),
            () -> "Registered providers: " + providers
        );
    }
}
