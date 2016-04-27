var Message = require('./models/message');
var Square = require('./models/square');
var User = require('./models/user');
var http = require('http');
var request = require('request');
var gcm = require('node-gcm');
var async = require('async');
var geolib = require('geolib');

module.exports = function(router, squares)
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
			}
		});

		socket.on('pong', function(data){

		});

		socket.on('tellRoom', function( msg, roomName ) {
			socket.to( roomName ).emit('heyThere', msg, socket.id );
		});

		socket.on('sendMessage', function(msg, callback)
		{
			if(msg.message=="bot"){
				var message = {};
				message.userid = "123";
				message.date = (new Date()).getTime();
				message.username = "inSquare";
				message.contents = "Non ci sono ancora! Ma arriveranno presto!!";
				message.msg_id = "123";
				message.userSpot = false;
				message.room = msg.room;
				message.profile = "http";
				socket.to(msg.room).broadcast.emit('newMessage', message);
				callback(message);
			} else {
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
							message.date = m.createdAt;
							socket.to(room).broadcast.emit('newMessage', message);
							callback(message);
						}.bind({message : message}));
				})


				addToRecents(room, msg.userid);
				notifyFavourers(room, msg.userid, msg.message);
				notifyRecents(room, msg.userid, msg.message);

			}
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


	router.get('/', function(req, res) {
				res.send("Socket.io Up and Running!");
	});

	function sendHeartbeat(){
	    setTimeout(sendHeartbeat, 8000);
	    squares.emit('ping', { beat : 1 });
	}
};


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

function addToRecents(squareId, userId) {
	request({
			method: 'POST',
			uri: 'http://insquare-insquare.rhcloud.com/recents?squareId=' + squareId + "&userId=" + userId
		})
}

function notifyFavourers(squareId, userId, text) {
	request({
			method: 'POST',
			uri: 'http://insquare-insquare.rhcloud.com/notifyFavourers?squareId=' + squareId + "&userId=" + userId + "&message=" + text
		})
}

function notifyRecents(squareId, userId, text) {
	request({
			method: 'POST',
			uri: 'http://insquare-insquare.rhcloud.com/notifyRecents?squareId=' + squareId + "&userId=" + userId + "&message=" + text
		})
}

function notifyEvent(userId, squareId, event) {
	request({
			method: 'POST',
			uri: 'http://insquare-insquare.rhcloud.com/notifyEvent?squareId=' + squareId + "&userId=" + userId + "&event=" + event
		})
}
