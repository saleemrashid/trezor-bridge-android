package com.saleemrashid.trezor.bridge.helpers;

import android.util.Log;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.Reader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;

public class SSLHelper {
    private static final String TAG = SSLHelper.class.getSimpleName();

    private static final String ENTRY_ALIAS = SSLHelper.class.getSimpleName();

    /* Prevent instantiation */
    private SSLHelper() {}

    public static SSLServerSocketFactory createFactory(final Reader certificateReader, final Reader privkeyReader) {
        final KeyStore store = createKeyStore(certificateReader, privkeyReader);

        if (store == null) {
            return null;
        }

        try {
            final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            factory.init(store, null);

            return NanoHTTPD.makeSSLSocketFactory(store, factory);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            Log.e(TAG, "Could not instantiate SSLSocketFactory", e);

            return null;
        }
    }

    public static KeyStore createKeyStore(final Reader certificateReader, final Reader privkeyReader) {
        PEMParser parser;

        parser = new PEMParser(certificateReader);

        final JcaX509CertificateConverter converter = new JcaX509CertificateConverter()
                .setProvider("BC");

        final List<X509Certificate> certificates = new ArrayList<>();

        X509CertificateHolder holder;
        try {
            while ((holder = (X509CertificateHolder) parser.readObject()) != null) {
                certificates.add(converter.getCertificate(holder));
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not parse certificate", e);

            return null;
        } catch (CertificateException e) {
            Log.e(TAG, "Could not convert certificate holder to certificate", e);

            return null;
        }

        parser = new PEMParser(privkeyReader);

        final PEMKeyPair pair;
        try {
            pair = (PEMKeyPair) parser.readObject();
        } catch (IOException e) {
            Log.e(TAG, "Could not parse private key", e);

            return null;
        }

        final PrivateKey key;
        try {
            key = new JcaPEMKeyConverter()
                    .setProvider("BC")
                    .getPrivateKey(pair.getPrivateKeyInfo());
        } catch (PEMException e) {
            Log.e(TAG, "Could not convert PEM to native format", e);

            return null;
        }

        final KeyStore store;
        try {
            store = KeyStore.getInstance("BKS");
            store.load(null);

            store.setKeyEntry(ENTRY_ALIAS, key, null, certificates.toArray(new Certificate[certificates.size()]));
        } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
            Log.e(TAG, "Could not instantiate KeyStore", e);

            return null;
        }

        return store;
    }
}
