var mongoose = require('mongoose');
var mongoosastic = require('mongoosastic');
var bcrypt = require('bcrypt-nodejs');
var Message = require('./message');
var Square = require('./square');
var db = process.env.OPENSHIFT_FACETFLOW;

// schema for our user model
var userSchema = mongoose.Schema({
	local : {
		email: String,
		password: String,
	},
	facebook : {
		id: String,
		token: String,
		email: String,
		name: String,
		profilePhoto: String
	},
	twitter : {
		id: String,
		token: String,
		displayName: String,
		username: String
	},
	google : {
		id: String,
		token: String,
		email: String,
		name: String,
		profilePhoto: String
	},
	messages: [{type: mongoose.Schema.Types.ObjectId, ref: 'Message',
  	es_schema: Message}],
	favourites: [{type: mongoose.Schema.Types.ObjectId, ref: 'Square',
		es_schema: Square}],
	mute: [{
		square: {type: mongoose.Schema.Types.ObjectId, ref: 'Square',
			es_schema: Square},
		expireTime: {type: Date}
	}],
	gcmToken: String,
	lastLocation: {
    type: String,
    es_type: 'geo_point'
  },
	apnToken: String,
	apnTokenList: [String],
	recents: [{
		square: {type: mongoose.Schema.Types.ObjectId, ref: 'Square',
			es_schema: Square},
		lastUserMessage: {type: Date}
	}]
});

// methods
// =======
userSchema.methods.generateHash = function(password){
	return bcrypt.hashSync( password, bcrypt.genSaltSync(8), null);
};

// checking if valid password
// ==========================
userSchema.methods.validPassword = function(password)
{
	return bcrypt.compareSync(password, this.local.password);
};

userSchema.plugin(mongoosastic, {
	hosts: [db],
  populate: [{path: 'messages'},{path: 'squares'}]
});

// create model and expose for our app to see
User = module.exports = mongoose.model('User', userSchema);

User.createMapping(function (err,mapping) {
	if(err){
    console.log('error creating mapping (you can safely ignore this)');
    console.log(err);
  }else{
    console.log('mapping created!');
    console.log(mapping);
	}
});
