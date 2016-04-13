var mongoose = require('mongoose');
var mongoosastic = require('mongoosastic');
var Message = require('./message');
var User = require('./user');
var db = process.env.OPENSHIFT_FACETFLOW;

var squareSchema = mongoose.Schema({
  name: {
    type : String
  },
  searchName: {
    type: String,
    es_type: 'completion',
    es_index_analyzer: 'simple',
    es_search_analyzer: 'simple',
    es_payloads: true
  },
  createdAt: {type: Date},
  geo_loc: {
    type: String,
    es_type: 'geo_point'
  },
  messages: [{type: mongoose.Schema.Types.ObjectId, ref: 'Message',
   es_schema: Message}],
  ownerId: {type: mongoose.Schema.Types.ObjectId, ref: 'User',
   es_schema: User},
  views: {type: Number, default: 0},
  favouredBy: {type: Number, default: 0},
  favourers: [{type: mongoose.Schema.Types.ObjectId, ref: 'User',
   es_schema: User}],
  userLocated: {type: Number, default: 0},
  description: {type: String},
  state: {type: String, default: "asleep"},
  lastMessageDate: {type: Date},
  type: {type: Number, default: 0},
  expireTime: {type: Date}
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
