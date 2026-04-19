package org.keycloak.broker.spid.realm;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

/**
 * TODO Il test deve prevedere la configurazione di un realm in cui e' configurato
 * il provider spid cosi' da poterlo testare.
 */
@KeycloakIntegrationTest
public class SpidKeycloakRealmTest {

    @InjectRealm(config = SpidRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm spidRealm;

    @Test
    void testSpidRealm() {
        assertNotNull(spidRealm);
        assertEquals("spid-realm", spidRealm.getName());
    }

    static class SpidRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder builder) {
            return builder.name("spid-realm");
        }
    }
}
