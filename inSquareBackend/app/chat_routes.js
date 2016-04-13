var Message = require('./models/message');
var Square = require('./models/square');
var User = require('./models/user');
var http = require('http');
var request = require('request');
var gcm = require('node-gcm');
var async = require('async');
var geolib = require('geolib');

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
				Square.findById(data.room, function(err, sqr) {
					if (err) console.log(err);
					sqr.views = sqr.views+1;
					sqr.save(function(err) {
						if(err) console.log(err);
					});
				});

				//socket.to(data.room).broadcast.emit('newMessage', serverMessage);
			}
		});

		socket.on('pong', function(data){

		});

		socket.on('tellRoom', function( msg, roomName ) {
			socket.to( roomName ).emit('heyThere', msg, socket.id );
		});

		socket.on('sendMessage', function(msg, callback)
		{
			var message = {};
			var room = msg.room;

			if(room != "" || room != null || room != undefined) {
				console.log("Room: " + room);
				console.log("UserId: " + msg.userid);
				console.log(msg.username + " said: " + msg.message);

				message.room = msg.room;
				message.userid = msg.userid;
				message.username = msg.username;
				message.contents = msg.message;

				User.findById(msg.userid, function(err,usr) {
					if(err) console.log(err);
					if(usr.google.name)
						message.profile = usr.google.profilePhoto;
					else if(usr.facebook.name)
						message.profile = usr.facebook.profilePhoto;
						sendMessage(msg.message, msg.userid, room, function(m) {
							message.msg_id = m._id;
							message.userSpot = false;
							socket.to(room).broadcast.emit('newMessage', message);
							callback(message);
						}.bind({message : message}));
				})

				// socket.emit('newMessage', data.user, data.message);
				addToRecents(room, msg.userid);
				notifyFavourers(room, msg.userid, msg.message);
				notifyRecents(room, msg.userid, msg.message);

				/*  VECCHIA POST PER PUSH
				request({
					method: 'POST',
    			uri: 'https://android.googleapis.com/gcm/send',
    			headers: {
        		'Content-Type': 'application/json',
						//Chiave di test
        		'Authorization':'key=AIzaSyBzr7mpZzDMqdADEZvqK4733UkJewb0O0A'
    			},
    			body: JSON.stringify({
  					"data" : {"message":msg.message},
						"to":"/topics/global"})
    			},
					function (error, response, body) {
						if(error) console.log(error);
    			}
  			)
				*/

				// Nuova versione push
				/*
				var sender = new gcm.Sender("AIzaSyAyKlOD_EyfZBP2vDEOusMq97W_gE_aQzA");
				var message = new gcm.Message();
				message.addData("message", msg.message);

				sender.send(message, {topic: '/topics/global'}, function(err, response){
					if (err)
						console.log(err);
					console.log(response);
				});
				*/
			}
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
			//socket.broadcast.to(socket.room).emit('newMessage', serverMessage);
			socket.leave(socket.room);
		});

		setTimeout(sendHeartbeat, 8000);
	});

		/*
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
		*/

    // Get Messaggi Recenti con parametri:
    // - size = quantita'
    // - squareId = da quale Square
	//Documentata
	router.get('/messages?', function(req, res)
    {
    	if((req.query.size!="" || req.query.size!=null || req.query.size!=undefined)
    		&& req.query.recent=='true' &&
    		(req.query.square!="" || req.query.square!=null || req.query.square!=undefined)){

				Message.find({'squareId' : req.query.square})
				.limit(req.query.size)
				.sort('-createdAt')
				.populate('senderId')
				.populate('squareId')
				.select('senderId squareId text createdAt')
				.exec(function(err,messages) {
					if(err) console.log(err);
					var result = [];
					if(messages.length != 0) {
						console.log(messages);
						if(messages[0].squareId.geo_loc!=undefined){
							var squareLocation = (messages[0].squareId.geo_loc).split(',');
							var squareLocationObject = {};
							squareLocationObject.latitude = squareLocation[0];
							squareLocationObject.longitude = squareLocation[1];
						}

						for(var i = 0; i<messages.length; i++) {
							if(messages[i].senderId!=undefined && messages[i].senderId!=null && messages[i].senderId!=""){
								var msg = {}
								if(messages[i].senderId.google.name) {
									msg.name = messages[i].senderId.google.name;
									msg.picture = messages[i].senderId.google.profilePhoto;
								}
								else if(messages[i].senderId.facebook.name) {
									msg.name = messages[i].senderId.facebook.name;
									msg.picture = messages[i].senderId.google.profilePhoto;
								}
								msg.userSpot = 'false';
								if(messages[i].senderId.lastLocation!=undefined){
									var userLocation = (messages[i].senderId.lastLocation).split(',');
									var userLocationObject = {};
									userLocationObject.latitude = userLocation[0];
									userLocationObject.longitude = userLocation[1];

									//482 metri è la distanza da n1 a n11 roma tre
									if(geolib.getDistance(squareLocationObject, userLocationObject)<482){
										msg.userSpot = 'true';
									}
								}


								msg.text = messages[i].text;
								msg.createdAt = messages[i].createdAt;
								msg.msg_id = messages[i].id;
								msg.from = messages[i].senderId.id;
								result.push(msg);
								}
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

		//Documentata
		router.get('/profilePictures/:userId', function(req, res) {
			var userId = req.params.userId;
			User.findById(userId, function(err, user) {
				if(err) console.log(err);
				if(user.google.name)
					res.send(user.google.profilePhoto);
				else if(user.facebook.name)
					res.send(user.facebook.profilePhoto);
			})
		})

	// CHATS
	// =====
	// router.use('/chat', function(req, res, next)
	// {
	// 	var id = req.query.squareId;
	// 	next();
	// });
	/*
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
*/
	function sendHeartbeat(){
	    setTimeout(sendHeartbeat, 8000);
	    squares.emit('ping', { beat : 1 });
	}
};

function addToRecents(squareId, userId) {
	Square.findById(squareId, function(err,square) {
		if(err) console.log(err);
		User.findById(userId, function(err,user) {
			if(err) console.log(err);
			if(user.recents == undefined) {
				user.recents.push({'square' : square, 'lastUserMessage' : new Date().getTime()});
			} else {
				indexSquare(square.id, user.recents, function(result) {
					console.log(result);
					if(result == -1) {
						user.recents.push({'square' : square, 'lastUserMessage' : new Date().getTime()});
					} else {
						user.recents[result].lastUserMessage = new Date().getTime();
					}
					user.save();
				})
			}
		})
	})
}

function indexSquare(squareId, squares, callback) {
	console.log(squareId + ' | ' + squares);
	for(var i = 0; i<squares.length; i++) {
		if(squares[i].square == squareId) {
			console.log(squares[i].square + ' | ' + squareId);
			callback(i);
			return;
		}
	}
	callback(-1);
}

function isLoggedIn(req, res, next)
{
	//if user is auth-ed, go on
	if(req.isAuthenticated())
		return next();

	// else redirect to home page
	res.redirect('/');
}

function sendMessage(text, user, square, callback)
{
	var mes = new Message();

	mes.text = text;
	mes.createdAt = (new Date()).getTime();
	mes.senderId = user;
	mes.squareId = square;

	User.findOne({'_id' : user}, function(err, usr) {
		if(err) console.log(err);
		usr.messages.push(mes);
		usr.save();
	});

	Square.findOne({'_id' : square}, function(err, sqr) {
		if(err) console.log(err);
		sqr.messages.push(mes);
		sqr.lastMessageDate = mes.createdAt;
		sqr.save(function(err) {
			if(err) console.log(err);
			getCurrentState(sqr.id, function(state) {
	      this.sqr.state = state;
				console.log("State : " + this.sqr.state);
	      this.sqr.save();
	    }.bind({sqr : sqr}))
			notifyEvent(user, square, "update");
		})
	});

	mes.save(function(err) {
		if(err) console.log(err);
		callback(mes);
		console.log(mes);
	}.bind({callback : callback}));

}

function getRecentMessages(square,size) {
	Message.find({'squareId' : square})
	.limit(size)
	.sort('-createdAt')
	//.populate('senderId')
	.exec(function(err,messages) {
		if(err) console.log(err);
		return messages;
	})
};

function deleteMessage(messageId) {
	Message.findById(messageId, function(err, message) {
		if(err) console.log(err);
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

function notifyFavourers(squareId, userId, text) {
	findPushTokenByFavouriteSquares(squareId, userId, function(gcmTokens) {
		Square.findById(squareId, function(err, square) {
			if(err) console.log(err);
			User.findById(userId, function(err, user) {
				if(err) console.log(err);
				var sender = new gcm.Sender(process.env.GCM_SERVER_TOKEN);
				var message = new gcm.Message();
				var name;
				if(user.google.name)
					name = user.google.name;
				else if(user.facebook.name)
					name = user.facebook.name;
				message.addData("message", name + ': ' + text);
				message.addData("squareName", square.name);
				message.addData("squareId", square.id);
				sender.send(message, {registrationTokens: gcmTokens}, function(err, response) {
					if (err) console.log(err);
					console.log(response);
				})
			})
		})
	})
}

function notifyRecents(squareId, userId, text) {
	findPushTokensByRecentSquares(squareId, userId, function(gcmTokens) {
		Square.findById(squareId, function(err, square) {
			if(err) console.log(err);
			User.findById(userId, function(err, user) {
				if(err) console.log(err);
				var sender = new gcm.Sender(process.env.GCM_SERVER_TOKEN);
				var message = new gcm.Message();
				var name;
				if(user.google.name)
					name = user.google.name;
				else if(user.facebook.name)
					name = user.facebook.name;
				message.addData("message", name + ': ' + text);
				message.addData("squareName", square.name);
				message.addData("squareId", square.id);
				sender.send(message, {registrationTokens: gcmTokens}, function(err, response) {
					if (err) console.log(err);
					console.log(response);
				})
			})
		})
	})
}

function findPushTokensByRecentSquares(squareId, userId, callback) {
	User.search({
		bool : {
			must : [
				{
					match : {
						'recents.square' : squareId
					}
				},
				{
					range : {
						'recents.lastUserMessage' : {
							gte : "now-3d",
							lte : "now"
						}
					}
				}
			],
			must_not: [
				{
					ids: {
						values: [userId]
					}
				},
				{
    			term: {
        		'mute.square': squareId
    			}
				},
				{
					match : {
						favourites : squareId
					}
				}
			]
		}
	},
	{
		fields: ["gcmToken"]
 	}, function(err, result) {
		if (err) console.log(err);
		var gcmTokens=[];
		async.forEachOf(result.hits.hits, function(item, k, callback){
			gcmTokens.push(item.fields.gcmToken[0]);
			callback();
		}, function(err){
				if(err) console.log(err);
				callback(gcmTokens);
		}.bind({callback : callback}));
	})
}

function findPushTokenByFavouriteSquares(squareId, userId, callback){
	User.search({
		bool: {
			must: [
				{
					term:{
						favourites: squareId
					}
				}
			],
			must_not:
			[
				{
					ids: {
						values: [userId]
					}
				},
				{
    			term: {
        		'mute.square': squareId
    		}
			}
		]
		}
	},
	{
		fields: ["gcmToken"]
	}, function(err, result){
		if (err) console.log(err);
		var gcmTokens=[];
		async.forEachOf(result.hits.hits, function(item, k, callback){
			gcmTokens.push(item.fields.gcmToken[0]);
			callback();
		}, function(err){
				if(err) console.log(err);
				callback(gcmTokens);
		}.bind({callback : callback}));
	});
}

function getCurrentState(square, callback) {
  Message.esCount({
    bool : {
      must : [
        {
          match : {
            squareId : square
          }
        },
        {
          range : {
            createdAt : {
              gte : "now-1d",
              lte : "now"
            }
          }
        }
      ]
    }
  }, function(err,response) {
    if(err) console.log(err);
    getState(response.count, function(state) {
      callback(state);
    });
  })
}

function getState(count, callback) {
  if(count == 0) {
    callback("asleep");
  }
  else if(count > 10) {
    callback("caffeinated");
  }
  else {
    callback("awoken");
  }
}

function notifyEvent(userId, squareId, event) {
  var sender = new gcm.Sender(process.env.GCM_SERVER_TOKEN);
  var message = new gcm.Message();
  message.addData("event", event);
  message.addData("userId", userId);
  message.addData("squareId",squareId);

  sender.send(message, {topic: '/topics/global'}, function(err, response){
    if (err)
      console.log(err);
    console.log(response);
  });
}
