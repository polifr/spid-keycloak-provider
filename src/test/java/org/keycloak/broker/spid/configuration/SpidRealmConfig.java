package org.keycloak.broker.spid.configuration;

import static org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder.alias;
import static org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder.authenticator;

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
                .authenticationFlows(this.authenticationFlowsBuilder())
                .identityProviders(this.spidSpTestIdentityProviderBuilder())
                ;
    }

    private RoleBuilder offlineAccessRoleBuilder() {
        return RoleBuilder.create("offline_access")
                .description("${role_offline-access}")
                .composite(false);
    }

    private RoleBuilder umaAuthorizationRoleBuilder() {
        return RoleBuilder.create("uma_authorization")
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

    private AuthenticationFlowBuilder[] authenticationFlowsBuilder() {
        return new AuthenticationFlowBuilder[] {
                AuthenticationFlowBuilder.create()
                        .alias("first broker login SPID")
                        .description("Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account")
                        .builtIn(false)
                        .providerId("basic-flow")
                        .topLevel(true)
                        .authenticationExecutions(
                                authenticator("idp-review-profile", "DISABLED", 10, false),
                                alias("first broker login SPID User creation or linking", "REQUIRED", 20, false)
                        ),
                AuthenticationFlowBuilder.create()
                        .alias("first broker login SPID Account verification options")
                        .description("Method with which to verity the existing account")
                        .builtIn(false)
                        .providerId("basic-flow")
                        .topLevel(false)
                        .authenticationExecutions(
                                authenticator("idp-email-verification", "ALTERNATIVE", 10, false),
                                alias("first broker login SPID Verify Existing Account by Re-authentication", "ALTERNATIVE", 20, false)
                                ),
                AuthenticationFlowBuilder.create()
                        .alias("first broker login SPID First broker login - Conditional OTP")
                        .description("Flow to determine if the OTP is required for the authentication")
                        .builtIn(false)
                        .providerId("basic-flow")
                        .topLevel(false)
                        .authenticationExecutions(
                                authenticator("conditional-user-configured", "REQUIRED", 10, false),
                                authenticator("auth-otp-form", "REQUIRED", 20, false)
                                ),
                AuthenticationFlowBuilder.create()
                        .alias("first broker login SPID Handle Existing Account")
                        .description("Handle what to do if there is existing account with same email/username like authenticated identity provider")
                        .builtIn(false)
                        .providerId("basic-flow")
                        .topLevel(false)
                        .authenticationExecutions(
                                authenticator("idp-auto-link", "REQUIRED", 10, false),
                                authenticator("idp-confirm-link", "DISABLED", 20, false),
                                alias("first broker login SPID Account verification options", "ALTERNATIVE", 21, false)
                                ),
                AuthenticationFlowBuilder.create()
                        .alias("first broker login SPID User creation or linking")
                        .description("Flow for the existing/non-existing user alternatives")
                        .builtIn(false)
                        .providerId("basic-flow")
                        .topLevel(false)
                        .authenticationExecutions(
                                authenticator("idp-create-user-if-unique", "ALTERNATIVE", 10, false),
                                alias("first broker login SPID Handle Existing Account", "ALTERNATIVE", 20, false)
                                ),
                AuthenticationFlowBuilder.create()
                        .alias("first broker login SPID Verify Existing Account by Re-authentication")
                        .description("Reauthentication of existing account")
                        .builtIn(false)
                        .providerId("basic-flow")
                        .topLevel(false)
                        .authenticationExecutions(
                                authenticator("idp-username-password-form", "REQUIRED", 10, false),
                                alias("first broker login SPID First broker login - Conditional OTP", "CONDITIONAL", 20, false)
                        )
                };
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
