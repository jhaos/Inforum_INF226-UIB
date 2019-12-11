package inf226.inforum;

import java.time.Instant;


public class Message {
   public final String sender; // User name of sender
   public final String message;
   public final Instant date; // Date of posting.


   public Message(String sender, String message, Instant date) {
      this.sender = sender;
      this.message = message;
      this.date = date;
   }

   // Copy constructor
   public Message(Message m) {
      this.sender = m.sender;
      this.message = m.message;
      this.date = m.date;
   }


   public Message setMessage(String message) {
      return new Message(sender,message,date);
   }

   @Override
   public final boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final Message message_other = (Message) other;
    boolean equal = true;
    if(sender == null) {
       equal = equal && message_other.sender == null;
    } else {
       equal = equal && sender.equals(message_other.sender);
    }
    if(message == null) {
       equal = equal && message_other.message == null;
    } else {
       equal = equal && message.equals(message_other.message);
    }
    return equal;
   }
}

