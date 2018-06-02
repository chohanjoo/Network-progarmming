import java.net.*;
import java.rmi.Naming;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import java.io.*;

public class WordChainingServer implements Runnable {
	final static int PLAYERNUM = 2;
	private WordChaingServerRunnable clients[] = new WordChaingServerRunnable[PLAYERNUM];
	static String wordList = "";
	static ArrayList<String> words = new ArrayList<String>();
	public int clientCount = 0;

	private int ePort = -1;
	final String runRoot = "C://Users//����//eclipse-workspace//netword_project//bin//";
	SSLServerSocketFactory sslServerSocketFactory = null;
	SSLServerSocket sslServerSocket = null;
	SSLSocket sslSocket = null;
	private KeyStore keyStore;
	private KeyManagerFactory keyManagerFactory;
	private SSLContext sslContext;

	String ksName = runRoot + ".keystore/WordChainingServerKey";

	char keyStorePass[] = "000000".toCharArray();
	char keyPass[] = "000000".toCharArray();

	public WordChainingServer(int port) {
		this.ePort = port;

	}

	public void run() {

		try {
			keyStore = KeyStore.getInstance("JKS");
			keyStore.load(new FileInputStream(ksName), keyStorePass);

			keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			keyManagerFactory.init(keyStore, keyPass);

			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
			/* SSLServerSocket */
			sslServerSocketFactory = sslContext.getServerSocketFactory();
			sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(ePort);

			// serverSocket = new ServerSocket(ePort);
			System.out.println("Server started: socket created on " + ePort);

			Dictionary dic = new DictionaryImpl();
			Naming.rebind("rmi://" + "localhost" + ":1099/" + "WordChaining", dic);

			while (true) {
				addClient(sslServerSocket);
			}

		} catch (BindException b) {
			System.out.println("Can't bind on: " + ePort);
		} catch (IOException i) {
			System.out.println(i);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				if (sslServerSocket != null)
					sslServerSocket.close();
			} catch (IOException i) {
				System.out.println(i);
			}
		}
	}

	public int whoClient(int clientID) {
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getClientID() == clientID)
				return i;
		return -1;
	}

	public void putClient(String msg) {
		for (int i = 0; i < clientCount; i++) {
			clients[i].out.println(msg);
		}
	}

	public void putClient(int clientID, String msg) {
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getClientID() == clientID)
				clients[i].out.println(msg);

	}

	public void putClient(int clientID, String inputLine, String wordList) {
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getClientID() == clientID) {
				System.out.println("�ۼ���: " + clientID);
			} else {
				System.out.println("�޽��� �޴� ���: " + clients[i].getClientID());
				if (this.getPlayerState()) {
					clients[i].out.println(clientID + "���� �Է��� �ܾ� : " + inputLine);
					clients[i].out.println("��������� �ܾ� : " + wordList);
				}
			}
	}

	public void addClient(SSLServerSocket sslServerSocket) {
		SSLSocket clientSocket = null;

		if (clientCount < clients.length) {
			try {
				clientSocket = (SSLSocket) sslServerSocket.accept();
				clientSocket.setSoTimeout(40000); // 1000/sec
			} catch (IOException i) {
				System.out.println("Accept() fail: " + i);
			}
			clients[clientCount] = new WordChaingServerRunnable(this, clientSocket);
			new Thread(clients[clientCount]).start();
			clientCount++;
			System.out.println("Client connected: " + clientSocket.getPort() + ", CurrentClient: " + clientCount);
		} else {
			try {
				SSLSocket dummySocket = (SSLSocket) sslServerSocket.accept();
				WordChaingServerRunnable dummyRunnable = new WordChaingServerRunnable(this, dummySocket);
				new Thread(dummyRunnable);
				dummyRunnable.out.println(dummySocket.getPort() + " < Sorry maximum user connected now");
				System.out.println("Client refused: maximum connection " + clients.length + " reached.");
				dummyRunnable.close();
			} catch (IOException i) {
				System.out.println(i);
			}
		}
	}

	/**
	 * ��� �÷��̾ READY�������� �Ǵ��ϴ� �Լ�
	 * 
	 * @return true: ��� �÷��̾� READY ���� ��� / false: �Ѹ��̶� READY �ȴ��� ���
	 */
	public boolean getPlayerState() {
		int readyCount = 0;

		for (int i = 0; i < clientCount; i++) {
			if (clients[i].isReady == true)
				readyCount++;
		}

		if (readyCount == PLAYERNUM)
			return true;
		else
			return false;
	}

	public synchronized void delClient(int clientID) {
		int pos = whoClient(clientID);
		WordChaingServerRunnable endClient = null;
		if (pos >= 0) {
			endClient = clients[pos];
			if (pos < clientCount - 1)
				for (int i = pos + 1; i < clientCount; i++)
					clients[i - 1] = clients[i];
			clientCount--;
			System.out
					.println("Client removed: " + clientID + " at clients[" + pos + "], CurrentClient: " + clientCount);
			endClient.close();
		}
	}

	public static void main(String[] args) throws IOException {

		if (args.length != 1) {
			System.out.println("Usage: Classname ServerPort");
			System.exit(1);
		}
		int sPort = Integer.parseInt(args[0]);

		new Thread(new WordChainingServer(sPort)).start();

	}

	public void changeTurn(int clientID) {
		int index = -1;
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getClientID() == clientID) {
				index = i;
				clients[i].turn = false;
			} else if (index + 1 == clientCount) {
				clients[0].turn = true;
			} else if (index + 1 < clientCount) {
				clients[index + 1].turn = true;
			}
	}
}

class WordChaingServerRunnable implements Runnable {
	protected WordChainingServer chatServer = null;
	protected SSLSocket clientSocket = null;
	protected PrintWriter out = null;
	protected BufferedReader in = null;
	public int clientID = -1;
	public boolean isReady = false;
	public boolean isGameStarted = false;
	public boolean turn = true;

	public WordChaingServerRunnable(WordChainingServer server, SSLSocket socket) {
		this.chatServer = server;
		this.clientSocket = socket;
		clientID = clientSocket.getPort();
		try {
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {
		}
	}

	public void run() {
		try {
			String inputLine;
			final String readyMsg = "���� READY�� �������ϴ�.";
			final String unReadyMsg = "���� UNREADY�� �������ϴ�.";
			final String startMsg = "�����ձ� ������ �����մϴ�.";
			final String terminateMsg = "���� ������ �����߽��ϴ�.";
			String lastWord = "";

			while ((inputLine = in.readLine()) != null) {
				if (inputLine.equalsIgnoreCase("Bye.")) {
					chatServer.putClient(getClientID() + terminateMsg);
					chatServer.delClient(getClientID());
					break;
				}

				if (inputLine.equalsIgnoreCase("READY")) {
					this.isReady = !this.isReady;
					if (this.isReady == true)
						chatServer.putClient(getClientID() + readyMsg);
					else
						chatServer.putClient(getClientID() + unReadyMsg);
				}

				if (chatServer.getPlayerState()) {
					/**
					 * ������ PLAYER�� "READY"�� �Է��� ��� wordList�� "READY"���ڰ� ���� ���� ���� ���� continue���
					 * (...����ǥ�������� �� �� ����ϰ� �ٲ� �� ���� ������...)
					 */
					if (inputLine.equalsIgnoreCase("READY")) {
						isGameStarted = true;
						chatServer.putClient(startMsg);
						continue;
					}

					if (this.turn) { // PLAYER ���� ���� �ܾ� �Է��� �ǵ���
						if (!WordChainingServer.words.isEmpty() && chackWord(inputLine)) { // �Էµ� �ܾ ù �ܾ �ƴ� ��
							lastWord = WordChainingServer.words.get(WordChainingServer.words.size() - 1);
							if (lastWord.charAt(lastWord.length() - 1) == inputLine.charAt(0)) {
								WordChainingServer.words.add(inputLine);
								WordChainingServer.wordList += inputLine + "->";
								chatServer.putClient(getClientID(), inputLine, WordChainingServer.wordList);
								chatServer.changeTurn(getClientID());
							} else
								chatServer.putClient(getClientID(), "�߸��� �ܾ �Է��ϼ̽��ϴ�.\n�ٽ��Է����ּ���.");

						} else { // ù �ܾ �Էµ� ��
							if (chackWord(inputLine)) {
								WordChainingServer.words.add(inputLine);
								WordChainingServer.wordList += inputLine + "->";
								chatServer.putClient(getClientID(), inputLine, WordChainingServer.wordList);
								chatServer.changeTurn(getClientID());
							} else
								chatServer.putClient(getClientID(), "�߸��� �ܾ �Է��ϼ̽��ϴ�.\n�ٽ��Է����ּ���.");
						}

					}
				}
			}
		} catch (SocketTimeoutException ste) {
			System.out.println("Socket timeout occurred, force close() : " + getClientID());
			chatServer.delClient(getClientID());
		} catch (IOException e) {
			chatServer.delClient(getClientID());
		}
	}

	public int getClientID() {
		return clientID;
	}

	public void close() {
		try {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (clientSocket != null)
				clientSocket.close();
		} catch (IOException i) {
		}
	}

	public boolean chackWord(String word) {
		if (word.contains(" "))
			return false;

		try {
			if (!word.matches("^[��-�� ��-�� ��-�R]*$"))
				return false;
		} catch (PatternSyntaxException e) {
			System.err.println("An Exception Occured");
			e.printStackTrace();
		}
		return true;
	}
}