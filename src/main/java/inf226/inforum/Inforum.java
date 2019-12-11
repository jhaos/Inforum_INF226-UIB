package inf226.inforum;


import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.lang.IllegalArgumentException;

import java.io.Closeable;
import java.io.IOException;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

import java.time.Instant;
import inf226.inforum.storage.*;
import inf226.inforum.storage.DeletedException;


/**
 *
 * Inforum is the implementation of the forum logic.
 * It provieds an abstract interface for usual forum
 * actions, such as posting messages, login and user
 * registration.
 *
 * In order to prevent information leakage to the
 * user interface, all operations either fail or succeed,
 * by returning a @Maybe value.
 *
 **/

public class Inforum implements Closeable
{

  private final MessageStorage messageStore;
  private final UserStorage userStore;
  private final ThreadStorage threadStore;
  private final ForumStorage forumStore;
  private final UserContextStorage contextStore;
  private final Connection connection;

  public Inforum(String path) throws SQLException {
       final String dburl = "jdbc:sqlite:" + path;
       connection = DriverManager.getConnection(dburl);
       connection.createStatement().executeUpdate("PRAGMA foreign_keys = ON");
       messageStore = new MessageStorage(connection);
       messageStore.initialise();
       userStore = new UserStorage(connection);
       userStore.initialise();
       threadStore = new ThreadStorage(messageStore, connection);
       threadStore.initialise();
       forumStore = new ForumStorage(threadStore, connection);
       forumStore.initialise();
       contextStore = new UserContextStorage(forumStore, userStore, connection);
       contextStore.initialise();
  }

  @Override
  public void close() throws IOException{
     try {
       connection.close();
     } catch (SQLException e) {
       throw new IOException(e.toString());
     }
  }


  /**
   *  Login function.
   */
  public Maybe<Stored<UserContext>> login(String username, String password) {
     // We simply call on the storage.UserContextStore.getUserContext method.
     return userStore
         .getUser(username)
         .bind( user -> contextStore.getUserContext(user, password) );
  }


  /**
   *  Register a new user.
   */
  public Maybe<Stored<UserContext>> registerUser(String username, String password) {
     try {
      System.out.println("Register User");

        Stored<User> user = userStore.save(new User(username,"/img/user.svg",Instant.now(),password));
        return Maybe.just(contextStore.save(new UserContext(user)));
     } catch (SQLException e) {
         // Mostlikely the username is not unique
         System.out.println("Fallo register");

         System.err.println(e);
     }
     return Maybe.nothing();
  }

  /**
   *  Create a new top level forum for a user.
   */
  public Maybe<Stored<Forum>> createForum(String name, Stored<UserContext> context) {
     try {
         // TODO: Make handle URL safe
         String handle = name.toLowerCase().replace(' ','_');
         Stored<Forum> forum = forumStore.save(new Forum(handle, name));
         Util.updateSingle(context, contextStore,
            con -> con.value.addForum(forum));
         return Maybe.just(forum);
     } catch (SQLException e) {
         System.err.println(e);
     } catch (DeletedException e) {
         System.err.println(e);
     }
     return Maybe.nothing();
  }

  /**
   *  Restore a session from the UUID (session cookie)
   */
  public Maybe<Stored<UserContext>> restore(UUID id) {
     try {
         return Maybe.just(contextStore.renew(id));
     } catch (DeletedException e){
         // Session token expired or otherwise deleted from storage
         System.err.println("Session token expried:" + id);
     } catch (SQLException e) {
         // Retrieving session from storage failed
         System.err.println(e);
     } 
    return Maybe.nothing();
  }

  /**
   *  Post a meesage to a thread
   */
  public Maybe<Stored<Message>> postMessage(Stored<Thread> thread, String message, Stored<UserContext> context) {
     try {
        // We fill out some values automatically.
        Stored<Message> m 
            = messageStore.save(new Message(context.value.user.value.name,
                                            message,
                                            Instant.now()));
        Util.updateSingle(thread, threadStore,
          t -> t.value.addMessage(m));
        return Maybe.just(m);
     } catch (Exception e) {
        System.err.println(e);
        return Maybe.nothing();
     }
  }

  /**
   *   Create a new thread in a forum.
   */
  public Maybe<Stored<Thread>> createThread(Stored<Forum> forum, Thread thread) {
     try {
        Stored<Thread> t = threadStore.save(thread);
        Util.updateSingle(forum,forumStore,
          f -> f.value.addThread(t));
        return Maybe.just(t);
     } catch (Exception e) {
        return Maybe.nothing();
     }
  }

  /**
   *   Refresh a forum after update
   */
  public Maybe<Stored<Forum>> refreshForum(Stored<Forum> forum) {
     try {
        return Maybe.just(forumStore.renew(forum.identity));
     } catch (Exception e) {
        System.err.println(e);
        return Maybe.nothing();
     }
  }
  
  /**
   *   Refresh a thread after update
   */
  public Maybe<Stored<Thread>> refreshThread(Stored<Thread> thread) {
     try {
        return Maybe.just(threadStore.renew(thread.identity));
     } catch (Exception e) {
        System.err.println(e);
        return Maybe.nothing();
     }
  }

  /**
   *   Delete a message.
   */
  public void deleteMessage(UUID message, Stored<UserContext> context) {
     try {
      Stored<Message> current = messageStore.renew(message);
      if(current.value.sender.equals(context.value.user.value.name)){
         messageStore.delete(current);
         System.err.println("Message deleted.");
      }        
     } catch (Exception e) {
        System.err.println(e);
     }
  }

  public boolean invite(Stored<UserContext> context, String username, Stored<Forum> forum) {
      try {
         Stored<User> user = userStore.getUser(username).get();
         return contextStore.invite(forum, user);
      } catch (Maybe.NothingException e) {
         System.err.println("User not found:" + username);
         return false;
      }
  }

  public void editMessage(UUID message, String content, Stored<UserContext> context) {
      try {
         Stored<Message> current = messageStore.renew(message);

         if(current.value.sender.equals(context.value.user.value.name)){
            Util.updateSingle(current, messageStore,
            msg -> msg.value.setMessage(content));
            System.err.println("Message edited.");
         }

      } catch (Exception e) {
         System.err.println(e);
      }
  }

  public UserStorage getUserStorage(){
     return userStore;
  }
}