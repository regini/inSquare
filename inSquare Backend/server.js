var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var mysql = require('mysql');
var vsprintf = require("sprintf-js").vsprintf;

/*
  Setup delle variabili prese dall'environment
*/
var server_port = process.env.OPENSHIFT_NODEJS_PORT || 8080;
var server_ip_address = process.env.OPENSHIFT_NODEJS_IP || '127.0.0.1';
var db_host = process.env.OPENSHIFT_MYSQL_DB_HOST;  // ||localhost da testare
var db_port = process.env.OPENSHIFT_MYSQL_DB_PORT; //idem
var db_username = process.env.OPENSHIFT_MYSQL_DB_USERNAME;
var db_password = process.env.OPENSHIFT_MYSQL_DB_PASSWORD;
var db_name = process.env.OPENSHIFT_GEAR_NAME;

var socketCount = 0;

// CONNESSIONE AL SERVER
http.listen(server_port, server_ip_address, function () {
    console.log("Example app listening at http://%s:%s", server_ip_address, server_port);
});

//CONNESSIONE AL DATABASE
var connection = mysql.createConnection({
    host     : db_host,
    port     : db_port,
    user     : db_username,
    password : db_password,
    database : db_name
});

connection.connect(function(err) {
  if (err) {
    console.error('error connecting: ' + err.stack);
    return;
  }
  console.log('connected as id ' + connection.threadId);
});

app.get('/', function (req, res) {
    res.sendFile(__dirname + '/index.html');
});

io.on('connection', function(socket){
  console.log('a user connected');
  socketCount++;
  io.emit('users connected', socketCount);
  socket.on('chat message', function(msg){
    io.emit('chat message', msg);
    connection.query('INSERT INTO `message`(`text`) VALUES (?)',msg);
  });
  socket.on('disconnect', function(){
    socketCount--;
    io.emit('users connected', socketCount)
    console.log('user disconnected');
  });
});

//POST request alla home che risponde con hello post
app.post('/', function (req, res) {
   console.log("Got a POST request for the homepage");
   res.send('Hello POST');
});

/*
    POST request per l'invio di un messaggio
    prende dalla query 'numericid' e 'message', connette al db, crea la query, la svolge restituendo un errore o la conferma
*/
app.post('/inviaMessaggio', function(req,res) {
    console.log("richiesta di invio di un messaggio");
    var message = req.query.message;
    var query = vsprintf('INSERT INTO `Message`(`text`) VALUES ("%s")',[message]);
    console.log(query);
    connection.query(query, function(error) {
        if (error) {
            res.send(error);
        } else {
            var response = vsprintf('inserito messaggio: %s', [message]);
            res.send(response);
        }
    });
});


/*
    GET request per ottenere i messaggi nella table
*/
app.get('/getMessaggi', function(req,res) {
    console.log("chiamata a getMessaggi");
    connection.query('SELECT * FROM Message', function(err, result) {
      if (err)
        res.send(error);
      else
        var response = '';
        for (var i = 0; i < result.length; i++)
            response += result[i].text + '<br>'
        res.send(response);
    });
})
