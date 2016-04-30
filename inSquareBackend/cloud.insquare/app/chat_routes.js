var Message = require('./models/message');
var Square = require('./models/square');
var User = require('./models/user');
var http = require('http');
var request = require('request');
var gcm = require('node-gcm');
var async = require('async');
var geolib = require('geolib');
var apn = require('apn');
var flatten = require('flatten');
var agSender = require( "unifiedpush-node-sender" ),
    settings = {
        url: process.env.OPENSHIFT_PUSH_SERVER,
        applicationId: process.env.OPENSHIFT_PUSH_ID,
        masterSecret: process.env.OPENSHIFT_MASTER_SECRET
    };

/*
var options = {
  //pfx : '../develop/Certificati.p12',
  //cert: './keys/cert.pem',
  //key: './keys/key.pem',
  passphrase: "nick",
  ca: null,
	pfx: null,
	production: (process.env.NODE_ENV === "production"),
	voip: false,
	address: null,
	port: 2195,
	rejectUnauthorized: true,
	cacheLength: 1000,
	autoAdjustCache: true,
	maxConnections: 1,
	minConnections: 1,
	connectTimeout: 10000,
	connectionTimeout: 3600000,
	connectionRetryLimit: 10,
	buffersNotifications: true,
	fastMode: false,
	disableNagle: false,
	disableEPIPEFix: false
};
*/

module.exports = function(router, passport, squares)
{
	router.post('/recents', function(req, res){
		var squareId = req.query.squareId;
		var userId = req.query.userId;

		if(squareId!=null && squareId!=undefined && squareId!=""
			&& userId!=null && userId!=undefined && userId!=""){
				addToRecents(squareId, userId);
				res.send("Ok");
		}
		else{
			res.send("Error");
		}
	})

	router.post('/notifyFavourers', function(req, res){
		var squareId = req.query.squareId;
		var userId = req.query.userId;
		var message = req.query.message;
		if(squareId!=null && squareId!=undefined && squareId!=""
			&& userId!=null && userId!=undefined && userId!=""
		&& message!=null && message!=undefined && message!=""){
				notifyFavourers(squareId, userId, message);
				res.send("Ok");
		}
		else{
			res.send("Error");
		}
	})

	router.post('/notifyRecents', function(req, res){
		var squareId = req.query.squareId;
		var userId = req.query.userId;
		var message = req.query.message;
		if(squareId!=null && squareId!=undefined && squareId!=""
			&& userId!=null && userId!=undefined && userId!=""
			&& message!=null && message!=undefined && message!=""){
				notifyRecents(squareId, userId, message);
				res.send("Ok");
		}
		else{
			res.send("Error");
		}
	})

	router.post('/notifyEvent', function(req, res){
		var squareId = req.query.squareId;
		var userId = req.query.userId;
		var event = req.query.event;
		if(squareId!=null && squareId!=undefined && squareId!=""
			&& userId!=null && userId!=undefined && userId!=""
		&& event!=null && event!=undefined && event!=""){
				notifyEvent(userId, squareId, event);
				res.send("Ok");
		}
		else{
			res.send("Error");
		}
	})

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

/*
    router.get('/testApple', function(req, res){
      console.log("Chiamata a testApple");
      var tokens = ["<226d8087c7ceb34624eafcf99d67f69da51b7b08dd022d67388817bcb0c77efe>",
       "226d8087c7ceb34624eafcf99d67f69da51b7b08dd022d67388817bcb0c77efe",
       "226d8087 c7ceb346 24eafcf9 9d67f69d a51b7b08 dd022d67 388817bc b0c77efe",
     "<226d8087 c7ceb346 24eafcf9 9d67f69d a51b7b08 dd022d67 388817bc b0c77efe>"];

      var myDevice1 = new apn.Device("226d8087c7ceb34624eafcf99d67f69da51b7b08dd022d67388817bcb0c77efe");
      console.log("Device1");
      console.log(myDevice1);

      var options = {};
      var service = new apn.Connection(options);
      console.log("1:");
      console.log(service);
      var note = new apn.Notification();
      //note.retryLimit(5);
      note.setAlertText("Hello, from node-apn!");
      note.badge = 1
      note.expiry = Math.floor(Date.now() / 1000) + 3600; // Expires 1 hour from now.
      note.badge = 3;
      note.sound = "ping.aiff";
      console.log("2:");
      console.log(note);
      service.pushNotification(note, myDevice1);

    })
*/
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
	});

findApnTokensByFavouritesSquares(squareId, userId, function(apnTokens) {
  User.findById(userId, function(err, user) {
    if(err) console.log(err);
    var name;
    if(user.google.name!=null && user.google.name!="" && user.google.name!=undefined)
      name = user.google.name;
    else if(user.facebook.name!=null && user.facebook.name!="" && user.facebook.name!=undefined)
      name = user.facebook.name;
    if (apnTokens!=undefined && apnTokens!=null && apnTokens!=[]){
      if(apnTokens.length>0){

      var apnMessage = {};
      apnMessage.alert = name + ': ' + text;
      apnMessage.sound = "default";
      apnMessage.badge = 1;
      console.log("APN Favourite SquareId: " + squareId);
      apnMessage.userData = {};
      apnMessage.userData.squareId = squareId;

      var options = {};
      options.criteria = {};
      options.criteria.categories = null;
      options.criteria.variants = null;
      options.criteria.alias = apnTokens;
      options.criteria.deviceType = null;
      options.config = {};
      options.config.ttl = -1;

      agSender.Sender( settings ).send( apnMessage, options ).on( "success", function( response ) {
          console.log( "success called", response );
      });
    }
  }
})
});
}
  /*
  findApnTokensByFavouritesSquares(squareId, userId, function(apnTokens) {
    Square.findById(squareId, function(err, square) {
			if(err) console.log(err);
			User.findById(userId, function(err, user) {
				if(err) console.log(err);
				var name;
				if(user.google.name)
					name = user.google.name;
				else if(user.facebook.name)
					name = user.facebook.name;
				var service = new apn.connection(options);
        service.on("connected", function() {
          console.log("Connected");
        });
        service.on("transmitted", function(notification, device) {
          console.log("Notification transmitted to:" + device.token.toString("hex"));
        });
        service.on("transmissionError", function(errCode, notification, device) {
          console.error("Notification caused error: " + errCode + " for device ", device, notification);
          if (errCode === 8) {
            console.log("A error code of 8 indicates that the device token is invalid. This could be for a number of reasons - are you using the correct environment? i.e. Production vs. Sandbox");
          }
        });
        service.on("timeout", function () {
          console.log("Connection Timeout");
        });
        service.on("disconnected", function() {
          console.log("Disconnected from APNS");
        });
        service.on("socketError", console.error);

        var note = new apn.notification();
        //note.retryLimit(5);
        note.setAlertText("Hello, from node-apn!");
        note.badge = 1;
        service.pushNotification(note, apnTokens);
			})
		})
  })
  */


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
  findApnTokensByRecentSquares(squareId, userId, function(apnTokens) {
    User.findById(userId, function(err, user) {
      if(err) console.log(err);
      var name;
      if(user.google.name!=null && user.google.name!="" && user.google.name!=undefined)
        name = user.google.name;
      else if(user.facebook.name!=null && user.facebook.name!="" && user.facebook.name!=undefined)
        name = user.facebook.name;
      if (apnTokens!=undefined && apnTokens!=null && apnTokens!=[]){
        if(apnTokens.length>0){
          var apnMessage = {};
          apnMessage.alert =  name + ': ' + text;
          apnMessage.sound = "default";
          apnMessage.badge = 1;
          console.log("APN Recent SquareId: " + squareId);
          apnMessage.userData = {};
          apnMessage.userData.squareId = squareId;

          var options = {};
          options.criteria = {};
          options.criteria.categories = null;
          options.criteria.variants = null;
          options.criteria.alias = apnTokens;
          options.criteria.deviceType = null;
          options.config = {};
          options.config.ttl = -1;

          agSender.Sender( settings ).send( apnMessage, options ).on( "success", function( response ) {
              console.log( "success called", response );
            });
          }
        }
    })
  });
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
      if(item.fields != undefined) {
        gcmTokens.push(item.fields.gcmToken[0]);
      }
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
      if(item.fields != undefined) {
        gcmTokens.push(item.fields.gcmToken[0]);
      }
			callback();
		}, function(err){
				if(err) console.log(err);
				callback(gcmTokens);
		}.bind({callback : callback}));
	});
}

function findApnTokensByFavouritesSquares(squareId, userId, callback) {
  User.search({
		bool: {
			must: [
				{
					term:{
						favourites: squareId
					}
				},
        {
          term: {
            	apnToken: "true"
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
		fields: ["_id"]
	},function(err, result){
		if (err) console.log(err);

		var apnTkns=[];

		async.forEachOf(result.hits.hits, function(item, k, callback){
      if(item != undefined) {
        apnTkns.push(item._id);
      }
			callback();
		}, function(err){
				if(err) console.log(err);
        console.log("Token APN");
        console.log(apnTkns);
				callback(apnTkns);
		}.bind({callback : callback}));


	});
}

function findApnTokensByRecentSquares(squareId, userId, callback) {
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
		fields: ["_id"]
 	}, function(err, result) {
		if (err) console.log(err);
		var apnTkns=[];
		async.forEachOf(result.hits.hits, function(item, k, callback){
      if(item != undefined) {
        apnTkns.push(item._id);
      }
			callback();
		}, function(err){
				if(err) console.log(err);
        console.log("Token APN");
        console.log(apnTkns);
				callback(apnTkns);
		}.bind({callback : callback}));
	})
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
