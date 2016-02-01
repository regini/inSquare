/**
 *     	Load tests for elmongo to ensure sanity during lots of load
 */
var mongoose = require('mongoose'),
	Schema = mongoose.schema,
	request = require('request'),
	assert = require('assert'),
	models = require('./models'),
	async = require('async'),
	mongodb = require('mongodb'),
	ObjectID = mongodb.ObjectID,
	util = require('util'),
	elmongo = require('../lib/elmongo'),
	helpers = require('../lib/helpers'),
	testHelper = require('./testHelper')

var connStr = 'mongodb://localhost/elmongo-test'

/**
 *
 * Elmongo load tests - ensure functionality and correctness during mass inserts, updates and re-indexing
 *
 */
describe('elmongo load tests', function () {

	before(function (done) {
		async.series({
			connectMongo: function (next) {
				mongoose.connect(connStr, next)
			},
			dropCollections: testHelper.dropCollections,
			deleteIndices: testHelper.deleteIndices,
			refresh: testHelper.refresh,
			waitForYellowStatus: testHelper.waitForYellowStatus,
			checkSearchRunning: function (next) {
				// make sure elasticsearch is running
				request('http://localhost:9200', function (err, res, body) {
					// console.log('err', err, 'body', body)
					assert.equal(err, null)
					assert(body)

					var parsedBody = JSON.parse(body)
					assert.equal(helpers.elasticsearchBodyOk(parsedBody), true)
					assert.equal(parsedBody.status, 200)

					return next()
				})
			},
			// need to make initial sync call so that indices are setup correctly
			createIndex: function (next) {
				models.Cat.sync(next)
			}
		}, done)
	})

	after(function (done) {
		async.series({
			dropCollections: testHelper.dropCollections,
			refreshIndex: testHelper.refresh,
			disconnectMongo: function (next) {
				mongoose.disconnect()
				return next()
			}
		}, done)
	})

	it('insert 10K cats into the DB, reindexing while searching should keep returning results', function (done) {
		var numDocs = 10*1000

		// set timeout of 60s for this test
		this.timeout(60*1000)

		async.series({
			insert10KCats: function (next) {
				console.log('\nsaving %s documents to the DB', numDocs)
				testHelper.insertNDocs(numDocs, models.Cat, next)
			},
			wait: function (next) {
				// wait 3s for cluster update
				setTimeout(next, 3000)
			},
			refresh: testHelper.refresh,
			reindexWhileSearching: function (next) {
				var searchesPassed = 0

				// perform a search query every 50ms during reindexing
				var interval = setInterval(function () {
					models.Cat.search({ query: '*', pageSize: 25 }, function (err, results) {
						testHelper.assertErrNull(err)

						assert.equal(results.total, 10000)
						assert.equal(results.hits.length, 25)
						searchesPassed++
					})
				}, 50)

				// kick off reindexing while searches are being performed
				models.Cat.sync(function (err) {
					testHelper.assertErrNull(err)

					clearInterval(interval)

					console.log('performed %s successful searches during reindexing', searchesPassed)

					return next()
				})
			},
			cleanup: function (next) {
				async.series({
					dropCollections: testHelper.dropCollections,
					refreshIndex: testHelper.refresh
				}, next)
			}
		}, done)
	})

	it('insert 10K cats into the DB, update them and make sure they are all updated in search results', function (done) {
		var numDocs = 10*1000

		// set timeout of 60s for this test
		this.timeout(60*1000)

		// keep a cache of updated cat info to check that elasticsearch has the updated info
		var catUpdateCache = {}

		async.series({
			insert10KCats: function (next) {
				console.log('\nsaving %s documents to the DB', numDocs)
				testHelper.insertNDocs(numDocs, models.Cat, next)
			},
			wait: function (next) {
				// wait 3s for cluster update
				setTimeout(next, 3000)
			},
			refresh: testHelper.refresh,
			// update all 10K cats in the DB
			update10KCats: function (next) {

				models
				.Cat
				.find()
				.exec(function (err, cats) {
					assert.equal(err, null)
					assert.equal(cats.length, numDocs)

					var i = 0
					var updated = 0

					async.each(cats, function (cat, catNext) {
						cat.name = 'cat-update-'+i
						i++

						catUpdateCache[cat.id] = {
							name: cat.name
						}

						testHelper.saveDocs(cat, function (err) {
							updated++

							if (!(updated%500))
								console.log('updated %s cats', updated)

							return catNext()
						})

					}, next)
				})
			},
			firstRefresh: testHelper.refresh,
			wait: function (next) {
				// wait for cluster update
				setTimeout(next, 10*1000)
			},
			secondRefresh: testHelper.refresh,
			search10KCats: function (next) {

				models
				.Cat
				.search({ query: '*', page: 1, pageSize: numDocs }, function (err, results) {
					testHelper.assertErrNull(err)

					assert.equal(results.total, numDocs)
					assert.equal(results.hits.length, numDocs)

					results.hits.forEach(function (hit) {
						assert(hit._source)
						assert(hit._source._id)

						if (!catUpdateCache[hit._source._id]) {
							console.log('search result not found in catCache. search result:', util.inspect(hit, true, 10, true))
						}

						assert(catUpdateCache[hit._source._id])
						assert.equal(catUpdateCache[hit._source._id].name, hit._source.name)

						// delete the `catCache` entry for this hit, so we will error out if we have duplicate search results
						delete catUpdateCache[hit._source._id]
					})

					return next()
				})
			},
			dropCollections: testHelper.dropCollections
		}, done)
	})

	it('syncing twice should not result in duplicates', function (done) {
		var numDocs = 10*1000

		// set timeout of 60s for this test
		this.timeout(60*1000)

		function reindexWhileSearching (next) {
			var searchesPassed = 0

			// perform a search query every 50ms during reindexing
			var interval = setInterval(function () {
				models.Cat.search({ query: '*', pageSize: 25 }, function (err, results) {
					testHelper.assertErrNull(err)

					assert.equal(results.total, 10000)
					assert.equal(results.hits.length, 25)
					searchesPassed++
				})
			}, 50)

			// kick off reindexing while searches are being performed
			models.Cat.sync(function (err) {
				testHelper.assertErrNull(err)

				clearInterval(interval)

				console.log('performed %s successful searches during reindexing', searchesPassed)

				return next()
			})
		}

		async.series({
			deleteIndices: testHelper.deleteIndices,
			syncOnce: function (next) {
				models.Cat.sync(next)
			},
			insert10KCats: function (next) {
				console.log('\nsaving %s documents to the DB', numDocs)
				testHelper.insertNDocs(numDocs, models.Cat, next)
			},
			wait: function (next) {
				// wait 3s for cluster update
				setTimeout(next, 3000)
			},
			refresh: testHelper.refresh,
			reindexWhileSearching: reindexWhileSearching,
			reindexWhileSearchingAgain: reindexWhileSearching,
			cleanup: function (next) {
				async.series({
					dropCollections: testHelper.dropCollections,
					refreshIndex: testHelper.refresh
				}, next)
			}
		}, done)
	})
})
