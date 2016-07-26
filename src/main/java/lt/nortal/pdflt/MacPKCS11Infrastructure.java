package lt.nortal.pdflt;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import iaik.pkcs.pkcs11.DefaultInitializeArgs;
import iaik.pkcs.pkcs11.Info;
import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.objects.X509PublicKeyCertificate;
import iaik.pkcs.pkcs11.provider.Constants;
import iaik.pkcs.pkcs11.provider.IAIKPkcs11;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import iaik.security.provider.IAIK;
import lt.nortal.components.unisign.applet.SigningApplet;
import lt.nortal.components.unisign.applet.infrastructure.InfrastructureException;
import lt.nortal.components.unisign.applet.infrastructure.SignatureInfrastructure;

import org.apache.commons.codec.binary.Base64;

/**
 * Created by DK on 7/20/16.
 */
public class MacPKCS11Infrastructure implements SignatureInfrastructure {

	private static final Logger LOG = Logger.getLogger(SigningApplet.class.getName());
	private Session session;
	private Object[] matchingKeys;
	private RSAPrivateKey searchTemplate;
	private final int MAX_OBJECTS_TO_FIND = 10;
	private final int SLOT_NUMBER = 1; //TODO this needs a GUI selection by user

	public MacPKCS11Infrastructure(String pin, String nativeModulePath, String nativeWrapperPath) {
		try {
			Security.addProvider(new IAIK());
			Properties properties = new Properties();
			properties.put(Constants.PKCS11_NATIVE_MODULE, nativeModulePath);
			properties.put(Constants.PKCS11_WRAPPER_PATH, nativeWrapperPath);
			Module module = IAIKPkcs11.getModule(properties);
			module.initialize(new DefaultInitializeArgs());
			Info info = module.getInfo();
			System.out.println(info);

			// list all slots (readers) in which there is currently a token present
			Slot[] slotsWithToken =
					module.getSlotList(Module.SlotRequirement.TOKEN_PRESENT);
			Token token = slotsWithToken[SLOT_NUMBER].getToken();

			session = token.openSession(Token.SessionType.SERIAL_SESSION,
					Token.SessionReadWriteBehavior.RW_SESSION,
					null,
					null);
			session.login(Session.UserType.SO, pin.toCharArray());

			// we search for a RSA private key which we can use for signing
			searchTemplate = new RSAPrivateKey();
			searchTemplate.getSign().setBooleanValue(Boolean.TRUE);
		} catch (TokenException e) {
			LOG.info(e.getMessage());
		}
	}

	@Override
	public byte[] sign(final X509Certificate x509Certificate, final byte[] bytes) throws InfrastructureException {
		try {
			if (matchingKeys.length > 0) {
				session.findObjectsFinal();
				RSAPrivateKey signatureKey = (RSAPrivateKey) matchingKeys[0];
				// select the signature mechanism, ensure your token supports it
				Mechanism signatureMechanism = Mechanism.get(PKCS11Constants.CKM_RSA_PKCS);

				// initialize for signing
				session.signInit(signatureMechanism, signatureKey);
				return session.sign(bytes);
			} else {
				session.findObjectsFinal();
				throw new InfrastructureException(InfrastructureException.Code.SIGN_ERROR);
				// we have not found a suitable key, we cannot contiue
			}
			// do not forget to finish the find operation
		} catch (Throwable e) {
			LOG.info(e.getMessage());
			throw new InfrastructureException(InfrastructureException.Code.SIGN_ERROR);
		}
	}

	@Override
	public List<X509Certificate> getCertificates() throws InfrastructureException {
		try {
			// search for a key
			session.findObjectsInit(searchTemplate);
			matchingKeys = session.findObjects(MAX_OBJECTS_TO_FIND);
			session.findObjectsFinal();

			if (matchingKeys.length > 0) {
				List<X509Certificate> certificates = new ArrayList<X509Certificate>();
				for (Object key : matchingKeys) {
					RSAPrivateKey signatureKey = (RSAPrivateKey) key;
					byte[] keyID = signatureKey.getId().getByteArrayValue();
					// this is the implementation that uses a concrete object class (X509PublicKeyCertificate) for
					// searching
					X509PublicKeyCertificate certificateSearchTemplate = new X509PublicKeyCertificate();
					certificateSearchTemplate.getId().setByteArrayValue(keyID);
					session.findObjectsInit(certificateSearchTemplate);
					Object[] foundCertificateObjects = session.findObjects(MAX_OBJECTS_TO_FIND);
					session.findObjectsFinal();

					for (Object certificate : foundCertificateObjects) {
						X509PublicKeyCertificate cert = (X509PublicKeyCertificate) certificate;
						String pem = convertToPem(cert.getValue().toString());
						InputStream stream = new ByteArrayInputStream(pem.getBytes("UTF-8"));
						CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "IAIK");

						certificates.add((X509Certificate) certificateFactory.generateCertificate(stream));
					}
				}
				return certificates;
			} else {
				session.findObjectsFinal();
				return Collections.emptyList();
				// we have not found a suitable key, we cannot contiue
			}
			// do not forget to finish the find operation
		} catch (Throwable e) {
			LOG.info(e.getMessage());
			throw new InfrastructureException(InfrastructureException.Code.GET_CERTIFICATES_ERROR);
		}
	}

	protected static String convertToPem(String value) throws CertificateEncodingException {
		Base64 encoder = new Base64(64);
		String cert_begin = "-----BEGIN CERTIFICATE-----\n";
		String end_cert = "-----END CERTIFICATE-----";

		byte[] derCert = new BigInteger(value, 16).toByteArray();
		String pemCertPre = new String(encoder.encode(derCert));
		return cert_begin + pemCertPre + end_cert;
	}
}
