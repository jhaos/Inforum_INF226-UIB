# INFORUM – The INsecure FORUM

Welcome to this third mandatory assignment of INF226.
In this assignment you will be improving the security
of a program called Inforum – a very simple discussion
forum in the form of a web-application.

Inforum has been especially crafted to contain a number
of security flaws. You can imagine that it has been
programmed by a less competent collegue, and that after
numerous securiy incidents, your organisation has decided
that you – a competent security professional – should take
some time to secure the app.

For your convenience, the task is separated into specific
exercises or tasks. These task might have been the result
of a security analysis – like the one you had in the
second assignment. If you discover any security issues
beyond these tasks, you can make a note of them at the
end of this report.

For each task, you should make a short note how you solved
it – ideally with a reference to the relevant git-commits you
have made.

## Evaluation

This assigment is mandatory for the course, and counts 10%
of your final grade. The assigment is graded 0–10 points,
where you must get a minimum of 3 points in order to pass
the assignment.

## Groups

As with the previous assignments, you can work in groups of 1–3 students
on this assginment. Make sure that everyone is signed up for the group
on [MittUiB](https://mitt.uib.no/courses/19886/groups#tab-6923).
One good way to collaborate is that one person on the group makes a
fork and adds the other group members to that project.

## Getting and building the project

Log into [`git.app.uib.no`](https://git.app.uib.no/Hakon.Gylterud/inf226-2019-inforum) and make your
own fork of the project there. You can then clone your repo to your
own persion machine.

To build the project you can use Maven on the command line, or configure
your IDE to use Maven to build the project.

 - `mvn compile` builds the project
 - `mvn test` runs the tests. (There are only a few unit test – feel free to add more).
 - `mvn exec:java` runs the web app.

Once the web-app is running, you can access it on [`localhost:8080`](http://localhost:8080).

## Handing in the assignment

Before you hand in your assignment, make sure that you
have included all dependencies in the file `pom.xml`, and
that your program compiles and runs well.

Once you are done, you submit the assignment on
[`mitt.uib.no`](https://mitt.uib.no/) as a link to your fork – one link per group. This means you should not commit to the
repository after the deadline has passed. Include the commit hash
of the final commit (which you can find `git log`, for instance) in
your submission on MittUiB.


## Updates

Most likely the source code of the project will be updated
while you are working on it. Therefore, it will be part of
your assignment to merge any new commits into your own branch.

## Improvements?

Have you found a non-security related bug?
Feel free to open an issue on the project GitLab page.
The best way is to make a separate `git branch` for these
changes, which do not contain your sulutions.

(This is ofcourse completely volountary – and not a graded
part of the assignment)

If you want to add your own features to the forum - feel free
to do so! If you want to share them, contact Håkon and we can
incorporate them into the main repo.

## Tasks

The tasks below has been separated out, and marked with their *approximate* weight.

### Task 0 – Authentication (2 points)

The original authentication mechanisms of Inforum was so insecure it had to be removed
immediately and all traces of the old passwords have been purged
from the database. Therefore, the code in `inf226.inforum.User`, which is
supposed to check the password, always returns `true`.

*Update the code to use a secure password authentication method in `User.checkPassword` – one
of the secure methods we have discussed
in lecture.*

Any data you need to store for the password check can be kept in the `User` class, with
appropriate updates to `storage.UserStorage`. Remember that the `User` class is *immutable*.
Any new field must be immutable and `final`
as well.

*Additionally, while the session cookie is an unguessable UUID, you must set the
correct protection flags on the session cookie.*

**Hint**: 
- We discussed password authentication in
   [lecture 09](https://hakon.gylterud.net/teaching/inf226/2019/lecture-09.pdf),
   where we among other things discussed the
   [NIST](https://pages.nist.gov/800-63-3/sp800-63b.html) (in English)
   and [NSM](https://www.nsm.stat.no/aktuelt/passordanbefalinger-fra-nasjonal-sikkerhetsmyndighet/) (in Norwegian)
   authentication guidelines and [`scrypt`](https://www.tarsnap.com/scrypt/scrypt.pdf).
 - An implementation of `scrypt` is already included as a dependency in `pom.xml`.
   If you prefer to use `argon2`, make sure to include it as well.


#### Notes – task 0

We made the authentication check in inforumServer class in getSession(...) method that the username and password is valid in the registration using regex as well as check the password in User class as we explain below. Also we set the HttpOnly flag to true to the cookies. In the User class we added password as a parameter and also we added to the query to create the User table in UserStorage.

Scrypt:
We have add the function to hash the password in InforumServer.getSession when the user is registering. And when the user is login we used the SCryptUtil.check() to check that the hashed password in the database and the password introduce by the user are the same.

### Task 1 – SQL injection (2 points)

The SQL code is currently wildly concatenating strings, leaving
it wide open to injection attacks.

*Use the techniques we hav studied to prevent
SQL injection attacks on the forum.*

#### Notes – task 1

In the next classes ForumStorage, MessageStorage, ThreadStorage, UserContextStorage, UserStorage. 
In this classes to prevent SQL injection we have used PreparedStatements to java which helps us. 
A prepared statement is synonymous with a dynamic query. It is a parameterised SQL query for the purpose of reuse 
some illegitimate querys.

We have filled the strings an ints in the querys of the classes mentioned before using the prepared statement
instead of statement avoiding the sql injection completely thank to this tool.

### Task 2 – Cross-site scripting (2.5 points)

The user interface is generated in `InforumServer`. The current
implementation is returning a lot of user data without properly
escaping it for the context it is displayed (for instance HTML body).

*Take measures to prevent XSS attacks on the forum.*

**Hint**: In addition to the books and the lecture slides, you should
take a look at the [OWASP XSS prevention cheat sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)

#### Notes – task 2

In InforumServer we have used the sanitizer from OWASP. The OWASP HTML Sanitizer is a fast and easy to configure HTML Sanitizer written in Java which lets you include HTML authored by third-parties in your web application while protecting against XSS.

We have two different policies: one doesn't allow any html code and the other only allows formatting and links (this is only used for messages between the users).

We have used them in printForumBlob(...), printThreadBlob(...), displayFrontPage(...), diplayEditMessageForm(...), displayNewThreadForm(...), printForum(...) and printThread(...)


### Task 3 – Cross-site request forgery (1 point)

While the code uses UUIDs to identify most objects, some
form actions are still susceptible to cross-site request forgery attacks
(for instane the `newthread` and the `newforum` actions.

*Implement anti-CSRF tokens on the vulnerable forms.*

**Hint:** it is OK to use the session cookie as such a token.

#### Notes – task 3

In the InforumServer.java in handle(...) we save the session token in a string called token and pass it through the application.

We have changed the headings of displayEditMessageForm(...), handleForumObject(...), displayNewForum(...), displayNewThreadForum(...), handleForum(...), handleThread(...), printForum(...) and printThread(...).

Then we have added an input hidden in the html code of all the forms.

We check that the token from the user POST is the same as the cookie session token, with this we avoid that an attacker could inject a form.

### Task 4 – Access control (2.5 points)

Inforum has no access control on operations such as *deleting a message*,
or *posting a new message*.

 - Identify which actions need access control, and decide
   on an access control model.
 - Implement your access control model.

At *minimum*, your access control system should prevent users from editing
and deleting each other's posts (unless you implement special moderator
priviledges which allows deletion). Other than that, this excercise is
open-ended – its up to you!

Depending on how you choose to go about it, you should implement access control in either the `Inforum` class or the storage classes.

#### Notes – task 4

In this section we added the access control controlling what user is the owner in the methods of Inforum.java: deleteMessage(...) and editMessage(...). We checked that the user who wants to delete or edit the message its the same user who is the owner of the message on this thread if the user is not the owner the message will not be deleted or edited.

### Task ω – Other security holes

There are more security issues in this web application.
If you find any of them, improve the source code and
explain below.

#### Notes – task ω

Here you write your notes about how this task was performed.
