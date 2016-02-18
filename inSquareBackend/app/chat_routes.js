var Message = require('./models/message');
var Square = require('./models/square');
var User = require('./models/user');
var http = require('http');

module.exports = function(router, passport, squares)
{
	squares.on('connection', function(socket)
	{
		socket.on('addUser', function(data)
		{
			var room = data.room;

			if(room != "" || room != null || room != undefined)
			{
				console.log("%j", data);
				socket.username = data.username;
				socket.userid = data.userid;
				socket.room = room;
				socket.username = data.username;

				// newMessage event must emit an object that contains
				// 1 - room
				// 2 - userid
				// 3 - username
				// 4 - message
				var serverMessage = {};
				serverMessage.room = room;
				serverMessage.userid = "0";
				serverMessage.username = "Server";
				serverMessage.contents = data.username + " is now connected";

				socket.join(data.room);

				socket.to(data.room).broadcast.emit('newMessage', serverMessage);
			}

		});

		socket.on('tellRoom', function( msg, roomName ) {
			socket.to( roomName ).emit('heyThere', msg, socket.id );
		});

		socket.on('sendMessage', function(msg)
		{
			var message = {};
			var room = msg.room;

			if(room != "" || room != null || room != undefined)
			{
				console.log("Room: " + room);
				console.log("UserId: " + msg.userid);
				console.log(msg.username + " said: " + msg.message);

				message.room = msg.room;
				message.userid = msg.userid;
				message.username = msg.username;
				message.contents = msg.message;

				// socket.emit('newMessage', data.user, data.message);
				socket.to(room).emit('newMessage', message);

				sendMessage(msg.message, msg.userid, room);
			};
		});

		socket.on('disconnect', function()
		{
			console.log(socket.username + " disconnected");

			var data = {};
			data.user = socket.user;

			var serverMessage = {};
			serverMessage.room = socket.room;
			serverMessage.userid = "0";
			serverMessage.username = "Server";
			serverMessage.contents = socket.username + " is now disconnected";
			console.log("Message %j", serverMessage);
			socket.broadcast.to(socket.room).emit('newMessage', serverMessage);
			socket.leave(socket.room);
		});
	});

	// Send messages
    router.post('/send_message', function(req, res)
    {
    	console.log("Trying to send a message!");
			var text = req.query.message;
			var square = req.query.square;
			var user = req.user;
			/*var email = (function()
			{
				if(req.user.local.email)
					return req.user.local.email;
				if(req.user.facebook.email)
					return req.user.facebook.email;
				if(req.user.google.email)
					return req.user.google.email;
			}) ();*/

			sendMessage(text, user, square);

			res.send(req.user + " created a new message!");
    });

    router.get('/get_messages', isLoggedIn, function(req, res)
    {
    	// If url is http://<...>/get_messages/squareId=Sapienza
		// it'll get the value in the parameter (->Sapienza)
    	var squareId = req.query.squareId;
    	var senderId = req.user.id;

    	if(senderId && squareId)
    	{
	    	console.log(squareId);
	    	console.log(senderId);

	    	var params = { 'senderId':senderId, 'squareId':squareId };

	    	var query = findMessages(params, res);
	    	query.exec(
	    		function(err,messages)
		    	{
		    		if(err) throw err;

		    		res.send(messages[0]);
		    	});

    	}else if(senderId)
    	{
    		console.log(senderId);
	    	var params = { 'senderId':senderId };
			var query = findMessages(params, res);
	    	query.exec(
	    		function(err,messages)
		    	{
		    		if(err) throw err;

		    		res.send(messages[0]);
		    	});

    	}else if(squareId)
    	{
    		console.log(squareId);
	    	var params = { 'squareId':squareId};
	    	var query = findMessages(params, res);
	    	query.exec(
	    		function(err,messages)
		    	{
		    		if(err) throw err;

		    		res.send(messages);
		    	});

    	}
    	else
    	{
    		console.log("No parameters!");
			var query = findMessages({}, res);
	    	query.exec(
	    		function(err,messages)
		    	{
		    		if(err) throw err;

		    		res.send(messages);
		    	});

	    }
    });


    // Get Messaggi Recenti con parametri:
    // - size = quantita'
    // - squareId = da quale Square
	router.get('/messages?', function(req, res)
    {
    	if((req.query.size!="" || req.query.size!=null || req.query.size!=undefined)
    		&& req.query.recent &&
    		(req.query.square!="" || req.query.square!=null || req.query.square!=undefined)){

				Message.find({'squareId' : req.query.square})
				.limit(req.query.size)
				.sort('-createdAt')
				.populate('senderId')
				.select('senderId text createdAt')
				.exec(function(err,messages) {
					if(err) throw err;
					var result = [];
					if(messages.length != 0) {
						console.log(messages);
						for(var i = 0; i<messages.length; i++) {
							var msg = {}
							if(messages[i].senderId.google.name)
								msg.name = messages[i].senderId.google.name;
							else if(messages[i].senderId.facebook.name)
								msg.name = messages[i].senderId.facebook.name;
							msg.text = messages[i].text;
							msg.createdAt = messages[i].createdAt;
							msg.msg_id = messages[i].id;
							msg.from = messages[i].senderId.id;
							console.log("%j", msg);
							result.push(msg);
						}
					}
					res.header("Cache-Control", "no-cache, no-store, must-revalidate");
					res.header("Pragma", "no-cache");
					res.removeHeader('ETag');
					res.header("expires", 0);

					res.json(result);
				})
	  	} else {
				res.send("Malformed query");
			}
		});

	// CHATS
	// =====
	// router.use('/chat', function(req, res, next)
	// {
	// 	var id = req.query.squareId;
	// 	next();
	// });
	router.get('/chat', function(req, res)
	{
		// If url is http://<...>/chat?squareId=Sapienza
		// it'll get the value in the parameter (->Sapienza)
		var squareId = req.query.squareId;
		var userid = req.query.userId;
		var username = req.query.username;

		if(squareId == undefined)
		{
			console.log("No Id specified");
		}

		console.log("Currently in " + squareId);

		res.render('chat2ejs', {
			data: {
				user: userid,
				username: username,
				room: squareId
			}
		});
	});
};

function isLoggedIn(req, res, next)
{
	//if user is auth-ed, go on
	if(req.isAuthenticated())
		return next();

	// else redirect to home page
	res.redirect('/');
}

function sendMessage(text, user, square)
{
	var mes = new Message();

	mes.text = text;
	mes.createdAt = (new Date()).getTime();
	mes.senderId = user;
	mes.squareId = square;

	User.findOne({'_id' : user}, function(err, usr) {
		if(err) throw err;
		usr.messages.push(mes);
		usr.save();
	});

	Square.findOne({'_id' : square}, function(err, sqr) {
		if(err) throw err;
		sqr.messages.push(mes);
		sqr.save();
	});

	mes.save(function(err)
	{
		if(err) throw err;
		console.log(mes);
	});

}

function getRecentMessages(square,size) {
	Message.find({'squareId' : square})
	.limit(size)
	.sort('-createdAt')
	//.populate('senderId')
	.exec(function(err,messages) {
		if(err) throw err;
		return messages;
	})
};

function deleteMessage(messageId) {
	Message.findById(messageId, function(err, message) {
		message.remove(function(err, message) {
	  	if (err) {
	    	console.log(err);
	      return;
	    }
	  });
	});
};

function findMessages(params)
{
	var vals = 	Message.find(params);
	return vals;
}
