package inf226.inforum;
import java.util.function.Consumer;

public class Mutable<A> implements Consumer<A>{
   private A value;

   public Mutable(A value) {
     this.value = value;
   }

   public static<U> Mutable<U> init(U value) {
     return new Mutable<U>(value);
   }
 
   @Override
   public void accept(A value) {
      this.value = value;
   }

   public A get() {
      return value;
   }
}

