package inf226.inforum;

import org.junit.jupiter.api.Test;
import inf226.inforum.storage.Stored;

import java.sql.SQLException;

public class InforumTest{
    @Test
    void deleteForumMessage() throws Maybe.NothingException,SQLException {
       Inforum inforum = new Inforum(":memory:");
       Stored<UserContext> context = inforum.registerUser("ichor","").get();
       Stored<Forum> forum = inforum.createForum("TestForum",context).get();
       Stored<Thread> thread = inforum.createThread(forum, new Thread("Topic")).get();
       Stored<Message> message = inforum.postMessage(thread, "Hello !", context).get();
       inforum.deleteMessage(message.identity, context);
       thread = inforum.refreshThread(thread).get();
    }
}
