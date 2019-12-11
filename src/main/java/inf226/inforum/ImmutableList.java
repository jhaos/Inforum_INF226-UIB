package inf226.inforum;

import inf226.inforum.Maybe;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class ImmutableList<T> {
   private final Maybe<ListItem<T> > items;
   public final Maybe<T> last;
   public final int length;
   
   private ImmutableList() {
      this.items = Maybe.nothing();
      this.last = Maybe.nothing();
      this.length = 0;
   }
   private ImmutableList(T head, ImmutableList<T> tail) {
      this.items = new Maybe<ListItem<T>>(new ListItem<T>(head, tail));

      /* Construct a reference to the last element of
         the list. */
      T new_last;
      try {
         new_last = tail.last.get();
      } catch (Maybe.NothingException e) {
         new_last = head;
      }
      this.last = new Maybe<T>(new_last);
      this.length = tail.length + 1;
   }

   public static<U> ImmutableList<U> empty() {
      return new ImmutableList<U>();
   }
   public static<U> ImmutableList<U> cons(U head, ImmutableList<U> tail) {
      return new ImmutableList<U>(head,tail);
   }
   public Maybe<T> head() {
      try {
         return new Maybe<T>(items.get().head);
      } catch (Maybe.NothingException e) {
         return Maybe.nothing();
      }
   }

   public Maybe< ImmutableList<T> > tail() {
      try {
         return new Maybe<ImmutableList<T> >(items.get().tail);
      } catch (Maybe.NothingException e) {
         return Maybe.nothing();
      }
   }

   public ImmutableList<T> add(T element) {
      return cons(element, this);
   }

   public<U> ImmutableList<U> map(Function<T,U> f) {
    ImmutableList<U> result = empty();
    try {
       for(ImmutableList<T> l = this.reverse(); ; l = l.tail().get()) {
          result = cons(f.apply(l.head().get()), result);
       }
    } catch (Maybe.NothingException e) {
       // No more elements
    }
    return result;
   }

   public<B,C> ImmutableList<C> zipWith(ImmutableList<B> other, BiFunction<T,B,C> f) {
    Builder<C> result = builder();
    try {
       ImmutableList<T> l0 = this.reverse();
       ImmutableList<B> l1 = other.reverse();
       while(true) {
           result.accept(f.apply(l0.items.get().head, l1.items.get().head));
           l0 = l0.items.get().tail;
           l1 = l1.items.get().tail; 
       }
    } catch (Maybe.NothingException e) {
       // No more elements
    }
    return result.getList();
   }

   @Override
   public final boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final ImmutableList<Object> list_other = (ImmutableList<Object>) other;
    final Mutable<Boolean> equal = new Mutable<Boolean>(length == list_other.length);
    ImmutableList<Boolean> equalList = zipWith(list_other, (a, b) ->  a.equals(b));
    equalList.forEach(e -> { equal.accept(equal.get() && e);});
    return equal.get();
   }

   public void forEach(Consumer<T> c) {
      sequenceConsumer(c).accept(this);
   }

   public static<U> Consumer<ImmutableList<U>> sequenceConsumer(Consumer<U> c) {
    return new Consumer<ImmutableList<U>>(){
         @Override
         public void accept(ImmutableList<U> l) { 
            try {
               for(ListItem<U> e = l.items.get(); ; e = e.tail.items.get()) {
                  c.accept(e.head);
               }
            } catch (Maybe.NothingException e) {
               // No more elements
            }
        } };
   }

   public static class Builder<U> implements Consumer<U> {
      private ImmutableList<U> list;
      public Builder() { list = empty(); }
      @Override
      public synchronized void accept(U element) { list = cons(element, list) ; }
      public ImmutableList<U> getList() { return list ; }
   }

   public ImmutableList<T> reverse() {
    ImmutableList<T> result = empty();
    try {
       for(ListItem<T> e = this.items.get(); ; e = e.tail.items.get()) {
          result = cons(e.head, result);
       }
    } catch (Maybe.NothingException e) {
       // No more elements
    }
    return result;
   }

   public static<U> Builder<U> builder(){return new Builder<U>();}

   private static class ListItem<T> {
      public final T head;
      public final ImmutableList<T> tail;
      ListItem(T head, ImmutableList<T> tail) {
         this.head = head;
         this.tail = tail;
      }
   }
}
