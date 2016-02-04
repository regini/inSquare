var mongoose = require('mongoose');
var mongoosastic = require('mongoosastic');
var User = require('./user');

// schema for our messages model
var feedbackSchema = mongoose.Schema({
	text: String,
	createdAt : Date,
	user: {type: mongoose.Schema.Types.ObjectId, ref: 'User',
		es_schema: User, required: false},
	activity: String
});

feedbackSchema.plugin(mongoosastic, {
	hosts: ['http://elastic-insquare.rhcloud.com'],
	populate: [{path:"users"}]
})

Feedback = module.exports = mongoose.model('Feedback', feedbackSchema);

Feedback.createMapping(function (err,mapping) {
	if(err){
    console.log('error creating mapping (you can safely ignore this)');
    console.log(err);
  }else{
    console.log('mapping created!');
    console.log(mapping);
	}
});
