/**
 *      Mapping tests
 */
var mongoose = require('mongoose'),
    request = require('request'),
    assert = require('assert'),
    models = require('./models'),
    async = require('async'),
    util = require('util'),
    elmongo = require('../lib/elmongo'),
    helpers = require('../lib/helpers'),
    testHelper = require('./testHelper'),
    mapping = require('../lib/mapping')

var connStr = 'mongodb://localhost/elmongo-test'

describe('elmongo mapping tests', function () {

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

    it('generates correct mapping for Cats model', function (done) {
        var generatedMap = mapping.generateMapping(models.CatSchema)

        // console.log('generatedMap for Cats', generatedMap)

        assert(generatedMap.properties)
        assert(generatedMap.properties.name)
        assert.equal(generatedMap.properties.name.type, 'string')
        assert(generatedMap.properties.age)
        assert.equal(generatedMap.properties.age.type, 'double')
        assert(generatedMap.properties.breed)
        assert.equal(generatedMap.properties.breed.type, 'string')
        assert(generatedMap.properties.toys)
        assert.equal(generatedMap.properties.toys.type, 'string')
        assert(generatedMap.properties.owner)
        assert.equal(generatedMap.properties.owner.type, 'string')
        assert(generatedMap.properties.nicknames)
        assert.equal(generatedMap.properties.nicknames.type, 'string')
        assert.equal(generatedMap.properties.nicknames.index_analyzer, 'autocomplete_index')
        assert.equal(generatedMap.properties.nicknames.search_analyzer, 'autocomplete_search')
        assert(generatedMap.properties.friends)
        assert.equal(generatedMap.properties.friends.type, 'string')
        assert(generatedMap.properties.isHappy)
        assert.equal(generatedMap.properties.isHappy.type, 'boolean')

        return done()
    })

    it('generates correct mapping for Person model', function (done) {
        var generatedMap = mapping.generateMapping(models.PersonSchema)

        // console.log('generatedMap for Persons', generatedMap)

        assert(generatedMap.properties)
        assert(generatedMap.properties.name)
        assert.equal(generatedMap.properties.name.type, 'string')
        assert(generatedMap.properties.email)
        assert.equal(generatedMap.properties.email.type, 'string')
        assert(generatedMap.properties.siblings)
        assert(generatedMap.properties.siblings.properties)
        assert(generatedMap.properties.siblings.properties.brothers)
        assert.equal(generatedMap.properties.siblings.properties.brothers.type, 'string')
        assert(generatedMap.properties.siblings.properties.sisters)
        assert.equal(generatedMap.properties.siblings.properties.sisters.type, 'string')
        assert(generatedMap.properties.parents)
        assert(generatedMap.properties.parents.properties)
        assert(generatedMap.properties.parents.properties.mother)
        assert.equal(generatedMap.properties.parents.properties.mother.type, 'string')
        assert(generatedMap.properties.parents.properties.father)
        assert.equal(generatedMap.properties.parents.properties.father.type, 'string')

        return done()
    })

    it('generates correct mapping for Hetero model', function (done) {
        var generatedMap = mapping.generateMapping(models.HeteroSchema)

        assert(generatedMap.properties)
        assert(generatedMap.properties.arrayOfNumbers)
        assert.equal(generatedMap.properties.arrayOfNumbers.type, 'double')
        assert(generatedMap.properties.singleNumber)
        assert.equal(generatedMap.properties.singleNumber.type, 'double')
        assert(generatedMap.properties.arrayOfStrings)
        assert.equal(generatedMap.properties.arrayOfStrings.type, 'string')
        assert(generatedMap.properties.singleString)
        assert.equal(generatedMap.properties.singleString.type, 'string')
        assert(generatedMap.properties.arrayOfObjectIds)
        assert.equal(generatedMap.properties.arrayOfObjectIds.type, 'string')
        assert(generatedMap.properties.singleObjectId)
        assert.equal(generatedMap.properties.singleObjectId.type, 'string')
        assert(generatedMap.properties._id)
        assert.equal(generatedMap.properties._id.type, 'string')

        return done()
    })
})