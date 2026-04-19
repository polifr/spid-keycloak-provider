package org.keycloak;

import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;

/**
 * Basic class for testing the Keycloak general configuration of the managed realm of
 * the Test Framework. It is inspired by the code published here:
 * https://www.keycloak.org/2026/02/deprecating-arquillian-and-keycloak-test-framework-support
 */
@KeycloakIntegrationTest
class BasicKeycloakRealmTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser
    ManagedUser managedUser;

    @Test
    void managedRealmTest() {
        assertNotNull(managedRealm.admin().toRepresentation());
    }

    @Test
    void managedUserTest() {
        assertNotNull(managedRealm.admin().users().get(managedUser.getId()));
    }
}
