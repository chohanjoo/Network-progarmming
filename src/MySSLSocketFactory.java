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
   SSLContext sslcontext;
  // Implement abstract methods.   
   @Override
   public String[] getSupportedCipherSuites() { 
      /*String pickedCipher[] = {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"};
       return pickedCipher;  */
      return sslSocketFactory.getSupportedCipherSuites();
   }  
   
   @Override  
   public Socket createSocket(InetAddress inetAddress, int i) throws IOException {  
      return sslSocketFactory.createSocket(inetAddress, i);
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
   
   public SSLSocketFactory get() {
	   
	try {
		sslcontext = SSLContext.getInstance("TLS");
	} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		try {
			sslcontext.init(null,new TrustManager[] { new X509TrustManager() {
				
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
			}
			},null);
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