var Square = require('./models/square');
var Message = require('./models/message');
var User = require('./models/user');
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

      var square = createSquare(squareName, latitude, longitude, ownerId);
      res.json(square);

    });

    //solo per aggiornamento dei modelli
    router.patch('/squares?', function(req,res) {
      if(req.query.update) {
        Square.find({}, function(err,squares) {
          if(err) throw err;
          for(var i = 0; i<squares.length; i++) {
            if(squares[i].name != undefined) {
              squares[i].searchName = squares[i].name;
              console.log(squares[i]);
              squares[i].save(function(err) {
                if(err) throw err;
              });
            }
          }
        })
      }
      res.send('done');
    })

    router.post('/favouritesquares', function(req, res) {
        var squareId = req.body.squareId;
        if(squareId != null && squareId != undefined && squareId != ""){
          var userId = req.body.userId;
          if(userId != null && userId != undefined && userId != ""){
            Square.findById(squareId, function (err, sqr) {
                if (err) throw err;
                User.findById(userId, function(err, usr) {
                  usr.favourites.push(sqr);
                  usr.save(function(err){
                    if(err) throw err;
                    console.log(usr);
                  })
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
          var userId = req.body.userId;
          if(userId != null && userId != undefined && userId != ""){
                User.findById(userId, function(err, usr){
                  if(err) throw err;
                  var index = (usr.favourites).indexOf(squareId);
                  (usr.favourites).splice(index,1);
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
      Square.findOne({'_id' : req.params.id}, function(err,square) {
        if(err) throw err;
        res.json(square);
      })
    });

    router.delete('/squares?', function(req,res) {
      deleteSquare(req.query.name, req.query.userId);
      console.log("Stai per cancellare la square: " + req.query.name + " Con userId: " + req.query.userId);
      res.send('done');
    });

    router.get('/squares?', function(req,res) {
      if(req.query.autocomplete && req.query.name != undefined) {
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
        }, {size : 5}, function(err,squares) {
          if(err) throw err;
          res.json(squares.hits.hits);
        })
      }
      else if(req.query.name) {
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
function createSquare(squareName, latitude, longitude, ownerId) {
	var square = new Square();

    // TODO aggiungere la ricerca di una piazza gia' esistente (o farlo tramite validation)
	square.name = squareName;
  square.searchName = squareName;
	square.geo_loc = latitude + ',' + longitude;
  square.ownerId = ownerId;
	square.save(function(err) {
		if(err) throw err;
		console.log("Created square ====\n" + square + "\n====");
	});
  return square;

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
  });
}
