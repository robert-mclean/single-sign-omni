package com.singlesignomni.web_api.util;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class KeyStoreUtil {
    private static final String KEYSTORE_FILE = "src/main/resources/saml-idp-keystore.jks";
    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final String KEY_ALIAS = "saml-idp";
    private static final String KEY_PASSWORD = "changeit";

    public static PrivateKey getPrivateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD.toCharArray());
        return (PrivateKey) keyStore.getKey(KEY_ALIAS, KEY_PASSWORD.toCharArray());
    }

    public static X509Certificate getCertificate() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD.toCharArray());
        return (X509Certificate) keyStore.getCertificate(KEY_ALIAS);
    }
}
