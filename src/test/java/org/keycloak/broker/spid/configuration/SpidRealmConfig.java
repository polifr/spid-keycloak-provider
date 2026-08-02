package org.keycloak.broker.spid.configuration;

import static org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder.alias;
import static org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder.authenticator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.logging.Logger;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.realm.AuthenticationFlowBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RoleBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class SpidRealmConfig implements RealmConfig {

    private static final Logger logger = Logger.getLogger(SpidRealmConfig.class);
    private static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @Override
    public RealmBuilder configure(RealmBuilder builder) {
        return builder.name("spid")
                .realmRoles(
                        this.offlineAccessRole(), 
                        this.umaAuthorizationRole(), 
                        this.defaultRolesSpidRole()
                )
                .authenticationFlows(this.authenticationFlows())
                .identityProviders(this.identityProviders())
//                .identityProviders(this.spidSpTestIdentityProvider())
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

    private IdentityProviderRepresentation[] identityProviders() {
        try {
            return readArray("identity-providers.json", IdentityProviderRepresentation.class);
        } catch (IOException ex) {
            logger.error("IOException reading identity providers json file", ex);
            return new IdentityProviderRepresentation[] {};
        }
    }

    private IdentityProviderMapperRepresentation[] identityProviderMappers() {
        try {
            return readArray("identity-provider-mappers.json", IdentityProviderMapperRepresentation.class);
        } catch (IOException ex) {
            logger.error("IOException reading identity provider mapperss json file", ex);
            return new IdentityProviderMapperRepresentation[] {};
        }
    }
    
    private static <T> T[] readArray(String jsonFileName, Class<T> elementType) throws IOException {
        try (InputStream is = SpidRealmConfig.class.getResourceAsStream(jsonFileName)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + SpidRealmConfig.class.getPackageName() + "/" + jsonFileName);
            }
            T[] array = MAPPER.readValue(is, MAPPER.getTypeFactory().constructArrayType(elementType));
            logger.info("Objects read from json: " + array.length);
            return array;
        }
    }
}
