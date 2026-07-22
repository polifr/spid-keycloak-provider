package org.keycloak.broker.spid.realm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder;
import org.keycloak.testframework.realm.AuthenticationFlowBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RoleBuilder;

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
        assertEquals("spid", spidRealm.getName());
    }

    static class SpidRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder builder) {
            return builder.name("spid")
                    .realmRoles(
                            this.offlineAccessRoleBuilder(), 
                            this.umaAuthorizationRoleBuilder(), 
                            this.defaultRolesSpidRoleBuilder()
                    )
                    .authenticationFlows(this.firstBrokerLoginSpidAuthenticationFlowBuilder())
                    .identityProviders(this.spidSpTestIdentityProviderBuilder())
                    ;
        }

        private RoleBuilder offlineAccessRoleBuilder() {
            return RoleBuilder.create()
                    .name("offline_access")
                    .description("${role_offline-access}")
                    .composite(false);
        }

        private RoleBuilder umaAuthorizationRoleBuilder() {
            return RoleBuilder.create()
                    .name("uma_authorization")
                    .description("${role_uma_authorization}")
                    .composite(false);
        }

        private RoleBuilder defaultRolesSpidRoleBuilder() {
            return RoleBuilder.create("default-roles-spid")
                    .composite(true)
                    .description("${role_default-roles}")
                    .realmComposite("offline_access", "uma_authorization")
                    .clientComposite("account", "view-profile", "manage-account");
        }

        private AuthenticationFlowBuilder firstBrokerLoginSpidAuthenticationFlowBuilder() {
            return AuthenticationFlowBuilder.create()
                    .alias("first broker login SPID")
                    .description("Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account")
                    .builtIn(false)
                    .providerId("basic-flow")
                    .topLevel(true)
                    .authenticationExecutions(
                            AuthenticationExecutionExportBuilder.create().authenticator("idp-review-profile").authenticatorFlow(false).priority(10).requirement("DISABLED").userSetupAllowed(false),
                            AuthenticationExecutionExportBuilder.create().authenticatorFlow(true).flowAlias("first broker login SPID User creation or linking").priority(20).requirement("REQUIRED").userSetupAllowed(false)
                    );
        }

        private IdentityProviderBuilder spidSpTestIdentityProviderBuilder() {
            return IdentityProviderBuilder.create()
                    .alias("spid-spid-sp-test")
                    .displayName("SPID spid-sp-test")
                    .storeToken(false)
                    .addReadTokenRoleOnCreate(false)
                    .providerId("spid-saml");
        }
    }
}
