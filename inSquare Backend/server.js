var express = require('express');
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var mongoose = require('mongoose');
var mongoosastic = require('mongoosastic');
var elasticsearch = require('elasticsearch');
var passport = require('passport');
var flash = require('connect-flash');

var morgan = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require("body-parser");
var session = require('express-session');

/*
  Setup delle variabili prese dall'environment
*/
var server_port = process.env.OPENSHIFT_NODEJS_PORT || 8080;
var server_ip_address = process.env.OPENSHIFT_NODEJS_IP || '127.0.0.1';
var connection_string = 'mongodb://localhost:27017/insquare';
if (process.env.OPENSHIFT_MONGODB_DB_URL) {
  connection_string = process.env.OPENSHIFT_MONGODB_DB_URL +
                      process.env.OPENSHIFT_APP_NAME;
}

app.use(morgan('dev'));
app.use(cookieParser());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());
app.use(session({
  secret: 'supersecret',
  resave: false,
  saveUninitialized: false
}));
app.use(passport.initialize());
app.use(passport.session());
app.use(flash());

require('./config/passport')(passport);

app.set('view engine', 'ejs');

var router = express.Router();

// CONNESSIONE AL SERVER
mongoose.connect(connection_string, function(err) {
  console.log(connection_string);
  if (err) {
    console.error(err);
  }
  console.log('connected.... unless you see an error the line before this!');
});

http.listen(server_port, server_ip_address, function () {
    console.log("Example app listening at http://%s:%s", server_ip_address, server_port);
});

var client = new elasticsearch.Client({
  host: 'http://elastic-insquare.rhcloud.com',
  log: 'trace'
});

// client.ping({
//   // ping usually has a 3000ms timeout
//   requestTimeout: Infinity,
//   // undocumented params are appended to the query string
//   hello: "elasticsearch!"
// }, function(error) {
//   if (error) {
//     console.trace('elasticsearch cluster is down!');
//   } else {
//     console.log('All is well');
//   }
// });
var Message = require('./app/models/message')
var numUsers = 0;

io.on('connection', function (socket) {
  var addedUser = false;

  // when the client emits 'new message', this listens and executes
  socket.on('new message', function (msg) {
    // we tell the client to execute 'new message'
    //io.emit('chat message', msg) ??;
    socket.broadcast.emit('new message', {
      username: socket.username,
      message: msg
    });
    var mess = new Message();
    mess.text = msg;
    mess.createdAt = new Date();
    mess.senderId = socket.id;
    mess.senderEmail = socket.email;
    //la query per salvare su elasticsearch, cambiare se necessario
    mess.save(function(err) {
      if(err) throw err;
      return mess;
    });
  });

  // when the client emits 'add user', this listens and executes
  socket.on('add user', function (username) {
    if (addedUser) return;

    // we store the username in the socket session for this client
    socket.username = username;
    ++numUsers;
    addedUser = true;
    socket.emit('login', {
      numUsers: numUsers
    });
    // echo globally (all clients) that a person has connected
    socket.broadcast.emit('user joined', {
      username: socket.username,
      numUsers: numUsers
    });
  });

  // when the client emits 'typing', we broadcast it to others
  socket.on('typing', function () {
    socket.broadcast.emit('typing', {
      username: socket.username
    });
  });

  // when the client emits 'stop typing', we broadcast it to others
  socket.on('stop typing', function () {
    socket.broadcast.emit('stop typing', {
      username: socket.username
    });
  });

  // when the user disconnects.. perform this
  socket.on('disconnect', function () {
    if (addedUser) {
      --numUsers;

      // echo globally that this client has left
      socket.broadcast.emit('user left', {
        username: socket.username,
        numUsers: numUsers
      });
    }
  });
});
require('./app/routes.js')(router,passport);

app.use(router);
