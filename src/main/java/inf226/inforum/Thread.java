package inf226.inforum;

import inf226.inforum.storage.*;

public class Thread {
   public final String topic;
   public final ImmutableList<Stored<Message>> messages;

   public Thread(String topic, ImmutableList<Stored<Message>> messages) {
      this.topic = topic;
      this.messages = messages;
   }

   public Thread(String topic) {
      this.topic = topic;
      this.messages = ImmutableList.empty();
   }

   public Thread addMessage(Stored<Message> message) {
      return new Thread(topic, messages.add(message));
   }

   
   @Override
   public final boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final Thread thread_other = (Thread) other;
    boolean equal = true;
    if(topic == null) {
       equal = equal && thread_other.topic == null;
    } else {
       equal = equal && topic.equals(thread_other.topic);
    }
    if(messages == null) {
       equal = equal && thread_other.messages == null;
    } else {
       equal = equal && messages.equals(thread_other.messages);
    }
    return equal;
   }
}

