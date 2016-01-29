var Square = require('./models/square');
var fs = require('fs');

module.exports = function(router, passport)
{

    router.get('/loadsquares', isLoggedIn, function(req, res) {
      var sediRomaTre = JSON.parse(fs.readFileSync('./squaresJson/sediRomaTre.json', 'utf8')); //se non funziona var sediRomaTre = require('./squaresJson/sediRomaTre.json')
      var sediSapienza = JSON.parse(fs.readFileSync('./squaresJson/sediSapienza.json', 'utf8'));
      var buildingsStructures = JSON.parse(fs.readFileSync('./squaresJson/buildingsStructures.json', 'utf8'));
      var monumentiMemoriali = JSON.parse(fs.readFileSync('./squaresJson/monumentiMemoriali.json', 'utf8'));
      var parchiRoma = JSON.parse(fs.readFileSync('./squaresJson/parchiRoma.json', 'utf8'));

      var JsonFiles = [sediRomaTre, sediSapienza, buildingsStructures, monumentiMemoriali, parchiRoma];

      for (var singleFile in JsonFiles) {
        for (var i=0; i<singleFile.length; i++) {
          var squareName = singleFile[i].name;
          var latitude = singleFile[i].latitude;
          var longitude = singleFile[i].longitude;
          createSquare(squareName, latitude, longitude);
        }
      }
      res.send("done");
    });

    router.post('/squares', isLoggedIn, function(req, res)
    {
		// Se l'url is http://<...>/create_square?nome=Sapienza&lat=12.3183128&lon=44.1293129
        var squareName = req.body.name;
        var latitude = req.body.lat;
        var longitude = req.body.lon;

        // TODO portare queste validations dentro allo schema di mongoose
        if(squareName == undefined || squareName == "")
        {
            var error = { message:"\'name\' is missing" };
            res.send(error);
        }
        else if(latitude == undefined || latitude == "" || isNaN(latitude))
        {
            var error = { message:"\'latitude\' missing" };
            res.send(error);
        }else if(longitude == undefined || longitude == "" || isNaN(longitude))
        {
            var error = { message:"\'longitude\' missing" };
            res.send(error);
        }

        var result =
        {
            square : squareName,
            latitude : latitude,
            longitude : longitude
        };

        createSquare(squareName, latitude, longitude);

        res.json(result);

    });

    router.get('/squares?', isLoggedIn, function(req,res) {
      if(req.query.fulltext && req.query.name != undefined) {
        Square.search ({
          match : {
            name : req.query.name
          }
        }, function(err,squares) {
          if(err) throw err;
          console.log(squares);
          res.json(squares.hits.hits);
        })
      }
      else if(req.query.name) {
        console.log(req.query.name);
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
        }, function(err,squares) {
          if(err) throw err;
          console.log(squares);
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

function createSquare(squareName, latitude, longitude)
{
	var square = new Square();

    // TODO aggiungere la ricerca di una piazza gia' esistente (o farlo tramite validation)
	square.name = squareName;
	square.geo_loc = latitude + ',' + longitude;
	square.save(function(err)
	{
		if(err) throw err;
		console.log(square);
	});
}
