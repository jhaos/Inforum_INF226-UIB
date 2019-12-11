package inf226.inforum;

import java.time.Instant;

import com.lambdaworks.crypto.SCryptUtil;

public class User {
   public final String name;
   public final String imageURL;
   public final Instant joined;
   public final String pass;

   public User(String name, String imageURL, Instant joined, String pass) {
     this.name = name;
     this.imageURL = imageURL;
     this.joined = joined;
     this.pass = pass;
   }

   public boolean checkPassword(String password) {
      // TODO: Implement proper authentication.
      System.out.println("Check password");

      System.out.println(password);
      System.out.println(this.pass);

      return SCryptUtil.check(password,this.pass);
   }


   @Override
   public final boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final User user_other = (User) other;
    boolean equal = true;
    if(name == null) {
       equal = equal && user_other.name == null;
    } else {
       equal = equal && name.equals(user_other.name);
    }
    if(imageURL == null) {
       equal = equal && user_other.imageURL == null;
    } else {
       equal = equal && imageURL.equals(user_other.imageURL);
    }
    if(joined == null) {
       equal = equal && user_other.joined == null;
    } else {
       equal = equal && joined.equals(user_other.joined);
    }
    if(pass == null) {
      equal = equal && user_other.pass == null;
   } else {
      equal = equal && pass.equals(user_other.pass);
   }
    return equal;
   }

}