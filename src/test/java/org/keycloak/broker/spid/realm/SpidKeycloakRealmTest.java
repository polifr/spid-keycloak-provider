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
 * TODO Il test deve prevedere la configurazione di un realm in cui e' configurato
 * il provider spid cosi' da poterlo testare.
 */
@KeycloakIntegrationTest(config = SpidSamlKeycloakServerConfig.class)
public class SpidKeycloakRealmTest {

    @InjectRealm(config = SpidRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm spidRealm;

    @Test
    void testSpidRealm() {
        assertNotNull(spidRealm);
        assertEquals("spid", spidRealm.getName());
    }

}
