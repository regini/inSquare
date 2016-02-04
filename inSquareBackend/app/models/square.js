var mongoose = require('mongoose');
var mongoosastic = require('mongoosastic');
var Message = require('./message');

var squareSchema = mongoose.Schema({
  name: String,
  geo_loc: {
    type: String,
    es_type: 'geo_point'
  },
  messages: [{type: mongoose.Schema.Types.ObjectId, ref: 'Message',
   es_schema: Message}]
});

squareSchema.plugin(mongoosastic, {
	hosts: ['http://elastic-insquare.rhcloud.com'],
  populate: [{path: 'messages'}]
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
