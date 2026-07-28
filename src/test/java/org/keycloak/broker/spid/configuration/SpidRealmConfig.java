package org.keycloak.broker.spid.configuration;

import org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder;
import org.keycloak.testframework.realm.AuthenticationFlowBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RoleBuilder;

public class SpidRealmConfig implements RealmConfig {

    @Override
    public RealmBuilder configure(RealmBuilder builder) {
        return builder.name("spid")
                .realmRoles(
                        this.offlineAccessRoleBuilder(), 
                        this.umaAuthorizationRoleBuilder(), 
                        this.defaultRolesSpidRoleBuilder()
                )
                //.authenticationFlows(this.firstBrokerLoginSpidAuthenticationFlowBuilder())
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
                        AuthenticationExecutionExportBuilder.create()
                                .authenticator("idp-review-profile")
                                .authenticatorFlow(false)
                                .priority(10)
                                .requirement("DISABLED")
                                .userSetupAllowed(false),
                        AuthenticationExecutionExportBuilder.create()
                                .authenticatorFlow(true)
                                .flowAlias("first broker login SPID User creation or linking")
                                .priority(20)
                                .requirement("REQUIRED")
                                .userSetupAllowed(false)
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
