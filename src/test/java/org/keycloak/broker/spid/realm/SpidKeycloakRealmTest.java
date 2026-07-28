package org.keycloak.broker.spid.realm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.keycloak.broker.spid.configuration.SpidRealmConfig;
import org.keycloak.broker.spid.configuration.SpidSamlKeycloakServerConfig;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;

/**
 * Il test deve prevedere la configurazione di un realm in cui e' configurato il
 * provider spid e le configurazioni di base per poter effettuare le successive
 * verifiche di integrazione.
 * 
 * TODO Completare la configurazione del realm e la verifica delle diverse
 * configurazioni
 * 
 * @see SpidKeycloakRealmTest
 */
@KeycloakIntegrationTest(config = SpidSamlKeycloakServerConfig.class)
public class SpidKeycloakRealmTest {

    /**
     * Riferimento al realm configurato tramite {@link SpidRealmConfig}
     */
    @InjectRealm(config = SpidRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm spidRealm;

    /**
     * Verifica che il realm e' stato iniettato
     */
    @Test
    final void spidRealShouldBeInjected() {
        assertNotNull(spidRealm, "spidRealm not injected");
    }

    /**
     * Verifica del nome del realm
     */
    @Test
    final void spidRealmNameIsSpid() {
        assertEquals("spid", spidRealm.getName());
    }
}
