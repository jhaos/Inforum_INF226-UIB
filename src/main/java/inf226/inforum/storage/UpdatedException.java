package inf226.inforum.storage;
import inf226.inforum.storage.Stored;

public class UpdatedException extends Exception {
    private static final long serialVersionUID = 8163663032597379968L;
    public final Stored newObject;
    public UpdatedException(Stored newObject) {
        super("Object was updated");
        this.newObject = newObject;
    }

     @Override
     public Throwable fillInStackTrace() {
         return this;
     }
}
