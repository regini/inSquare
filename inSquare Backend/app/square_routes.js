var Square = require('./models/square');

module.exports = function(router, passport, squares)
{

    router.post('/create_square', function(req, res)
    {
		// Se l'url is http://<...>/create_square?nome=Sapienza&lat=12.3183128&lon=44.1293129
        var squareName = req.query.nome;
        var latitude = req.query.lat;
        var longitude = req.query.lon;   

        // TODO portare queste validations dentro allo schema di mongoose
        if(squareName == undefined || squareName == "")
        {
            var error = { message:"\'nome\' is missing" };
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
            square:squareName,
            latitude:latitude,
            longitude:longitude
        };

        createSquare(squareName, latitude, longitude);

        res.send(result);

    });

    router.get('/create_square', function(req,res)
    {
    	// If url is http://<...>/create_square?name=Sapienza&lat=123183128
    	var squareName = req.query.nome;
    	var latitude = req.query.lat;
    	var longitude = req.query.lon;

    	if(squareName == undefined || squareName == "")
    	{
    		var error = { message:"\'nome\' is missing" };
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
    		square:squareName,
    		latitude:latitude,
    		longitude:longitude
    	};

    	res.send(result);
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
	square.squareName = squareName;
	square.loc.lat = latitude;
	square.loc.lon = longitude;

	square.save(function(err)
	{
		if(err) throw err;
		console.log(square);
	});
}