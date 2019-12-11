package inf226.inforum.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import inf226.inforum.Forum;
import inf226.inforum.ImmutableList;
import inf226.inforum.Maybe;
import inf226.inforum.Mutable;
import inf226.inforum.User;
import inf226.inforum.UserContext;
import inf226.inforum.Util;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 */

public class UserContextStorage implements Storage<UserContext,SQLException> {
   final Connection connection;
   final Storage<Forum,SQLException> forumStore;
   final Storage<User,SQLException> userStore;

    public UserContextStorage(Storage<Forum,SQLException> forumStore, 
                              Storage<User,SQLException> userStore,
                              Connection connection) throws SQLException {
      this.forumStore = forumStore;
      this.userStore = userStore;
      this.connection = connection; 
    }


   public synchronized void initialise() throws SQLException {
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS UserContext (id TEXT PRIMARY KEY, version TEXT, user TEXT, FOREIGN KEY(user) REFERENCES User(id) ON DELETE CASCADE)");
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS UserContextForum (context TEXT, forum TEXT, ordinal INTEGER, PRIMARY KEY(context, forum), FOREIGN KEY(forum) REFERENCES Forum(id) ON DELETE CASCADE, FOREIGN KEY(context) REFERENCES UserContext(id) ON DELETE CASCADE)");
   }

   @Override
   public synchronized Stored<UserContext> renew(UUID id) throws DeletedException,SQLException {
      final String contextsql = "SELECT version,user FROM UserContext WHERE id = '" + id.toString() + "'";
      final String forumsql = "SELECT forum,ordinal FROM UserContextForum WHERE context = '" + id.toString() + "' ORDER BY ordinal DESC";

      final Statement contextStatement = connection.createStatement();
      final Statement forumStatement = connection.createStatement();

      final ResultSet contextResult = contextStatement.executeQuery(contextsql);
      final ResultSet forumResult = forumStatement.executeQuery(forumsql);

      if(contextResult.next()) {
          final UUID version = UUID.fromString(contextResult.getString("version"));
          final Stored<User> user = userStore.renew(UUID.fromString(contextResult.getString("user")));
          // Get all the forums in this context
          final ImmutableList.Builder<Stored<Forum>> forums = ImmutableList.builder();
          while(forumResult.next()) {
              final UUID forumId = UUID.fromString(forumResult.getString("forum"));
              forums.accept(forumStore.renew(forumId));
          }
          return (new Stored<UserContext>(new UserContext(user,forums.getList()),id,version));
      } else {
          throw new DeletedException();
      }
   }

   @Override
   public synchronized Stored<UserContext> save(UserContext context) throws SQLException {
     final Stored<UserContext> stored = new Stored<UserContext>(context);
     final String sql =  "INSERT INTO UserContext VALUES('" + stored.identity + "','"
                                                       + stored.version  + "','"
                                                       + context.user.identity  + "')";
     connection.createStatement().executeUpdate(sql);
     final Maybe.Builder<SQLException> exception = Maybe.builder();
     final Mutable<Integer> ordinal = new Mutable<Integer>(0);
     context.forums.forEach(forum -> {
        final String msql = "INSERT INTO UserContextForum VALUES('" + stored.identity + "','"
                                                                 + forum.identity + "', ?)";
        try { 
         PreparedStatement prep = connection.prepareStatement(msql);
         prep.setString(1, ordinal.get().toString());
         prep.executeUpdate();
         }
        catch (SQLException e) { exception.accept(e) ; }
        ordinal.accept(ordinal.get() + 1);
      });

     Util.throwMaybe(exception.getMaybe());
         
     return stored;
   }

   @Override
   public synchronized Stored<UserContext> update(Stored<UserContext> context, UserContext new_context) throws UpdatedException,DeletedException,SQLException {
     final Stored<UserContext> current = renew(context.identity);
     final Stored<UserContext> updated = current.newVersion(new_context);
     if(current.version.equals(context.version)) {
        String sql =  "UPDATE UserContext SET (version,user) = ('"
                                                     + updated.version  + "','"
                                                     + new_context.user.identity + "') WHERE id='" + updated.identity + "'";
        connection.createStatement().executeUpdate(sql);
        connection.createStatement().executeUpdate("DELETE FROM UserContextForum WHERE context='" + context.identity + "'");
        
        final Maybe.Builder<SQLException> exception = Maybe.builder();
        final Mutable<Integer> ordinal = new Mutable<Integer>(0);
        new_context.forums.forEach(forum -> {
           final String msql = "INSERT INTO UserContextForum VALUES('" + updated.identity + "','"
                                                                    + forum.identity + "',?)";
           try {
            PreparedStatement prep = connection.prepareStatement(msql);
            prep.setString(1, ordinal.get().toString());
            prep.executeUpdate();
           }
           catch (SQLException e) { exception.accept(e);}
           ordinal.accept(ordinal.get() + 1);
         });
        Util.throwMaybe(exception.getMaybe());
     } else {
        throw new UpdatedException(current);
     }
     return updated;
   }

   @Override
   public synchronized void delete(Stored<UserContext> context) throws UpdatedException,DeletedException,SQLException {
     final Stored<UserContext> current = renew(context.identity);
     if(current.version.equals(context.version)) {
        connection.createStatement().executeUpdate("DELETE FROM UserContextForum WHERE context='" + context.identity + "'");
        String sql =  "DELETE FROM UserContext WHERE id ='" + context.identity + "'";
        connection.createStatement().executeUpdate(sql);
     } else {
        throw new UpdatedException(current);
     }
   }

   public synchronized Maybe<Stored<UserContext>> getUserContext(Stored<User> user, String password) {
      try {
      if (user.value.checkPassword(password)) {
         final String contextsql = "SELECT id FROM UserContext WHERE user = '" + user.identity + "'";
         final Statement contextStatement = connection.createStatement();
         final ResultSet contextResult = contextStatement.executeQuery(contextsql);
         if(contextResult.next()) {
            final UUID id = UUID.fromString(contextResult.getString("id"));
            return Maybe.just(renew(id));
            
         }
      }
      } catch (Exception e) {
         // Intensionally left blank to avoid info leakage from login.
      }
      return Maybe.nothing();
   }

   /**
    *  Invite another user to the forum.
    */
   public synchronized boolean invite(Stored<Forum> forum, Stored<User> user) {
     try {
      final String sql  = "SELECT id FROM UserContext WHERE user ='" + user.identity + "'";
      ResultSet rs = connection.createStatement().executeQuery(sql);
      if(rs.next()) {
        final String msql = "INSERT INTO UserContextForum VALUES(?,'"
                                                                    + forum.identity + "','"
                                                                    + (-1) + "')";
         PreparedStatement prep = connection.prepareStatement(msql);
         prep.setString(1, rs.getString("id"));
         prep.executeUpdate();
         
        return true;
        
      }
      System.out.println("No user in DB:" +sql);
    } catch (SQLException e) {System.err.println(e);}
    return false;
   }
}
