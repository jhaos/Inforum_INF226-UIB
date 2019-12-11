package inf226.inforum.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

import inf226.inforum.ImmutableList;
import inf226.inforum.Maybe;
import inf226.inforum.User;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 */

public class UserStorage implements Storage<User,SQLException> {
   final Connection connection;

    public UserStorage(Connection connection) throws SQLException {
      this.connection = connection;
    }


   public synchronized  void initialise() throws SQLException {
      connection.createStatement().executeUpdate("DROP TABLE User");
      connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS User (id TEXT PRIMARY KEY, version TEXT, name TEXT, imageURL TEXT, joined TEXT, pass TEXT, UNIQUE(name))");
   }

   @Override
   public Stored<User> save(User user) throws SQLException {
      System.out.println("Save");
      
     final Stored<User> stored = new Stored<User>(user);
     String sql =  "INSERT INTO User VALUES('" + stored.identity + "','"+stored.version+"',?,?,?,?)";

     PreparedStatement prep = connection.prepareStatement(sql);
     prep.setString(1, user.name);
     prep.setString(2, user.imageURL);
     prep.setString(3, user.joined.toString());
     prep.setString(4, user.pass);

     prep.executeUpdate();

     System.out.println(user.pass);
     return stored;
   }

   @Override
   public synchronized Stored<User> update(Stored<User> user, User new_user) throws UpdatedException,DeletedException,SQLException {
     final Stored<User> current = renew(user.identity);
     final Stored<User> updated = current.newVersion(new_user);
     if(current.version.equals(user.version)) {
        String sql =  "UPDATE User SET (version,name,imageURL,joined) =('" 
                                                     + updated.version  + "',?,?,?) WHERE id='"+ updated.identity + "'";
        PreparedStatement prep = connection.prepareStatement(sql);
        prep.setString(1, new_user.name);
        prep.setString(2, new_user.imageURL);
        prep.setString(3, new_user.joined.toString());

        prep.executeUpdate();
     } else {
        throw new UpdatedException(current);
     }
     return updated;
   }

   @Override
   public synchronized void delete(Stored<User> user) throws UpdatedException,DeletedException,SQLException {
     final Stored<User> current = renew(user.identity);
     if(current.version.equals(user.version)) {
        String sql =  "DELETE FROM User WHERE id ='" + user.identity + "'";
        connection.createStatement().executeUpdate(sql);
     } else {
        throw new UpdatedException(current);
     }
   }

   @Override
   public synchronized Stored<User> renew(UUID id) throws DeletedException,SQLException{
      final String sql = "SELECT version,name,imageURL,joined, pass FROM User WHERE id = '" + id.toString() + "'";
      final Statement statement = connection.createStatement();
      final ResultSet rs = statement.executeQuery(sql);

      if(rs.next()) {
          final UUID version = UUID.fromString(rs.getString("version"));
          final String name = rs.getString("name");
          final String imageURL = rs.getString("imageURL");
          final Instant joined = Instant.parse(rs.getString("joined"));
          final String pass = rs.getString("pass");
          return (new Stored<User>(new User(name,imageURL,joined,pass),id,version));
      } else {
          throw new DeletedException();
      }
   }

   public synchronized Maybe<Stored<User>> getUser(String name) {
      try {
         final String sql = "SELECT id FROM User WHERE name = ?";
         PreparedStatement stmt = connection.prepareStatement(sql);
         stmt.setString(1, name);
         final ResultSet rs = stmt.executeQuery();
         if(rs.next()) {
          final UUID id = UUID.fromString(rs.getString("id"));
          return Maybe.just(renew(id));
         }
      } catch (SQLException e) {
         System.out.println(e);
         // Intensionally left blank
      } catch (DeletedException e) {
         System.out.println(e);
         // Intensionally left blank
      }
      return Maybe.nothing();

   }
}
