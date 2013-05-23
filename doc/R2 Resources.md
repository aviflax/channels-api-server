# R2 Resources

/groups
/groups/{group}
/groups/{group}/topics
/groups/{group}/topics/{topic}
/groups/{group}/topics/{topic}/messages
/groups/{group}/topics/{topic}/messages/{message}
/groups/{group}/hooks
/groups/{group}/hooks/{hook}

* But wait — what about topics/threads/discussions?
* Let’s think about this — what works for Google Groups? A Group is essentially a topic. It’s a set of people who are talking about a specific broad topic (like Node or Java) with lots of more specific discussions — i.e. threads
* So we need to support some kind of threading or discussions, but it needs to be WAY easy to create one — it needs to be a super-lightweight action
* With a mailing list, you just send a new message to the list with a subject. The subject becomes the topic of the thread. People participate in the thread simply by replying to any message in the thread. All replies always belong to a specific thread. You fork a thread by changing the subject.
* With twitter, a thread is a tweet any any replies to it, and any replies to them, etc. the topic is whatever is discussed. You don’t have a subject field so the cognitive overhead of starting a discussion is lower. (This is good and bad.)
* With Discourse, discussions are called topics. Topics can be assigned categories, but they don’t necessarily live in them — one can see a list of all topics across all categories (that’s the default view). Categories are basically tags.
* So there definitely needs to be a way to get a list of threads/discussions
* And when retrieving data, that should be how they’re organized/grouped
* But when starting a discussion it needs to be super easy

## Cross-medium Equivalents

* Server
	* email: none
	* IRC: server
	* IM: ?
* Group
	* email: mailing list
	* IRC: channel
	* IM: chat room
Discussion
	* email: thread
	* IRC: tree of messages based on @-replies
	* IM: tree of messages based on @-replies

## Starting a discussion

* Email: you send an email to the group. e.g. `all@rr.arc90.com`
 	* the subject of the email becomes the subject of the discussion
* IRC: you post a message in the channel for the group, e.g. channel “all” on server rr.arc90.com
 	* the first N chars of the message are used as the subject of the discussion
* IM: you post a message in the “chat room” for the group
 	* the first N chars of the message are used as the subject of the discussion
* web/native apps: after choosing a group, you get a form with a subject and a body field

## Viewing Discussions

* Email: threading provided by mail clients
* IRC: join the channel, see incoming messages — no threading
* IM: join the chat room, see incoming messages — no threading

## Replying to a message

* Email: just reply to the message
* IRC/IM use @name or name and your message will be considered a reply to that person’s most recent message. Maybe use fuzzy name matching.
* IM: 