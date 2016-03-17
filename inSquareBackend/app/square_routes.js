var Square = require('./models/square');
var Message = require('./models/message');
var User = require('./models/user');
var async = require('async');
var gcm = require('node-gcm');
var fs = require('fs');

module.exports = function(router, passport)
{
  /*
  LOAD SQUARE

    router.get('/loadsquares', isLoggedIn, function(req, res) {
      var sediRomaTre = JSON.parse(fs.readFileSync('./squaresJson/sediRomaTre.json', 'utf8')); //se non funziona var sediRomaTre = require('./squaresJson/sediRomaTre.json')
      var sediSapienza = JSON.parse(fs.readFileSync('./squaresJson/sediSapienza.json', 'utf8'));
      var buildingsStructures = JSON.parse(fs.readFileSync('./squaresJson/buildingsStructures.json', 'utf8'));
      var monumentiMemoriali = JSON.parse(fs.readFileSync('./squaresJson/monumentiMemoriali.json', 'utf8'));
      var parchiRoma = JSON.parse(fs.readFileSync('./squaresJson/parchiRoma.json', 'utf8'));

      var jsonFiles = [sediRomaTre, sediSapienza, buildingsStructures, monumentiMemoriali, parchiRoma];

      for (var j=0; j<jsonFiles.length-1; j++) {
        var singleFile = jsonFiles[j];
        for (var i=0; i<singleFile.length-1; i++) {
          var squareName = singleFile[i].name;
          var latitude = singleFile[i].latitude;
          var longitude = singleFile[i].longitude;
          createSquare(squareName, latitude, longitude, res);
        }
      }
      res.send("done");
    });
*/
    //Documentata
    router.post('/squares', function(req, res)
    //FARE CONTROLLO SU LAT E LON che devono essere float
    {
      var squareName = req.body.name;
      var latitude = parseFloat(req.body.lat);
      var longitude = parseFloat(req.body.lon);
      var ownerId = req.body.ownerId;
      var description = req.body.description;

      if(squareName == undefined || squareName == null || squareName == "")
      {
        res.send("Non e' stato inserito un nome valido");
      }
      /*
      if(ownerId == undefined)
      {
        res.send("Non e' stato inserito un ownerID valido");
      }
      */
      if(latitude == undefined || latitude == null || latitude == ""
        || longitude == undefined || longitude == null || latitude == "")
      {
        res.send("Bisogna specificare delle coordinate valide!");
      }

      createSquare(squareName, latitude, longitude, description, ownerId, function(square) {
        User.findById(ownerId, function(err,user) {
          if(err) console.log(err);
          user.favourites.push(square["_id"]);
          user.save(function(err) {
            if(err) console.log(err);
          });
          res.json(square);
        }.bind({res:res}));
      });
    });

    //Documentata
    router.patch('/squares?', function(req,res) {
      var description = req.query.description;
      var squareId = req.query.squareId;
      if(req.query.update=='true' && description!=null && description!=undefined
        && description!='' && squareId!=null && squareId!='' && squareId!=undefined) {
        Square.findById(req.query.squareId, function(err,square) {
          if(err) console.log(err);
          square.description=req.query.description;
          square.save(function(err) {
            if(err) console.log(err);
            res.send("La descrizione square è stata modificata");
            notifyEvent(square.ownedBy, squareId, "update")
         });
        })
      }else{
      console.log("Errore");
      res.send("La descrizione square NON è stata modificata");
    }
    })

    //Documentata
    router.get('/favouritesquares/:id', function(req, res) {
      var userId = req.params.id;
      if(userId!=null && userId != "" && userId != undefined){
        User.findById(userId, function(err, usr){
          if (err) console.log(err);
          async.forEachOf(usr.favourites, function(item, k, callback) {
            if(item == null) {
              usr.favourites.splice(k, 1);
              usr.save();
            }
            callback();
          }, function(err) {
            if(err) console.log(err);
            Square.search({
              filtered:{
                filter:{
                  terms:{
                    _id: usr.favourites
                  }
                }
              }
            },{
              sort : [
                {
                  lastMessageDate : {
                    order : "desc"
                  }
                }
              ],
              size:1000
            }, function(err, squares){
                if(err) console.log(err);
                console.log(squares);
                res.header("Cache-Control", "no-cache, no-store, must-revalidate");
                res.header("Pragma", "no-cache");
                res.removeHeader('ETag');
                res.header("expires", 0);
                console.log(squares);
                res.json(squares.hits.hits);
            });
          })
        });
      }
    })

    //Documentata
    router.post('/favouritesquares', function(req, res) {
      var user = req.query.userId;
      var square = req.query.squareId;

      if(square != null && square != undefined && square != "")
      {
        if(user != null && user != undefined && user != "")
        {
          Square.findById(square, function(err, sqr)
          {
            if(err) console.log(err);
            User.findById(user, function(err, usr)
            {
              if(err) console.log(err);
              if(usr.favourites.indexOf(sqr.id) == (-1)) {
                usr.favourites.push(sqr);
                usr.save(function(err) {
                  if(err) console.log(err);
                  console.log(usr);
                })
                sqr.favouredBy = sqr.favouredBy+1;
                sqr.save(function(err) {
                  if(err) console.log(err);
                  res.send("La square è stata aggiunta come preferita");
                  notifyEvent(user, square, "update")
               });
              } else {
                res.send("Errore la square è già tra i preferiti");
              }
            })
          });
        } else {
          res.send("Errore la square NON è stata aggiunta come preferita. userId mancante: " + user);
        }
      } else {
        res.send("Errore la square NON è stata aggiunta come preferita. squareId mancante: " + square);
      }
    });

    //Documentata
    router.delete('/favouritesquares', function(req, res) {
      console.log(req.query.squareId + "  " + req.query.userId);
      var squareId = req.query.squareId;
      if(squareId != null && squareId != undefined && squareId != "") {
        var userId = req.query.userId;
        if(userId != null && userId != undefined && userId != "") {
          User.findById(userId, function(err, usr){
            if(err) console.log(err);
            var index = usr.favourites.indexOf(squareId);
            usr.favourites.splice(index,1);
            usr.save(function(err){
              if(err) console.log(err);
              console.log(usr);
            });
          });
          Square.findById(squareId, function(err, sqr) {
            if(err) console.log(err);
            sqr.favouredBy = sqr.favouredBy-1;
            sqr.save(function(err) {
              if(err) console.log(err);
              console.log(sqr);
              notifyEvent(userId, squareId, "update")
            });
          });
          res.status(200).send("La square è stata eliminata dai preferiti");
        } else {
          res.status(403).send("Errore La square NON è stata eliminata dai preferiti. UserId mancante: " + userId);
        }
      } else {
        res.status(403).send("Errore La square NON è stata eliminata dai preferiti. SquareId mancante: " + squareId);
      }
    });

    //Documentata
    router.get('/recentSquares/:userId', function(req, res) {
      User.findById(req.params.userId)
      .exec(function(err, user) {
        if(err) console.log(err);
        var squares = [];
        async.forEachOf(user.recents, function(item, k, callback) {
          squares.push(item.square);
          callback();
        }, function(err) {
          if(err) console.log(err);
          Square.search({
            filtered : {
              filter : {
                terms : {
                  _id : squares
                }
              }
            }
          },{
            sort : [
              {
                lastMessageDate : {
                  order : "desc"
                }
              }
            ],
            size:1000}, function(err, result) {
            if(err) console.log(err);
            res.header("Cache-Control", "no-cache, no-store, must-revalidate");
            res.header("Pragma", "no-cache");
            res.removeHeader('ETag');
            res.header("expires", 0);
            res.json(result.hits.hits);
          })
        })
      })
    })

    //Documentata
    router.delete('/recentSquares?', function(req, res) {
      if(req.query.userId != null && req.query.userId != undefined && req.query.userId != "") {
        User.findById(req.query.userId, function(err, user) {
          if(err) console.log(err);
          user.recents = [];
          user.save();
          res.send("Recenti eliminate");
        })
      } else {
        res.send("Recenti non eliminate, userId non valido");
      }
    })

    //Documentata
    router.get('/squares/:id', function(req,res) {
      Square.findById(req.params.id, function(err,square) {
        if(err) console.log(err);
        getCurrentState(req.params.id, function(state) {
          square.state = state;
          square.save(function(err) {
            if(err) console.log(err);
            res.json(square);
          })
        });
      });
    });

    //Documentata
    router.delete('/squares?', function(req,res) {
      if(req.query.squareId!=undefined && req.query.squareId!=null && req.query.squareId!=''){
        Square.findById(req.query.squareId, function(err, result){
          if (err) console.log(err);
          if(result.ownerId == req.query.ownerId){
            deleteSquare(result, req.query.ownerId);
            res.send("Done");
          } else if(result.ownerId==null || result.ownerId==undefined || result.ownerId==''){
            deleteSquare(result, null);
            res.send("Done");
          } else {
            res.send("Error");
          }
        })
      }
    });

    /*
    //Documentata
    router.delete('/squaresNotOwned?', function(req,res) {
      if(req.query.name != null && req.query.name != undefined && req.query.name != "") {
        deleteSquareWithNoOwner(req.query.name);
        console.log("Stai per cancellare la square: " + req.query.name + " Con userId: " + req.query.userId);
        res.send("Square eliminata con successo!");
      } else {
        res.send("Square NON eliminata devi fornire un nome valido");
      }
    });
    */

    //Documentata
    router.get('/squares?', function(req,res) {
      if(req.query.ownerId!=undefined && req.query.ownerId!='' &&
      req.query.ownerId!=null && req.query.byOwner=='true'){
        Square.search({
          bool:{
            must:{
              query_string:{
                default_field : "square.ownerId",
                query: req.query.ownerId
              }
            }
          }
        },
        {
          sort: [
            {
              createdAt : {
                order : "desc"
              }
            },
            {
              lastMessageDate : {
                order : "desc"
              }
            }
          ],
          size : 10000
          }, function(err, squares){
            if (err) console.log(err);
            res.json(squares.hits.hits);
        });
      }
      else if(req.query.autocomplete=='true') {
        if(req.query.name != '' && req.query.name != undefined && req.query.name != null) {
          var name = req.query.name;
          if(req.query.lat != '' && req.query.lat != undefined && req.query.lon != null) {
            var lat = req.query.lat;
            if(req.query.lon != '' && req.query.lon != undefined && req.query.lon != null) {
              var splitted = req.query.name.split(" ");
              Square.search({
                bool : {
                  should : [
                    {
                      match : {
                        name : {
                          query : req.query.name,
                          fuzziness : 'AUTO'
                        }
                      }
                    },
                    {
                      prefix : {
                        name : splitted[splitted.length-1]
                      }
                    }
                  ]
                }
              },
              {
                sort : [
                  {
                    _geo_distance : {
                      geo_loc : req.query.lat + ',' + req.query.lon,
                      order : "asc",
                      unit : "km"
                    }
                  }
                ],
                size : 5
              }, function(err,squares) {
                if(err) console.log(err);
                res.json(squares.hits.hits);
              })
            }
          }
        }
      }
      //RICERCA PER TOPIC
      //TODO MESSAGGI RECENTI IN ORDINE DI CREATED BY
      else if(req.query.topic=='true' && req.query.name!=undefined &&
            req.query.name!=null && req.query.name!='' &&
            req.query.lat != undefined && req.query.lat != null &&
            req.query.lat !=  '' && req.query.lon != undefined &&
          req.query.lon != null && req.query.lon !=''){
          Message.search({
            bool:{
              should:
              [
                {
                  fuzzy:{
                    text:{
                      value: req.query.name
                    }
                  }
                },
                {
                  range : {
                    createdAt : {
                      gte : "now-7d",
                      lte : "now"
                    }
                  }
                }
              ]
            }
          },
          {
            aggs:{
              names :{
                terms:{
                  field: "squareId",
                  size : 10000
                }
              }
            },
            size : 10000
          }, function(err, result){
            //TODO SQUARES VICINO A ME IN ORDINE DI DISTANZA
              if (err) console.log("Error while searching for topic: " + err);
              findSquares(result.aggregations.names.buckets, req.query.lat, req.query.lon,
                function(err, configs){
                  if(err) console.log(err);
              res.json(configs);
              });
            });
      }
      else if(req.query.name!=undefined) {
        console.log("REQ QUERY NAME:\n" + req.query.name);
        Square.findOne({ 'name' : req.query.name }, function(err,square) {
            if(err) res.send(err);
            if(square) res.json(square);
            else res.send("No square with this name");});
      }
      else if(req.query.distance != undefined && req.query.lat != undefined
        && req.query.lon != undefined) {
        Square.search ({
          filtered : {
            query : {
              match_all : {}
            },
            filter : {
              geo_distance : {
                distance : req.query.distance,
                geo_loc :  req.query.lat + ',' + req.query.lon
              }
            }
          }
        },
        {
          sort : [
            {
              _geo_distance : {
                geo_loc : req.query.lat + ',' + req.query.lon,
                order : "asc",
                unit : "km"
              }
            }
          ],
          from : 0,
          size : 200
        }, function(err,squares) {
          if(err)
            console.log("Error while searching for squares: " + err);
          res.json(squares.hits.hits);
        })
      } else {res.send("Invalid query")};
    });
};



//Aggiunto ownerID
function createSquare(squareName, latitude, longitude, description, ownerId, callback) {
	var square = new Square();

	square.name = squareName;
  square.searchName = squareName;
  square.createdAt = (new Date()).getTime();
	square.geo_loc = latitude + ',' + longitude;
  square.ownerId = ownerId;
  square.description = description;
  square.favouredBy = square.favouredBy+1;
  square.lastMessageDate = square.createdAt;
  square.save(function(err) {
		if(err) console.log(err);
    Square.findOne({'name' : squareName}, function(err,sqr) {
      if(err) console.log(err);
      var square = {};
      square["_id"] = sqr.id;
      square["_source"] = {};
      square["_source"].name = sqr.name;
      square["_source"].description = sqr.description;
      square["_source"].searchName = sqr.searchName;
      square["_source"].geo_loc = sqr.geo_loc;
      square["_source"].messages = sqr.messages;
      square["_source"].ownerId = sqr.ownerId;
      square["_source"].views = sqr.views;
      square["_source"].favouredBy = sqr.favouredBy;
      square["_source"].state = sqr.state;
      square["_source"].lastMessageDate = sqr.lastMessageDate;
      console.log(square);
      notifyEvent(ownerId, sqr.id, "creation");
      callback(square);
    })
	})
}

function deleteSquare(square, ownerId) {
     Message.find({'squareId' : square.id})
       .exec(function(err, messages) {
         if(err) console.log(err);
         for(var j=0; j<messages.length; j++) {
           messages[j].remove(function (err,message) {
             if(err) console.log(err);
           });
         }
       });
       square.remove(function(err, square) {
         if(err) console.log(err);
         if(ownerId!=null && ownerId!=undefined && ownerId!=""){
           notifyEvent(ownerId, square.id, "deletion");
         }else{
           notifyEvent("", square.id, "deletion");
         }
       });
       forgetSquare(square.id);
}


function forgetSquare(squareId) {
  User.find({'favourites' : squareId}, function(err,users) {
    console.log(users);
    for(var i = 0; i<users.length; i++) {
      var index = users[i].favourites.indexOf(squareId);
      users[i].favourites.splice(index, 1);
      users[i].save(function(err) {
        if(err) console.log(err);
      })
      forgetRecent(squareId);
    }
  })
}

function forgetRecent(squareId) {
  User.find({'recents.square' : squareId}, function(err, users) {
    if(err) console.log(err);
    for(var i = 0; i<users.length; i++) {
      indexSquare(squareId, users[i].recents, function(result) {
        console.log(result);
        users[i].recents.splice(result, 1);
        users[i].save(function(err) {
          if(err) console.log(err);
        });
      })
    }
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

function notifyEvent(userId, squareId, event) {
  var sender = new gcm.Sender("AIzaSyAyKlOD_EyfZBP2vDEOusMq97W_gE_aQzA");
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

function findSquares(squares, lat, lon, callback){
  var keys = [];
  async.forEachOf(squares, function(item, k, callback){
    keys.push(item.key);
    callback();
  }, function(err){
    if(err) console.log(err)
    Square.search({
      filtered:{
        filter:{
          terms:{
            _id: keys
          }
        }
      }
    },
    {
      sort : [
        {
        state: {
          order: "desc"
        }
      },
        {
          _geo_distance : {
            geo_loc : lat + ',' + lon,
            order : "asc",
            unit : "km"
          }
        }
      ],
      from : 0,
      size : 10000
    }, function(err, result){
      if(err) console.log(err);
      callback(null,result);
    }.bind({ callback : callback }));
  });
}


setInterval(updateSquareState, 2*60*1000);

function updateSquareState() {
  console.log("Updating states");
  Square.find({}, function(err,squares) {
    if(err) console.log(err);
    for(var i = 0; i<squares.length; i++) {
      getCurrentState(squares[i].id, function(state) {
        this.square.state = state;
        this.square.save();
      }.bind({square : squares[i]}))
    }
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
