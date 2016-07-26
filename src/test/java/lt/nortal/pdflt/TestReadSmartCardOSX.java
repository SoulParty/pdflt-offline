package lt.nortal.pdflt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;

import lt.nortal.components.unisign.applet.infrastructure.InfrastructureException;

/**
 * Created by DK on 7/19/16.
 * Tested on Estonian ID card with reader: ACS ACR 38U-CCID.
 */
public class TestReadSmartCardOSX {

	Logger logger = Logger.getLogger(TestReadSmartCardOSX.class.getName());
	private static final String PIN2 = "04033";

	@Test
	public void testSmccGetCertificates() throws InfrastructureException, KeyStoreException {
		SmccInfrastructure infrastructure = new SmccInfrastructure(PIN2);
		List<X509Certificate> certificates = infrastructure.getCertificates();
		logger.info("I am: " + certificates.get(0).getSubjectDN().getName());
	}

	@Test
	public void testSmccSign() throws InfrastructureException, GeneralSecurityException, IOException {
		SmccInfrastructure infrastructure = new SmccInfrastructure(PIN2);

		InputStream in = new FileInputStream(new File("/Users/DK/Desktop/pdfa2a.pdf"));

		MessageDigest md = MessageDigest.getInstance("SHA-1");
		// calculate message digest
		byte[] digest = new byte[md.getDigestLength()];
		for (int l; (l = in.read(digest)) != -1; ) {
			md.update(digest, 0, l);
		}
		digest = md.digest();
//		byte[] digest = Base64.decode("fuLJAyekLoKHTiLTIztYTdbP3lQt6Tv6M2jeMIrd1F0=");

		byte[] signatureBytes = infrastructure.sign(null, digest);

		X509Certificate x509Certificate = infrastructure.getCertificates().get(0);
		Signature signature = Signature.getInstance("SHA1with" + x509Certificate.getPublicKey().getAlgorithm());
		signature.initVerify(x509Certificate.getPublicKey());
		signature.update(digest);

		if (!signature.verify(signatureBytes)) {
			throw new GeneralSecurityException("Failed to verify user signature using public key stored in the certificate.");
		} else {
			logger.info("Success");
		}
	}
}
