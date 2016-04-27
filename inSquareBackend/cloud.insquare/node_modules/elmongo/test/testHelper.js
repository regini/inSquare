/**
 *
 * Helper functions for tests
 *
 */
var assert = require('assert'),
    util = require('util'),
	request = require('request'),
	async = require('async'),
	models = require('./models'),
	helpers = require('../lib/helpers')

/**
 * Force a refresh on all indices so we an expect elasticsearch to be up-to-date
 *
 * @param  {Function} cb
 */
exports.refresh = function (cb) {
	request.post('http://localhost:9200/_refresh', function (err, res, body) {
		assert.equal(err, null)
		var parsedBody = JSON.parse(body)
		assert.equal(helpers.elasticsearchBodyOk(parsedBody), true)
		return cb()
	})
}

exports.deleteIndices = function (cb) {
	var indicesToDelete = [ 'Cat', 'Person' ].map(function (key) {
		var model = models[key];

		return model.collection.name.toLowerCase()
	})

	var deleteUri = 'http://localhost:9200/'+indicesToDelete.join(',')

	// console.log('deleteUri', deleteUri)

	var reqOpts = {
	    method: 'DELETE',
	    json: true,
	    url: deleteUri
	}

	request(reqOpts, function (err, res, body) {
	    assert.equal(err, null)
	    assert(helpers.elasticsearchBodyOk(body) || body.status === 404)

	    return cb()
	})
}

exports.waitForYellowStatus = function (cb) {
	console.log('waiting for yellow cluster status')
	request('http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=50s', function (err, res, body) {
		// console.log('yellow status body', body)
		assert.equal(err, null)
		return cb()
	})
}

/**
 * Assert that `err` is null, output a helpful error message if not.
 *
 * @param  {Any type} err
 */
exports.assertErrNull = function (err) {
	if (err) console.log('err:', util.inspect(err, true, 10, true))
	assert.equal(err, null)
}

/**
 * Save a Mongoose document, or an Array of them, call `cb` on completion.
 *
 * @param  {Array|Object}   docs
 * @param  {Function} cb
 */
exports.saveDocs = function (docs, cb) {
	if (!Array.isArray(docs)) {
		docs = [ docs ]
	}

	async.each(docs, function (doc, docNext) {
		if (!doc.save) {
			return docNext(new Error('Invalid argument: `docs` is expected to be a Mongoose document, or array of them'))
		}

		doc.once('elmongo-indexed', function (esearchBody) {
			var bodyOk = helpers.elasticsearchBodyOk(esearchBody)
			if (!bodyOk) {
				var error = new Error('elmongo-index error: '+util.inspect(esearchBody, true, 10, true))
				error.esearchBody = esearchBody

				return docNext(error)
			}

			return docNext()
		})

		doc.save(function (err) {
			if (err) {
				return docNext(err)
			}
		})
	}, cb)
}

/**
 * Remove a Mongoose document, or an Array of them, call `cb` on completion.
 *
 * @param  {Array|Object}   docs
 * @param  {Function} cb
 */
exports.removeDocs = function (docs, cb) {
	if (!Array.isArray(docs)) {
		docs = [ docs ]
	}

	async.each(docs, function (doc, docNext) {
		if (!doc.remove) {
			return docNext(new Error('Invalid argument: `docs` is expected to be a Mongoose document, or array of them'))
		}

		doc.once('elmongo-unindexed', function (esearchBody) {
			var bodyOk = helpers.elasticsearchBodyOk(esearchBody)

			if (!bodyOk) {
				var error = new Error('elmongo-unindex error: '+util.inspect(esearchBody, true, 10, true))
				error.esearchBody = esearchBody

				return docNext(error)
			}

			return docNext()
		})

		doc.remove(function (err) {
			if (err) {
				return docNext(err)
			}
		})
	}, cb)
}

/**
 * Insert `n` instances of `model` into the DB, call `cb` on completion.
 * @param  {Number}   n
 * @param  {Object}   model
 * @param  {Function} cb
 */
exports.insertNDocs = function (n, model, cb) {
	var modelsToSave = []

	for (var i = 0; i < n; i++) {
		var instance = new model({
			name: 'model '+i
		})

		modelsToSave.push(instance)
	}

	exports.saveDocs(modelsToSave, cb)
}

/**
 * Drop all test collections from the DB, call `cb` on completion.
 *
 * @param  {Function} cb
 */
exports.dropCollections = function (cb) {

	// drop all collections from `models` in parallel
	var deletionFns = [ 'Cat', 'Person' ].map(function (modelName) {
		var model = models[modelName];

		return function (modelNext) {

			model.find().exec(function (err, documents) {
				if (err) {
					return modelNext(err)
				}

				if (!documents || !documents.length) {
					return modelNext()
				}

				var numDocs = documents.length
				var numDeleted = 0

				documents.forEach(function (doc) {
					doc.once('elmongo-unindexed', function (esearchBody) {
						numDeleted++

						if (numDeleted === numDocs) {
							return modelNext()
						}
					})

					doc.remove()
				})
			})
		}
	})

	async.parallel(deletionFns, cb)
}
