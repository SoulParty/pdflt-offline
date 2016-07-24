package lt.nortal.pdflt;

import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import be.cardon.utils.OperatingSystem;
import iaik.pkcs.pkcs11.provider.Constants;
import lt.nortal.components.unisign.applet.SigningApplet;
import lt.nortal.components.unisign.applet.infrastructure.InfrastructureException;
import lt.nortal.components.unisign.applet.infrastructure.PKCS11SignatureInfrastructure;
import lt.nortal.components.unisign.applet.infrastructure.SignatureInfrastructure;
import lt.nortal.components.unisign.applet.infrastructure.WindowsSignatureInfrastructure;
import lt.nortal.components.unisign.utils.Base64;
import lt.nortal.components.unisign.utils.CertificateUtils;
import lt.nortal.components.unisign.utils.IssuerPolicyOIDFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * Created by DK on 7/5/16.
 */
@Component
public class SigningService {

	private static final long serialVersionUID = -5605856500792187103L;
	private Logger logger;
	private String personalCode;
	private IssuerPolicyOIDFilter issuerPolicyOIDFilter;
	private boolean skippNonRepudiationCheck;
	private PublicKey serverPublicKey;
	private boolean showNotSuitableCertificates;
	private SignatureInfrastructure infrastructure;
	private ExecutorService executor;
	private FormView formView;
	private String pin;

	public void init(FormView formView) {
		try {
			this.formView = formView;
			InputStream e = this.getClass().getResourceAsStream("/log4j.properties");
			LogManager.getLogManager().readConfiguration(e);
			this.logger = Logger.getLogger(SigningApplet.class.getName());
			this.logger.info("Applet is initializing.");
			this.executor = Executors.newSingleThreadExecutor();
			this.personalCode = null;
			this.issuerPolicyOIDFilter = new IssuerPolicyOIDFilter("");
			this.skippNonRepudiationCheck = false;
			ResourceBundle properties = ResourceBundle.getBundle("configuration");
			this.showNotSuitableCertificates = false;
			this.logger.info("Applet initialized successfully.");
			this.appletLoadSuccess();
		} catch (Exception var3) {
			this.appletLoadError(var3);
		}
	}

	public void setPin(final String pin) {
		this.pin = pin;
	}

	private void createInfrastructure() {
		if (OperatingSystem.isWindows()) {
			this.infrastructure = new WindowsSignatureInfrastructure();
		} else {
			this.infrastructure = new MacPKCS11Infrastructure(
					pin,
					"/Library/EstonianIDCard/lib/esteid-pkcs11.so",
					"/Library/libpkcs11wrapper.jnilib");
		}

	}

	private void appletLoadSuccess() {
		this.logger.info("Calling appletLoadSuccess java script function.");
//		JSObject.getWindow(this).call("appletLoadSuccess", new Object[0]);
	}

	private void appletLoadError(Exception e) {
		this.logger.log(Level.SEVERE, "Applet initialization failed.", e);
		InfrastructureException.Code errorCode = this.extractErrorCode(e);
//		JSObject.getWindow(this).call("appletLoadError", new Object[] { errorCode.getValue(), e.getMessage() });
	}

	public long getCertificates() {
		final long actionId = this.getUniqueActionId();
		this.logger.info("Called getCertificates function. Unique action ID: " + actionId);
		this.executor.execute(new Runnable() {
			public void run() {
				AccessController.doPrivileged(new PrivilegedAction() {
					public Void run() {
						SigningService.this.doGetCertificates(actionId);
						return null;
					}
				});
			}
		});
		return actionId;
	}

	private void appletGetCertificatesSuccess(long actionId, String certificateListJson) {
		this.logger.info("Calling appletGetCertificatesSuccess java script function. Unique action ID: " + actionId);
//		JSObject.getWindow(this).call("appletGetCertificatesSuccess", new Object[] { Long.valueOf(actionId), certificateListJson });
//		JOptionPane.showMessageDialog(source, certificateListJson);
	}

	private void appletGetCertificatesSuccessNotSuitable(long actionId, String certificateListJson) {
		this.logger.info("Calling appletGetCertificatesSuccessNotSuitable java script function. Unique action ID: " + actionId);
//		JOptionPane.showMessageDialog(source, "appletGetCertificatesSuccessNotSuitable");
//		JSObject.getWindow(this).call("appletGetCertificatesSuccessNotSuitable", new Object[] { Long.valueOf(actionId), certificateListJson });
	}

	private void appletGetCertificatesError(long actionId, Exception e) {
		this.logger.log(Level.SEVERE, "Failed to refresh certificates list. Unique action ID: " + actionId, e);
		InfrastructureException.Code errorCode = this.extractErrorCode(e);
//		JOptionPane.showMessageDialog(source, errorCode.getValue());
//		JSObject.getWindow(this).call("appletGetCertificatesError", new Object[] { Long.valueOf(actionId), errorCode.getValue(), e.getMessage() });
	}

	private void doGetCertificates(long actionId) {
		this.logger.info("Refreshing certificate list.");

		try {
			this.createInfrastructure();
			List exc = this.infrastructure.getCertificates();
			this.logger.info("Found " + exc.size() + " certificates.");
			ArrayList suitableCertificates = new ArrayList(exc.size());
			ArrayList notSuitableCertificates = new ArrayList(exc.size());
			Iterator i$ = exc.iterator();

			while (true) {
				while (i$.hasNext()) {
					X509Certificate certificate = (X509Certificate) i$.next();
					if (this.isValidCertificate(certificate) && this.isSuitablePersonalCode(certificate) && this.isAcceptableIssuer(certificate)) {
						suitableCertificates.add(certificate);
					} else {
						notSuitableCertificates.add(certificate);
					}
				}

				this.logger.log(Level.INFO, "Found {0} certificates suitable for signing, and {1} not suitible certificates.",
						new Object[] { Integer.valueOf(suitableCertificates.size()), Integer.valueOf(notSuitableCertificates.size()) });
				this.appletGetCertificatesSuccess(actionId, this.getJSONCertificates(suitableCertificates));
				if (!suitableCertificates.isEmpty()) {
					formView.doPreSign((X509Certificate) suitableCertificates.get(1));
				} else if (!notSuitableCertificates.isEmpty()) {
					formView.doPreSign((X509Certificate) notSuitableCertificates.get(0));
				}

				if (this.showNotSuitableCertificates) {
					this.appletGetCertificatesSuccessNotSuitable(actionId, this.getJSONCertificates(notSuitableCertificates));
				}
				break;
			}
		} catch (Exception var8) {
			this.appletGetCertificatesError(actionId, var8);
		}

	}

	private boolean isValidCertificate(X509Certificate certificate) {
		boolean fits = false;
		if (CertificateUtils.isExpired(certificate)) {
			this.logger.warning("Certificate is expired. Rejecting certificate.");
		} else if (!CertificateUtils.isDigitalSignatureUsage(certificate)) {
			this.logger.warning("Certificate usage not suitable for document signing. Rejecting certificate.");
		} else if (!this.skippNonRepudiationCheck && !CertificateUtils.isNonRepudiationUsage(certificate)) {
			this.logger.warning("Certificate usage not suitable for non-repudiational signing. Rejecting certificate.");
		} else {
			fits = true;
		}

		return fits;
	}

	private boolean isSuitablePersonalCode(X509Certificate certificate) {
		String subjectPersonalCode = CertificateUtils.getSubjectPersonalCode(certificate);
		boolean fits = this.personalCode == null || this.personalCode.length() == 0 || this.personalCode.equals(subjectPersonalCode);
		if (!fits) {
			this.logger.warning("Personal code associated with this instance [" + this.personalCode + "] does not match one from certificate ["
					+ subjectPersonalCode + "]. Rejecting certificate.");
		}

		return fits;
	}

	private boolean isAcceptableIssuer(X509Certificate certificate) throws CertificateEncodingException {
		boolean fits = this.issuerPolicyOIDFilter.matches(certificate);
		if (!fits) {
			this.logger.warning("User has certificate from unsupported issuer. Rejecting certificate.");
		}

		return fits;
	}

	private String getJSONCertificates(List<X509Certificate> certificates) throws CertificateEncodingException, JSONException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		JSONArray array = new JSONArray();
		Iterator i$ = certificates.iterator();

		while (i$.hasNext()) {
			X509Certificate certificate = (X509Certificate) i$.next();
			JSONObject object = new JSONObject();
			object.put("certificate", Base64.encode(CertificateUtils.encodeCertificate(certificate)));
			object.put("subjectCN", CertificateUtils.getSubjectCN(certificate));
			object.put("subjectPrincipal", certificate.getSubjectX500Principal().toString());
			object.put("issuerCN", CertificateUtils.getIssuerCN(certificate));
			object.put("issuerPrincipal", certificate.getIssuerX500Principal().toString());
			object.put("notBefore", dateFormat.format(certificate.getNotBefore()));
			object.put("notAfter", dateFormat.format(certificate.getNotAfter()));
			array.put(object);
		}

		return array.toString();
	}

	public long sign(final String digestString, final X509Certificate certificate) {
		final long actionId = this.getUniqueActionId();
		this.logger.info("Called sign function. Unique action ID: " + actionId);
		this.executor.execute(new Runnable() {
			public void run() {
				AccessController.doPrivileged(new PrivilegedAction() {
					public Void run() {
						doSign(actionId, digestString, certificate);
						return null;
					}
				});
			}
		});
		return actionId;
	}

	private void appletSignSuccess(long actionId, byte[] signatureBytes) {
		this.logger.info("Calling appletSignSuccess java script function. Unique action ID: " + actionId);
//		JSObject.getWindow(this).call("appletSignSuccess", new Object[] { Long.valueOf(actionId), signature });
		formView.sign(signatureBytes);
	}

	private void appletSignError(long actionId, Exception e) {
		this.logger.log(Level.SEVERE, "Signing failed. Unique action ID: " + actionId, e);
		InfrastructureException.Code errorCode = this.extractErrorCode(e);
//		JSObject.getWindow(this).call("appletSignError", new Object[] { Long.valueOf(actionId), errorCode.getValue(), e.getMessage() });
	}

	private void doSign(long actionId, String dataToSignString, X509Certificate certificate) {
		this.logger.info("Signing.");

		try {
//			MACBuilder e = new MACBuilder();
//			e.append("dataToSign", dataToSignString);
//			e.append("certificate", certificateString);

			byte[] dataToSign = Base64.decode(dataToSignString);
			byte[] signatureBytes = this.infrastructure.sign(certificate, dataToSign);
			this.appletSignSuccess(actionId, signatureBytes);
		} catch (Exception var10) {
			this.appletSignError(actionId, var10);
		}

	}

	private InfrastructureException.Code extractErrorCode(Exception e) {
		InfrastructureException.Code errorCode;
		if (e instanceof InfrastructureException) {
			errorCode = ((InfrastructureException) e).getCode();
		} else if (OperatingSystem.isWindows()) {
			errorCode = InfrastructureException.Code.WINDOWS_INFRASTRUCTURE_ERROR;
		} else {
			errorCode = InfrastructureException.Code.PKCS11_INFRASTRUCTURE_ERROR;
		}

		return errorCode;
	}

	private long getUniqueActionId() {
		return System.currentTimeMillis();
	}
}
