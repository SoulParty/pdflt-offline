package lt.nortal.pdflt;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
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
import sun.nio.cs.StandardCharsets;
import sun.security.pkcs11.SunPKCS11;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * Created by DK on 7/19/16.
 */
public class TestReadSmartCard {

	@Test
	public void testIAIK() {
		try {
			Properties properties = new Properties();
			properties.put(Constants.PKCS11_NATIVE_MODULE, "/Library/EstonianIDCard/lib/esteid-pkcs11.so");
			properties.put(Constants.PKCS11_WRAPPER_PATH, "/Library/libpkcs11wrapper.jnilib");
			Module module = IAIKPkcs11.getModule(properties);
			module.initialize(new DefaultInitializeArgs());
			Info info = module.getInfo();
			System.out.println(info);
			// list all slots (readers) in which there is currenlty a token present
			Slot[] slotsWithToken =
					module.getSlotList(Module.SlotRequirement.TOKEN_PRESENT);
//			Token token = slotsWithToken[0].getToken();
			Token token = slotsWithToken[1].getToken();
			Session session =
					token.openSession(Token.SessionType.SERIAL_SESSION,
							Token.SessionReadWriteBehavior.RO_SESSION,
							null,
							null);

			session.login(Session.UserType.USER, "04033".toCharArray());

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
				byte[] data = "hello world".getBytes();
				// select the signature mechanism, ensure your token supports it
				Mechanism signatureMechanism = Mechanism.get(PKCS11Constants.CKM_RSA_PKCS);
				// initialize for signing
				session.signInit(signatureMechanism, signatureKey);
				byte[] signatureValue = session.sign(data);
				System.out.println(signatureValue);

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
				X509Certificate x509Certificate = (X509Certificate)certificateFactory.generateCertificate(stream);
			} else {
				session.findObjectsFinal();
				// we have not found a suitable key, we cannot contiue
			}
			// do not forget to finish the find operation
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}


	@Test
	public void testIAIK2() {
		try {
			Properties properties = new Properties();
			properties.put(Constants.PKCS11_NATIVE_MODULE, "/Library/EstonianIDCard/lib/esteid-pkcs11.so");
			properties.put(Constants.PKCS11_WRAPPER_PATH, "/Library/libpkcs11wrapper.jnilib");
			Module module = IAIKPkcs11.getModule(properties);
			module.initialize(new DefaultInitializeArgs());
			Info info = module.getInfo();
			System.out.println(info);
			// list all slots (readers) in which there is currenlty a token present
			Slot[] slotsWithToken =
					module.getSlotList(Module.SlotRequirement.TOKEN_PRESENT);
//			Token token = slotsWithToken[0].getToken();
			Token token = slotsWithToken[1].getToken();
			Session session =
					token.openSession(Token.SessionType.SERIAL_SESSION,
							Token.SessionReadWriteBehavior.RO_SESSION,
							null,
							null);

			session.login(Session.UserType.SO, "04033".toCharArray());

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
				X509Certificate x509Certificate = (X509Certificate)certificateFactory.generateCertificate(stream);


//128 baitai

				byte[] data =
//						("Hello-World!!!!!!!!aa"
//								+ "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//								+ "!!!!!!!!!!!!!!!!!!!!!!")
				("Hello-World!!!!!!!!aa"
								+ "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
								+ "!!!!!!!!!!!!!!!!!!!!!!!!!!!")
						.getBytes();
//				byte[] data = "hello world".getBytes();
				// select the signature mechanism, ensure your token supports it
				Mechanism signatureMechanism = Mechanism.get(PKCS11Constants.CKM_RSA_PKCS);

				// initialize for signing
				session.signInit(signatureMechanism, signatureKey);
				byte[] signatureValue = session.sign(data);
				System.out.println(signatureValue);


			} else {
				session.findObjectsFinal();
				// we have not found a suitable key, we cannot contiue
			}
			// do not forget to finish the find operation
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

//
//	@Test
//	public void testPKCS1viaPKCS11() throws Exception {
//		Logger logger = Logger.getLogger(TestReadSmartCard.class.getName());
//
//		SunPKCS11 provider = new SunPKCS11("/etc/registrucentras/pkcs11.cfg");
//		Security.addProvider(provider);
//		provider.login(null, new DummyCallbackHandler());
//		KeyStore keyStore = KeyStore.getInstance("PKCS11", provider);
//		keyStore.load(null, "0685".toCharArray());
//		KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection("0685".toCharArray());
//		KeyStore.Builder builder = KeyStore.Builder.newInstance("PKCS11", provider, protection);
//		KeyStore keyStore = builder.getKeyStore();
//		keyStore.load(null, protection.getPassword());
//		keyStore.load(null, "04033".toCharArray());
//		keyStore.load(null, null);
//		KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("Authentication", null);
//		KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("PrivateKeyEntry", null);
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
//	@Test
//	public void testOtherMethod() {
//		Provider provider = new sun.security.pkcs11.SunPKCS11("/etc/registrucentras/pkcs11.cfg");
//		Security.addProvider(provider);
//		KeyStore keyStore = null;
//		String pin = "0685";
//		String pin = "04033";
//		try {
//			keyStore = KeyStore.getInstance("PKCS11", provider);
//			KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection(pin.toCharArray());
//			keyStore.load(null, pp.getPassword());
//			Enumeration aliases = keyStore.aliases();
//			while (aliases.hasMoreElements()) {
//				Object alias = aliases.nextElement();
//				try {
//					X509Certificate cert0 = (X509Certificate) keyStore.getCertificate(alias.toString());
//					System.out.println("I am: " + cert0.getSubjectDN().getName());
//					PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias.toString(), null);
//					System.out.println("Private key: " + privateKey);
//				} catch (Exception e) {
//					continue;
//				}
//			}
//			KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("Signature", null);
//			Signature signature = Signature.getInstance("SHA1withRSA");
//			signature.initSign(privateKeyEntry.getPrivateKey());
//			byte[] toBeSigned = "hello world".getBytes();
//			signature.update(toBeSigned);
//			byte[] signatureValue = signature.sign();
//			Security.removeProvider(provider.getName());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	@Test
//	public void testOtherMethod2() throws InfrastructureException, KeyStoreException {
//		PKCS11SignatureInfrastructure2 pkcs11SignatureInfrastructure2 = new PKCS11SignatureInfrastructure2("/etc/registrucentras/pkcs11.cfg");
//		X509Certificate certificate = pkcs11SignatureInfrastructure2.getCertificate();
//		byte[] signedBytes = pkcs11SignatureInfrastructure2.sign(certificate, "Hello World".getBytes());
//	}
}
