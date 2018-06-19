import java.net.*;
import java.rmi.Naming;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Scanner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import java.io.*;

public class WordChainingServer implements Runnable {

   final static int PLAYERNUM = 2;
   private WordChaingServerRunnable clients[] = new WordChaingServerRunnable[PLAYERNUM];
   public int clientCount = 0;

   static String wordList = "";
   static ArrayList<String> words = new ArrayList<String>();

   private int ePort = -1;
   final String runRoot = "C:\\Users\\한주\\eclipse-workspace\\netword_project\\bin\\";
   SSLServerSocketFactory sslServerSocketFactory = null;
   SSLServerSocket sslServerSocket = null;
   SSLSocket sslSocket = null;
   private KeyStore keyStore;
   private KeyManagerFactory keyManagerFactory;
   private SSLContext sslContext;
   static InetAddress inetaddr = null;

   String ksName = runRoot + ".keystore/SSLSocketServerKey";
   char keyStorePass[];
   char keyPass[];

   public WordChainingServer(int port) {
      this.ePort = port;

      try {
         File file = new File("C:\\Users\\한주\\eclipse-workspace\\netword_project\\password.txt");
         Scanner sc = new Scanner(file);
         keyStorePass = keyPass = sc.nextLine().toCharArray();
         sc.close();
      } catch (FileNotFoundException fe) {
         fe.printStackTrace();
      }
   }

   public void run() {

      try {
         inetaddr = InetAddress.getLocalHost();

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
         Naming.rebind("rmi://" + inetaddr.getHostAddress() + ":1099/" + "WordChaining", dic);

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

   // 클라이언트 전체 메시지 전달
   public void putClient(String msg) {
      for (int i = 0; i < clientCount; i++) {
         clients[i].out.println(msg);
      }
   }

   // 해당 클라이언트에게만 메시지 전달
   public void putClient(int clientID, String msg) {
      for (int i = 0; i < clientCount; i++)
         if (clients[i].getClientID() == clientID)
            clients[i].out.println(msg);
   }

   // 끝말잇기 정보 전달 및 출력
   public void putClient(int clientID, String inputLine, String wordList) {
      for (int i = 0; i < clientCount; i++) {
         if (clients[i].getClientID() == clientID) {
            System.out.println("작성자: " + clientID);
         } else {
            System.out.println("메시지 받는 사람: " + clients[i].getClientID());
            if (this.getPlayerState()) {
            	clients[i].out.println("********************************************************************************************");
               clients[i].out.println("* [플레이어 " + clientID + "] 님이 입력한 단어 : " + inputLine);
               clients[i].out.println("* 현재까지의 단어 : " + wordList);
               clients[i].out.println("********************************************************************************************");
            }
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
         clients[clientCount] = new WordChaingServerRunnable(this, clientSocket, clientCount);
         new Thread(clients[clientCount]).start();
         clientCount++;
         System.out.println("Client connected: " + clientSocket.getPort() + ", CurrentClient: " + clientCount);
      } else {
         try {
            SSLSocket dummySocket = (SSLSocket) sslServerSocket.accept();
            WordChaingServerRunnable dummyRunnable = new WordChaingServerRunnable(this, dummySocket, -1);
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
    * 모든 플레이어가 READY상태인지 판단하는 함수
    * 
    * @return true: 모든 플레이어 READY 누른 경우 / false: 한명이라도 READY 안누른 경우
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

   // 모든 클라이언트 삭제
   public synchronized void delAllClient() {
      for (WordChaingServerRunnable r : clients) {
         try {
            r.NumOfWrongWord = 0;
            r.clientSocket.close();
            System.out.println("Client removed: " + r.clientID + ",CurrentClient: " + clientCount);
         } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
         clientCount--;
      }
      clients = new WordChaingServerRunnable[PLAYERNUM];
      clientCount = 0;
      wordList = "";
      words = new ArrayList<String>();
      System.out.println("All Client removed");
   }

   // 해당 클라이언트 삭제
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

   // 처음 들어온 클라이언트부터 시작하도록 설정하는 함수
   public void setFirstTurn() {
      clients[0].turn = true;
      putClient(clients[0].clientID, "\n당신 차례입니다.\n>>");
   }

   // 클라이언트 순서대로 턴을 넘겨주는 함수
   public void changeTurn(int clientID) {
         int index = -1;
         for (int i = 0; i < clientCount; i++)
            if (clients[i].getClientID() == clientID) {
               index = i;
               clients[i].turn = false;
               break;
            }
         if (index + 1 == clientCount) {
            clients[0].turn = true;
            putClient(clients[0].clientID, "\n당신 차례입니다.\n>>");
         } else if (index + 1 < clientCount) {
            clients[index + 1].turn = true;
            putClient(clients[index + 1].clientID, "\n당신 차례입니다.\n>>");
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
}

class WordChaingServerRunnable implements Runnable {
   protected WordChainingServer chatServer = null;
   protected SSLSocket clientSocket = null;
   protected PrintWriter out = null;
   protected BufferedReader in = null;
   public int clientID = -1;
   public int NumOfWrongWord = 0;
   public boolean isReady = false;
   public boolean isGameStarted = false;
   public boolean turn = false;

   public WordChaingServerRunnable(WordChainingServer server, SSLSocket socket, int clientCount) {
      this.chatServer = server;
      this.clientSocket = socket;
      clientID = clientCount;
      try {
         out = new PrintWriter(clientSocket.getOutputStream(), true);
         in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      } catch (IOException e) {
      }
   }

   public void run() {
      try {
         String inputLine;
         final String readyMsg = "님이 READY를 눌렀습니다.";
         final String unReadyMsg = "님이 UNREADY를 눌렀습니다.";
         final String startMsg = "끝말잇기 게임을 시작합니다.";
         final String terminateMsg = "님이 게임을 종료했습니다.";
         final String unPreparedMsg = "님 READY를 누르셔야 게임이 시작됩니다.";
         final String winMsg = "님이 이겼습니다.";
         final String loseMsg = "님이 졌습니다.";
         final String welcomeMsg = "님이 게임에 입장하셨습니다. (Type Message \"Bye.\" to leave)\n";
         String lastWord = "";

         while ((inputLine = in.readLine()) != null) {
            
            if (inputLine.equals("WELCOME")) {
               chatServer.putClient("[플레이어 " + clientID + "] " + welcomeMsg);
               continue;
            }

            // 사용자가 Bye. 를 입력하여 종료할 경우
            if (inputLine.equals("BYE")) {
               isReady = false;
               chatServer.putClient("[플레이어 " + getClientID()  + "] " + terminateMsg);
               chatServer.delAllClient();
               // chatServer.delClient(getClientID());
               break;
            }

            // 사용자가 READY를 누른 경우
            if (inputLine.equals("READY")) {
               this.isReady = !this.isReady;
               if (this.isReady == true)
                  chatServer.putClient("[플레이어 " + getClientID()  + "] "+ readyMsg);
               else
                  chatServer.putClient("[플레이어 " + getClientID()  + "] "+ unReadyMsg);
            }

            // 사용자가 사전에 등록된 단어를 입력하지 않은 경우
            if (inputLine.equals("WRONG") && this.turn) {
               if (chatServer.getPlayerState()) {
                  this.NumOfWrongWord++;
                  chatServer.putClient(this.getClientID(), "사전에 등록된 단어가 아닙니다.");
               } else {
                  chatServer.putClient(this.getClientID(), this.getClientID() + unPreparedMsg);
               }

               if (this.NumOfWrongWord >= 3) {
                  chatServer.putClient("[플레이어 " + this.getClientID() + "] " + loseMsg);
                  chatServer.delAllClient();
                  break;
               }
            }

            else if (chatServer.getPlayerState()) {
               // 마지막 PLAYER가 "READY"를 입력한 경우 wordList에 "READY"문자가 들어가는 것을 막기 위해 continue사용
               if (inputLine.equals("READY")) {
                  isGameStarted = true;
                  chatServer.putClient("\n--------------------------------");
                  chatServer.putClient("- " + startMsg + " -");
                  chatServer.putClient("--------------------------------");
                  chatServer.setFirstTurn();
                  continue;
               }

               if (this.turn) { // PLAYER 턴일 때만 단어 입력이 되도록
                  if (!WordChainingServer.words.isEmpty()) { // 입력된 단어가 첫 단어가 아닐 때

                     lastWord = WordChainingServer.words.get(WordChainingServer.words.size() - 1);
                     boolean double_check = true;
                     for(int k=0;k<WordChainingServer.words.size();++k)
                    	 if(WordChainingServer.words.get(k).compareTo(inputLine)==0)
                    		 double_check = false;

                     if (lastWord.charAt(lastWord.length() - 1) == inputLine.charAt(0) && double_check) {
                        WordChainingServer.words.add(inputLine);
                        WordChainingServer.wordList += inputLine + " -> ";
                        chatServer.putClient(getClientID(), inputLine, WordChainingServer.wordList);
                        chatServer.changeTurn(getClientID());
                     } else {
                        this.NumOfWrongWord++;
                        chatServer.putClient(this.getClientID(), "틀린 단어이거나 이미 입력한 단어입니다.");
                     }
                     
                      if (this.NumOfWrongWord >= 3) {
                                 chatServer.putClient("[플레이어 " + getClientID() +"] "+ loseMsg);
                                 chatServer.delAllClient();
                                 break;
                              }
                  } else { // 첫 단어가 입력될 때
                     WordChainingServer.words.add(inputLine);
                     WordChainingServer.wordList += inputLine + " -> ";
                     chatServer.putClient(getClientID(), inputLine, WordChainingServer.wordList);
                     chatServer.changeTurn(getClientID());
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
}