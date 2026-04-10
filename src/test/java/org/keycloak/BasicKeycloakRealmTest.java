package org.keycloak;

import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

@KeycloakIntegrationTest
class BasicKeycloakRealmTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @Test
    void managedRealmTest() {
        assertNotNull(managedRealm.admin().toRepresentation());
    }
}
