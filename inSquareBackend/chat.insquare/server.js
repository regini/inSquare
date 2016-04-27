var express = require('express');
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var connect = require('connect');

// Path for the chat sockets
var squares = io.of('/squares');
// =========================

var mongoose = require('mongoose');
var mongoosastic = require('mongoosastic');
var elasticsearch = require('elasticsearch');

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
var connection_string = process.env.OPENSHIFT_MLAB_MONGO;



//Log delle get e delle post ricevute
app.use(morgan('dev'));

//Gestisce sessione su browser web
app.use(cookieParser());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());
app.use(session({
  secret: 'supersecret',
  resave: false,
  saveUninitialized: false
}));

app.use(flash());
app.set('json spaces', 4);



app.set('view engine', 'ejs');

app.use(function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*");
  res.header('Access-Control-Allow-Methods', 'GET, POST, PATCH, DELETE');
  res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
  next();
});

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
    console.log("App listening");
});




// ROUTES FOR THE CHATS
require('./app/chat_service.js')(router, squares);
// ====================


app.use(router);
