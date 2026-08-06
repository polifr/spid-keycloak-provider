package org.keycloak.broker.spid.realm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.broker.spid.configuration.SpidRealmConfig;
import org.keycloak.broker.spid.configuration.SpidSamlKeycloakServerConfig;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

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

    private static final List<String> IDENTITY_PROVIDER_ALIASES = List.of("spid-spid-sp-test", "spid-demo");

    private static final Logger logger = Logger.getLogger(SpidKeycloakRealmTest.class);

    /**
     * Riferimento al realm configurato tramite {@link SpidRealmConfig}
     */
    @InjectRealm(config = SpidRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm spidRealm;

    private static WireMockServer wireMock;
    private static HttpClient httpClient;

    @BeforeAll
    static void setup() {
        wireMock = new WireMockServer(options().port(8180).usingFilesUnderClasspath("wiremock"));
        wireMock.start();

        WireMock.configureFor("localhost", wireMock.port());

        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/spid-sp-test.xml"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/xml")
                .withBodyFile("spid-sp-test.xml")));
        
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    static void shutdown() {
        wireMock.shutdown();
    }

    /**
     * Verifica che il realm e' stato iniettato
     */
    @Test
    final void spidRealmShouldBeInjected() {
        assertNotNull(spidRealm, "spidRealm not injected");
    }

    /**
     * Verifica del nome del realm
     */
    @Test
    final void spidRealmNameIsSpid() {
        assertEquals("spid", spidRealm.getName());
    }

    @Test
    final void spidRealmServiceProviderEntityIdIsReadable() throws Exception {
        this.checkIfUrlIsReadable("http://localhost:8080/realms/spid");
    }

    @Test
    final void spidRealmSamlDescriptorIsReadable() throws Exception {
        this.checkIfUrlIsReadable("http://localhost:8080/realms/spid/protocol/saml/descriptor");
    }

    @Test
    final void spidRealmOpenIdConfigurationIsReadable() throws Exception {
        this.checkIfUrlIsReadable("http://localhost:8080/realms/spid/.well-known/openid-configuration");
    }

    @Test
    final void spidIdentityProviderIsConfigured() throws Exception {
        List<IdentityProviderRepresentation> identityProviders = spidRealm.getCreatedRepresentation().getIdentityProviders();
        assertNotNull(identityProviders, "SPID realm identityProviders is null.");
        assertFalse(identityProviders.isEmpty(), "SPID realm identityProviders is empty.");
        List<String> aliases = identityProviders.stream().map(IdentityProviderRepresentation::getAlias).toList();
        logger.info("SPID realm Identity Providers: " + aliases.toString());

        for(String i : IDENTITY_PROVIDER_ALIASES) {
            this.checkIdentityProviders(identityProviders, i);
        }
    }

    private void checkIdentityProviders(List<IdentityProviderRepresentation> identityProvider, String identityProviderAlias) throws Exception {
        IdentityProviderRepresentation ip = identityProvider.stream()
                .filter(p -> p.getAlias().equals(identityProviderAlias))
                .findFirst().orElseGet(() -> Assertions.fail("No identity provider configured in SPID realm for alias " + identityProviderAlias));
        assertTrue(ip.isTrustEmail(), "Attribute Trust Email has to be true.");
        
        this.checkIfUrlIsReadable(ip.getConfig().get("metadataDescriptorUrl"));
    }

    @Test
    final void spidIdentityProviderMappersAreConfigured() {
        List<IdentityProviderMapperRepresentation> identityProviderMappers = spidRealm.getCreatedRepresentation().getIdentityProviderMappers();
        assertNotNull(identityProviderMappers, "SPID realm identityProviderMappers is null.");
        assertFalse(identityProviderMappers.isEmpty(), "SPID realm identityProviderMappers is empty.");

        for(String i : IDENTITY_PROVIDER_ALIASES) {
            this.checkIdentityProviderMappers(identityProviderMappers, i);
        }
    }
    
    private void checkIdentityProviderMappers(List<IdentityProviderMapperRepresentation> identityProviderMappers, String identityProviderAlias) {
        List<IdentityProviderMapperRepresentation> ipm = identityProviderMappers.stream()
                .filter(p -> p.getIdentityProviderAlias().equals(identityProviderAlias)).toList();
        assertFalse(ipm.isEmpty(), "No identity provider mappers configured in SPID realm for alias " + identityProviderAlias);
    }

    private String checkIfUrlIsReadable(String requestUrl) throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(requestUrl))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
                );
        assertTrue(response.statusCode() < 400, "Response status calling " + requestUrl + " is not ok: " + response.statusCode());
        assertTrue(StringUtils.isNotEmpty(response.body()), "Response body is null or empty");
        return response.body();
    }
}
