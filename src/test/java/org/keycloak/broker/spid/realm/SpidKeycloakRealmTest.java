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
import java.util.Optional;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
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
    final void spidSpTestMetadataIsReadable() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8180/spid-sp-test.xml"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
                );
        assertNotNull(response.body());
    }

    @Test
    final void spidRealmServiceProviderEntityIdIsReadable() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/realms/spid"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
                );
        assertNotNull(response.body());
    }

    @Test
    final void spidRealmSamlDescriptorIsReadable() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/realms/spid/protocol/saml/descriptor"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
                );
        assertNotNull(response.body());
    }

    @Test
    final void spidRealmOpenIdConfigurationIsReadable() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/realms/spid/.well-known/openid-configuration"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
                );
        assertNotNull(response.body());
    }

    @Test
    final void spidIdentityProviderIsConfigured() {
        List<IdentityProviderRepresentation> identityProviders = spidRealm.getCreatedRepresentation().getIdentityProviders();
        assertNotNull(identityProviders, "SPID realm identityProviders is null.");
        assertFalse(identityProviders.isEmpty(), "SPID realm identityProviders is empty.");
        List<String> aliases = identityProviders.stream().map(IdentityProviderRepresentation::getAlias).toList();
        logger.info("SPID realm Identity Providers: " + aliases.toString());
        
        this.checkIdentityProviders(identityProviders, "spid-spid-sp-test");
        this.checkIdentityProviders(identityProviders, "spid-demo");
    }

    private void checkIdentityProviders(List<IdentityProviderRepresentation> identityProvider, String identityProviderAlias) {
        Optional<IdentityProviderRepresentation> ipOpt = identityProvider.stream().filter(p -> p.getAlias().equals(identityProviderAlias)).findFirst();
        assertFalse(ipOpt.isEmpty(), "No identity provider configured in SPID realm for alias " + identityProviderAlias);
        IdentityProviderRepresentation ip = ipOpt.get();
        assertTrue(ip.isTrustEmail(), "Attribute Trust Email has to be true.");
    }

    @Test
    final void spidIdentityProviderMappersAreConfigured() {
        List<IdentityProviderMapperRepresentation> identityProviderMappers = spidRealm.getCreatedRepresentation().getIdentityProviderMappers();
        assertNotNull(identityProviderMappers, "SPID realm identityProviderMappers is null.");
        assertFalse(identityProviderMappers.isEmpty(), "SPID realm identityProviderMappers is empty.");
        
        this.checkIdentityProviderMappers(identityProviderMappers, "spid-spid-sp-test");
        this.checkIdentityProviderMappers(identityProviderMappers, "spid-demo");
    }
    
    private void checkIdentityProviderMappers(List<IdentityProviderMapperRepresentation> identityProviderMappers, String identityProviderAlias) {
        List<IdentityProviderMapperRepresentation> ipm = identityProviderMappers.stream().filter(p -> p.getIdentityProviderAlias().equals("spid-spid-sp-test")).toList();
        assertFalse(ipm.isEmpty(), "No identity provider mappers configured in SPID realm for alias " + identityProviderAlias);
    }
}
