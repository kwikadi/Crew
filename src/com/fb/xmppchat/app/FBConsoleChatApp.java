package com.fb.xmppchat.app;
 
import com.fb.xmppchat.helper.CustomSASLDigestMD5Mechanism;
import com.fb.xmppchat.helper.FBMessageListener;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import javax.security.sasl.SaslException;
 
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
 
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
 
public class FBConsoleChatApp {
 
   public static final String FB_XMPP_HOST = "chat.facebook.com";
   public static final int FB_XMPP_PORT = 5222;
 
   private ConnectionConfiguration config;
   private XMPPConnection connection;
   private BidiMap friends = new DualHashBidiMap();
   private FBMessageListener fbml;
 
   public String connect() throws XMPPException, SmackException, IOException {
      config = new ConnectionConfiguration(FB_XMPP_HOST, FB_XMPP_PORT);
      SASLAuthentication.registerSASLMechanism("DIGEST-MD5", CustomSASLDigestMD5Mechanism.class);
      //config.setSASLAuthenticationEnabled(true);
      config.setDebuggerEnabled(false);
      connection = new XMPPTCPConnection(config);
      connection.connect();
      fbml = new FBMessageListener(connection);
      return connection.getConnectionID();
   }
 
   public void disconnect() throws NotConnectedException {
      if ((connection != null) && (connection.isConnected())) {
         Presence presence = new Presence(Presence.Type.unavailable);
         presence.setStatus("offline");
         connection.disconnect(presence);
      }
   }
 
   public boolean login(String userName, String password) 
     throws XMPPException, SaslException, SmackException, IOException {
      if ((connection != null) && (connection.isConnected())) {
         connection.login(userName, password);
         return true;
      }
      return false;
   }
 
   public String readInput() throws IOException {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      return br.readLine();
   }
 
   public void showMenu() {
      System.out.println("Please select one of the following menu.");
      System.out.println("1. List of Friends online");
      System.out.println("2. Send Message");
      System.out.println("3. EXIT");
      System.out.print("Your choice [1-3]: ");
   }
 
   public void getFriends() {
      if ((connection != null) && (connection.isConnected())) {
         Roster roster = connection.getRoster();
         int i = 1;
         for (RosterEntry entry : roster.getEntries()) {
            Presence presence = roster.getPresence(entry.getUser());
            if ((presence != null) 
               && (presence.getType() != Presence.Type.unavailable)) {
               friends.put("#" + i, entry);
               System.out.println(entry.getName() + "(#" + i + ")");
               i++;
            }
         }
         fbml.setFriends(friends);
      }
   }
 
   public void sendMessage() throws XMPPException
     , IOException, NotConnectedException {
      System.out.println("Type the key number of your friend (e.g. #1) and the text that you wish to send !");
      String friendKey = null;
      String text = null;
      System.out.print("Your friend's Key Number: ");
      friendKey = readInput();
      System.out.print("Your Text message: ");
      text = readInput();
      sendMessage((RosterEntry) friends.get(friendKey), text);
   }
 
   public void sendMessage(final RosterEntry friend, String text) 
     throws XMPPException, NotConnectedException {
      if ((connection != null) && (connection.isConnected())) {
         ChatManager chatManager = ChatManager.getInstanceFor(connection);
         Chat chat = chatManager.createChat(friend.getUser(), fbml);
         chat.sendMessage(text);
         System.out.println("Your message has been sent to "
            + friend.getName());
      }
   }
   
   public static void main(String[] args) throws SmackException {
      if (args.length == 0) {
        System.err.println("Usage: java FBConsoleChatApp [username_facebook] [password]");
        System.exit(-1);
      }
 
      String username = args[0];
      String password = args[1];

      FBConsoleChatApp app = new FBConsoleChatApp();
 
      try {
         app.connect();
         if (!app.login(username, password)) {
            System.err.println("Access Denied...");
            System.exit(-2);
         }
         app.showMenu();
         String data = null;
         menu:
         while((data = app.readInput().trim()) != null) {
            if (!Character.isDigit(data.charAt(0))) {
               System.out.println("Invalid input.Only 1-3 is allowed !");
               app.showMenu();
               continue;
            }
            int choice = Integer.parseInt(data);
            if ((choice != 1) && (choice != 2) && (choice != 3)) {
               System.out.println("Invalid input.Only 1-3 is allowed !");
               app.showMenu();
               continue;
            }
            switch (choice) {
               case 1: app.getFriends();
                       app.showMenu();
                       continue menu;
               case 2: app.sendMessage();
                       app.showMenu();
                       continue menu;
               default: break menu;
            }
         }
         app.disconnect();
      } catch (XMPPException e) {
       /* if (e.getXMPPError() != null) {
           System.err.println("ERROR-CODE : " + e.getXMPPError().getCode());
           System.err.println("ERROR-CONDITION : " + e.getXMPPError().getCondition());
           System.err.println("ERROR-MESSAGE : " + e.getXMPPError().getMessage());
           System.err.println("ERROR-TYPE : " + e.getXMPPError().getType());
        } */
    	  System.out.println("Error lol");
        app.disconnect();
      } catch (IOException e) {
        System.err.println(e.getMessage());
        app.disconnect();
      }
  }
}