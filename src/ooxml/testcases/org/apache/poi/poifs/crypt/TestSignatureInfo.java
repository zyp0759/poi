/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

/* ====================================================================
   This product contains an ASLv2 licensed version of the OOXML signer
   package from the eID Applet project
   http://code.google.com/p/eid-applet/source/browse/trunk/README.txt  
   Copyright (C) 2008-2014 FedICT.
   ================================================================= */ 
package org.apache.poi.poifs.crypt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.apache.poi.POIDataSamples;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.crypt.dsig.SignatureConfig;
import org.apache.poi.poifs.crypt.dsig.SignatureInfo;
import org.apache.poi.poifs.crypt.dsig.SignatureInfo.SignaturePart;
import org.apache.poi.poifs.crypt.dsig.facets.EnvelopedSignatureFacet;
import org.apache.poi.poifs.crypt.dsig.facets.KeyInfoSignatureFacet;
import org.apache.poi.poifs.crypt.dsig.facets.XAdESSignatureFacet;
import org.apache.poi.poifs.crypt.dsig.facets.XAdESXLSignatureFacet;
import org.apache.poi.poifs.crypt.dsig.services.RevocationData;
import org.apache.poi.poifs.crypt.dsig.services.RevocationDataService;
import org.apache.poi.poifs.crypt.dsig.services.TimeStampService;
import org.apache.poi.poifs.crypt.dsig.services.TimeStampServiceValidator;
import org.apache.poi.poifs.crypt.dsig.spi.DigestInfo;
import org.apache.poi.util.DocumentHelper;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.xmlbeans.XmlObject;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.etsi.uri.x01903.v13.DigestAlgAndValueType;
import org.etsi.uri.x01903.v13.QualifyingPropertiesType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3.x2000.x09.xmldsig.ReferenceType;
import org.w3.x2000.x09.xmldsig.SignatureDocument;
import org.w3c.dom.Document;

public class TestSignatureInfo {
    private static final POILogger LOG = POILogFactory.getLogger(TestSignatureInfo.class);
    private static final POIDataSamples testdata = POIDataSamples.getXmlDSignInstance();

    private static Calendar cal;
    private KeyPair keyPair = null;
    private X509Certificate x509 = null;
    

    
    @BeforeClass
    public static void initBouncy() throws MalformedURLException {
        File bcProvJar = new File("lib/bcprov-ext-jdk15on-1.51.jar");
        File bcPkixJar = new File("lib/bcpkix-jdk15on-151.jar");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URLClassLoader ucl = new URLClassLoader(new URL[]{bcProvJar.toURI().toURL(),bcPkixJar.toURI().toURL()}, cl);
        Thread.currentThread().setContextClassLoader(ucl);
        CryptoFunctions.registerBouncyCastle();

        /*** TODO : set cal to now ... only set to fixed date for debugging ... */ 
        cal = Calendar.getInstance();
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(2014, 7, 6, 21, 42, 12);
    }

    @Test
    public void getSignerUnsigned() throws Exception {
        String testFiles[] = { 
            "hello-world-unsigned.docx",
            "hello-world-unsigned.pptx",
            "hello-world-unsigned.xlsx",
            "hello-world-office-2010-technical-preview-unsigned.docx"
        };
        
        for (String testFile : testFiles) {
            OPCPackage pkg = OPCPackage.open(testdata.getFile(testFile), PackageAccess.READ);
            SignatureConfig sic = new SignatureConfig();
            sic.setOpcPackage(pkg);
            SignatureInfo si = new SignatureInfo();
            si.setSignatureConfig(sic);
            List<X509Certificate> result = new ArrayList<X509Certificate>();
            for (SignaturePart sp : si.getSignatureParts()) {
                if (sp.validate()) {
                    result.add(sp.getSigner());
                }
            }
            pkg.revert();
            pkg.close();
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
    
    @Test
    public void getSigner() throws Exception {
        String testFiles[] = { 
            "hyperlink-example-signed.docx",
            "hello-world-signed.docx",
            "hello-world-signed.pptx",
            "hello-world-signed.xlsx",
            "hello-world-office-2010-technical-preview.docx",
            "ms-office-2010-signed.docx",
            "ms-office-2010-signed.pptx",
            "ms-office-2010-signed.xlsx",
            "Office2010-SP1-XAdES-X-L.docx",
            "signed.docx",
        };
        
        for (String testFile : testFiles) {
            OPCPackage pkg = OPCPackage.open(testdata.getFile(testFile), PackageAccess.READ);
            SignatureConfig sic = new SignatureConfig();
            sic.setOpcPackage(pkg);
            SignatureInfo si = new SignatureInfo();
            si.setSignatureConfig(sic);
            List<X509Certificate> result = new ArrayList<X509Certificate>();
            for (SignaturePart sp : si.getSignatureParts()) {
                if (sp.validate()) {
                    result.add(sp.getSigner());
                }
            }

            assertNotNull(result);
            assertEquals("test-file: "+testFile, 1, result.size());
            X509Certificate signer = result.get(0);
            LOG.log(POILogger.DEBUG, "signer: " + signer.getSubjectX500Principal());

            boolean b = si.verifySignature();
            assertTrue("test-file: "+testFile, b);
            pkg.revert();
        }
    }

    @Test
    public void getMultiSigners() throws Exception {
        String testFile = "hello-world-signed-twice.docx";
        OPCPackage pkg = OPCPackage.open(testdata.getFile(testFile), PackageAccess.READ);
        SignatureConfig sic = new SignatureConfig();
        sic.setOpcPackage(pkg);
        SignatureInfo si = new SignatureInfo();
        si.setSignatureConfig(sic);
        List<X509Certificate> result = new ArrayList<X509Certificate>();
        for (SignaturePart sp : si.getSignatureParts()) {
            if (sp.validate()) {
                result.add(sp.getSigner());
            }
        }

        assertNotNull(result);
        assertEquals("test-file: "+testFile, 2, result.size());
        X509Certificate signer1 = result.get(0);
        X509Certificate signer2 = result.get(1);
        LOG.log(POILogger.DEBUG, "signer 1: " + signer1.getSubjectX500Principal());
        LOG.log(POILogger.DEBUG, "signer 2: " + signer2.getSubjectX500Principal());

        boolean b = si.verifySignature();
        assertTrue("test-file: "+testFile, b);
        pkg.revert();
    }
    
    @Test
    public void testSignSpreadsheet() throws Exception {
        String testFile = "hello-world-unsigned.xlsx";
        OPCPackage pkg = OPCPackage.open(copy(testdata.getFile(testFile)), PackageAccess.READ_WRITE);
        sign(pkg, "Test", "CN=Test", 1);
        pkg.close();
    }

    @Test
    public void testSignSpreadsheetWithSignatureInfo() throws Exception {
        initKeyPair("Test", "CN=Test");
        String testFile = "hello-world-unsigned.xlsx";
        OPCPackage pkg = OPCPackage.open(copy(testdata.getFile(testFile)), PackageAccess.READ_WRITE);
        SignatureConfig sic = new SignatureConfig();
        sic.setOpcPackage(pkg);
        sic.setKey(keyPair.getPrivate());
        sic.setSigningCertificateChain(Collections.singletonList(x509));
        SignatureInfo si = new SignatureInfo();
        si.setSignatureConfig(sic);
        // hash > sha1 doesn't work in excel viewer ...
        si.confirmSignature();
        List<X509Certificate> result = new ArrayList<X509Certificate>();
        for (SignaturePart sp : si.getSignatureParts()) {
            if (sp.validate()) {
                result.add(sp.getSigner());
            }
        }
        assertEquals(1, result.size());
        pkg.close();
    }

    @Test
    public void testSignEnvelopingDocument() throws Exception {
        String testFile = "hello-world-unsigned.xlsx";
        OPCPackage pkg = OPCPackage.open(copy(testdata.getFile(testFile)), PackageAccess.READ_WRITE);

        initKeyPair("Test", "CN=Test");
        final X509CRL crl = PkiTestUtils.generateCrl(x509, keyPair.getPrivate());
        
        // setup
        SignatureConfig signatureConfig = new SignatureConfig();
        signatureConfig.setOpcPackage(pkg);
        signatureConfig.setKey(keyPair.getPrivate());

        /*
         * We need at least 2 certificates for the XAdES-C complete certificate
         * refs construction.
         */
        List<X509Certificate> certificateChain = new ArrayList<X509Certificate>();
        certificateChain.add(x509);
        certificateChain.add(x509);
        signatureConfig.setSigningCertificateChain(certificateChain);
        
        signatureConfig.addSignatureFacet(new EnvelopedSignatureFacet());
        signatureConfig.addSignatureFacet(new KeyInfoSignatureFacet());
        signatureConfig.addSignatureFacet(new XAdESSignatureFacet());
        signatureConfig.addSignatureFacet(new XAdESXLSignatureFacet());
        
        boolean mockTsp = false;
        // http://timestamping.edelweb.fr/service/tsp
        // http://tsa.belgium.be/connect
        signatureConfig.setTspUrl("http://timestamping.edelweb.fr/service/tsp");
        signatureConfig.setTspOldProtocol(true);

        if (mockTsp) {
            TimeStampService tspService = new TimeStampService(){
                public byte[] timeStamp(byte[] data, RevocationData revocationData) throws Exception {
                    revocationData.addCRL(crl);
                    return "time-stamp-token".getBytes();                
                }
                public void setSignatureConfig(SignatureConfig config) {}
            };
            signatureConfig.setTspService(tspService);
        } else {
            TimeStampServiceValidator tspValidator = new TimeStampServiceValidator() {
                @Override
                public void validate(List<X509Certificate> certificateChain,
                RevocationData revocationData) throws Exception {
                    for (X509Certificate certificate : certificateChain) {
                        LOG.log(POILogger.DEBUG, "certificate: " + certificate.getSubjectX500Principal());
                        LOG.log(POILogger.DEBUG, "validity: " + certificate.getNotBefore() + " - " + certificate.getNotAfter());
                    }
                }
            };
            signatureConfig.setTspValidator(tspValidator);
            signatureConfig.setTspOldProtocol(signatureConfig.getTspUrl().contains("edelweb"));
        }
        
        final RevocationData revocationData = new RevocationData();
        revocationData.addCRL(crl);
        OCSPResp ocspResp = PkiTestUtils.createOcspResp(x509, false,
                x509, x509, keyPair.getPrivate(), "SHA1withRSA", cal.getTimeInMillis());
        revocationData.addOCSP(ocspResp.getEncoded());

        RevocationDataService revocationDataService = new RevocationDataService(){
            public RevocationData getRevocationData(List<X509Certificate> certificateChain) {
                return revocationData;
            }
        };
        signatureConfig.setRevocationDataService(revocationDataService);

        // operate
        SignatureInfo si = new SignatureInfo();
        si.setSignatureConfig(signatureConfig);
        si.confirmSignature();
        
        // verify
        Iterator<SignaturePart> spIter = si.getSignatureParts().iterator();
        assertTrue(spIter.hasNext());
        SignaturePart sp = spIter.next();
        boolean valid = sp.validate();
        assertTrue(valid);
        
        SignatureDocument sigDoc = sp.getSignatureDocument();
        String declareNS = 
            "declare namespace xades='http://uri.etsi.org/01903/v1.3.2#'; "
          + "declare namespace ds='http://www.w3.org/2000/09/xmldsig#'; ";
        
        String digestValXQuery = declareNS +
            "$this/ds:Signature/ds:SignedInfo/ds:Reference";
        for (ReferenceType rt : (ReferenceType[])sigDoc.selectPath(digestValXQuery)) {
            assertNotNull(rt.getDigestValue());
            assertEquals(HashAlgorithm.sha1.xmlSignUri, rt.getDigestMethod().getAlgorithm());
        }

        String certDigestXQuery = declareNS +
            "$this//xades:SigningCertificate/xades:Cert/xades:CertDigest";
        XmlObject xoList[] = sigDoc.selectPath(certDigestXQuery);
        assertEquals(xoList.length, 1);
        DigestAlgAndValueType certDigest = (DigestAlgAndValueType)xoList[0];
        assertNotNull(certDigest.getDigestValue());

        String qualPropXQuery = declareNS +
            "$this/ds:Signature/ds:Object/xades:QualifyingProperties";
        xoList = sigDoc.selectPath(qualPropXQuery);
        assertEquals(xoList.length, 1);
        QualifyingPropertiesType qualProp = (QualifyingPropertiesType)xoList[0];
        boolean qualPropXsdOk = qualProp.validate();
        assertTrue(qualPropXsdOk);

        pkg.close();
    }
    
    private OPCPackage sign(OPCPackage pkgCopy, String alias, String signerDn, int signerCount) throws Exception {
        initKeyPair(alias, signerDn);

        SignatureConfig signatureConfig = new SignatureConfig();
        signatureConfig.setKey(keyPair.getPrivate());
        signatureConfig.setSigningCertificateChain(Collections.singletonList(x509));
        signatureConfig.setExecutionTime(cal.getTime());
        signatureConfig.setDigestAlgo(HashAlgorithm.sha1);
        signatureConfig.setOpcPackage(pkgCopy);
        
        SignatureInfo si = new SignatureInfo();
        si.setSignatureConfig(signatureConfig);

        Document document = DocumentHelper.createDocument();

        // operate
        DigestInfo digestInfo = si.preSign(document, null);

        // verify
        assertNotNull(digestInfo);
        LOG.log(POILogger.DEBUG, "digest algo: " + digestInfo.hashAlgo);
        LOG.log(POILogger.DEBUG, "digest description: " + digestInfo.description);
        assertEquals("Office OpenXML Document", digestInfo.description);
        assertNotNull(digestInfo.hashAlgo);
        assertNotNull(digestInfo.digestValue);

        // setup: key material, signature value
        byte[] signatureValue = si.signDigest(digestInfo.digestValue);
        
        // operate: postSign
        si.postSign(document, signatureValue);

        // verify: signature
        si.getSignatureConfig().setOpcPackage(pkgCopy);
        List<X509Certificate> result = new ArrayList<X509Certificate>();
        for (SignaturePart sp : si.getSignatureParts()) {
            if (sp.validate()) {
                result.add(sp.getSigner());
            }
        }
        assertEquals(signerCount, result.size());

        return pkgCopy;
    }

    private void initKeyPair(String alias, String subjectDN) throws Exception {
        final char password[] = "test".toCharArray();
        File file = new File("build/test.pfx");

        KeyStore keystore = KeyStore.getInstance("PKCS12");

        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            keystore.load(fis, password);
            fis.close();
        } else {
            keystore.load(null, password);
        }

        if (keystore.isKeyEntry(alias)) {
            Key key = keystore.getKey(alias, password);
            x509 = (X509Certificate)keystore.getCertificate(alias);
            keyPair = new KeyPair(x509.getPublicKey(), (PrivateKey)key);
        } else {
            keyPair = PkiTestUtils.generateKeyPair();
            Calendar cal = Calendar.getInstance();
            Date notBefore = cal.getTime();
            cal.add(Calendar.YEAR, 1);
            Date notAfter = cal.getTime();
            KeyUsage keyUsage = new KeyUsage(KeyUsage.digitalSignature);
            
            x509 = PkiTestUtils.generateCertificate(keyPair.getPublic(), subjectDN
                , notBefore, notAfter, null, keyPair.getPrivate(), true, 0, null, null, keyUsage);

            keystore.setKeyEntry(alias, keyPair.getPrivate(), password, new Certificate[]{x509});
            FileOutputStream fos = new FileOutputStream(file);
            keystore.store(fos, password);
            fos.close();
        }
    }

    private static File copy(File input) throws IOException {
        String extension = input.getName().replaceAll(".*?(\\.[^.]+)?$", "$1");
        if (extension == null || "".equals(extension)) extension = ".zip";
        File tmpFile = new File("build", "sigtest"+extension);
        FileOutputStream fos = new FileOutputStream(tmpFile);
        FileInputStream fis = new FileInputStream(input);
        IOUtils.copy(fis, fos);
        fis.close();
        fos.close();
        return tmpFile;
    }

}