package org.keycloak.broker.spid.realm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
    private static final String SPID_REALM_BASE_URL = "http://localhost:8080/realms/spid";

    private static final Logger logger = Logger.getLogger(SpidKeycloakRealmTest.class);
    private static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();
    private static 

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
        String response = this.checkIfUrlIsReadable(SPID_REALM_BASE_URL);
        assertTrue(StringUtils.isNotEmpty(response), "Response body is null or empty");
        this.checkValidJson(response);
        // TODO Ulteriori verifiche su contenuto json
    }

    @Test
    final void spidRealmSamlDescriptorIsReadable() throws Exception {
        String response = this.checkIfUrlIsReadable(SPID_REALM_BASE_URL + "/protocol/saml/descriptor");
        assertTrue(StringUtils.isNotEmpty(response), "Response body is null or empty");
        this.checkValidXml(response);
        // TODO Ulteriori verifiche su contenuto xml
    }

    @Test
    final void spidRealmOpenIdConfigurationIsReadable() throws Exception {
        String response = this.checkIfUrlIsReadable(SPID_REALM_BASE_URL + "/.well-known/openid-configuration");
        assertTrue(StringUtils.isNotEmpty(response), "Response body is null or empty");
        this.checkValidJson(response);
        // TODO Ulteriori verifiche su contenuto json
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

    /**
     * Verifica che per l'alias fornito sia presente un identity provider e che l'url a cui
     * e' esposto il metadata descriptor sia raggiungibile.
     *
     * @param identityProviders Elenco degli identity providers del realm
     * @param identityProviderAlias Alias dell'identity provider
     * @throws InterruptedException
     * @throws IOException
     */
    private void checkIdentityProviders(List<IdentityProviderRepresentation> identityProviders, String identityProviderAlias) throws InterruptedException, IOException {
        IdentityProviderRepresentation ip = identityProviders.stream()
                .filter(p -> p.getAlias().equals(identityProviderAlias))
                .findFirst().orElseGet(() -> Assertions.fail("No identity provider configured in SPID realm for alias " + identityProviderAlias));
        assertTrue(ip.isTrustEmail(), "Attribute Trust Email has to be true.");

        String response = this.checkIfUrlIsReadable(ip.getConfig().get("metadataDescriptorUrl"));
        assertTrue(StringUtils.isNotEmpty(response), "Response body is null or empty");
        this.checkValidXml(response);
        // TODO Ulteriori verifiche su contenuto xml

        String endpointDescriptorUrl = SPID_REALM_BASE_URL + "/broker/" + identityProviderAlias + "/endpoint/descriptor";
        response = this.checkIfUrlIsReadable(endpointDescriptorUrl);
        assertTrue(StringUtils.isNotEmpty(response), "Response body is null or empty");
        this.checkValidXml(response);
        // TODO Ulteriori verifiche su contenuto xml
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

    /**
     * Verifica che per l'alias fornito sia presente almeno un mapper 
     * 
     * @param identityProviderMappers Elenco dei mappers del realm
     * @param identityProviderAlias Alias dell'identity provider
     */
    private void checkIdentityProviderMappers(List<IdentityProviderMapperRepresentation> identityProviderMappers, String identityProviderAlias) {
        List<IdentityProviderMapperRepresentation> ipm = identityProviderMappers.stream()
                .filter(p -> p.getIdentityProviderAlias().equals(identityProviderAlias)).toList();
        assertFalse(ipm.isEmpty(), "No identity provider mappers configured in SPID realm for alias " + identityProviderAlias);
    }

    /**
     * Verifica se l'url fornito come parametro e' leggibile in get, ossia se la chiamata
     * riceve uno status con codice strettamente minore di 400. Il metodo restituisce il
     * body della risposta, sotto forma di stringa.
     *
     * @param requestUrl Url da interrogare
     * @return Body della risposta, se ricevuta con status < 400
     * @throws InterruptedException
     * @throws IOException
     */
    private String checkIfUrlIsReadable(String requestUrl) throws InterruptedException, IOException {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(requestUrl))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
                );
        assertTrue(response.statusCode() < 400, "Response status calling " + requestUrl + " is not ok: " + response.statusCode());
        return response.body();
    }

    private JsonNode checkValidJson(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            assertNotNull(node, "Json parsing produced null node.");
            assertTrue(node.isObject(), "Json parsing produced a not object node.");
            return node;
        } catch (JsonProcessingException ex) {
            return fail("Exception parsing json string: " + ex.getMessage());
        }
    }

    private boolean checkValidXml(String xml) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            factory.newSAXParser().parse(new InputSource(new StringReader(xml)), new DefaultHandler());
            return true;
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            return fail("Exception parsing xml string: " + ex.getMessage());
        }
    }
}
