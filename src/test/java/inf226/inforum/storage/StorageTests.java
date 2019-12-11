package inf226.inforum.storage;

import inf226.inforum.*;
import inf226.inforum.Thread;

import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

public class StorageTests{
    final String dburl = "jdbc:sqlite::memory:";

    @Test
    void storedNewVerion() {
        Stored<String> stored = new Stored<String>("foobar");
        Stored<String> updated = stored.newVersion("barbar");
        assertTrue(updated.identity.equals(stored.identity));
        assertFalse(updated.version.equals(stored.version));
        assertEquals(updated.value,"barbar");
    }
    private<U,Q,E extends Exception> void testSave(Storage<U,E> storage, U value) {
        try {
            Stored<U> stored = storage.save(value);
            assertTrue(stored.value.equals(value));
        } catch (Exception exception) {
            fail("Could not save to storage:\n" + exception.toString());
        }
    }
    private<U,Q,E extends Exception> void testRenew(Storage<U,E> storage, U value0, U value1) {
        assertFalse(value0.equals(value1));
        try {
            Stored<U> stored = storage.save(value0);
            assertTrue(stored.value.equals(value0));
            boolean updated = true;
            while(updated) {
                try {
                    assertTrue(storage.update(stored,value1).value.equals(value1));
                    updated = false;
                } catch (UpdatedException e) {
                    stored = (Stored<U>)e.newObject;
                }
            }
            Stored<U> renewed = storage.renew(stored.identity);
            assertTrue(renewed.value.equals(value1));
        } catch (Exception exception) {
            fail(exception);
        }
    }

    @SuppressWarnings("unchecked")
	private<U,Q,E extends Exception> void testUpdate(Storage<U,E> storage, U value0, U value1) {
        assertFalse(value0.equals(value1));
        try {
            Stored<U> stored = storage.save(value0);
            assertTrue(stored.value.equals(value0));
            boolean updated = true;
            while(updated) {
                try {
                    assertTrue(storage.update(stored,value1).value.equals(value1));
                    updated = false;
                } catch (UpdatedException e) {
                    stored = (Stored<U>)e.newObject;
                }
            }
        } catch (Exception exception) {
            fail(exception);
        }
    }
    private<U,Q,E extends Exception> void testDelete(Storage<U,E> storage, U value0, U value1) {
        assertFalse(value0.equals(value1));
        try {
            Stored<U> stored = storage.save(value0);
            assertTrue(stored.value.equals(value0));
            storage.delete(stored);
            try {
                storage.renew(stored.identity);
                fail("renew() must throw DeletedException after delete()");
                storage.update(stored,value1);
                fail("update() must throw DeletedException after delete()");
            } catch (DeletedException e) {
                 // Intensionally left blank
            }
            
        } catch (Exception exception) {
            fail(exception);
        }
    }

    @Test
    void testMessageStorageSave() {
        try(Connection connection = DriverManager.getConnection(dburl)){
            MessageStorage messageStore = new MessageStorage(connection);
            messageStore.initialise();

            Message message = new Message("Alice","Hello world!",Instant.now());
            testSave(messageStore,message);
        } catch (SQLException e) {
            fail(e);
        }
    }
    @Test
    void testMessageStorageUpdate() {
        try(Connection connection = DriverManager.getConnection(dburl)){
            MessageStorage messageStore = new MessageStorage(connection);
            messageStore.initialise();

            Message message0 = new Message("Alice","Hello world!",Instant.now());
            Message message1 = new Message("Bob","Hello Alice!",Instant.now());
            testUpdate(messageStore,message0,message1);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testMessageStorageRenew() {
        try(Connection connection = DriverManager.getConnection(dburl)){
            MessageStorage messageStore = new MessageStorage(connection);
            messageStore.initialise();

            Message message0 = new Message("Alice","Hello world!",Instant.now());
            Message message1 = new Message("Bob","Hello Alice!",Instant.now());
            testRenew(messageStore,message0,message1);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testThreadStorageSave() {
        try (Connection connection = DriverManager.getConnection(dburl)){
            
            MessageStorage messageStore = new MessageStorage(connection);
            ThreadStorage threadStore = new ThreadStorage(messageStore, connection);

            messageStore.initialise();
            threadStore.initialise();

            Message message0 = new Message("Alice","Hello world!",Instant.now());
            Message message1 = new Message("Bob","Hello Alice!",Instant.now());
            ImmutableList.Builder<Stored<Message>> msgList = ImmutableList.builder();
            msgList.accept(messageStore.save(message0));
            msgList.accept(messageStore.save(message1));
            Thread thread = new Thread("An important topic!",msgList.getList());
            testSave(threadStore,thread);
        } catch (Exception e) {
            fail(e);
        }
    }
    @Test
    void testThreadStorageUpdate() {
        try (Connection connection = DriverManager.getConnection(dburl)){
            
            MessageStorage messageStore = new MessageStorage(connection);
            ThreadStorage threadStore = new ThreadStorage(messageStore, connection);

            messageStore.initialise();
            threadStore.initialise();

            Message message0 = new Message("Alice","Hello world!",Instant.now());
            Message message1 = new Message("Bob","Hello Alice! What’s up`",Instant.now());
            Message message2 = new Message("Alice","Not much, Bob. Not much…",Instant.now());
            ImmutableList.Builder<Stored<Message>> msgList = ImmutableList.builder();
            msgList.accept(messageStore.save(message0));
            msgList.accept(messageStore.save(message1));
            Thread thread0 = new Thread("An important topic!",msgList.getList());
            msgList.accept(messageStore.save(message2));
            Thread thread1 = new Thread("An important topic!",msgList.getList());
            testUpdate(threadStore,thread0,thread1);
        } catch (Exception e) {
            fail(e);
        }
    }
    @Test
    void testThreadStorageRenew() {
        try (Connection connection = DriverManager.getConnection(dburl)){
            
            MessageStorage messageStore = new MessageStorage(connection);
            ThreadStorage threadStore = new ThreadStorage(messageStore, connection);

            messageStore.initialise();
            threadStore.initialise();

            Message message0 = new Message("Alice","Hello world!",Instant.now());
            Message message1 = new Message("Bob","Hello Alice! What’s up`",Instant.now());
            Message message2 = new Message("Alice","Not much, Bob. Not much…",Instant.now());
            ImmutableList.Builder<Stored<Message>> msgList = ImmutableList.builder();
            msgList.accept(messageStore.save(message0));
            msgList.accept(messageStore.save(message1));
            Thread thread0 = new Thread("An important topic!",msgList.getList());
            msgList.accept(messageStore.save(message2));
            Thread thread1 = new Thread("An important topic!",msgList.getList());
            testRenew(threadStore,thread0,thread1);
        } catch (Exception e) {
            fail(e);
        }
    }


    @Test
    void testUserStorageSave() {
        try(Connection connection = DriverManager.getConnection(dburl)){
            UserStorage userStore = new UserStorage(connection);
            userStore.initialise();

            User user = new User("Alice","image",Instant.now());
            testSave(userStore,user);
        } catch (SQLException e) {
            fail(e);
        }
    }

    @Test
    void testUserContextStorageRenew() {
        try (Connection connection = DriverManager.getConnection(dburl)){
            
            MessageStorage messageStore = new MessageStorage(connection);
            ThreadStorage threadStore = new ThreadStorage(messageStore, connection);
            ForumStorage forumStore = new ForumStorage(threadStore, connection);
            UserStorage userStore = new UserStorage(connection);
            UserContextStorage contextStore = new UserContextStorage(forumStore,userStore,connection);

            messageStore.initialise();
            threadStore.initialise();
            forumStore.initialise();
            userStore.initialise();
            contextStore.initialise();

            Stored<User> user0 = userStore.save(new User("alice","image",Instant.now()));

            Message message0 = new Message("Alice","Hello world!",Instant.now());
            Message message1 = new Message("Bob","Hello Alice! What’s up`",Instant.now());
            Message message2 = new Message("Alice","Not much, Bob. Not much…",Instant.now());

            ImmutableList.Builder<Stored<Message>> msgList = ImmutableList.builder();
            msgList.accept(messageStore.save(message0));
            msgList.accept(messageStore.save(message1));
            Thread thread0 = new Thread("An important topic!",msgList.getList());
            msgList.accept(messageStore.save(message2));
            
            ImmutableList.Builder<Stored<Thread>> threads = ImmutableList.builder();
            threads.accept(threadStore.save(new Thread("An important topic!",msgList.getList())));
            ImmutableList.Builder<Stored<Forum>> forums = ImmutableList.builder();
            UserContext context0 = new UserContext(user0,forums.getList());
            forums.accept(forumStore.save((new Forum("Testforum", "test",threads.getList(), ImmutableList.empty()))));
            UserContext context1 = new UserContext(user0,forums.getList());

            testRenew(contextStore,context0,context1);
        } catch (Exception e) {
            fail(e);
        }
    }


}

