import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.security.Security;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class WordChainingClient {
   
   static String eServer = "";
   static int ePort = 0000;
   static Socket chatSocket = null;
   static SSLSocketFactory sslSocketFactory = null;
   static SSLSocket sslSocket = null;
   
   static MySSLSocketFactory mySslSocketFactory;
   static InetAddress inetaddr = null;


   public static void main(String[] args) {

      if (args.length != 2) {
         System.out.println("Usage: Classname ServerName ServerPort");
         System.exit(1);
      }

      eServer = args[0];
      ePort = Integer.parseInt(args[1]);

      try {
         inetaddr = InetAddress.getByName(eServer);

         sslSocketFactory =  (SSLSocketFactory) MySSLSocketFactory.myGetSSLSocketFactory();
         sslSocket = (SSLSocket) sslSocketFactory.createSocket(inetaddr, ePort);
         

         sslSocket.startHandshake();
      } catch (BindException b) {
         System.out.println("Can't bind on: " + ePort);
         System.exit(1);
      } catch (IOException i) {
         System.out.println(i);
         System.exit(1);
      }
      new Thread(new ClientReceiver(sslSocket)).start();
      new Thread(new ClientSender(sslSocket, eServer)).start();
      
      System.out.println("모든 플레이어가 \"READY\"를 입력해야 게임이 진행됩니다.\n");
   }
}

class ClientSender implements Runnable {
   private SSLSocket chatSocket = null;
   private String eServer;
   static int playerNum = 1;
   

   ClientSender(SSLSocket socket, String eServer) {
      this.chatSocket = socket;
      this.eServer = eServer;
   }

   public void run() {
      Scanner KeyIn = null;
      PrintWriter out = null;
      try {
         KeyIn = new Scanner(System.in);
         out = new PrintWriter(chatSocket.getOutputStream(), true);

         String userInput = "";

         out.println("WELCOME");
         out.flush();
         while ((userInput = KeyIn.nextLine()) != null) {
            
            Dictionary dic = (Dictionary) Naming.lookup("rmi://" + eServer + "/" + "WordChaining");
            
         // 사용자가 게임종료를 한 경우
            if (userInput.equalsIgnoreCase("Bye.")) {
               out.println("BYE");
            }
            
            // 해당 단어가 사전에 있으면 출력
            if (dic.checkWord(userInput)) {
               out.println(userInput);
               out.flush();
            }
            // READY를 누른 경우 
            else if (userInput.equals("READY")) {
               out.println(userInput);
               out.flush();
            } 
            // 사전에 문자가 없는 경우
            else {
               out.println("WRONG");
               out.flush();
            }
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