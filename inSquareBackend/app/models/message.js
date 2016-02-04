var mongoose = require('mongoose');
var mongoosastic = require('mongoosastic');
var Square = require('./square');
var User = require('./user');

// schema for our messages model
var messageSchema = mongoose.Schema({
	text: String,
	createdAt : Date,
	senderId: {type: mongoose.Schema.Types.ObjectId, ref: 'User',
		es_schema: User},
	squareId: {type: mongoose.Schema.Types.ObjectId, ref: 'Square',
		es_schema: Square}
});

messageSchema.plugin(mongoosastic, {
	hosts: ['http://elastic-insquare.rhcloud.com'],
	populate: [{path: 'squares'},{path:"users"}]
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
