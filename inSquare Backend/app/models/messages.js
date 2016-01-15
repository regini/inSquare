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

module.exports = mongoose.model('Message', messageSchema);
