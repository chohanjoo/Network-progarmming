import java.net.*;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;
import java.io.*; 

public class WordChainingServer implements Runnable {
	final static int PLAYERNUM = 2;
	private WordChaingServerRunnable clients[] = new WordChaingServerRunnable[PLAYERNUM];
	static String wordList = "";
	static ArrayList<String> words = new ArrayList<String>();
	public int clientCount = 0;

	private int ePort = -1;

	public WordChainingServer(int port) {
		this.ePort = port;
	}

	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(ePort);
			System.out.println ("Server started: socket created on " + ePort);
			
			while (true) {
				addClient(serverSocket);
			}
			
		} catch (BindException b) {
			System.out.println("Can't bind on: "+ePort);
		} catch (IOException i) {
			System.out.println(i);
		} finally {
			try {
				if (serverSocket != null) serverSocket.close();
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
	
	public void putClient(int clientID,String msg) {
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getClientID() == clientID) 
				clients[i].out.println(msg);
			
	}
	
	public void putClient(int clientID, String inputLine, String wordList) {
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getClientID() == clientID) {
				System.out.println("ÀÛ¼ºÀÚ: "+clientID);
			} else {
				System.out.println("¸Þ½ÃÁö ¹Þ´Â »ç¶÷: "+clients[i].getClientID());
				if (this.getPlayerState()) {
					clients[i].out.println(clientID + "´ÔÀÌ ÀÔ·ÂÇÑ ´Ü¾î : " + inputLine);
					clients[i].out.println("ÇöÀç±îÁöÀÇ ´Ü¾î : " + wordList);
				}
			}
	}
	
	public void addClient(ServerSocket serverSocket) {
		Socket clientSocket = null;
		
		if (clientCount < clients.length) { 
			try {
				clientSocket = serverSocket.accept();
				clientSocket.setSoTimeout(40000); // 1000/sec
			} catch (IOException i) {
				System.out.println ("Accept() fail: "+i);
			}
			clients[clientCount] = new WordChaingServerRunnable(this, clientSocket);
			new Thread(clients[clientCount]).start();
			clientCount++;
			System.out.println ("Client connected: " + clientSocket.getPort()
					+", CurrentClient: " + clientCount);
		} else {
			try {
				Socket dummySocket = serverSocket.accept();
				WordChaingServerRunnable dummyRunnable = new WordChaingServerRunnable(this, dummySocket);
				new Thread(dummyRunnable);
				dummyRunnable.out.println(dummySocket.getPort()
						+ " < Sorry maximum user connected now");
				System.out.println("Client refused: maximum connection "
						+ clients.length + " reached.");
				dummyRunnable.close();
			} catch (IOException i) {
				System.out.println(i);
			}	
		}
	}
	
	/**
	 * ¸ðµç ÇÃ·¹ÀÌ¾î°¡ READY»óÅÂÀÎÁö ÆÇ´ÜÇÏ´Â ÇÔ¼ö
	 * @return true: ¸ðµç ÇÃ·¹ÀÌ¾î READY ´©¸¥ °æ¿ì / false: ÇÑ¸íÀÌ¶óµµ READY ¾È´©¸¥ °æ¿ì
	 */
	public boolean getPlayerState() {
		int readyCount = 0;
		
		for (int i = 0; i < clientCount; i++) {
			if (clients[i].isReady == true) 
				readyCount++;
		}
		
		if (readyCount == PLAYERNUM) return true;
		else return false;
	}
	
	public synchronized void delClient(int clientID) {
		int pos = whoClient(clientID);
		WordChaingServerRunnable endClient = null;
	      if (pos >= 0) {
	    	   endClient = clients[pos];
	    	  if (pos < clientCount-1)
	    		  for (int i = pos+1; i < clientCount; i++)
	    			  clients[i-1] = clients[i];
	    	  clientCount--;
	    	  System.out.println("Client removed: " + clientID
	    			  + " at clients[" + pos +"], CurrentClient: " + clientCount);
	    	  endClient.close();
	      }
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Usage: Classname ServerPort");
			System.exit(1);
		}
		int ePort = Integer.parseInt(args[0]);
		
		new Thread(new WordChainingServer(ePort)).start();
	}
	
	public void changeTurn(int clientID) {
		int index = -1;
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getClientID() == clientID) {
				index = i;
				clients[i].turn = false;
			} else if (index+1 == clientCount){
				clients[0].turn = true;
			} else if (index + 1 <clientCount) {
				clients[index+1].turn = true;
			}
	}
}

class WordChaingServerRunnable implements Runnable {
	protected WordChainingServer chatServer = null;
	protected Socket clientSocket = null;
	protected PrintWriter out = null;
	protected BufferedReader in = null;
	public int clientID = -1;
	public boolean isReady = false;
	public boolean isGameStarted = false;
	public boolean turn = true;
	
	public WordChaingServerRunnable (WordChainingServer server, Socket socket) {
		this.chatServer = server;
		this.clientSocket = socket;
		clientID = clientSocket.getPort();
		try {
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {}
	}
	
	public void run() {
		try {
			String inputLine;
			final String readyMsg = "´ÔÀÌ READY¸¦ ´­·¶½À´Ï´Ù.";
			final String unReadyMsg = "´ÔÀÌ UNREADY¸¦ ´­·¶½À´Ï´Ù.";
			final String startMsg = "³¡¸»ÀÕ±â °ÔÀÓÀ» ½ÃÀÛÇÕ´Ï´Ù.";
			final String terminateMsg = "´ÔÀÌ °ÔÀÓÀ» Á¾·áÇß½À´Ï´Ù.";
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
					 * ¸¶Áö¸· PLAYER°¡ "READY"¸¦ ÀÔ·ÂÇÑ °æ¿ì wordList¿¡ "READY"¹®ÀÚ°¡ µé¾î°¡´Â °ÍÀ» ¸·±â À§ÇØ continue»ç¿ë
					 * (...Á¤±ÔÇ¥Çö½ÄÀ¸·Î Á» ´õ ±ò²ûÇÏ°Ô ¹Ù²Ü ¼ö ÀÖÁö ¾ÊÀ»±î...)
					 */
					if (inputLine.equalsIgnoreCase("READY")) {
						isGameStarted = true;
						chatServer.putClient(startMsg);
					//	turn = true;
						continue;
					}
						
					if(this.turn) {
					if(!WordChainingServer.words.isEmpty() && chackWord(inputLine)) {
						lastWord = WordChainingServer.words.get(WordChainingServer.words.size()-1);
						if(lastWord.charAt(lastWord.length()-1) == inputLine.charAt(0)) {
							WordChainingServer.words.add(inputLine);
							WordChainingServer.wordList += inputLine + "->";
							chatServer.putClient(getClientID(), inputLine, WordChainingServer.wordList);
							chatServer.changeTurn(getClientID());
						}
						else
							chatServer.putClient(getClientID(), "Àß¸øµÈ ´Ü¾î¸¦ ÀÔ·ÂÇÏ¼Ì½À´Ï´Ù.\n´Ù½ÃÀÔ·ÂÇØÁÖ¼¼¿ä.");
						
						
					}
					else {
						if(chackWord(inputLine)) {
						WordChainingServer.words.add(inputLine);
						WordChainingServer.wordList += inputLine + "->";
						chatServer.putClient(getClientID(), inputLine, WordChainingServer.wordList);
						chatServer.changeTurn(getClientID());
						}
						else
							chatServer.putClient(getClientID(), "Àß¸øµÈ ´Ü¾î¸¦ ÀÔ·ÂÇÏ¼Ì½À´Ï´Ù.\n´Ù½ÃÀÔ·ÂÇØÁÖ¼¼¿ä.");
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
			if (in != null) in.close();
			if (out != null) out.close();
			if (clientSocket != null) clientSocket.close();
		} catch (IOException i) {}
	}
	
	public boolean chackWord(String word) {
		if(word.contains(" "))
			return false;
		
		try {
			if(!word.matches("^[¤¡-¤¾ ¤¿-¤Ó °¡-ÆR]*$"))
				return false;
		}catch(PatternSyntaxException e) {
			System.err.println("An Exception Occured");
			e.printStackTrace();
		}
		return true;
	}
}