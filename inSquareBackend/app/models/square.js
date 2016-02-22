var mongoose = require('mongoose');
var mongoosastic = require('mongoosastic');
var Message = require('./message');
var User = require('./user');
var db = process.env.OPENSHIFT_NODEJS_ELASTIC_URL;

var squareSchema = mongoose.Schema({
  name: String,
  geo_loc: {
    type: String,
    es_type: 'geo_point'
  },
  messages: [{type: mongoose.Schema.Types.ObjectId, ref: 'Message',
   es_schema: Message}],
  ownerId: {type: mongoose.Schema.Types.ObjectId, ref: 'User',
   es_schema: User}
});


squareSchema.plugin(mongoosastic, {
  hosts: [db],
  populate: [{path: 'messages'},{path:"users"}]
})

Square = module.exports = mongoose.model('Square', squareSchema);

Square.createMapping(function (err,mapping) {
	if(err){
    console.log('error creating mapping (you can safely ignore this)');
    console.log(err);
  }else{
    console.log('mapping created!');
    console.log(mapping);
	}
});
