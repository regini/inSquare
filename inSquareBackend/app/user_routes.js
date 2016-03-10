var User = require('./models/user');
var Square = require('./models/square');
var fs = require('fs');
var gcm = require('node-gcm');
var async = require('async');
var sender = new gcm.Sender("AIzaSyAyKlOD_EyfZBP2vDEOusMq97W_gE_aQzA");

module.exports = function(router, passport){
  
  //Documentata
  router.patch('/user', function(req, res){
    var latitude = parseFloat(req.body.lat);
    var longitude = parseFloat(req.body.lon);
    var userId = req.body.userId;
    if(req.body.updateLocation=='true' &&
      latitude!=undefined && latitude!='' && latitude!=null
    && longitude!=undefined && longitude!='' && longitude!=null
   && userId!=undefined && userId!='' && userId!=null){
     User.findById(userId, function(err, user){
       user.lastLocation = latitude + ',' + longitude;

      // squaresNearYou(user, latitude, longitude);

       user.save(function(err) {
        if(err) console.log(err);
        res.send("User lastLocation update SUCCESSFUL");
     });
   })
 }else{
   res.send("User lastLocation update NOT SUCCESSFUL");
 }
})

  //Documentata
  router.get('/user?', function(req, res){
    var userId = req.query.userId;
    if(userId!=null && userId!=undefined && userId!=''
    && req.query.byLocation=='true'){
      User.findById(userId, function(err, user){
        if (err) console.log(err);
        res.json(user.lastLocation);
      });
    } else {
      var squareId = req.query.squareId;
      var distance = req.query.distance;
      if(squareId!=null && squareId!=undefined && squareId!='' &&
      distance!= null && distance!=undefined && distance!=''
      && req.query.onSquare=='true'){
        getUsersOnSquare(squareId, distance, function(err, users){
          res.json(users.hits.hits);
        });
      } else {
          res.send("Errore");
      }
    }
  });

}



function getUsersOnSquare(squareId, distance, callback){
  Square.findById(squareId, function(err, square){
    if (err) console.log(err);
    User.search ({
      filtered : {
        query : {
          match_all : {}
        },
        filter : {
          geo_distance : {
            distance : distance,
            lastLocation :  square.geo_loc
          }
        }
      }
    }, function(err, users){
      if (err) console.log(err);
      callback(null, users);
    })
  }.bind({callback : callback}))
}


function squaresNearYou(user, latitude, longitude, callback){
  var distance = "1km";
  Square.search (
    {
      filtered : {
          query : {
            bool : {
                must: [{
                    term: {
                        state: "caffeinated"
                    }
                }],
                must_not: [{
                    term: {
                        ownerId : user.id
                    }
                }, {
                    term: {
                        _id : user.favourites
                    }
                }, {
                    term: {
                        _id : user.recents
                    }
                }]
            }
        },
         filter : {
             geo_distance : {
                distance : distance,
                geo_loc :  latitude + ',' + longitude
                }
             }
           }
         }, function(err, squares){
           if(err) console.log(err);
           else if (squares != undefined){
             var message = new gcm.Message();
             message.addData("squares", squares.hits.hits);
             message.addData("nAroundYou", squares.hits.total);
             console.log("SQUARES " + squares.hits.hits);
             console.log("nAroundYou " + squares.hits.total);
             var userToken = [];
             userToken.push(user.gcmToken);
             sender.send(message, {registrationTokens: userToken}, function(err, response) {
     					if (err) console.log(err);
     					console.log(response);
     				});
          }
        })
  }
