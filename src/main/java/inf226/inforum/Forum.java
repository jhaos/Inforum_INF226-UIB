package inf226.inforum;

import inf226.inforum.storage.Stored;

public class Forum {
   public final String handle;
   public final String name;
   public final ImmutableList<Stored<Thread>> threads;
   public final ImmutableList<Stored<Forum>> subforums;

   public Forum(String handle, String name, ImmutableList<Stored<Thread>> threads, ImmutableList<Stored<Forum>> subforums) {
      // TODO: Verify that handle is URL safe.
      this.handle = handle;
      this.name = name;
      this.threads = threads;
      this.subforums = subforums;
   }

   public Forum(String handle, String name) {
      this.handle = handle;
      this.name = name;
      this.threads = ImmutableList.empty();
      this.subforums = ImmutableList.empty();
   }

   public Forum addThread(Stored<Thread> thread) {
       return new Forum(handle, name,  ImmutableList.cons(thread,threads), subforums);
   }

   public Forum addSubforum(Stored<Forum> forum) {
       return new Forum(handle, name, threads, ImmutableList.cons(forum,subforums));
   }


   @Override
   public final boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final Forum forum_other = (Forum) other;
    boolean equal = true;
    if(name == null) {
       equal = equal && forum_other.name == null;
    } else {
       equal = equal && name.equals(forum_other.name);
    }
    if(handle == null) {
       equal = equal && forum_other.handle == null;
    } else {
       equal = equal && handle.equals(forum_other.handle);
    }
    if(threads == null) {
       equal = equal && forum_other.threads == null;
    } else {
       equal = equal && threads.equals(forum_other.threads);
    }
    if(subforums == null) {
       equal = equal && forum_other.subforums == null;
    } else {
       equal = equal && subforums.equals(forum_other.subforums);
    }
    return equal;
   }

   public static Pair<Stored<Forum>,String> resolveSubforum(Stored<Forum> that , String path) {
     Mutable<Pair<Stored<Forum>,String>> result
        = new Mutable<Pair<Stored<Forum>,String>>(Pair.pair(that,path));
     that.value.subforums.forEach( forum -> {
         if (path.startsWith(forum.value.handle + "/")) {
            result.accept(resolveSubforum(forum,path.substring(forum.value.handle.length() + 1)));
         } });
     return result.get();
   }
}