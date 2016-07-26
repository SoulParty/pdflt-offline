package lt.nortal.pdflt;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;

import org.junit.Test;

import lt.nortal.components.unisign.utils.Base64;
import sun.security.provider.X509Factory;
import sun.security.rsa.RSACore;
import sun.security.rsa.RSAKeyFactory;
import sun.security.rsa.RSAPadding;

/**
 * Created by DK on 7/24/2016.
 */
public class TestSign {
//	OARlEpQe2F1XLZZCOWXsIQERSWh9vWj8eZE3a3ctfe8=

	private byte[] digest = Base64
			.decode
					("MYGVMBgGCSqGSIb3DQEJAzELBgkqhkiG9w0BBwEwHAYJKoZIhvcNAQkFMQ8XDTE2MDcyNDExMzkxMFowIwYJKoZIhvcNAQkEMRYEFC9Df1MdFeHR9kjTkVDlESQkeiW1MDYGCyqGSIb3DQEJEAIvMScwJTAjMCEwCQYFKw4DAhoFAAQUwIuU9VQzAr6/YjRVruxRlJH2kSI=");
	private byte[] digest256 = Base64
			.decode
					("MYGxMBgGCSqGSIb3DQEJAzELBgkqhkiG9w0BBwEwHAYJKoZIhvcNAQkFMQ8XDTE2MDcyNDE0MzczOFowLwYJKoZIhvcNAQkEMSIEIAZPCOA08LEt2Tj25XIt2iViCBdYL2GylkeAfWwwa9W4MEYGCyqGSIb3DQEJEAIvMTcwNTAzMDEwDQYJYIZIAWUDBAIBBQAEILmmfBFsmsZYoiboiO8rlNLtGS5Pr8L8FyWqZ9HbRQUm");
	private byte[] digestDigiDoc = Base64
			.decode("MDEwDQYJYIZIAWUDBAIBBQAEIP7VfqR6seJSb4OuyZmh/5X69IKqWq4qiwL/TwqEviJ5");
	private String encodedCertificate = X509Factory.BEGIN_CERT +
			"\nMIIGKzCCBROgAwIBAgIObV8h37aTlaYAAQAIAnEwDQYJKoZIhvcNAQEFBQAwgZ0xCzAJBgNVBAYT\n" +
			"AkxUMS0wKwYDVQQKEyRWSSBSZWdpc3RydSBDZW50cmFzIC0gSS5rLiAxMjQxMTAyNDYxLjAsBgNV\n" +
			"BAsTJVJlZ2lzdHJ1IENlbnRybyBTZXJ0aWZpa2F2aW1vIENlbnRyYXMxLzAtBgNVBAMTJlZJIFJl\n" +
			"Z2lzdHJ1IENlbnRyYXMgUkNTQyAoSXNzdWluZ0NBLUEpMB4XDTE2MDcyMDA3MTAzN1oXDTE4MDcy\n" +
			"MDA3MTAzN1owgYsxIjAgBgkqhkiG9w0BCQEWE2RraWJhcnRhc0BnbWFpbC5jb20xCzAJBgNVBAYT\n" +
			"AkxUMRswGQYDVQQDExJEQU5JRUxJVVMgS0lCQVJUQVMxETAPBgNVBAQTCEtJQkFSVEFTMRIwEAYD\n" +
			"VQQqEwlEQU5JRUxJVVMxFDASBgNVBAUTCzM5MTA3MjkwMTU1MIIBIjANBgkqhkiG9w0BAQEFAAOC\n" +
			"AQ8AMIIBCgKCAQEAmRB/sOexZti0B1seukRNsXk/BEdHDSZECRGSkowsT6i1Sni3iBmasL8xvhuv\n" +
			"Aa5eX6kvAgLWhDUlrOjzrkVCSnjOGNyNFajHWF5eo6CZl2ZEwlTYkLkm8CxLlXCyodJUHdlc0cx8\n" +
			"hm8c4r0mu3swrdlAE78Z+yXPqPDL2b3ngX4/YjtwlyL0YRciGYFE9L9WJfwyMIxg/AUH79bas3GQ\n" +
			"YA8BgLfc9rJLHJIqt0QB9yKrOFe8CMb0HCMyY+jhl0w4EkHoZ0zey9k7nzYMLYIppLsozRXJjWeD\n" +
			"NPTPoBfGkwlKDcHtYWoxm0FndBN+IP4WP9aC8FXaCXAyzO153QyeuQIDAQABo4ICdzCCAnMwHQYD\n" +
			"VR0OBBYEFDJ0nwmjSisR2Xrs+cxsb+5zlJj8MA4GA1UdDwEB/wQEAwIGwDAeBgNVHREEFzAVgRNk\n" +
			"a2liYXJ0YXNAZ21haWwuY29tMB8GA1UdIwQYMBaAFH5G1fE4Tq4NhqhJScFc3waP6r+TMF0GA1Ud\n" +
			"HwRWMFQwUqBQoE6GTGh0dHA6Ly9jc3AucmNzYy5sdC9jZHAvVkklMjBSZWdpc3RydSUyMENlbnRy\n" +
			"YXMlMjBSQ1NDJTIwKElzc3VpbmdDQS1BKSgxKS5jcmwwgZ4GCCsGAQUFBwEBBIGRMIGOMFgGCCsG\n" +
			"AQUFBzAChkxodHRwOi8vY3NwLnJjc2MubHQvYWlhL1ZJJTIwUmVnaXN0cnUlMjBDZW50cmFzJTIw\n" +
			"UkNTQyUyMChJc3N1aW5nQ0EtQSkoMSkuY3J0MDIGCCsGAQUFBzABhiZodHRwOi8vb2NzcC5yY3Nj\n" +
			"Lmx0L29jc3ByZXNwb25kZXIucmNzYzA9BgkrBgEEAYI3FQcEMDAuBiYrBgEEAYI3FQiCnOx7hKbA\n" +
			"CoTlkQ6Gk5hjh4LRXoFhhLGgD/veUAIBZAIBBDAfBgNVHSUEGDAWBggrBgEFBQcDBAYKKwYBBAGC\n" +
			"NwoDDDBFBgNVHSAEPjA8MDoGCysGAQQBgfE3AQIDMCswKQYIKwYBBQUHAgEWHWh0dHA6Ly93d3cu\n" +
			"cmNzYy5sdC9yZXBvc2l0b3J5MCkGCSsGAQQBgjcVCgQcMBowCgYIKwYBBQUHAwQwDAYKKwYBBAGC\n" +
			"NwoDDDAvBggrBgEFBQcBAwQjMCEwCAYGBACORgEBMAsGBgQAjkYBAwIBCjAIBgYEAI5GAQQwDQYJ\n" +
			"KoZIhvcNAQEFBQADggEBAGsPhOjE+zw/lkM+xhpR+NuscEvRDgVNzuPt4Afd4/9NMAYQOlKPovVY\n" +
			"8PSGp0ZpdoujpVbg0zTGDSM+sWSpSMsjhz165kwFRdpkkgRtS3RLPXVPc9VD2nJ1A+QG6+GW24GK\n" +
			"tt8jAn4XXRBIlSJqnrdMF1wc7wGLLAd4bBbefFdvwnww+EYWgijPhUGbe26O0TGFoKRynq6P/2kB\n" +
			"SMaJUYI4zpD3i5OaSWFOyNgefDNC8iqDF1qAWfe3QebzRPWQCslr74QIAE+aobV449uxwKzEH44k\n" +
			"EIAcJsY021RTxOyX/ugmtH6LkpHvlcrKZ2EQ4Hg27kULe0KJQjUO01jqTmc=\n" +
			X509Factory.END_CERT;
	private String encodedCertificate2 = X509Factory.BEGIN_CERT +
			"\nMIIE+jCCA+KgAwIBAgIQegEAJ98u+kZVllMfN/qmPDANBgkqhkiG9w0BAQsFADBkMQswCQYDVQQG\n" +
			"EwJFRTEiMCAGA1UECgwZQVMgU2VydGlmaXRzZWVyaW1pc2tlc2t1czEXMBUGA1UEAwwORVNURUlE\n" +
			"LVNLIDIwMTExGDAWBgkqhkiG9w0BCQEWCXBraUBzay5lZTAeFw0xNTA3MDMwOTE3MTlaFw0xODA3\n" +
			"MDEyMDU5NTlaMIGsMQswCQYDVQQGEwJFRTEkMCIGA1UECgwbRVNURUlEIChESUdJLUlEIEUtUkVT\n" +
			"SURFTlQpMRcwFQYDVQQLDA5hdXRoZW50aWNhdGlvbjEkMCIGA1UEAwwbR1VCQUlEVUxJTixJR09S\n" +
			"LDM4NDA4MTMwMDEwMRMwEQYDVQQEDApHVUJBSURVTElOMQ0wCwYDVQQqDARJR09SMRQwEgYDVQQF\n" +
			"EwszODQwODEzMDAxMDCCASEwDQYJKoZIhvcNAQEBBQADggEOADCCAQkCggEAmQRlSR0/bBSwT5id\n" +
			"2kz7asSFsQZD4YgqXZW7+YEJXbWghMtAemAPll55vUVViQU8s9vhn5tZX2P0LvsnjVsNfk1nbPK2\n" +
			"UbiIZbBAl5t7MlaNPl4fXMQ1mwro3YaGfT5ngVs7x5c7qSWaT/COs4enleV1JzruRsUfIQPCuYR5\n" +
			"KlZLYKlCSIhaHHQuQSrr0F7vpypwrrqdtA0bt6e7OwemYYDdOQ+epuOpZsKL0btFAHOjdCe1mS/9\n" +
			"r4+uWE6cgeGqMNYI+spwG+qO77/yzsnBEGc8e10Qu3W4vo09J2+gHWigEdSRWoB/ySL5W/ZUJVn3\n" +
			"EObicAM9c9ef/Pad7wfQbwIDAQABo4IBXjCCAVowCQYDVR0TBAIwADAOBgNVHQ8BAf8EBAMCBLAw\n" +
			"UAYDVR0gBEkwRzBFBgorBgEEAc4fAQIEMDcwEgYIKwYBBQUHAgIwBhoEbm9uZTAhBggrBgEFBQcC\n" +
			"ARYVaHR0cDovL3d3dy5zay5lZS9jcHMvMCMGA1UdEQQcMBqBGGlnb3IuZ3ViYWlkdWxpbkBlZXN0\n" +
			"aS5lZTAdBgNVHQ4EFgQUQNof6+pizSXixw4chlE98T9UoR4wIAYDVR0lAQH/BBYwFAYIKwYBBQUH\n" +
			"AwIGCCsGAQUFBwMEMCIGCCsGAQUFBwEDBBYwFDAIBgYEAI5GAQEwCAYGBACORgEEMB8GA1UdIwQY\n" +
			"MBaAFHtq8lVQXLjZegiHQa76ois9W1d2MEAGA1UdHwQ5MDcwNaAzoDGGL2h0dHA6Ly93d3cuc2su\n" +
			"ZWUvcmVwb3NpdG9yeS9jcmxzL2VzdGVpZDIwMTEuY3JsMA0GCSqGSIb3DQEBCwUAA4IBAQCyEQFw\n" +
			"RwHAEcjpClvP6i0yy6e3QTwaH5ySE9AHrztDxFA/r0E54xgvDi2ClVTRNoay9srdqbtWNX95FPlG\n" +
			"fl2pSI/65R0MCLEt1rWnjdqRGVDCAvKXG77psmmdmkf3hShZqotlmCJi2wxhfA3c5fMlje2OpF4f\n" +
			"nPELW6pZSCvAZ6Sfxyw0eAMw3lDqrfeSDVCUtULqxAHH7Qtiu+ItTbBBFsSDC20ZJyMzywlH3iTW\n" +
			"CfWGoSXbc4LkbdEGeab/pc891LJL05chlX6acL+ouykKf9OXhBrvaAPBfvlozjJRGK0dhK0AQ4Ux\n" +
			"ozZ+SogCTBFf+a6u+Ik42Os0nFGT41cm\n" +
			X509Factory.END_CERT;
	private String encodedCertificateDigiDoc = X509Factory.BEGIN_CERT +
			"\nMIIEtjCCA56gAwIBAgIQOrBqYMrznTJVllMp7DKXgzANBgkqhkiG9w0BAQsFADBkMQswCQYDVQQGEwJFRTEiMCAGA1UECgwZQVMgU2VydGlmaXRzZWVyaW1pc2tlc2t1czEXMBUGA1UEAwwORVNURUlELVNLIDIwMTExGDAWBgkqhkiG9w0BCQEWCXBraUBzay5lZTAeFw0xNTA3MDMwOTE3MjlaFw0xODA3MDEyMDU5NTlaMIGvMQswCQYDVQQGEwJFRTEkMCIGA1UECgwbRVNURUlEIChESUdJLUlEIEUtUkVTSURFTlQpMRowGAYDVQQLDBFkaWdpdGFsIHNpZ25hdHVyZTEkMCIGA1UEAwwbR1VCQUlEVUxJTixJR09SLDM4NDA4MTMwMDEwMRMwEQYDVQQEDApHVUJBSURVTElOMQ0wCwYDVQQqDARJR09SMRQwEgYDVQQFEwszODQwODEzMDAxMDCCASEwDQYJKoZIhvcNAQEBBQADggEOADCCAQkCggEAlPbkXzSS6XzUvthHUxV5fNuQZCdHnw/BvIqqkb44wQ9LupoAJZimQjh59aLHADXdRuoKlT45nKNt2q+KgnEs+0P4e8HU50np7SMMjWmpXzxHZGb7myvIYJRe2pH7U1OjJDWPNdB6qomM1rUHDnVN74wfWLKnsMxwM5w1K2RQyqKqqXbLuaCKnx2C4W/Rn/fzls3SJt0pBCyoHuEgV+ya+vG22kK5tq7Kixk3rXyyH1Fq1Tz0xvI2ESQn++KByq3dVNT2p0Dy0gSZnG+hI+B6+W3COJgxtVRZc4ev9ioxx6xYqSrkDXpFAIa4asCdXCOpi6yUUd3/hhXzvgO5O35pLwIDAQABo4IBFzCCARMwCQYDVR0TBAIwADAOBgNVHQ8BAf8EBAMCBkAwUAYDVR0gBEkwRzBFBgorBgEEAc4fAQIEMDcwEgYIKwYBBQUHAgIwBhoEbm9uZTAhBggrBgEFBQcCARYVaHR0cDovL3d3dy5zay5lZS9jcHMvMB0GA1UdDgQWBBRlARNlRR/36sn5PwJyugXyF9IBsTAiBggrBgEFBQcBAwQWMBQwCAYGBACORgEBMAgGBgQAjkYBBDAfBgNVHSMEGDAWgBR7avJVUFy42XoIh0Gu+qIrPVtXdjBABgNVHR8EOTA3MDWgM6Axhi9odHRwOi8vd3d3LnNrLmVlL3JlcG9zaXRvcnkvY3Jscy9lc3RlaWQyMDExLmNybDANBgkqhkiG9w0BAQsFAAOCAQEAfF0fxMiTuXonKrjbk19E+pV69KhQvcw+WPcD8QBf3TtHgQuO4Vl6iy2UTJddvRWMxHjmaCOLrrgR7ql2nvdYhJVcopssULbN5PtIAlh5DYpV2DFuCUnDcMwChocO/HKN460guIFZfyJztGciuR+Hs1Zfob+jMgEMtAkLV3qfh916UrUT1pIsPoXjZvLz+Rzg0OoJHbEj6C9ciI+VujS5wsldzczcFri3ULnDVG1ewjvzsNkxKQjVZpcCs6YDss8bdl8GP4OJ8h5E8Ow1+cobDGG5I2savo5JkbdxAt2621rV1XtjeOZsB8lktcmXc3bEaJx4qW8VZqByOtufiimqng==\n"
			+
			X509Factory.END_CERT;
	private byte[] signatureBytes = Base64
			.decode("aLZ08Rj/mQ/0l+C2eE/fKaj3+T4Tdv0yBmJFzSZqg4oiGY3OAUWU4j4+b6pdTgAAOmvSDHskqqG7dy8Dq4x/WAO7/oaiMM6gbqXiMo+T0Me6hYm81cVJwvIcRhzAz8"
					+ "/F5aisJgWfch+BRt1EPVA7CVwWD6BcswrHuxTRSuc+63bYMe4wgQng4jF5zb5xJaV1WVD6bsTaqNMpvW9jUulfBBJyMfUtuL57q05EJPO"
					+ "/zQMj9GQeFGclDoSvlzVuAdB1v+cUgG7WjRSn99Ek7MXFF+5kMJOmKRI3+NR1W4CJqi1TJfaNXEEkEJBmpsJUhuOq6w0AFpla7XI5+KBC45FU2w==");
	private byte[] signatureBytesSHA1 = Base64
			.decode("ipD1pyRgFbXlP5vNfpoTBdyJ+rIWYflg+YWk35rMUm+QX9Nk8NaSBwUx7iL/l+LNd0I6/TYLqoSb+jj0kkyM"
					+ "/bXDqauvUtpWFcndOHvXwN8onaPZDNuUc2kiAchlnyyLs57DgWLRWIDKqN3LGXR6RxXrwAZsbdMiXLE1cF8y0A+6JAVq+S933yMVrUySOZ5pd2zdnlc+CH"
					+ "+zsDEQDJRQjQ4m3ZXn84jRNx+Tjx0RmMBIh9o1QElDaYR3iUkYNhDsmvjcS4Ge4AtYrjr8lWxwPjEHvH706/lxQgQwLb"
					+ "+BqFQnjkL2PVTLHkenoB234VdtUEddVId7OJ1hGiHt1nnPlg==");
	private byte[] signatureBytesSHA256 = Base64
			.decode
					("MYGxMBgGCSqGSIb3DQEJAzELBgkqhkiG9w0BBwEwHAYJKoZIhvcNAQkFMQ8XDTE2MDcyNDE0MjMxNVowLwYJKoZIhvcNAQkEMSIEIGtu95ppP9bWfF9kTJ4Pm3pqme7xxkYGYIXsTRYRZauhMEYGCyqGSIb3DQEJEAIvMTcwNTAzMDEwDQYJYIZIAWUDBAIBBQAEILmmfBFsmsZYoiboiO8rlNLtGS5Pr8L8FyWqZ9HbRQUm");
	private byte[] signatureBytesDigiDoc = Base64.decode("HlBPRDutFYPn+S+CUVi9LzpHyXXUr4+tB5OGQ6YZH3nleBnA9CCpb6+rKEAlwBNRx+b9stZ1r8vZk1KZp772/1sxy2ZX6QNZs7N/B+Y/0z3u5Dp0SeGP3DxtkWScrnP9EIFPZwc8iiR80iAy/6nLv/WAP00XIgN5vW9LzMfIFh54OPQHe1TcNn23WvYk93QwkEu4nQiTnwzMlv/7seEa05dCMxg5Z3gJx0RqmKIYjoM2Di0ncnEoWsPcCrxkT0JWbzYrEFn4Bi546MeOcsHEHHOPlR1+Rra59k8lJk98TL+70M6tnb0l58bxIKOpV30/8/9ksc+PTR8thEYASn4Vpg==");

	@Test
	public void testSign() throws GeneralSecurityException, UnsupportedEncodingException {

		InputStream stream = new ByteArrayInputStream(encodedCertificate.getBytes("UTF-8"));
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(stream);

		PublicKey publicKey = x509Certificate.getPublicKey();
		Signature signature = Signature.getInstance("SHA1with" + publicKey.getAlgorithm());
		signature.initVerify(publicKey);
		signature.update(digest);

		RSAPublicKey rsaPublicKey = (RSAPublicKey) RSAKeyFactory.toRSAKey(publicKey);
		byte[] rsa = RSACore.rsa(signatureBytes, rsaPublicKey);
		int var3 = RSACore.getByteLength(rsaPublicKey);
		RSAPadding padding;
		try {
			padding = RSAPadding.getInstance(1, var3, null);
		} catch (InvalidAlgorithmParameterException var5) {
			throw new InvalidKeyException(var5.getMessage());
		}

		int var4 = padding.getMaxDataSize();
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		if (8 + 7 + md.getDigestLength() > var4) {
			throw new InvalidKeyException("Key is too short for this signature algorithm");
		}
		byte[] unpaddedBytes = padding.unpad(rsa);

		if (!signature.verify(signatureBytes)) {
			throw new GeneralSecurityException("Failed to verify user signature using public key stored in the certificate.");
		} else {
			System.out.println("Success");
		}
	}

	@Test
	public void testSign2() throws GeneralSecurityException, UnsupportedEncodingException {

		InputStream stream = new ByteArrayInputStream(encodedCertificateDigiDoc.getBytes("UTF-8"));
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(stream);

		PublicKey publicKey = x509Certificate.getPublicKey();
		Signature signature = Signature.getInstance("SHA256with" + publicKey.getAlgorithm());
		signature.initVerify(publicKey);
		signature.update(digestDigiDoc);

		RSAPublicKey rsaPublicKey = (RSAPublicKey) RSAKeyFactory.toRSAKey(publicKey);
		byte[] rsa = RSACore.rsa(signatureBytesDigiDoc, rsaPublicKey);
		int var3 = RSACore.getByteLength(rsaPublicKey);
		RSAPadding padding;
		try {
			padding = RSAPadding.getInstance(1, var3, null);
		} catch (InvalidAlgorithmParameterException var5) {
			throw new InvalidKeyException(var5.getMessage());
		}

		int var4 = padding.getMaxDataSize();
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		if (8 + 7 + md.getDigestLength() > var4) {
			throw new InvalidKeyException("Key is too short for this signature algorithm");
		}
		byte[] unpaddedBytes = padding.unpad(rsa);

		if (!signature.verify(signatureBytesDigiDoc)) {
			throw new GeneralSecurityException("Failed to verify user signature using public key stored in the certificate.");
		} else {
			System.out.println("Success");
		}
	}

	@Test
	public void testVerify() throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
//		md.update(Base64.decode("MDEwDQYJYIZIAWUDBAIBBQAEIMAkIMIc2M7ZWEMg50SwZcny3r1Sm57uO9QhGzTXKlz/gAAAAAAAAAAAAAABmA=="));
		md.update(Base64.decode("MDEwDQYJYIZIAWUDBAIBBQAEIP7VfqR6seJSb4OuyZmh/5X69IKqWq4qiwL/TwqEviJ5"));
		byte[] digest = md.digest();
		System.out.println(Base64.encode(digest));

		InputStream stream = new ByteArrayInputStream(encodedCertificate2.getBytes("UTF-8"));
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(stream);

		PublicKey publicKey = x509Certificate.getPublicKey();
		Signature signature = Signature.getInstance("SHA256" + "with" + publicKey.getAlgorithm());
		signature.initVerify(publicKey);
		signature.update(digestDigiDoc);
		signature.verify(signatureBytesDigiDoc);
	}
}
