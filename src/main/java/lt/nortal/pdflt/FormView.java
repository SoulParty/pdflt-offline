package lt.nortal.pdflt;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.imageio.ImageIO;
import javax.swing.*;

import be.cardon.utils.OperatingSystem;
import lt.nortal.components.unisign.utils.Base64;
import lt.nortal.pdflt.domain.PresignData;
import lt.nortal.pdflt.domain.SignatureProperties;
import lt.nortal.pdflt.service.PdfService;
import lt.nortal.pdflt.utils.PdfLtHelper;

import com.itextpdf.text.pdf.PdfReader;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class FormView extends javax.swing.JFrame {

	private JRadioButton paprastasis;
	private JRadioButton isplestinis;
	private JTextField antraste;
	private JTextField tekstas;
	private JTextField siuntejoPav;
	private JCheckBox dokumentoRegistracija;
	private JLabel priedasLabel;
	private File priedas;

	private JTextField reason;
	private JTextField pdfLtSignerName;
	private JTextField notes;
	private JTextField role;
	private JTextField pdfLtContact;
	private JTextField pdfLtLocation;

	private JTextField pdfLtName;
	private JTextField pdfLtCode;
	private JTextField pdfLtEmail;
	private JTextField pdfLtAddress;
	private JTextField pdfLtRecipientName;
	private JTextField pdfLtRecipientCode;
	private JTextField pdfLtRecipientEmail;
	private JTextField pdfLtRecipientAddress;
	private JTextField pdfLtTitle;
	private JTextField pdfLtLang;
	private JTextField pdfLtDCIdentifier;
	private JTextField pdfLtRegistration;
	private JTextField pdfLtRegistrationCode;
	private JTextField pdfLtRegistrationDate;

	private PresignData presignData;

	private Color orange = new Color(255, 218, 0);

	@Autowired
	private SigningService signingService;

	@Autowired
	private PdfService pdfLtService;

	@Value("${edelivery.rest.endpoint}")
	private String endpoint;

	public FormView() {
		initComponents();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		/* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(FormView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(FormView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(FormView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(FormView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		//</editor-fold>

        /* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new FormView().setVisible(true);
			}
		});
	}

	public void setChosenFileText(File file) {
		priedasLabel.setText(file.getName());
		priedas = file;
	}

	public void doPreSign(X509Certificate certificate) throws Exception {
		try {
			OutputStream outputStream = null;
			if (OperatingSystem.isWindows()) {
				outputStream = new FileOutputStream(new File("C:\\tmp\\tmp.pdf"));
			} else {
				outputStream = new FileOutputStream(System.getProperty("user.home") + "/" + "tmp.pdf");
			}
			InputStream inputStream = new FileInputStream(priedas);
			PdfReader pdfReader = new PdfReader(IOUtils.toByteArray(inputStream));

			presignData = pdfLtService.prepareToSign(pdfReader, outputStream, createSignaturePropertiesFromInput(), certificate);

			outputStream.close();
			inputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (presignData != null) {
			byte[] dataToSign = presignData.getBytesToSign();
			String encodedDataToSign = Base64.encode(dataToSign);
			signingService.sign(encodedDataToSign, certificate);
		}
	}

	public void sign(byte[] signatureBytes) {
		try {
			OutputStream outputStream = null;
			InputStream inputStream = null;
			if (OperatingSystem.isWindows()) {
				outputStream = new FileOutputStream(new File("C:\\tmp\\" + priedas.getName()));
				inputStream = new FileInputStream(new File("C:\\tmp\\tmp.pdf"));
			} else {
				outputStream = new FileOutputStream(new File(System.getProperty("user.home") + "/" + priedas.getName()));
				inputStream = new FileInputStream(new File(System.getProperty("user.home") + "/" + "tmp.pdf"));
			}

			pdfLtService.sign(inputStream, outputStream, presignData, signatureBytes);

			outputStream.close();
			inputStream.close();

			JOptionPane.showMessageDialog(null, "Pavyko pasirasyti", "Success", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private SignatureProperties createSignaturePropertiesFromInput() {
		SignatureProperties signatureProperties = new SignatureProperties();
		PdfLtHelper.setPdfLtDetails(
				signatureProperties,
				pdfLtName.getText(),
				pdfLtCode.getText(),
				pdfLtEmail.getText(),
				pdfLtAddress.getText(),
				pdfLtRecipientName.getText(),
				pdfLtRecipientCode.getText(),
				pdfLtRecipientEmail.getText(),
				pdfLtRecipientAddress.getText(),
				pdfLtTitle.getText(),
				pdfLtLang.getText(),
				pdfLtDCIdentifier.getText(),
				pdfLtRegistration.getText(),
				pdfLtRegistrationCode.getText(),
				pdfLtRegistrationDate.getText(),
				pdfLtSignerName.getText(),
				pdfLtContact.getText(),
				pdfLtLocation.getText(),
				reason.getText(),
				notes.getText(),
				role.getText());

		return signatureProperties;
	}

	private void initComponents() {
		setTitle("E. pristatymas forma");
		setSize(1000, 700);
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		getContentPane().setBackground(orange);
		setLayout(null);

		Image image = null;
		try {
			URL url = new URL("http://www.antakalnioprogimnazija.vilnius.lm.lt/wp-content/uploads/2015/01/e-post.jpg");
			image = ImageIO.read(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
		JLabel imageLabel = new JLabel(new ImageIcon(image));
		imageLabel.setBounds(500, 0, 451, 112);
		add(imageLabel);

		priedasLabel = new JLabel("Prideti prieda...");
		priedasLabel.setBounds(10, 40, 200, 25);
		add(priedasLabel);

		JButton fcButton = new JButton("Pasirinkti faila");
		fcButton.setBounds(10, 70, 120, 25);
		add(fcButton);

		fcButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
				int result = fileChooser.showOpenDialog(FormView.this);
				if (result == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fileChooser.getSelectedFile();
					System.out.println("Selected file: " + selectedFile.getAbsolutePath());
					FormView.this.setChosenFileText(selectedFile);
				}
			}
		});

		JLabel sudarymoLabel = new JLabel("Dokumento sudarymo duomenys");
		sudarymoLabel.setBounds(140, 110, 320, 25);
		add(sudarymoLabel);

		JLabel autoriusLabel = new JLabel("Autorius");
		autoriusLabel.setBounds(140, 140, 320, 25);
		add(autoriusLabel);

		JLabel pdfLtNameLabel = new JLabel("Siuntejas");
		pdfLtNameLabel.setBounds(10, 170, 120, 25);
		add(pdfLtNameLabel);
		pdfLtName = new JTextField("test");
		pdfLtName.setBounds(140, 170, 320, 25);
		add(pdfLtName);

		JLabel pdfLtCodeLabel = new JLabel("Kodas");
		pdfLtCodeLabel.setBounds(10, 200, 120, 25);
		add(pdfLtCodeLabel);
		pdfLtCode = new JTextField("1234567890");
		pdfLtCode.setBounds(140, 200, 320, 25);
		add(pdfLtCode);

		JLabel pdfLtEmailLabel = new JLabel("E. Pastas");
		pdfLtEmailLabel.setBounds(10, 230, 120, 25);
		add(pdfLtEmailLabel);
		pdfLtEmail = new JTextField("test@test.lt");
		pdfLtEmail.setBounds(140, 230, 320, 25);
		add(pdfLtEmail);

		JLabel pdfLtAddressLabel = new JLabel("Adresas");
		pdfLtAddressLabel.setBounds(10, 260, 120, 25);
		add(pdfLtAddressLabel);
		pdfLtAddress = new JTextField("Adresas");
		pdfLtAddress.setBounds(140, 260, 320, 25);
		add(pdfLtAddress);

		JLabel gavejasLabel = new JLabel("Gavejas");
		gavejasLabel.setBounds(640, 140, 320, 25);
		add(gavejasLabel);

		JLabel pdfLtRecipientNameLabel = new JLabel("Vardas");
		pdfLtRecipientNameLabel.setBounds(510, 170, 120, 25);
		add(pdfLtRecipientNameLabel);
		pdfLtRecipientName = new JTextField("Gavejas");
		pdfLtRecipientName.setBounds(640, 170, 320, 25);
		add(pdfLtRecipientName);

		JLabel pdfLtRecipientCodeLabel = new JLabel("Kodas");
		pdfLtRecipientCodeLabel.setBounds(510, 200, 120, 25);
		add(pdfLtRecipientCodeLabel);
		pdfLtRecipientCode = new JTextField("1234567890");
		pdfLtRecipientCode.setBounds(640, 200, 320, 25);
		add(pdfLtRecipientCode);

		JLabel pdfLtRecipientEmailLabel = new JLabel("El. pastas");
		pdfLtRecipientEmailLabel.setBounds(510, 230, 120, 25);
		add(pdfLtRecipientEmailLabel);
		pdfLtRecipientEmail = new JTextField("gavejas@gavejas.lt");
		pdfLtRecipientEmail.setBounds(640, 230, 320, 25);
		add(pdfLtRecipientEmail);

		JLabel pdfLtRecipientAddressLabel = new JLabel("Adresas");
		pdfLtRecipientAddressLabel.setBounds(510, 260, 120, 25);
		add(pdfLtRecipientAddressLabel);
		pdfLtRecipientAddress = new JTextField("Adresas");
		pdfLtRecipientAddress.setBounds(640, 260, 320, 25);
		add(pdfLtRecipientAddress);

		JLabel kitiLabel = new JLabel("Kiti Duomenys");
		kitiLabel.setBounds(140, 290, 320, 25);
		add(kitiLabel);

		JLabel pdfLtTitleLabel = new JLabel("Pavadinimas");
		pdfLtTitleLabel.setBounds(10, 320, 120, 25);
		add(pdfLtTitleLabel);
		pdfLtTitle = new JTextField("pdf");
		pdfLtTitle.setBounds(140, 320, 320, 25);
		add(pdfLtTitle);

		JLabel pdfLtLangLabel = new JLabel("Kalba");
		pdfLtLangLabel.setBounds(10, 350, 120, 25);
		add(pdfLtLangLabel);
		pdfLtLang = new JTextField("LT");
		pdfLtLang.setBounds(140, 350, 320, 25);
		add(pdfLtLang);

		JLabel pdfLtDCIdentifierLabel = new JLabel("Identifikacinis Nr.");
		pdfLtDCIdentifierLabel.setBounds(10, 380, 120, 25);
		add(pdfLtDCIdentifierLabel);
		pdfLtDCIdentifier = new JTextField("123");
		pdfLtDCIdentifier.setBounds(140, 380, 320, 25);
		add(pdfLtDCIdentifier);

		JLabel registravimoLabel = new JLabel("Dokumento registravimo duomenys");
		registravimoLabel.setBounds(640, 290, 320, 25);
		add(registravimoLabel);

		JLabel pdfLtRegistrationLabel = new JLabel("Registracijos nr.");
		pdfLtRegistrationLabel.setBounds(500, 320, 120, 25);
		add(pdfLtRegistrationLabel);
		pdfLtRegistration = new JTextField("123");
		pdfLtRegistration.setBounds(640, 320, 320, 25);
		add(pdfLtRegistration);

		JLabel pdfLtRegistrationCodeLabel = new JLabel("Registracijos kodas");
		pdfLtRegistrationCodeLabel.setBounds(500, 350, 120, 25);
		add(pdfLtRegistrationCodeLabel);
		pdfLtRegistrationCode = new JTextField("123");
		pdfLtRegistrationCode.setBounds(640, 350, 320, 25);
		add(pdfLtRegistrationCode);

		JLabel pdfLtRegistrationDateLabel = new JLabel("Registracijos data");
		pdfLtRegistrationDateLabel.setBounds(500, 380, 120, 25);
		add(pdfLtRegistrationDateLabel);
		pdfLtRegistrationDate = new JTextField("2016-07-13");
		pdfLtRegistrationDate.setBounds(640, 380, 320, 25);
		add(pdfLtRegistrationDate);

		JLabel parasoLabel = new JLabel("Dokumento paraso duomenys");
		parasoLabel.setBounds(140, 410, 320, 25);
		add(parasoLabel);

		JLabel reasonLabel = new JLabel("Paskirtis");
		reasonLabel.setBounds(10, 440, 120, 25);
		add(reasonLabel);
		reason = new JTextField("signature");
		reason.setBounds(140, 440, 320, 25);
		add(reason);

		JLabel notesLabel = new JLabel("Uzrasai");
		notesLabel.setBounds(10, 470, 120, 25);
		add(notesLabel);
		notes = new JTextField("Komentaras");
		notes.setBounds(140, 470, 320, 25);
		add(notes);

		JLabel signerNameLabel = new JLabel("Pilnas Vardas");
		signerNameLabel.setBounds(10, 500, 120, 25);
		add(signerNameLabel);
		pdfLtSignerName = new JTextField("Siuntejas");
		pdfLtSignerName.setBounds(140, 500, 320, 25);
		add(pdfLtSignerName);

		JLabel roleLabel = new JLabel("Role");
		roleLabel.setBounds(10, 530, 120, 25);
		add(roleLabel);
		role = new JTextField("Vadovas");
		role.setBounds(140, 530, 320, 25);
		add(role);

		JLabel contactsLabel = new JLabel("Kontaktai");
		contactsLabel.setBounds(10, 560, 120, 25);
		add(contactsLabel);
		pdfLtContact = new JTextField("");
		pdfLtContact.setBounds(140, 560, 320, 25);
		add(pdfLtContact);

		JLabel locationLabel = new JLabel("Vieta");
		locationLabel.setBounds(10, 590, 120, 25);
		add(locationLabel);
		pdfLtLocation = new JTextField("Vilnius");
		pdfLtLocation.setBounds(140, 590, 320, 25);
		add(pdfLtLocation);

		JButton signButton = new JButton("Pasirasyti");
		signButton.setBounds(10, 620, 120, 25);
		add(signButton);

		signButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (!OperatingSystem.isWindows()) {
					signingService.setPin(JOptionPane.showInputDialog(this, "Iveskite PIN koda"));
				}
				signingService.init(FormView.this);
				signingService.getCertificates();
			}
		});
	}
}
