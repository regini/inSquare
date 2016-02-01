var mongoose = require('mongoose');
var mongoosastic = require('mongoosastic');

// schema for our messages model
var messageSchema = mongoose.Schema({
	text: String,
	createdAt : Date,
	senderId: String,
	senderEmail: String,
	squareId: String
});

messageSchema.plugin(mongoosastic, {
	hosts: ['http://elastic-insquare.rhcloud.com']
})

Message = module.exports = mongoose.model('Message', messageSchema);

Message.createMapping(function (err,mapping) {
	if(err){
    console.log('error creating mapping (you can safely ignore this)');
    console.log(err);
  }else{
    console.log('mapping created!');
    console.log(mapping);
	}
});
