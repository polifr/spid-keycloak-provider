package org.keycloak.broker.spid.configuration;

import static org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder.alias;
import static org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder.authenticator;

import java.util.Map;

import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
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
                        this.offlineAccessRole(), 
                        this.umaAuthorizationRole(), 
                        this.defaultRolesSpidRole()
                )
                .authenticationFlows(this.authenticationFlows())
                .identityProviders(this.spidSpTestIdentityProvider())
                .identityProviderMappers(this.identityProviderMappers())
                ;
    }

    private RoleBuilder offlineAccessRole() {
        return RoleBuilder.create("offline_access")
                .description("${role_offline-access}")
                .composite(false);
    }

    private RoleBuilder umaAuthorizationRole() {
        return RoleBuilder.create("uma_authorization")
                .description("${role_uma_authorization}")
                .composite(false);
    }

    private RoleBuilder defaultRolesSpidRole() {
        return RoleBuilder.create("default-roles-spid")
                .composite(true)
                .description("${role_default-roles}")
                .realmComposite("offline_access", "uma_authorization")
                .clientComposite("account", "view-profile", "manage-account");
    }

    private AuthenticationFlowBuilder[] authenticationFlows() {
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

    private IdentityProviderBuilder spidSpTestIdentityProvider() {
        return IdentityProviderBuilder.create()
                .alias("spid-spid-sp-test")
                .displayName("SPID spid-sp-test")
                .storeToken(false)
                .addReadTokenRoleOnCreate(false)
                .providerId("spid-saml");
    }

    private IdentityProviderMapperRepresentation[] identityProviderMappers() {
        return new IdentityProviderMapperRepresentation[] {
                this.buildIdentityProviderRepresentation("Username", "spid-spid-sp-test", "spid-saml-username-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "template", "${ATTRIBUTE.fiscalNumber}", "target", "BROKER_USERNAME")),
                this.buildIdentityProviderRepresentation("First Name", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "firstName", "attribute.name", "name")),
                this.buildIdentityProviderRepresentation("Last Name", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "lastName", "attribute", "familyName", "attribute.name", "familyName")),
                this.buildIdentityProviderRepresentation("SPID Code", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-spidCode", "attribute", "spidCode", "attribute.name", "spidCode")),
                this.buildIdentityProviderRepresentation("Email", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-email", "attribute", "email", "attribute.name", "email")),
                this.buildIdentityProviderRepresentation("Tax Id", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", // Verificare se Tax Id o Fiscal Number 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-fiscalNumber", "attribute", "fiscalNumber", "attribute.name", "fiscalNumber")),
                this.buildIdentityProviderRepresentation("Gender", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-gender", "attribute", "gender", "attribute.name", "gender")),
                this.buildIdentityProviderRepresentation("Date Of Birth", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-dateOfBirth", "attribute", "dateOfBirth", "attribute.name", "dateOfBirth")),
                this.buildIdentityProviderRepresentation("Place Of Birth", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-placeOfBirth", "attribute", "placeOfBirth", "attribute.name", "placeOfBirth")),
                this.buildIdentityProviderRepresentation("County Of Birth", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-countyOfBirth", "attribute", "countyOfBirth", "attribute.name", "countyOfBirth")),
                this.buildIdentityProviderRepresentation("Mobile Phone", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-mobilePhone", "attribute", "mobilePhone", "attribute.name", "mobilePhone")),
                this.buildIdentityProviderRepresentation("Address", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-address", "attribute", "address", "attribute.name", "address")),
                this.buildIdentityProviderRepresentation("Digital Address", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-digitalAddress", "attribute", "digitalAddress", "attribute.name", "digitalAddress")),
                this.buildIdentityProviderRepresentation("Company Name", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-companyName", "attribute", "companyName", "attribute.name", "companyName")),
                this.buildIdentityProviderRepresentation("Company Address", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-registeredOffice", "attribute", "registeredOffice", "attribute.name", "registeredOffice")),
                this.buildIdentityProviderRepresentation("VAT Number", "spid-spid-sp-test", "spid-user-attribute-idp-mapper", 
                        Map.of("syncMode", "INHERIT", "user.attribute", "spid-ivaCode", "attribute", "ivaCode", "attribute.name", "ivaCode")),
        };
    }
    
    private IdentityProviderMapperRepresentation buildIdentityProviderRepresentation(
            String name, String identityProviderAlias, String identityProviderMapper, Map<String, String> config) {
        IdentityProviderMapperRepresentation rep = new IdentityProviderMapperRepresentation();
        rep.setName(name);
        rep.setIdentityProviderAlias(identityProviderAlias);
        rep.setIdentityProviderMapper(identityProviderMapper);
        rep.setConfig(config);
        return rep;
    }
}
