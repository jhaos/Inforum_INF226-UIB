package inf226.inforum.storage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.sql.PreparedStatement;

import inf226.inforum.Forum;
import inf226.inforum.ImmutableList;
import inf226.inforum.Maybe;
import inf226.inforum.Mutable;
import inf226.inforum.Thread;
import inf226.inforum.Util;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 */

public class ForumStorage implements Storage<Forum,SQLException> {
   final Connection connection;
   final Storage<Thread,SQLException> threadStore;

    public ForumStorage(Storage<Thread,SQLException> threadStore, Connection connection) throws SQLException {
      this.threadStore = threadStore;
      
      this.connection = connection; 
    }


   public synchronized void initialise() throws SQLException {
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS Forum (id TEXT PRIMARY KEY, version TEXT, handle TEXT, name TEXT, UNIQUE (handle))");
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS ForumThread (forum TEXT, thread TEXT, ordinal INTEGER, PRIMARY KEY(forum, thread), FOREIGN KEY(thread) REFERENCES Thread(id) ON DELETE CASCADE, FOREIGN KEY(forum) REFERENCES Forum(id) ON DELETE CASCADE)");
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS SubForum (forum TEXT, subforum TEXT, ordinal INTEGER, PRIMARY KEY(forum, subforum), FOREIGN KEY(subforum) REFERENCES Forum(id) ON DELETE CASCADE, FOREIGN KEY(forum) REFERENCES Forum(id) ON DELETE CASCADE)");
   }

   @Override
   public synchronized Stored<Forum> renew(UUID id) throws DeletedException,SQLException {
      final String forumsql = "SELECT version,handle,name FROM Forum WHERE id = '" + id.toString() + "'";
      final String threadsql = "SELECT thread,ordinal FROM ForumThread WHERE forum = '" + id.toString() + "' ORDER BY ordinal DESC";
      final String subforumsql = "SELECT subforum,ordinal FROM SubForum WHERE forum = '" + id.toString() + "' ORDER BY ordinal DESC";

      final Statement forumStatement = connection.createStatement();
      final Statement threadStatement = connection.createStatement();
      final Statement subforumStatement = connection.createStatement();

      final ResultSet forumResult = forumStatement.executeQuery(forumsql);

      if(forumResult.next()) {
          final UUID version = UUID.fromString(forumResult.getString("version"));
          final String handle = forumResult.getString("handle");
          final String name = forumResult.getString("name");
          final ResultSet threadResult = threadStatement.executeQuery(threadsql);
          // Get all the threads in this forum
          final ImmutableList.Builder<Stored<Thread>> threads = ImmutableList.builder();
          while(threadResult.next()) {
              final UUID threadId = UUID.fromString(threadResult.getString("thread"));
              threads.accept(threadStore.renew(threadId));
          }
          final ResultSet subforumResult = threadStatement.executeQuery(subforumsql);
          // Get all the subforums in this forum
          final ImmutableList.Builder<Stored<Forum>> subforums = ImmutableList.builder();
          while(subforumResult.next()) {
              final UUID subforumId = UUID.fromString(subforumResult.getString("subforum"));
              subforums.accept(this.renew(subforumId));
          }
          return (new Stored<Forum>(new Forum(handle,name,threads.getList(), subforums.getList()),id,version));
      } else {
          throw new DeletedException();
      }
   }

   @Override
   public synchronized Stored<Forum> save(Forum forum) throws SQLException {
     final Stored<Forum> stored = new Stored<Forum>(forum);

     String sql = "INSERT INTO Forum VALUES('" + stored.identity + "','"
     + stored.version  + "','"
     + forum.handle  + "', ?)";

     PreparedStatement stmt = connection.prepareStatement(sql);
     
    stmt.setString(1, forum.name);
    stmt.executeUpdate();

     final Maybe.Builder<SQLException> exception = Maybe.builder();

     // Store the threads in the forum
     final Mutable<Integer> ordinal = new Mutable<Integer>(0);
     forum.threads.forEach(thread -> {
        final String msql = "INSERT INTO ForumThread VALUES('" + stored.identity + "','"
                                                                 + thread.identity + "', ?)";
         try {
            PreparedStatement prep = connection.prepareStatement(msql);
            prep.setString(1, ordinal.get().toString());
            prep.executeUpdate();
         } catch (SQLException e1) {
            e1.printStackTrace();
         }
        ordinal.accept(ordinal.get() + 1);
      });


     // Store the subforum in the forum
     ordinal.accept(0);
     forum.subforums.forEach(subforum -> {
        final String msql = "INSERT INTO Subforum VALUES('" + stored.identity + "','"
                                                            + subforum.identity + "', ?)";
        try {
         PreparedStatement prep = connection.prepareStatement(msql);
         prep.setString(1, ordinal.get().toString());
         prep.executeUpdate();
        } catch (Exception e) {
         e.printStackTrace();
        }
        ordinal.accept(ordinal.get() + 1);
      });

     Util.throwMaybe(exception.getMaybe());

     return stored;
   }

   @Override
   public synchronized Stored<Forum> update(Stored<Forum> forum, Forum new_forum) throws UpdatedException,DeletedException,SQLException {
     final Stored<Forum> current = renew(forum.identity);
     final Stored<Forum> updated = current.newVersion(new_forum);
     if(current.version.equals(forum.version)) {
        String sql =  "UPDATE Forum SET (version,handle,name) = ('" 
                                              + updated.version  + "','"
                                              + new_forum.handle + "',?) WHERE id='" + updated.identity + "'";

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, new_forum.name);                               
        stmt.executeUpdate();
        connection.createStatement().executeUpdate("DELETE FROM ForumThread WHERE forum='" + forum.identity + "'");
        
        final Maybe.Builder<SQLException> exception = Maybe.builder();
        final Mutable<Integer> ordinal = new Mutable<Integer>(0);
        new_forum.threads.forEach(thread -> {
           final String msql = "INSERT INTO ForumThread VALUES('" + updated.identity + "','"
                                                                  + thread.identity + "',?)";
         try {
            PreparedStatement prep = connection.prepareStatement(msql);
            prep.setString(1, ordinal.get().toString());
            prep.executeUpdate();
         }catch (SQLException e) { exception.accept(e);}
           ordinal.accept(ordinal.get() + 1);
         });

        ordinal.accept(0);
        new_forum.subforums.forEach(subforum -> {
           final String msql = "INSERT INTO Subforum VALUES('" + updated.identity + "','"
                                                               + subforum.identity + "', ?)";
           try {
            PreparedStatement prep = connection.prepareStatement(msql);
            prep.setString(1, ordinal.get().toString());
            prep.executeUpdate();
           }catch (SQLException e) { exception.accept(e);}
           ordinal.accept(ordinal.get() + 1);
         });

        Util.throwMaybe(exception.getMaybe());
     } else {
        throw new UpdatedException(current);
     }
     return updated;
   }

   @Override
   public synchronized void delete(Stored<Forum> forum) throws UpdatedException,DeletedException,SQLException {
     final Stored<Forum> current = renew(forum.identity);
     if(current.version.equals(forum.version)) {
        connection.createStatement().executeUpdate("DELETE FROM ForumThread WHERE forum='" + forum.identity + "'");
        connection.createStatement().executeUpdate("DELETE FROM Subforum WHERE forum='" + forum.identity + "'");
        String sql =  "DELETE FROM Forum WHERE id ='" + forum.identity + "'";
        connection.createStatement().executeUpdate(sql);
     } else {
        throw new UpdatedException(current);
     }
   }

}
