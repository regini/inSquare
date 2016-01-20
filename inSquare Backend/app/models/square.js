var mongoose = require('mongoose');
var mongoosastic = require('mongoosastic');

var squareSchema = mongoose.Schema({
  id: String,
  name: String
});

messageSchema.plugin(mongoosastic, {
	hosts: ['http://elastic-insquare.rhcloud.com']
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
