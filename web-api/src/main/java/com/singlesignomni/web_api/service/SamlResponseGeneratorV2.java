package com.singlesignomni.web_api.service;

import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.impl.XSStringBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.impl.*;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.impl.KeyInfoBuilder;
import org.opensaml.xmlsec.signature.impl.SignatureBuilder;
import org.opensaml.xmlsec.signature.impl.X509CertificateBuilder;
import org.opensaml.xmlsec.signature.impl.X509DataBuilder;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import com.singlesignomni.web_api.util.KeyStoreUtil;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class SamlResponseGeneratorV2 {

    public static String generateSAMLResponse(Arguments args) throws Exception {
        Response response = buildSAMLResponse(args);
        signSAMLObject(response);

        return encodeSAMLResponse(response);
    }

    private static Response buildSAMLResponse(Arguments args) {
        Response response = new ResponseBuilder().buildObject();
        response.setID("z" + UUID.randomUUID().toString().replaceAll("-", "null"));
        response.setIssueInstant(Instant.now());
        response.setIssuer(buildIssuer(args.idpEntityId));
        response.setVersion(SAMLVersion.VERSION_20);
        response.setDestination(args.assertionConsumerUrl);
        response.getAssertions().add(buildAssertion(args));
        response.setStatus(buildStatus());

        return response;
    }

    private static Assertion buildAssertion(Arguments args) {
        Assertion assertion = new AssertionBuilder().buildObject();
        assertion.setID("z" + UUID.randomUUID().toString().replaceAll("-", "null"));
        assertion.setIssueInstant(Instant.now());
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setIssuer(buildIssuer(args.idpEntityId));
        assertion.setSubject(buildSubject(args.nameIdValue, args.assertionConsumerUrl));
        assertion.setConditions(buildConditions(args.spEntityId));
        assertion.getAuthnStatements().add(buildAuthnStatement());
        assertion.getAttributeStatements().add(buildAttributeStatement(args.attributes));

        return assertion;
    }

    private static Issuer buildIssuer(String idpEntityId) {
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue(idpEntityId);
        return issuer;
    }

    private static Subject buildSubject(String nameIdValue, String assertionConsumerUrl) {
        NameID nameID = new NameIDBuilder().buildObject();
        nameID.setValue(nameIdValue);
        nameID.setFormat(NameID.EMAIL);

        SubjectConfirmationData subjectConfirmationData = new SubjectConfirmationDataBuilder().buildObject();
        subjectConfirmationData.setNotOnOrAfter(Instant.now().plusSeconds(300));
        subjectConfirmationData.setRecipient(assertionConsumerUrl);

        SubjectConfirmation subjectConfirmation = new SubjectConfirmationBuilder().buildObject();
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);

        Subject subject = new SubjectBuilder().buildObject();
        subject.setNameID(nameID);
        subject.getSubjectConfirmations().add(subjectConfirmation);

        return subject;
    }

    private static Conditions buildConditions(String spEntityId) {
        Conditions conditions = new ConditionsBuilder().buildObject();
        conditions.setNotBefore(Instant.now());
        conditions.setNotOnOrAfter(Instant.now().plusSeconds(300));

        Audience audience = new AudienceBuilder().buildObject();
        audience.setURI(spEntityId);

        AudienceRestriction audienceRestriction = new AudienceRestrictionBuilder().buildObject();
        audienceRestriction.getAudiences().add(audience);

        conditions.getAudienceRestrictions().add(audienceRestriction);
        return conditions;
    }

    private static AttributeStatement buildAttributeStatement(AttributeArgument[] attributeArguments) {
        AttributeStatement attributeStatement = new AttributeStatementBuilder().buildObject();

        Attribute name = new AttributeBuilder().buildObject();
        name.setName("username");
        name.setNameFormat(Attribute.URI_REFERENCE);

        XSString nameValue = new XSStringBuilder().buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        nameValue.setValue("nameIdValueInput");
        name.getAttributeValues().add(nameValue);

        attributeStatement.getAttributes().add(name);

        for (AttributeArgument attributeArgument : attributeArguments) {
            Attribute attribute = new AttributeBuilder().buildObject();
            attribute.setName(attributeArgument.name);
            attribute.setNameFormat(Attribute.BASIC);

            XSString attributeValue = new XSStringBuilder().buildObject(AttributeValue.DEFAULT_ELEMENT_NAME,
                    XSString.TYPE_NAME);
            attributeValue.setValue(attributeArgument.value);
            attribute.getAttributeValues().add(attributeValue);

            attributeStatement.getAttributes().add(attribute);
        }

        return attributeStatement;
    }

    private static AuthnStatement buildAuthnStatement() {
        AuthnStatement authnStatement = new AuthnStatementBuilder().buildObject();
        authnStatement.setAuthnInstant(Instant.now());

        AuthnContext authnContext = new AuthnContextBuilder().buildObject();
        AuthnContextClassRef authnContextClassRef = new AuthnContextClassRefBuilder().buildObject();
        authnContextClassRef.setURI(AuthnContext.PASSWORD_AUTHN_CTX);
        authnContext.setAuthnContextClassRef(authnContextClassRef);

        authnStatement.setAuthnContext(authnContext);
        return authnStatement;
    }

    private static Status buildStatus() {
        StatusCode code = new StatusCodeBuilder().buildObject();
        code.setValue(StatusCode.SUCCESS);

        Status status = new StatusBuilder().buildObject();
        status.setStatusCode(code);

        return status;
    }

    private static void signSAMLObject(SignableSAMLObject samlObject) throws Exception {
        PrivateKey privateKey = KeyStoreUtil.getPrivateKey();
        X509Certificate certificate = KeyStoreUtil.getCertificate();

        BasicX509Credential credential = new BasicX509Credential(certificate, privateKey);

        Signature signature = new SignatureBuilder().buildObject();
        signature.setSigningCredential(credential);
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        KeyInfo keyInfo = buildKeyInfo(credential);
        signature.setKeyInfo(keyInfo);

        samlObject.setSignature(signature);

        Marshaller marshaller = org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport.getMarshallerFactory()
                .getMarshaller(samlObject);
        marshaller.marshall(samlObject);

        Signer.signObject(signature);
    }

    private static KeyInfo buildKeyInfo(X509Credential credential) throws CertificateEncodingException {
        KeyInfo keyInfo = new KeyInfoBuilder().buildObject();

        X509Data x509Data = new X509DataBuilder().buildObject();

        org.opensaml.xmlsec.signature.X509Certificate x509Certificate = new X509CertificateBuilder().buildObject();
        String encodedCert = Base64.getEncoder().encodeToString(credential.getEntityCertificate().getEncoded());
        x509Certificate.setValue(encodedCert);

        x509Data.getX509Certificates().add(x509Certificate);

        keyInfo.getX509Datas().add(x509Data);

        return keyInfo;
    }

    private static String encodeSAMLResponse(Response samlResponse)
            throws MarshallingException, TransformerException, IOException {

        MarshallerFactory marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
        Marshaller marshaller = marshallerFactory.getMarshaller(samlResponse);
        Element responseElement = marshaller.marshall(samlResponse);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(responseElement), new StreamResult(outputStream));

        String encodedResponse = Base64.getEncoder().encodeToString(outputStream.toByteArray());

        return encodedResponse;
    }

    public static class Arguments {
        public String assertionConsumerUrl;
        public String spEntityId;
        public String nameIdValue;
        public String idpEntityId;
        public AttributeArgument[] attributes;
    }

    public static class AttributeArgument {
        public String name;
        public String value;
    }
}
