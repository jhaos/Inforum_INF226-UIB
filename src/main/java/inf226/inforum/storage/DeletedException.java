package inf226.inforum.storage;

public class DeletedException extends Exception {
    private static final long serialVersionUID = 7163663032597379968L;
    public DeletedException() {
        super("Object was deleted");
    }

     @Override
     public Throwable fillInStackTrace() {
         return this;
     }
}
