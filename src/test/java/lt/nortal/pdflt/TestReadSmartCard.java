package lt.nortal.pdflt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.Test;

import iaik.pkcs.pkcs11.DefaultInitializeArgs;
import iaik.pkcs.pkcs11.Info;
import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.objects.X509PublicKeyCertificate;
import iaik.pkcs.pkcs11.provider.Constants;
import iaik.pkcs.pkcs11.provider.IAIKPkcs11;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import lt.nortal.components.unisign.applet.infrastructure.InfrastructureException;
import lt.webmedia.sigute.service.common.utils.Base64;

/**
 * Created by DK on 7/19/16.
 */
public class TestReadSmartCard {

	Logger logger = Logger.getLogger(TestReadSmartCard.class.getName());

	private static final byte[] SHA256_PREFIX = {
			(byte) 0x30, (byte) 0x31, (byte) 0x30, (byte) 0x0d, (byte) 0x06,
			(byte) 0x09, (byte) 0x60, (byte) 0x86, (byte) 0x48, (byte) 0x01,
			(byte) 0x65, (byte) 0x03, (byte) 0x04, (byte) 0x02, (byte) 0x01,
			(byte) 0x05, (byte) 0x00, (byte) 0x04, (byte) 0x20
	};

	private static final byte[] SHA1_PREFIX = {
			(byte) 0x30, (byte) 0x21, (byte) 0x30,
			(byte) 0x09, (byte) 0x06, (byte) 0x05, (byte) 0x2b,
			(byte) 0x0e, (byte) 0x03, (byte) 0x02, (byte) 0x1a,
			(byte) 0x05, (byte) 0x00, (byte) 0x04, (byte) 0x14
	};

	@Test
	public void testIAIK() {
		try {
			Properties properties = new Properties();
			properties.put(Constants.PKCS11_NATIVE_MODULE, "/Library/EstonianIDCard/lib/esteid-pkcs11.so");
			properties.put(Constants.PKCS11_WRAPPER_PATH, "/Library/libpkcs11wrapper.jnilib");
			Module module = IAIKPkcs11.getModule(properties);
			module.initialize(new DefaultInitializeArgs());
			Info info = module.getInfo();
			// list all slots (readers) in which there is currenlty a token present
			Slot[] slotsWithToken =
					module.getSlotList(Module.SlotRequirement.TOKEN_PRESENT);
//			Token token = slotsWithToken[0].getToken(); // Authentication token
			Token token = slotsWithToken[1].getToken(); // Signature token
			Session session =
					token.openSession(Token.SessionType.SERIAL_SESSION,
							Token.SessionReadWriteBehavior.RW_SESSION,
							null,
							null);

			session.login(Session.UserType.USER, "04033".toCharArray());
			session.getSessionInfo();

			// we search for a RSA private key which we can use for signing
			RSAPrivateKey searchTemplate = new RSAPrivateKey();
			searchTemplate.getSign().setBooleanValue(Boolean.TRUE);
			// search for a key
			session.findObjectsInit(searchTemplate);
			Object[] matchingKeys = session.findObjects(1);
			RSAPrivateKey signatureKey = null;
			if (matchingKeys.length > 0) {
				session.findObjectsFinal();
				signatureKey = (RSAPrivateKey) matchingKeys[0];

				byte[] keyID = signatureKey.getId().getByteArrayValue();
				// this is the implementation that uses a concrete object class (X509PublicKeyCertificate) for
				// searching
				X509PublicKeyCertificate certificateSearchTemplate = new X509PublicKeyCertificate();
				certificateSearchTemplate.getId().setByteArrayValue(keyID);
				session.findObjectsInit(certificateSearchTemplate);
				Object[] foundCertificateObjects = session.findObjects(1);
				session.findObjectsFinal();
				X509PublicKeyCertificate cert = (X509PublicKeyCertificate) foundCertificateObjects[0];
				String pem = MacPKCS11Infrastructure.convertToPem(cert.getValue().toString());
				InputStream stream = new ByteArrayInputStream(pem.getBytes("UTF-8"));
				CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
				X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(stream);


				MessageDigest md;
				byte[] digest;
				try {
					InputStream in = new FileInputStream(new File("/Users/DK/Desktop/pdfa2a.pdf"));
					md = MessageDigest.getInstance("SHA-1");
					// calculate message digest
					digest = new byte[md.getDigestLength()];
					for (int l; (l = in.read(digest)) != -1;) {
						md.update(digest, 0, l);
					}
					digest = md.digest();
//					byte[] digest = Base64.decode("fuLJAyekLoKHTiLTIztYTdbP3lQt6Tv6M2jeMIrd1F0=");

					byte[] hashToSign = new byte[SHA1_PREFIX.length + digest.length];
					System.arraycopy(SHA1_PREFIX, 0, hashToSign, 0, SHA1_PREFIX.length);
					System.arraycopy(digest, 0, hashToSign, SHA1_PREFIX.length, digest.length);

					// select the signature mechanism, ensure your token supports it
					Mechanism signatureMechanism = Mechanism.get(PKCS11Constants.CKM_RSA_PKCS);
					// initialize for signing
					session.signInit(signatureMechanism, signatureKey);
					byte[] signatureBytes = session.sign(hashToSign);

					Signature signature = Signature.getInstance("SHA1with" + x509Certificate.getPublicKey().getAlgorithm());
					signature.initVerify(x509Certificate.getPublicKey());
					signature.update(digest);

					if (!signature.verify(signatureBytes)) {
						throw new GeneralSecurityException("Failed to verify user signature using public key stored in the certificate.");
					} else {
						logger.info("Success");
					}

				} catch (NoSuchAlgorithmException e) {
					logger.info("Failed to get MessageDigest.");
				}
			} else {
				session.findObjectsFinal();
				// we have not found a suitable key, we cannot contiue
			}
			// do not forget to finish the find operation
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	//	@Test
//	public void testPKCS1viaPKCS11() throws Exception {
//		SunPKCS11 provider = new SunPKCS11("/etc/registrucentras/pkcs11.cfg");
//		Security.addProvider(provider);
//		provider.login(null, new DummyCallbackHandler());
//		KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection("04033".toCharArray());
//		KeyStore.Builder builder = KeyStore.Builder.newInstance("PKCS11", provider, protection);
//		KeyStore keyStore = builder.getKeyStore();
//		keyStore.load(null, protection.getPassword());
//		keyStore.load(null, "04033".toCharArray());
////		KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("Authentication", null);
//		KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("Signature", null);
//		PrivateKey privateKey = privateKeyEntry.getPrivateKey();
//		Signature signature = Signature.getInstance("SHA1withRSA");
//		signature.initSign(privateKey);
//		byte[] toBeSigned = "hello world".getBytes();
//		signature.update(toBeSigned);
//		byte[] signatureValue = signature.sign();
//
//		X509Certificate certificate = (X509Certificate) privateKeyEntry.getCertificate();
//		RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
//		BigInteger signatureValueBigInteger = new BigInteger(signatureValue);
//		BigInteger messageBigInteger = signatureValueBigInteger.modPow(publicKey.getPublicExponent(),
//				publicKey.getModulus());
//		logger.info("original message: " + new String(Hex.encodeHex(messageBigInteger.toByteArray())));
//	}
//
	@Test
	public void testSunPKCS11() {
		Provider provider = new sun.security.pkcs11.SunPKCS11("/etc/registrucentras/pkcs11.cfg");
		Security.addProvider(provider);
		KeyStore keyStore = null;
		char[] pin = "04033".toCharArray();
		try {
			keyStore = KeyStore.getInstance("PKCS11", provider);
			KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection(pin);
			keyStore.load(null, pp.getPassword());
			Enumeration aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				Object alias = aliases.nextElement();
				try {
					X509Certificate cert0 = (X509Certificate) keyStore.getCertificate(alias.toString());
					logger.info("I am: " + cert0.getSubjectDN().getName());
					PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias.toString(), pin);
					logger.info("Private key: " + privateKey);
				} catch (Exception e) {
					continue;
				}
			}
			KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("Signature", null);
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(privateKeyEntry.getPrivateKey());
			byte[] toBeSigned = "hello world".getBytes();
			signature.update(toBeSigned);
			byte[] signatureValue = signature.sign();
			logger.info(Base64.encode(signatureValue));
			Security.removeProvider(provider.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSmccGetCertificates() throws InfrastructureException, KeyStoreException {
		SmccInfrastructure infrastructure = new SmccInfrastructure();
		List<X509Certificate> certificates = infrastructure.getCertificates();
		logger.info("I am: " + certificates.get(0).getSubjectDN().getName());
	}
}
