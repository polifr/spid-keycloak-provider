package org.keycloak.broker.spid;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@ExtendWith(MockitoExtension.class)
class SpidChecksTest {

	private static SpidChecks spidChecks;
	
	@BeforeAll
	public static final void initSpidChecks() {
		// TODO Init config
		SpidIdentityProviderConfig config = new SpidIdentityProviderConfig();
		config.setIdpEntityId("IDP_ENTITY_ID");

		spidChecks = new SpidChecks(config);
	}
	
	@Test
	void testValidateSpidResponseKO_08() {
		AuthenticationSessionModel authSession = mock(AuthenticationSessionModel.class); 
		when(authSession.getClientNote(eq(SamlProtocol.SAML_REQUEST_ID_BROKER))).thenReturn("SAML_REQUEST_ID_BROKER");
		when(authSession.getClientNote(eq(SpidIdentityProvider.SPID_REQUEST_ISSUE_INSTANT))).thenReturn("SPID_REQUEST_ISSUE_INSTANT");
		
		Element documentElement = null;
		Document samlDocument = null;
		
		Element assertionElement = null;
	}

}
