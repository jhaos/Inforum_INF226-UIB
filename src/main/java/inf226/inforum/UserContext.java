package inf226.inforum;

import inf226.inforum.*;
import inf226.inforum.storage.*;

public class UserContext {
   public final Stored<User> user;
   public final ImmutableList<Stored<Forum>> forums;

   public UserContext(Stored<User> user, ImmutableList<Stored<Forum>> forums) {
      this.forums = forums;
      this.user = user;
   }
   public UserContext(Stored<User> user) {
      this.forums = ImmutableList.empty();
      this.user = user;
   }
   public UserContext addForum(Stored<Forum> forum) {
      return new UserContext(user, ImmutableList.cons(forum, forums));
   }

   @Override
   public final boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final UserContext context_other = (UserContext) other;
    boolean equal = true;
    if(user == null) {
       equal = equal && context_other.user == null;
    } else {
       equal = equal && user.equals(context_other.user);
    }
    if(forums == null) {
       equal = equal && context_other.forums == null;
    } else {
       equal = equal && forums.equals(context_other.forums);
    }
    return equal;
   }


}

