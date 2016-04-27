/**
 * Test for elmongo helpers (lib/helpers.js)
 */
var helpers = require('../lib/helpers'),
	ObjectID = require('mongodb').ObjectID,
	assert = require('assert'),
    Cat = require('./models').Cat

describe('elmongo helpers.serialize', function () {
	it('`helpers.serialize` leaves primitives and null untouched', function (done) {
        assert.equal(undefined, helpers.serialize(undefined))
        assert.equal(null, helpers.serialize(null))
        assert.equal(true, helpers.serialize(true))
        assert.equal(false, helpers.serialize(false))
        assert.equal('a', helpers.serialize('a'))
        assert.equal(10, helpers.serialize(10))

        var date = new Date()
        assert.equal(date.toISOString(), helpers.serialize(date))

        done()
    })

    it('`helpers.serialize` converts object ids to strings in deeply nested objects', function (done) {
        var bStr = '111100000000000000000000',
            dStr = '111100000000000000000001',
            fStr1 = '111100000000000000000002',
            fStr2 = '111100000000000000000003',
            hStr = '111100000000000000000004'

        var doc = {
            a: 10,
            b: new ObjectID(bStr),
            c: {
                d: new ObjectID(dStr),
                e: 5,
                f: [ new ObjectID(fStr1), new ObjectID(fStr2) ],
                g: [
                    {
                        h: new ObjectID(hStr),
                        i: 'hello'
                    }
                ]
            }
        }

        var serialized = helpers.serialize(doc)

        assert.equal(serialized.a, doc.a)
        assert.equal(serialized.b, bStr)

        assert.equal(serialized.c.d, dStr)
        assert.equal(serialized.c.e, doc.c.e)
        assert.equal(serialized.c.f[0], fStr1)
        assert.equal(serialized.c.f[1], fStr2)
        assert.equal(serialized.c.g[0].h, hStr)
        assert.equal(serialized.c.g[0].i, 'hello')

        done()
    })
})

describe('elmongo helpers.mergeModelOptions', function () {
    it('when no options specified, uses default options', function () {
        var mergedOptions = helpers.mergeModelOptions(undefined, Cat)

        assert.equal(mergedOptions.protocol, 'http')
        assert.equal(mergedOptions.host, 'localhost')
        assert.equal(mergedOptions.port, 9200)
        assert.equal(mergedOptions.prefix, '')
        assert.equal(mergedOptions.type, 'cats')
    })

    it('when options.url specified, parses it correctly', function () {
        // https url
        var options = {
            url: 'https://127.0.0.1:7890'
        }

        var mergedOptions = helpers.mergeModelOptions(options, Cat)

        assert.equal(mergedOptions.protocol, 'https')
        assert.equal(mergedOptions.host, '127.0.0.1')
        assert.equal(mergedOptions.port, 7890)
        assert.equal(mergedOptions.prefix, '')
        assert.equal(mergedOptions.type, 'cats')

        // http url
        var options = {
            url: 'http://127.0.0.1:7890'
        }

        var mergedOptions = helpers.mergeModelOptions(options, Cat)

        assert.equal(mergedOptions.protocol, 'http')
        assert.equal(mergedOptions.host, '127.0.0.1')
        assert.equal(mergedOptions.port, 7890)
        assert.equal(mergedOptions.prefix, '')
        assert.equal(mergedOptions.type, 'cats')

        // url without protocol
        var options = {
            url: 'foo.bar.com:7890',
            protocol: 'https',
            prefix: 'test'
        }

        var mergedOptions = helpers.mergeModelOptions(options, Cat)

        assert.equal(mergedOptions.protocol, 'https')
        assert.equal(mergedOptions.host, 'foo.bar.com')
        assert.equal(mergedOptions.port, 7890)
        assert.equal(mergedOptions.prefix, 'test')
        assert.equal(mergedOptions.type, 'cats')

        // url with hostname fails - must specify host and port
        var options = {
            url: 'foo1.bar.io'
        }

        assert.throws(function () {
            var mergedOptions = helpers.mergeModelOptions(options, Cat)
        }, Error)

        // url with different protocol than options.protocol fails
        var options = {
            url: 'http://foo1.bar.io',
            protocol: 'https'
        }

        assert.throws(function () {
            var mergedOptions = helpers.mergeModelOptions(options, Cat)
        }, Error)

        // url with different port than options.protocol fails
        var options = {
            url: 'http://foo1.bar.io:9200',
            port: 9300
        }

        assert.throws(function () {
            var mergedOptions = helpers.mergeModelOptions(options, Cat)
        }, Error)
    })

    it('when no options.url specified, assigns host/port/protocol/prefix from options', function () {
        var options = {
            host: 'foo1.bar.io.baz',
            port: 4321,
            protocol: 'https'
        }

        var mergedOptions = helpers.mergeModelOptions(options, Cat)

        assert.equal(mergedOptions.protocol, 'https')
        assert.equal(mergedOptions.host, 'foo1.bar.io.baz')
        assert.equal(mergedOptions.port, 4321)
        assert.equal(mergedOptions.prefix, '')
        assert.equal(mergedOptions.type, 'cats')

        var options = {
            host: 'foo1.bar.io.baz',
            port: 4321,
            protocol: 'http',
            prefix: 'tolgatest'
        }

        var mergedOptions = helpers.mergeModelOptions(options, Cat)

        assert.equal(mergedOptions.protocol, 'http')
        assert.equal(mergedOptions.host, 'foo1.bar.io.baz')
        assert.equal(mergedOptions.port, 4321)
        assert.equal(mergedOptions.prefix, 'tolgatest')
        assert.equal(mergedOptions.type, 'cats')
    })
})