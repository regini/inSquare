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
				socket.username = data.user;
				socket.room = data.room;
				socket.join(data.room);
			}

		});

		socket.on('tellRoom', function( msg, roomName ) {
			socket.to( roomName ).emit('heyThere', msg, socket.id );
		});

		socket.on('sendMessage', function(data)
		{
			var message = {};
			var room = data.room;
			if(room != "" || room != null || room != undefined)
			{
				console.log("Room: " + room);

				console.log(data.user + " said: " + data.message);

				message.room = data.room;
				message.user = data.user;
				message.contents = data.message;

				// socket.emit('newMessage', data.user, data.message);
				socket.broadcast.emit('newMessage', message);

				sendMessage(data.message, data.user, data.email, room);
			}
		});

		socket.on('disconnect', function()
		{
			console.log(socket.username + " disconnected");
			var data = {};
			data.user = socket.username;
			// socket.broadcast.emit('newMessage', 'Server', socket.username + " is now disconnected");
			socket.broadcast.emit('userLeft', data);
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

			sendMessage(text, user, email, square);

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


	router.get('/messages?', function(req, res)
    {
    	if((req.query.size!="" || req.query.size!=null || req.query.size!=undefined)
    		&& req.query.recentMessages==true && 
    		(req.query.square!="" || req.query.square!=null || req.query.square!=undefined) ){

    	var messages = getRecentMessages(req.body.square,req.body.size);
	    	
	    res.json(messages);
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
		var username = req.query.user;

		if(squareId == undefined)
		{
			console.log("No Id specified");
			squareId = "Home";
		}

		console.log("Currently in " + squareId);

		res.render('chat.ejs',
		//Passing JS objects to the ejs template
		{
			user: username,
			squareId: squareId
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
	})

	Square.findOne({'_id' : square}, function(err, sqr) {
		if(err) throw err;
		sqr.messages.push(mes);
		sqr.save();
	})

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
	.exec(function(err,messages) {
		if(err) throw err;
		return messages;
	})
};

function findMessages(params)
{
	var vals = 	Message.find(params);
	return vals;
}
