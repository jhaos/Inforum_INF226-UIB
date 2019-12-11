package inf226.inforum.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

import inf226.inforum.ImmutableList;
import inf226.inforum.Message;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 */

public class MessageStorage implements Storage<Message,SQLException> {
   final Connection connection;

    public MessageStorage(Connection connection) throws SQLException {
      this.connection = connection;
    }


   public synchronized  void initialise() throws SQLException {
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS Message (id TEXT PRIMARY KEY, version TEXT, sender TEXT, message TEXT, date TEXT)");
   }

   @Override
   public Stored<Message> save(Message message) throws SQLException {
     final Stored<Message> stored = new Stored<Message>(message);
     String sql =  "INSERT INTO Message VALUES('"+stored.identity+"','"+stored.version+"',?,?,?)";
     PreparedStatement prepared = connection.prepareStatement(sql);

     prepared.setString(1,message.sender);
     prepared.setString(2,message.message);
     prepared.setString(3,message.date.toString());
     
     prepared.executeUpdate();
     return stored;
   }

   @Override
   public synchronized Stored<Message> update(Stored<Message> message, Message new_message) throws UpdatedException,DeletedException,SQLException {
     final Stored<Message> current = renew(message.identity);
     final Stored<Message> updated = current.newVersion(new_message);
     if(current.version.equals(message.version)) {
        String sql =  "UPDATE Message SET (version, sender, message, date) = ('"
                                                   + updated.version  + "',?,?,?) WHERE id='" + updated.identity + "'";
        PreparedStatement prepared = connection.prepareStatement(sql);

         prepared.setString(1,new_message.sender);
         prepared.setString(2,new_message.message);
         prepared.setString(3,new_message.date.toString());

         prepared.executeUpdate();
     } else {
        throw new UpdatedException(current);
     }
     return updated;
   }

   @Override
   public synchronized void delete(Stored<Message> message) throws UpdatedException,DeletedException,SQLException {
     final Stored<Message> current = renew(message.identity);
     if(current.version.equals(message.version)) {
        String sql =  "DELETE FROM Message WHERE id ='" + message.identity + "'";
        connection.createStatement().executeUpdate(sql);
     } else {
        throw new UpdatedException(current);
     }
   }

   @Override
   public synchronized Stored<Message> renew(UUID id) throws DeletedException,SQLException{
      final String sql = "SELECT version,sender,message,date FROM Message WHERE id = '" + id.toString() + "'";
      final Statement statement = connection.createStatement();
      final ResultSet rs = statement.executeQuery(sql);

      if(rs.next()) {
          final UUID version = UUID.fromString(rs.getString("version"));
          final String sender = rs.getString("sender");
          final String message = rs.getString("message");
          final Instant date = Instant.parse(rs.getString("date"));
          return (new Stored<Message>(new Message(sender,message,date),id,version));
      } else {
          throw new DeletedException();
      }
   }

}
