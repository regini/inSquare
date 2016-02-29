var Square = require('./models/square');
var Message = require('./models/message');
var User = require('./models/user');
var async = require('async');
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
    router.post('/squares', function(req, res)
    {
      var squareName = req.body.name;
      var latitude = parseFloat(req.body.lat);
      var longitude = parseFloat(req.body.lon);
      var ownerId = req.body.ownerId;
      var description = req.body.description;

      if(squareName == undefined)
      {
        res.send("Non e' stato inserito un nome valido");
      }
      /*
      if(ownerId == undefined)
      {
        res.send("Non e' stato inserito un ownerID valido");
      }
      */
      if(latitude == undefined || longitude == undefined)
      {
        res.send("Bisogna specificare delle coordinate valide!");
      }

      createSquare(squareName, latitude, longitude, description, ownerId, function(square) {
        User.findById(ownerId, function(err,user) {
          if(err) throw err;
          user.favourites.push(square);
          user.save(function(err) {
            if(err) throw err;
          });
          res.json(square);
        });
      }.bind({res:res}));
    });

    //solo per aggiornamento dei modelli
    router.patch('/squares?', function(req,res) {
      if(req.query.update) {
        Square.find({}, function(err,squares) {
          if(err) throw err;
          for(var i = 0; i<squares.length; i++) {
            if(squares[i].messages.length != 0) {
              Message.findById(squares[i].messages[(squares[i].messages.length)-1], function(err,mes) {
                if(err) throw err;
                this.sqr.lastMessageDate = mes.createdAt;
                this.sqr.save();
              }.bind({sqr : squares[i]}))
            }
          }
        })
      }
      res.send('done');
    })

    router.get('/favouritesquares/:id', function(req, res) {
      var userId = req.params.id;
      if(userId!=null && userId != "" && userId != undefined){
        User.findById(userId, function(err, usr){
          if (err) console.log(err);
          Square.search({
            filtered:{
              filter:{
                terms:{
                  _id: usr.favourites
                }
              }
            }
          },function(err, squares){
              res.json(squares.hits.hits);
          });
        });
      }
    })

    router.post('/favouritesquares', function(req, res) {
      var squareId = req.body.squareId;
      if(squareId != null && squareId != undefined && squareId != ""){
        var userId = req.body.userId;
        if(userId != null && userId != undefined && userId != ""){
          Square.findById(squareId, function (err, sqr) {
            if(err) throw err;
            User.findById(userId, function(err, usr) {
              usr.favourites.push(sqr);
              usr.save(function(err) {
                if(err) throw err;
                console.log(usr);
              })
            })
            sqr.favouredBy = sqr.favouredBy+1;
            sqr.save(function(err) {
              if(err) throw err;
              console.log(sqr)
            });
          });
        }
      }
      res.send("La square è stata aggiunta come preferita");
    });


    router.delete('/favouritesquares', function(req, res) {
      console.log(req.body.squareId + "  " + req.body.userId);
      var squareId = req.body.squareId;
      if(squareId != null && squareId != undefined && squareId != ""){
        Square.findById(squareId, function(err, sqr) {
          sqr.favouredBy = sqr.favouredBy-1;
          sqr.save(function(err) {
            if(err) throw err;
            console.log(sqr);
          });
        });
        var userId = req.body.userId;
        if(userId != null && userId != undefined && userId != ""){
          User.findById(userId, function(err, usr){
            if(err) throw err;
            var index = usr.favourites.indexOf(squareId);
            usr.favourites.splice(index,1);
            usr.save(function(err){
              if(err) throw err;
              console.log(usr);
            });
          });
        }
      }
      res.send("La square è stata eliminata dai preferiti");
    });


    router.get('/squares/:id', function(req,res) {
      Square.findById(req.params.id, function(err,square) {
        getCurrentState(req.params.id, function(state) {
          square.state = state;
          square.save(function(err) {
            if(err) throw err;
            res.json(square);
          })
        });
      });
    });

    router.delete('/squares?', function(req,res) {
      deleteSquare(req.query.name, req.query.userId);
      console.log("Stai per cancellare la square: " + req.query.name + " Con userId: " + req.query.userId);
      res.send('done');
    });

    router.get('/squares?', function(req,res) {
      if(req.query.ownerId && req.query.byOwner){
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
          ]
          }, function(err, squares){
          if (err) console.log(err);
            res.json(squares.hits.hits);
        });
      }
      else if(req.query.autocomplete) {
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
                if(err) throw err;
                res.json(squares.hits.hits);
              })
            }
          }
        }
      }
      //RICERCA PER TOPIC
      //TODO MESSAGGI RECENTI IN ORDINE DI CREATED BY
      else if(req.query.topic && req.query.name!=undefined &&
            req.query.lat != undefined && req.query.lon != undefined){
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
              'names' :{
                'terms':{
                  'field': "squareId",
                  'size' : 10000
                }
              }
            },
            size : 10000
          }, function(err, result){
            //TODO SQUARES VICINO A ME IN ORDINE DI DISTANZA
              if (err) console.log("Error while searching for topic: " + err);
              findSquares(result.aggregations.names.buckets, req.query.lat, req.query.lon,
                function(err, configs){
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
          console.log(squares.hits.hits);
          res.json(squares.hits.hits);
        })
      } else {res.send("Invalid query")};
    });
};


function isLoggedIn(req, res, next)
{
	//if user is auth-ed, go on
	if(req.isAuthenticated())
		return next();

	// else redirect to home page
	res.redirect('/');
}

//Aggiunto ownerID
function createSquare(squareName, latitude, longitude, description, ownerId, callback) {
	var square = new Square();

    // TODO aggiungere la ricerca di una piazza gia' esistente (o farlo tramite validation)
	square.name = squareName;
  square.searchName = squareName;
  square.createdAt = (new Date()).getTime();
	square.geo_loc = latitude + ',' + longitude;
  square.ownerId = ownerId;
  square.description = description;
  square.favouredBy = square.favouredBy+1;
  square.save(function(err) {
		if(err) throw err;
    Square.findOne({'name' : squareName}, function(err,sqr) {
      if(err) throw err;
      callback(sqr);
    })
	}.bind({callback : callback}));
}

function deleteSquare(squareName, userId) {
  Square.findOne({'name' : squareName , 'ownerId' : userId})
  .exec(function(err,mySquare) {
    Message.find({'squareId' : mySquare.id})
    .exec(function(err,messages) {
      for(var j=0; j<messages.length; j++) {
        messages[j].remove(function (err,message) {
          if(err) throw err;
        });
      }
    });
    mySquare.remove(function(err, square) {
      if(err) throw err;
	  });
    forgetSquare(mySquare.id);
  });
}

function forgetSquare(squareId) {
  User.find({'favourites' : squareId}, function(err,users) {
    console.log(users);
    for(var i = 0; i<users.length; i++) {
      var index = users[i].favourites.indexOf(squareId);
      users[i].favourites.splice(index, 1);
      users[i].save(function(err) {
        if(err) throw err;
      })
    }
  })
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
      callback(null,result);
    }.bind({ callback : callback }));
  });
}


setInterval(updateSquareState, 600000);

function updateSquareState() {
  console.log("Updating states");
  Square.find({}, function(err,squares) {
    if(err) throw err;
    for(var i = 0; i<squares.length; i++) {
      getCurrentState(squares[i].id, function(state) {
        this.square.state = state;
        this.square.save();
      }.bind({square : squares[i]}))
    }
  })
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
    if(err) throw err;
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
