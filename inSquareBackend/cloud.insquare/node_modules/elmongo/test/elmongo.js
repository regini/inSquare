var mongoose = require('mongoose'),
	Schema = mongoose.schema,
	request = require('request'),
	assert = require('assert'),
	models = require('./models'),
	async = require('async'),
	util = require('util'),
	elmongo = require('../lib/elmongo'),
	helpers = require('../lib/helpers'),
	testHelper = require('./testHelper')

// connect to DB
var connStr = 'mongodb://localhost/elmongo-test'

/**
 *
 * Basic tests for Elmongo functionality - load tests are done in load.js
 *
 */
describe('elmongo plugin', function () {

	// array of test cat models that tests in this suite share
	var testCats = [];

	before(function (done) {
		async.series({
			connectMongo: function (next) {
				mongoose.connect(connStr, next)
			},
			dropCollections: testHelper.dropCollections,
			checkSearchRunning: function (next) {
				// make sure elasticsearch is running
				request('http://localhost:9200', function (err, res, body) {
					assert.equal(err, null)
					assert(body)

					var parsedBody = JSON.parse(body)
					assert.equal(helpers.elasticsearchBodyOk(parsedBody), true)
					assert.equal(parsedBody.status, 200)

					return next()
				})
			},
			syncCats: function (next) {
				models.Cat.sync(next)
			},
			waitForYellowStatus: testHelper.waitForYellowStatus,
			insertCats: function (next) {

				testCats[0] = new models.Cat({
					name: 'Puffy',
					breed: 'siamese',
					age: 10
				})

				testCats[1] = new models.Cat({
					name: 'Mango',
					breed: 'siamese',
					age: 15
				})

				testCats[2] = new models.Cat({
					name: 'Siamese',
					breed: 'persian',
					age: 12
				})

				testCats[3] = new models.Cat({
					name: 'Zing Doodle',
					breed: 'savannah',
					age: 20
				})

				testHelper.saveDocs(testCats, next)
			},
			refreshIndex: testHelper.refresh
		}, done)
	})

	after(function (done) {
		async.series({
			refreshIndex: testHelper.refresh,
			disconnectMongo: function (next) {
				mongoose.disconnect()
				return next()
			}
		}, done)
	})

	it('Model.search() query with no matches should return empty array', function (done) {
		models.Cat.search({ query: 'nothingShouldMatchThis' }, function (err, results) {
			assert.equal(err, null)
			assert(results)

			if (results.hits.length || results.hits.total) console.log('results', util.inspect(results, true, 10, true))

			assert.equal(results.total, 0)
			assert.equal(results.hits.length, 0)

			return done()
		})
	})

	it('after creating a cat model instance, it should show up in Model.search()', function (done) {

		var testCat = null

		async.series({
			addCat: function (next) {
				testCat = new models.Cat({
					name: 'simba'
				})

				testHelper.saveDocs([ testCat ], next)
			},
			refreshIndex: testHelper.refresh,
			doSearch: function (next) {
				// search to make sure the cat got indexed
				models.Cat.search({ query: 'simba' }, function (err, results) {
					testHelper.assertErrNull(err)

					assert.equal(results.total, 1)
					assert.equal(results.hits.length, 1)

					var firstResult = results.hits[0];

					assert(firstResult)
					assert.equal(firstResult._source.name, 'simba')

					return next()
				})
			},
			cleanup: function (next) {
				testHelper.removeDocs([ testCat ], next)
			},
			refreshIndex: testHelper.refresh
		}, done)
	})

	it('after creating a cat model instance with a `Person` ref, and populating it, serializes properly', function (done) {
		var testCat = null, testPerson = null

		async.series({
			addCat: function (next) {
				testPerson = new models.Person({
					name: 'Tolga',
					email: 'foo@bar.com'
				})

				testCat = new models.Cat({
					name: 'populateTest',
					age: 11,
					owner: testPerson
				})

				testHelper.saveDocs([ testCat, testPerson ], next)
			},
			populateCat: function (next) {
				models.Cat.findById(testCat._id, function (err, foundTestCat) {
					models.Cat.populate(foundTestCat, { path: 'owner' }, function (err, populatedCat) {
						testHelper.assertErrNull(err)

						assert.equal(populatedCat.owner.name, testPerson.name)
						assert.equal(populatedCat.owner.email, testPerson.email)

						testHelper.saveDocs([ populatedCat ], next)
					})
				})
			},
			refreshIndex: testHelper.refresh,
			doSearch: function (next) {
				// search to make sure the cat got indexed
				models.Cat.search({ query: 'populateTest' }, function (err, results) {
					testHelper.assertErrNull(err)

					assert.equal(results.total, 1)
					assert.equal(results.hits.length, 1)

					var firstResult = results.hits[0]
					assert(firstResult)
					assert.equal(firstResult._source.name, 'populateTest')
					assert.equal(firstResult._source.owner, testPerson.id)

					return next()
				})
			},
			cleanup: function (next) {
				testHelper.removeDocs([ testCat, testPerson ], next)
			},
			refreshIndex: testHelper.refresh,
			waitForYellowStatus: testHelper.waitForYellowStatus
		}, done)
	})

	it('autocomplete behavior should work on a schema field with autocomplete: true', function (done) {

		var queries = [ 'M', 'Ma', 'Man', 'Mang', 'Mango' ];

		var searchFns = queries.map(function (query) {
			return function (next) {
				models.Cat.search({ query: query, fields: [ 'name' ] }, function (err, results) {
					testHelper.assertErrNull(err)

					// console.log('results', util.inspect(results, true, 10, true))

					assert.equal(results.total, 1)
					assert.equal(results.hits.length, 1)

					var firstResult = results.hits[0];

					assert(firstResult)
					assert.equal(firstResult._source.name, 'Mango')

					return next()
				})
			}
		})

		async.series(searchFns, done)
	})

	it('autocomplete should split on spaces', function (done) {

		var queries = [ 'z', 'zi', 'zin', 'zing', 'do', 'doo', 'dood', 'doodl', 'doodle' ];

		var searchFns = queries.map(function (query) {
			return function (next) {
				models.Cat.search({ query: query, fields: [ 'name' ] }, function (err, results) {
					testHelper.assertErrNull(err)

					// console.log('results', util.inspect(results, true, 10, true))

					assert.equal(results.total, 1)
					assert.equal(results.hits.length, 1)

					var firstResult = results.hits[0];

					assert(firstResult)
					assert.equal(firstResult._source.name, 'Zing Doodle')

					return next()
				})
			}
		})

		async.series(searchFns, done)
	})

	it('creating a cat model instance and editing properties should be reflected in Model.search()', function (done) {

		var testCat = null

		async.series({
			addCat: function (next) {
				testCat = new models.Cat({
					name: 'Tolga',
					breed: 'turkish',
					age: 5
				})

				testHelper.saveDocs([ testCat ], next)
			},
			refreshIndex: testHelper.refresh,
			// search to make sure the cat got indexed
			doSearch: function (next) {
				models.Cat.search({ query: 'Tolga', fields: [ 'name' ] }, function (err, results) {
					testHelper.assertErrNull(err)

					assert.equal(results.total, 1)
					assert.equal(results.hits.length, 1)

					var firstResult = results.hits[0];

					assert(firstResult)
					assert.equal(firstResult._source.name, 'Tolga')

					return next()
				})
			},
			// update the `testCat` model
			update: function (next) {
				models.Cat.findById(testCat._id).exec(function (err, cat) {
					assert.equal(err, null)

					assert(cat)
					cat.age = 7
					cat.breed = 'bengal'

					testHelper.saveDocs([ cat ], next)
				})
			},
			wait: function (next) {
				// wait 3s for age update
				setTimeout(next, 3000)
			},
			refreshIndex: testHelper.refresh,
			checkUpdates: function (next) {
					models.Cat.search({ query: 'tolga', fields: [ 'name' ] }, function (err, results) {
						assert.equal(err, null)

						// console.log('results after update', results)

						assert.equal(results.total, 1)
						assert.equal(results.hits.length, 1)

						var firstResult = results.hits[0]

						assert(firstResult)
						assert.equal(firstResult._source.name, 'Tolga')
						assert.equal(firstResult._source.age, 7)
						assert.equal(firstResult._source.breed, 'bengal')

						return next()
					})
			},
			cleanup: function (next) {
				testHelper.removeDocs([ testCat ], next)
			},
			refreshIndex: testHelper.refresh
		}, done)
	})



	it('creating a cat model instance and updating an array property should be reflected in Model.search()', function (done) {

		var testCat = null

		var testToys = [ 'scratcher', 'rubber duck' ];

		async.series({
			addCat: function (next) {
				testCat = new models.Cat({
					name: 'Tolga',
					breed: 'turkish',
					age: 5
				})

				testHelper.saveDocs([ testCat ], next)
			},
			refreshIndex: testHelper.refresh,
			// search to make sure the cat got indexed
			doSearch: function (next) {
				models.Cat.search({ query: 'Tolga', fields: [ 'name' ] }, function (err, results) {
					testHelper.assertErrNull(err)

					assert.equal(results.total, 1)
					assert.equal(results.hits.length, 1)
					assert(results.hits[0])
					assert.equal(results.hits[0]._source.name, 'Tolga')

					return next()
				})
			},
			// update the model
			update: function (next) {
				models.Cat.findById(testCat._id).exec(function (err, cat) {
					assert.equal(err, null)

					assert(cat)
					cat.toys = testToys
					cat.markModified('toys')

					testHelper.saveDocs([ cat ], next)
				})
			},
			wait: function (next) {
				// wait 3s for age update
				setTimeout(next, 3000)
			},
			refreshIndex: testHelper.refresh,
			checkAge: function (next) {
					models.Cat.search({ query: 'tolga', fields: [ 'name' ] }, function (err, results) {
						assert.equal(err, null)

						// console.log('results after toys update', util.inspect(results, true, 10, true))

						assert.equal(results.total, 1)
						assert.equal(results.hits.length, 1)

						var firstResult = results.hits[0]

						assert(firstResult)
						assert.equal(firstResult._source.name, 'Tolga')
						assert.deepEqual(firstResult._source.toys, testToys)

						return next()
					})
			},
			cleanup: function (next) {
				testHelper.removeDocs([ testCat ], next)
			},
			refreshIndex: testHelper.refresh
		}, done)
	})

	it('Model.search() with * should return all results', function (done) {

		setTimeout(function () {

			models.Cat.search({ query: '*' }, function (err, results) {
				testHelper.assertErrNull(err)

				assert.equal(results.total, testCats.length)
				assert.equal(results.hits.length, testCats.length)

				return done()
			})


		}, 5000)
	})

	it('elmongo.search() with * should return all results', function (done) {
		elmongo.search({ query: '*', collections: [ 'cats' ] }, function (err, results) {
			testHelper.assertErrNull(err)

			assert.equal(results.total, testCats.length)
			assert.equal(results.hits.length, testCats.length)

			return done()
		})
	})

	it('elmongo.search.config() then elmongo.search with * should return all results', function (done) {
		elmongo.search.config({ host: '127.0.0.1', port: 9200 })

		elmongo.search({ query: '*', collections: [ 'cats' ] }, function (err, results) {
			testHelper.assertErrNull(err)

			assert.equal(results.total, testCats.length)
			assert.equal(results.hits.length, testCats.length)

			return done()
		})
	})

	it('Model.search() with fuzziness 0.5 should return results for `Mangoo`', function (done) {
		models.Cat.search({ query: 'Mangoo', fuzziness: 0.5 }, function (err, results) {
			testHelper.assertErrNull(err)

			assert.equal(results.total, 1)
			assert.equal(results.hits.length, 1)

			var firstResult = results.hits[0]

			assert(firstResult)
			assert.equal(firstResult._source.name, 'Mango')

			return done()
		})
	})

	it('Model.search() with fuzziness 0.5 and with fields should return fuzzy matches for that field', function (done) {
		models.Cat.search({ query: 'siameez', fuzziness: 0.5, fields: [ 'breed'] }, function (err, results) {
			testHelper.assertErrNull(err)

			var siameseTestCats = testCats.filter(function (testCat) { return testCat.breed === 'siamese' })

			assert.equal(results.total, siameseTestCats.length)
			assert.equal(results.hits.length, siameseTestCats.length)

			assert(results.hits.every(function (hit) {
				return hit._source.breed === 'siamese'
			}))

			return done()
		})
	})

	it('Model.search() with fields returns only results that match on that field', function (done) {
		models.Cat.search({ query: 'Siamese', fields: [ 'name' ] }, function (err, results) {
			testHelper.assertErrNull(err)

			assert.equal(results.total, 1)
			assert.equal(results.hits.length, 1)

			var firstResult = results.hits[0]

			assert(firstResult)
			assert.equal(firstResult._source.name, 'Siamese')
			assert.equal(firstResult._source.breed, 'persian')

			return done()
		})
	})

	it('Model.search() with basic where clause returns results', function (done) {
		var searchOpts = {
			query: '*',
			where: {
				age: 10
			}
		}

		models.Cat.search(searchOpts, function (err, results) {
			testHelper.assertErrNull(err)

			assert.equal(results.total, 1)
			assert.equal(results.hits.length, 1)

			var firstResult = results.hits[0]

			assert(firstResult)
			assert.equal(firstResult._source.age, 10)

			return done()
		})
	})

	it('Model.search() with 3 where clauses returns correct results', function (done) {
		var searchOpts = {
			query: '*',
			where: {
				age: 15,
				breed: 'siamese',
				name: 'Mango'
			}
		}

		models.Cat.search(searchOpts, function (err, results) {
			testHelper.assertErrNull(err)

			assert.equal(results.total, 1)
			assert.equal(results.hits.length, 1)

			var firstResult = results.hits[0]

			assert(firstResult)
			assert.equal(firstResult._source.age, 15)
			assert.equal(firstResult._source.breed, 'siamese')
			assert.equal(firstResult._source.name, 'Mango')

			return done()
		})
	})

	it.skip('Model.search() with `not` clause returns correct results', function (done) {
		var searchOpts = {
			query: '*',
			where: { age: { not: 10 } }
		}

		var numTestCatsExpected = testCats.filter(function (testCat) { return testCat.age !== 10 }).length

		models.Cat.search(searchOpts, function (err, results) {
			testHelper.assertErrNull(err)

			assert.equal(results.total, 1)
			assert.equal(results.hits.length, 1)

			var firstResult = results.hits[0]

			assert(firstResult)
			assert.equal(firstResult._source.age, 15)
			assert.equal(firstResult._source.breed, 'siamese')
			assert.equal(firstResult._source.name, 'Mango')

			return done()
		})
	})

	it('Model.search() with where `or` clause returns results')

	it('Model.search() with where `in` clause returns results')

	it('Model.search() with where `gt` clause returns results')

	it('Model.search() with where `lt` clause returns results')
})
