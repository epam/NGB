/*
 * Verifies the enveloped XML signature on a SAML metadata document, cryptographically, against the
 * certificate the document carries in its own KeyInfo.
 *
 *   java VerifyXmlSignature.java /path/to/sp-metadata.xml
 *
 * `make saml-verify-signing` runs this over /catgenome/saml/metadata. It exists because nothing else
 * in this environment checks that signature: Keycloak validates <AuthnRequest> signatures (when
 * saml.client.signature is on) but never looks at the SP metadata, and neither curl nor Python has
 * XML-DSIG. The JDK does - java.xml.crypto - so this is a single-file source-launch program and needs
 * no dependencies.
 *
 * Exits 0 on a valid signature; prints which part failed - the SignedInfo signature or an individual
 * reference digest - and exits 1 otherwise.
 */
import java.io.File;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class VerifyXmlSignature {

    private VerifyXmlSignature() {
        // utility
    }

    /** Takes the public key straight out of the document's own X509Certificate. */
    private static final class CertificateKeySelector extends KeySelector {
        private X509Certificate certificate;

        @Override
        public KeySelectorResult select(final KeyInfo keyInfo, final Purpose purpose,
                                       final AlgorithmMethod method, final XMLCryptoContext context) {
            for (Object info : keyInfo.getContent()) {
                if (!(info instanceof X509Data)) {
                    continue;
                }
                for (Object entry : ((X509Data) info).getContent()) {
                    if (entry instanceof X509Certificate) {
                        certificate = (X509Certificate) entry;
                        final PublicKey key = certificate.getPublicKey();
                        return () -> key;
                    }
                }
            }
            throw new IllegalStateException("no X509Certificate in the signature's KeyInfo");
        }
    }

    /**
     * SAML documents reference themselves by their ID attribute, which is only an xml:id as far as a
     * schema says so - and this parses without one. Registering it by hand is what makes the
     * same-document reference "#_abc" resolvable.
     */
    private static void registerIdAttributes(final Node node) {
        if (node instanceof Element) {
            final Element element = (Element) node;
            if (element.hasAttribute("ID")) {
                element.setIdAttribute("ID", true);
            }
        }
        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            registerIdAttributes(children.item(i));
        }
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: VerifyXmlSignature <file>");
            System.exit(2);
        }

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final Document document = factory.newDocumentBuilder().parse(new File(args[0]));
        registerIdAttributes(document.getDocumentElement());

        final NodeList signatures =
                document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (signatures.getLength() == 0) {
            System.err.println("FAILED: the document is not signed - no ds:Signature element");
            System.exit(1);
        }

        final CertificateKeySelector keySelector = new CertificateKeySelector();
        final DOMValidateContext context = new DOMValidateContext(keySelector, signatures.item(0));
        final XMLSignature signature =
                XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);

        if (signature.validate(context)) {
            System.out.println("signature      : valid");
            System.out.println("algorithm      : "
                    + signature.getSignedInfo().getSignatureMethod().getAlgorithm());
            System.out.println("signed by      : " + keySelector.certificate.getSubjectX500Principal());
            System.out.println("references     : " + signature.getSignedInfo().getReferences().size()
                    + " (digests verified)");
            return;
        }

        System.err.println("FAILED: the signature did not validate");
        System.err.println("  SignedInfo signature: "
                + signature.getSignatureValue().validate(context));
        final Iterator<Reference> references = signature.getSignedInfo().getReferences().iterator();
        while (references.hasNext()) {
            final Reference reference = references.next();
            System.err.println("  reference " + reference.getURI() + ": " + reference.validate(context));
        }
        System.exit(1);
    }
}
