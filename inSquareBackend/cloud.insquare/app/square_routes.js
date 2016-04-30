var Square = require('./models/square');
var Message = require('./models/message');
var User = require('./models/user');
var async = require('async');
var gcm = require('node-gcm');
var fs = require('fs');
var arrayUniq = require('array-uniq');

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

    router.post('/squares', function(req, res)
    //FARE CONTROLLO SU LAT E LON che devono essere float
    {
      var squareName = req.query.name;
      var latitude = parseFloat(req.query.lat);
      var longitude = parseFloat(req.query.lon);
      var ownerId = req.query.ownerId;
      var description = req.query.description;
      var type = req.query.type;
      var expireTime = undefined;
      var facebook_id_page = req.query.facebookIdPage;
      var facebook_id_event = req.query.facebookIdEvent;

      if(req.query.expireTime != undefined && req.query.expireTime != null && req.query.expireTime != ""){
        expireTime = Date.parse(req.query.expireTime);
      }

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

      createSquare(squareName, latitude, longitude, description,
        ownerId, type, expireTime, facebook_id_page, facebook_id_event, function(square) {
        if(square == null) {
          res.status(500);
        }
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


    router.patch('/squares?', function(req,res) {
      var description = req.query.description;
      var name = req.query.name;
      var expireTime = req.query.expireTime;

      var squareId = req.query.squareId;
      var ownerId = req.query.ownerId;

      if(squareId!=null && squareId!='' && squareId!=undefined){
        Square.findById(req.query.squareId, function(err,square) {
          if(err) console.log(err);
          if( ownerId==undefined || ownerId==null || ownerId=='' || square.ownerId==ownerId ){
              if( description!=null && description!=undefined && description!='') {
                square.description=description;
              }
              if( name!=null && name!=undefined && name!='') {
                square.name=name;
              }
              if(expireTime != undefined && expireTime != null && expireTime != ""){
                square.expireTime = Date.parse(req.query.expireTime);
              }
              square.save(function(err) {
                if(err) console.log(err);
                res.send("La descrizione square è stata modificata");
                notifyEvent(square.ownedBy, squareId, "update")
              });
            } else {
                console.log("Errore");
                res.send("I campi della Square NON sono stati modificati");
            }
        })

    } else {
      console.log("Errore");
      res.send("I campi della Square NON sono stati modificati");
    }
    })


    router.delete('/recentSquares', function(req, res) {
      var squareId = req.query.squareId;
      if(squareId != null && squareId != undefined && squareId != "") {
        var userId = req.query.userId;
        if(userId != null && userId != undefined && userId != "") {
          User.findById(userId, function(err, usr){
            if(err) console.log(err);
            async.forEachOf(usr.recents, function(item, k, callback) {
              console.log(item);
              if(item != undefined && item.square == squareId) {
                usr.recents.splice(k, 1);
                usr.save();
              }
              callback();
            }, function(err) {
              if(err) console.log(err);
              res.status(200).send("La square è stata eliminata dalle recenti");
            })
          });
        } else {
          res.status(403).send("Errore La square NON è stata eliminata dalle recenti. UserId mancante: " + userId);
        }
      } else {
        res.status(403).send("Errore La square NON è stata eliminata dalle recenti. SquareId mancante: " + squareId);
      }
    })

    router.patch('/facebookSquares', function(req,res) {
      Square.find({}, function(err, squares) {
        if(err) console.log(err);
        if(squares) {
          async.forEachOf(squares, function(item, k, callback) {
            if(item.facebook_id_event == undefined || item.facebook_id_event == null || item.facebook_id_event == "") {
              item.facebook_id_event = "";
            } 
            if(item.facebook_id_page == undefined || item.facebook_id_page == null || item.facebook_id_page == "") {
              item.facebook_id_page = "";
            }
            item.save();
            callback();
          })
        }
      })
      res.status(200).send("done");
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
                if(err) {
                  console.log(err);
                  res.json(err);
                } else {
                console.log(squares);
                res.header("Cache-Control", "no-cache, no-store, must-revalidate");
                res.header("Pragma", "no-cache");
                res.removeHeader('ETag');
                res.header("expires", 0);
                console.log(squares);
                res.json(squares.hits.hits);
              }
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
                if(sqr.favourers.indexOf(usr.id) == (-1)) {
                  sqr.favourers.push(usr.id);
                  sqr.favouredBy = sqr.favouredBy+1;
                }
                sqr.save(function(err) {
                  if(err) console.log(err);
                  res.status(200).send("La square è stata aggiunta come preferita");
                  notifyEvent(user, square, "update")
                });
              } else {
                res.status(400).send("Errore la square è già tra i preferiti");
              }
            })
          });
        } else {
          res.status(400).send("Errore la square NON è stata aggiunta come preferita. userId mancante: " + user);
        }
      } else {
        res.status(400).send("Errore la square NON è stata aggiunta come preferita. squareId mancante: " + square);
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
            Square.findById(squareId, function(err, sqr) {
              if(err) console.log(err);
              if(sqr.favourers.indexOf(usr.id) >= 0) {
                sqr.favouredBy = sqr.favouredBy-1;
                sqr.favourers.splice(sqr.favourers.indexOf(usr.id), 1);
              }
              sqr.save(function(err) {
                if(err) console.log(err);
                console.log(sqr);
                notifyEvent(userId, squareId, "update")
              });
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

    //Documentata //VIENE USATA??
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
          if (err || result==undefined) {
            res.send("Error");
          } else if(result.ownerId == req.query.ownerId){
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
      else if(req.query.name != '' && req.query.name != undefined && req.query.name != null) {
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
      //RICERCA PER TOPIC
      //TODO MESSAGGI RECENTI IN ORDINE DI CREATED BY
      else if(req.query.name!=undefined &&
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
        Square.search({
          bool:{
              should:
              [
                {
                  fuzzy:{
                    name:{
                      value: req.query.name
                    }
                  }
                }
              ]
            }
        }, function(err, squares){
          if (err) console.log(err);
          console.log(squares);
          res.json(squares.hits.hits);
        })
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


/*
module.exports = {

  updateMute: function() {
    console.log("Updating mute squares");
    var date = (new Date()).getTime();
    console.log("Time now is: " + date);
    User.find({}, function(err,users) {
      if(err) console.log(err);
      for(var i=0; i<users.length; i++){
        for(var j=0; j<users[i].mute.length; j++){
          console.log(users[i].mute[j].expireTime.getTime());
          if(users[i].mute[j].expireTime.getTime() < date ){
            users[i].mute.splice(j, 1);
            users[i].save();
          }
        }
      }
    })
  }

}; //FINE module.exports
*/

//Aggiunto ownerID
function createSquare(squareName, latitude, longitude,
  description, ownerId, type, expireTime, facebook_id_page, facebook_id_event, callback) {
	var square = new Square();

	square.name = squareName;
  square.searchName = squareName;
  square.createdAt = (new Date()).getTime();
	square.geo_loc = latitude + ',' + longitude;
  square.ownerId = ownerId;
  square.description = description;
  square.favouredBy = square.favouredBy+1;
  square.favourers = [ownerId];
  square.lastMessageDate = square.createdAt;
  square.type = type;
  square.expireTime = expireTime;
  square.facebook_id_page = facebook_id_page;
  square.facebook_id_event = facebook_id_event;

  square.save(function(err) {
		if(err) {
      console.log(err);
      callback(null);
    } else {
      var sqr = {};
      sqr["_id"] = square._id;
      sqr["_source"] = {};
      sqr["_source"].name = square.name;
      sqr["_source"].description = square.description;
      sqr["_source"].searchName = square.searchName;
      sqr["_source"].geo_loc = square.geo_loc;
      sqr["_source"].messages = square.messages;
      sqr["_source"].ownerId = square.ownerId;
      sqr["_source"].views = square.views;
      sqr["_source"].favouredBy = square.favouredBy;
      sqr["_source"].favourers = square.favourers;
      sqr["_source"].state = square.state;
      sqr["_source"].lastMessageDate = square.lastMessageDate;
      sqr["_source"].type = square.type;
      sqr["_source"].expireTime = square.expireTime;
      sqr["_source"].facebook_id_page = square.facebook_id_page;
      sqr["_source"].facebook_id_event = square.facebook_id_event;
      console.log(sqr);
      notifyEvent(ownerId, square._id, "creation");
      callback(sqr);
    }
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

      async.forEachOf(users[i].mute, function(item, k, callback) {
        console.log(item);
        if(item != undefined && item.square == squareId) {
          users[i].mute.splice(k, 1);
          users[i].save();
        }
        callback();
      }, function(err) {
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

/*setTimeout(populateFavuorers, 5000);

function populateFavuorers() {
  console.log("populating")
  Square.find({}, function(err,squares) {
    if(err) {
      console.log(err);
    } else {
      async.forEachOf(squares, function(square, k, callback1) {
        square.favourers = [];
        User.find({"favourites" : square.id}, function(err, users) {
          if(err) {
            console.log(err);
            return callback1(err);
          } else {
            async.forEachOf(users, function(user, k2, callback2) {
              square.favourers.push(user.id);
              callback2();
            }.bind({square : square}), function(err) {
              square.save();
              callback1();
            })
          }
        })
      })
    }
  })
}*/

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
