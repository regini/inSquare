var express = require('express');
var app = express();
var fs      = require('fs');
var http    = require('http');
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

// CONNESSIONE AL SERVER
var server = app.listen(server_port, server_ip_address, function () {
    //stringhe per la console
    var host = server.address().address;
    var port = server.address().port;
    console.log("Example app listening at http://%s:%s", host, port);
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

//GET request alla home che risponde con hello world
app.get('/', function (req, res) {
    console.log("Got a GET request for the homepage");
    res.send('Hello World');
})

//POST request alla home che risponde con hello post
app.post('/', function (req, res) {
   console.log("Got a POST request for the homepage");
   res.send('Hello POST');
})

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
            var response = vsprintf('inseriti messaggio: %s', [message]);
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



