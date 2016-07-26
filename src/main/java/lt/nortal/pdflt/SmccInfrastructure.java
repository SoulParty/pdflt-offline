package lt.nortal.pdflt;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import at.gv.egiz.smcc.CancelledException;
import at.gv.egiz.smcc.PinInfo;
import at.gv.egiz.smcc.SignatureCard;
import at.gv.egiz.smcc.pin.gui.PINGUI;
import at.gv.egiz.smcc.util.SMCCHelper;
import lt.nortal.components.unisign.applet.infrastructure.InfrastructureException;
import lt.nortal.components.unisign.applet.infrastructure.SignatureInfrastructure;

/**
 * Created by DK on 7/20/16.
 */
public class SmccInfrastructure implements SignatureInfrastructure {

	private static final Logger LOG = Logger.getLogger(SmccInfrastructure.class.getName());

	private SignatureCard signatureCard;

	private char[] pin;

	public SmccInfrastructure() {
		SMCCHelper helper = new SMCCHelper();
		signatureCard = helper.getSignatureCard(Locale.getDefault());

		if (signatureCard == null) {
			throw new RuntimeException();
		}
	}

	@Override
	public byte[] sign(final X509Certificate x509Certificate, final byte[] bytes) throws InfrastructureException {
		try {
			InputStream data = new ByteArrayInputStream(bytes);
			return signatureCard.createSignature(
					data, SignatureCard.KeyboxName.SECURE_SIGNATURE_KEYPAIR, new ConsolePINGUI(), "http://www.w3.org/2000/09/xmldsig#rsa-sha1");
		} catch (Throwable e) {
			LOG.info(e.getMessage());
			throw new InfrastructureException(InfrastructureException.Code.SIGN_ERROR);
		}
	}

	@Override
	public List<X509Certificate> getCertificates() throws InfrastructureException {
		try {
			byte[] certificate = signatureCard.getCertificate(SignatureCard.KeyboxName.SECURE_SIGNATURE_KEYPAIR, new ConsolePINGUI());
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
			return Collections.singletonList((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certificate)));
		} catch (Throwable e) {
			LOG.info(e.getMessage());
			throw new InfrastructureException(InfrastructureException.Code.GET_CERTIFICATES_ERROR);
		}
	}

	public static class ConsolePINGUI implements PINGUI {

		@Override
		public void allKeysCleared() {
		}

		@Override
		public void correctionButtonPressed() {
		}

		@Override
		public void enterPIN(PinInfo spec, int retries) throws CancelledException,
				InterruptedException {
		}

		@Override
		public void enterPINDirect(PinInfo spec, int retries)
				throws CancelledException, InterruptedException {
		}

		@Override
		public void validKeyPressed() {
		}

		@Override
		public char[] providePIN(PinInfo pinSpec, int retries)
				throws CancelledException, InterruptedException {
//      System.out.print("Enter " + pinSpec.getLocalizedName() + ": ");
//      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//      String pin;
//      try {
//        pin = in.readLine();
//      } catch (IOException e) {
//        throw new CancelledException(e);
//      }
//      if (pin == null || pin.length() == 0) {
//        throw new CancelledException();
//      }
			return "04033".toCharArray();
		}

	}
}
