var User = require('./models/user');


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

}
