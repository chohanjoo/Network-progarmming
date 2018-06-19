import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class MySSLSocketFactory extends SSLSocketFactory {

	SSLSocketFactory sslSocketFactory;

	// Implement abstract methods.
	@Override
	public String[] getSupportedCipherSuites() {

		return sslSocketFactory.getSupportedCipherSuites();
	}

	@Override
	public Socket createSocket(InetAddress inetAddress, int port) throws IOException {
		sslSocketFactory = myGetSSLSocketFactory();
		SSLSocket sslsocket = (SSLSocket) sslSocketFactory.createSocket(inetAddress, port);
		String pickedCipher[] = { "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384" };
		sslsocket.setEnabledCipherSuites(pickedCipher);
		return sslsocket;
	}

	@Override
	public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		// TODO Auto-generated method stub
		return sslSocketFactory.createSocket(host, port);
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
			throws IOException, UnknownHostException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public static SSLSocketFactory myGetSSLSocketFactory() {
		SSLContext sslcontext = null;
		try {
			sslcontext = SSLContext.getInstance("TLS");
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					// TODO Auto-generated method stub

				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					// TODO Auto-generated method stub

				}
			} };
			sslcontext.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sslcontext.getSocketFactory();
	}

	public static SocketFactory getDefault() {
		try {
			return SSLContext.getDefault().getSocketFactory();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}
}