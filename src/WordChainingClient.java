import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class WordChainingClient {

	static String eServer = "";
	static int ePort = 0000;
	// static int clientID = -1;
	static Socket chatSocket = null;
	static SSLSocketFactory sslSocketFactory = null;
	static SSLSocket sslSocket = null;
	static InetAddress inetaddr = null;

	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.println("Usage: Classname ServerName ServerPort");
			System.exit(1);
		}

		eServer = args[0];
		ePort = Integer.parseInt(args[1]);

		try {
			inetaddr = InetAddress.getLocalHost();
			sslSocketFactory = (SSLSocketFactory) MySSLSocketFactory.getDefault();
			sslSocket = (SSLSocket) sslSocketFactory.createSocket(inetaddr, ePort);

			String[] supported = sslSocket.getSupportedCipherSuites();
			sslSocket.setEnabledCipherSuites(supported);

			sslSocket.startHandshake();
		} catch (BindException b) {
			System.out.println("Can't bind on: " + ePort);
			System.exit(1);
		} catch (IOException i) {
			System.out.println(i);
			System.exit(1);
		}
		new Thread(new ClientReceiver(sslSocket)).start();
		new Thread(new ClientSender(sslSocket)).start();
		System.out.println("모든 플레이어가 \"READY\"를 입력해야 게임이 진행됩니다.");
	}
}

class ClientSender implements Runnable {
	private SSLSocket chatSocket = null;

	ClientSender(SSLSocket socket) {
		this.chatSocket = socket;
	}

	public void run() {
		Scanner KeyIn = null;
		PrintWriter out = null;
		try {
			KeyIn = new Scanner(System.in);
			out = new PrintWriter(chatSocket.getOutputStream(), true);

			String userInput = "";
			System.out.print(chatSocket.getLocalPort() + "님이 게임에 입장하셨습니다. (Type Message \"Bye.\" to leave)\n");
			while ((userInput = KeyIn.nextLine()) != null) {
				Dictionary dic = (Dictionary) Naming.lookup("rmi://" + "localhost" + "/" + "WordChaining");
				if (dic.checkWord(userInput)) {
					out.println(userInput);
					out.flush();
				} else if (userInput.equals("READY")) {
					out.println(userInput);
					out.flush();
				} else {
					System.out.println("올바른 단어가 아닙니다.");
				}
				if (userInput.equalsIgnoreCase("Bye."))
					break;
			}
			KeyIn.close();
			out.close();
			chatSocket.close();
		} catch (IOException e) {
			try {
				if (out != null)
					out.close();
				if (KeyIn != null)
					KeyIn.close();
				if (chatSocket != null)
					chatSocket.close();
			} catch (IOException ie) {
			}
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(1);
	}
}

class ClientReceiver implements Runnable {
	private SSLSocket chatSocket = null;

	ClientReceiver(SSLSocket socket) {
		this.chatSocket = socket;
	}

	public void run() {
		while (chatSocket.isConnected()) {
			BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
				String readSome = null;
				while ((readSome = in.readLine()) != null) {
					System.out.println(readSome);
				}
				in.close();
				chatSocket.close();
			} catch (IOException e) {
				try {
					if (in != null)
						in.close();
					if (chatSocket != null)
						chatSocket.close();
				} catch (IOException ie) {
				}
				System.out.println("게임을 종료합니다.");
				System.exit(1);
			}
		}
	}
}