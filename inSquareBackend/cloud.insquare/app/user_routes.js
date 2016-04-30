var User = require('./models/user');
var Square = require('./models/square');
var fs = require('fs');
var gcm = require('node-gcm');
var async = require('async');
var mute = require('./mute.js');
var sender = new gcm.Sender(process.env.GCM_SERVER_TOKEN);

module.exports = function(router, passport){


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

       //squaresNearYou(user, latitude, longitude);

       user.save(function(err) {
        if(err) console.log(err);
        res.send("User lastLocation update SUCCESSFUL");
     });
   })
 }else{
   res.send("User lastLocation update NOT SUCCESSFUL");
 }
})

  router.patch('/user/google', passport.authorize("google-id-token"), function(req, res) {
    res.json({
      googleId : req.account.google.id,
      googleToken : req.account.google.token,
			googleName : req.account.google.name,
			googleEmail : req.account.google.email,
			googlePicture : req.account.google.profilePhoto
    })
  })

  router.patch('/user/facebook', passport.authorize("facebook-token"), function(req, res) {
    res.json({
      facebookId : req.account.facebook.id,
      facebookToken : req.account.facebook.token,
			facebookName : req.account.facebook.name,
			facebookEmail : req.account.facebook.email,
			facebookPicture : req.account.facebook.profilePhoto
    })
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

  //Documentata  DEPRECATA
  router.post('/gcmToken', function(req, res){
    console.log("GCM: " + req.body.gcm + "userId: " + req.body.userId);
    if(req.body.gcm && req.body.userId){
      User.findById(req.body.userId, function(err, usr){
        if (err) console.log(err)
        usr.gcmToken = req.body.gcm;
        usr.save(function(err){
          if (err) console.log(err);
          res.send("GCM TOKEN on Server");
        })
      })
    }
  });

  router.patch('/mute?', function(req, res){
    var userId = req.query.userId;
    var squareId = req.query.squareId;
    var expireTime = req.query.expireTime;

    // off Disattiva mute
    // on Abilita mute per sempre
    // 1h Mute per 1 ora
    // 8h Mute per 8 order
    // 2d Mute per 2 giorni


    if(userId!=undefined && userId!=null && userId!=""
      && squareId!=undefined && squareId!=null && squareId!=""
    && expireTime!=undefined && expireTime!=null && expireTime!=""
  && (expireTime=="off" || expireTime=="on" || expireTime=="1h" || expireTime=="8h" || expireTime=="2d")){
      User.findById(userId, function (err, user){
        var minutes = 1000 * 60;
        var hours = minutes * 60;
        var days = hours * 24;

        if (err) console.log (err)
        else {
          Square.findById(squareId, function (err, square){
            if (err) console.log (err)
            else {
            var elem = {}
            elem.square = squareId;
            if(expireTime=="off"){
              elem.expireTime = (new Date()).getTime() - hours;
            } else if (expireTime=="on"){
              elem.expireTime = Date.parse("2999-01-01");
            } else if(expireTime=="1h"){
              elem.expireTime = (new Date()).getTime() + hours;
            } else if(expireTime == "8h"){
              elem.expireTime = (new Date()).getTime() + (8 * hours);
            } else {
              elem.expireTime = (new Date()).getTime() + (2 * days);
            }

            for(var i = 0; i<user.mute.length; i++){
              if(user.mute[i].square==squareId){
                user.mute.splice(i,1);
              }
            }
            user.mute.push(elem);
            user.save(function(err){
              if(err) console.log(err);
              });
            }
          })
        }
          mute.updateMute();
          res.send("Aggiornato!");
        })
      }
      else {
        res.send("Qualcosa è andato storto");
      }
    })

  router.patch('/gcmToken', function(req, res){
    console.log("GCM: " + req.body.gcm + "userId: " + req.body.userId);
    if(req.body.gcm && req.body.userId){
      User.findById(req.body.userId, function(err, usr){
        if (err) console.log(err)
        usr.gcmToken = req.body.gcm;
        usr.save(function(err){
          if (err) console.log(err);
          res.send("GCM TOKEN on Server");
        })
      })
    }
  });

  router.patch('/apnToken', function(req,res) {
    var isApple = req.query.isApple;
    var userId = req.query.userId;
    var apn = req.query.token;

    if(isApple && userId && apn) {
      User.findById(userId, function(err, usr){
        if (err) console.log(err)

        if(usr.apnTokenList == undefined) {
          usr.apnTokenList = [];
        }
        if(usr.apnTokenList.indexOf(apn) < 0) {
          usr.apnTokenList.push(apn);
        }

        usr.apnToken = isApple;

        usr.save(function(err){
          if (err) console.log(err);
          res.send("APN TOKEN on Server");
        })
      })
    }
  })
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


function squaresNearYou(user, latitude, longitude){
  var distance = "5km";
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
             console.log("SQUARES " + JSON.stringify(squares));
             console.log("SQUARES HITS " + squares.hits.hits);
             console.log("nAroundYou " + squares.hits.total);

             if(squares.hits.total>=1){
               var userToken = [];
               userToken.push(user.gcmToken);
               sender.send(message, {registrationTokens: userToken}, function(err, response) {
                 if (err)
                  console.log(err);
                else
                   console.log(response);
               });
             }
          }
        })
  }

//  setInterval(muteUpdate, 2*60*1000);
