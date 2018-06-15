import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyStore;
import java.util.ArrayList;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

public class Server {

	public static void main(String[] args) {

		ArrayList<Thread> threads = new ArrayList<Thread>();
	    
		int count=0;
		String uName = "";
		final KeyStore ks;
		final KeyManagerFactory kmf;
		final SSLContext sc;

		final String runRoot = "C://Users//한주//eclipse-workspace//netword_project//bin//"; // root change : your system
																							// root

		SSLServerSocketFactory ssf = null;
		SSLServerSocket s = null;
		SSLSocket c = null;

		BufferedWriter w = null;
		BufferedReader r = null;

		if (args.length != 1) {
			System.out.println("Usage: Classname Port");
			System.exit(1);
		}

		int sPort = Integer.parseInt(args[0]);

		String ksName = runRoot + ".keystore/ServerKey";

		char keyStorePass[] = "wh1598".toCharArray();
		char keyPass[] = "wh1598".toCharArray();
		try {
			ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream(ksName), keyStorePass);

			kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, keyPass);

			sc = SSLContext.getInstance("TLS");
			sc.init(kmf.getKeyManagers(), null, null);

			
			/* SSLServerSocket */
			ssf = sc.getServerSocketFactory();
			s = (SSLServerSocket) ssf.createServerSocket(sPort);
			printServerSocketInfo(s);

			//c = (SSLSocket) s.accept();
			//printSocketInfo(c);
			
			while(true) {
				SSLSocket socket = (SSLSocket) s.accept();
				
				Thread thread = new Thread(new ClientHandler(socket));
				threads.add(thread);
				thread.setDaemon(true);
				thread.start();
				//String uName = String.format("%d", ++count);
				System.out.println(threads.size());
				
			}

		} catch (SSLException se) {
			System.out.println("SSL problem, exit~");
			try {
				w.close();
				r.close();
				s.close();
				c.close();
			} catch (IOException i) {
			}
		} catch (Exception e) {
			System.out.println("What?? exit~");
			try {
				w.close();
				r.close();
				s.close();
				c.close();
			} catch (IOException i) {
			}
		}
	}

	private static void printSocketInfo(SSLSocket s) {
		System.out.println("Socket class: " + s.getClass());
		System.out.println("   Remote address = " + s.getInetAddress().toString());
		System.out.println("   Remote port = " + s.getPort());
		System.out.println("   Local socket address = " + s.getLocalSocketAddress().toString());
		System.out.println("   Local address = " + s.getLocalAddress().toString());
		System.out.println("   Local port = " + s.getLocalPort());
		System.out.println("   Need client authentication = " + s.getNeedClientAuth());
		SSLSession ss = s.getSession();
		System.out.println("   Cipher suite = " + ss.getCipherSuite());
		System.out.println("   Protocol = " + ss.getProtocol());
	}

	private static void printServerSocketInfo(SSLServerSocket s) {
		System.out.println("Server socket class: " + s.getClass());
		System.out.println("   Server address = " + s.getInetAddress().toString());
		System.out.println("   Server port = " + s.getLocalPort());
		System.out.println("   Need client authentication = " + s.getNeedClientAuth());
		System.out.println("   Want client authentication = " + s.getWantClientAuth());
		System.out.println("   Use client mode = " + s.getUseClientMode());
	}
}

class ClientHandler implements Runnable{

	SSLSocket socket;
	String uName;

	public ClientHandler(SSLSocket socket) {
		// TODO Auto-generated constructor stub
		this.socket = socket;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
		 BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
/*
		String m = "SSLSocket based reverse echo,Type some words. exit'.'";
		w.write(m, 0, m.length());	
		w.newLine();
		w.flush();
*/
		String m="";
		while ((m = r.readLine()) != null) {
			
			if(m.contains("userName:")) {
				String[] word = m.split(":");
				uName = new String(word[1]);
				System.out.println(uName + "님이 접속하셨습니다.");
				
			}
			
			if(m.contains("break")) {
				
				//w.write(uB);
			}
			if (m.equals("."))
				break;
			
			m = uName + "> " + m;
			char[] a = m.toCharArray();
			int n = a.length;
			
			
			w.write(a, 0, n);
			w.newLine();
			w.flush();
			
			
		}
		w.close();
		r.close();
		}catch(IOException ie) {
			
		}
		
	}
	

}