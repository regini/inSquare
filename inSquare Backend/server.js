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

io.on('connection', function(socket,req,res) {
  console.log('a user connected');
  io.emit('user connected');
  socket.on('chat message', function(msg){
    io.emit('chat message', msg);
    client.create({
      index: 'message',
      type: 'messages',
      body: {
        text: msg,
        user: '',
        square:"Roma",
        timestamp: new Date()
      }
    });
  });
  socket.on('disconnect', function() {
    io.emit('user disconnected')
    console.log('user disconnected');
  });
});

require('./app/routes.js')(router,passport);

app.use(router);
