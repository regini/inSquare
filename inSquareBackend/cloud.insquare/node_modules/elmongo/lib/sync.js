/*
    Sync a collection's data into elasticsearch (with zero downtime)
 */
var request = require('request'),
    async = require('async'),
    util = require('util'),
    mongoose = require('mongoose'),
    url = require('url'),
    helpers = require('./helpers'),
    mapping = require('./mapping')

request.defaults({agent:false})

//how many docs to index at a time in bulk
var BATCH_SIZE = 1000

/**
 * Sync (re-index) the collection with Elasticsearch, with zero downtime.
 *
 * @param  {Mongoose schema}    schema
 * @param  {Object}             options
 * @param  {Function}           cb
 *
 */
module.exports = function (schema, options, cb) {
    var self = this
    var versionedUri = (helpers.makeIndexUri(options) + '-' + new Date().toISOString()).toLowerCase()

    // console.log('versionedUri', versionedUri)

    var docsToIndex = null

    var versionedIndexName = url.parse(versionedUri).pathname.slice(1)

    var indicesToRemove = null

    var autocompletePaths = [];

    var indexMap = mapping.generateMapping(this.schema)

    async.series({
        // create an elasticsearch index, versioned with the current timestamp
        createVersionedIndex: function (next) {

            // index creation options
            var body = {
                settings: {
                    analysis: {
                        // analyzer definitions go here
                        analyzer: {
                            // default analyzer used for search & indexing
                            default: {
                                tokenizer: 'uax_url_email',
                                // indexing/search analysis:
                                // - trims leading/trailing whitespace
                                // - is case-insensitive
                                // - transforms non ascii characters into ascii character equivalents
                                // - splits on word/number boundaries and other delimiter rules
                                filter: [ 'trim', 'lowercase', 'asciifolding', 'word_delimiter_1' ]
                            },
                            // analyzer to use in indexing for autocomplete - we want ngrams of our data to be indexed
                            autocomplete_index: {
                                type: 'custom',
                                // use our own own ngram tokenizer
                                tokenizer: 'autocomplete_ngram',
                                filter: [ 'trim', 'lowercase', 'asciifolding', 'word_delimiter_1' ]
                            },
                            // analyzer to use in analyzing autocomplete search queries (don't generate ngrams from search queries)
                            autocomplete_search: {
                                type: 'custom',
                                tokenizer: 'keyword',
                                filter: [ 'trim', 'lowercase', 'asciifolding' ]
                            }
                        },
                        // filter definitions go here
                        filter: {
                            // custom word_delimiter filter to preserve original input as well as tokenize it
                            word_delimiter_1: {
                                type: 'word_delimiter',
                                preserve_original: true
                            }
                        },
                        // tokenizer definitions go here
                        tokenizer: {
                            // tokenizer to use for generating ngrams of our indexed data
                            autocomplete_ngram: {
                                type: 'edgeNGram',
                                // min and max ngram length
                                min_gram: 1,
                                max_gram: 50,
                                // generate ngrams that start from the front of the token only (eg. c, ca, cat)
                                side: 'front'
                            }
                        }
                    }
                }
            }

            // if non-default mapping info was defined on the schema (e.g. autocomplete), apply the mappings to the index
            body.mappings = {}
            body.mappings[options.type] = indexMap

            // console.log('mapping for', options.type, util.inspect(indexMap, true, 10, true))

            var reqOpts = {
                method: 'PUT',
                url: versionedUri,
                body: JSON.stringify(body)
            }

            // console.log('versionedUri', versionedUri)

            helpers.backOffRequest(reqOpts, function (err, res, body) {
                if (err) {
                    return cb(err)
                }

                if (!helpers.elasticsearchBodyOk(body)) {
                    var error = new Error('Unexpected index creation reply: '+util.inspect(body, true, 10, true))
                    error.body = body

                    return next(error)
                }

                return next()
            })
        },
        // get a count of how many documents we have to index
        countDocs: function (next) {
            self.count().exec(function (err, count) {
                if (err) {
                    return next(err)
                }

                docsToIndex = count

                return next()
            })
        },
        // populate the newly created index with this collection's documents
        populateVersionedIndex: function (next) {
            // if no documents to index, skip population
            if (!docsToIndex) {
                return next()
            }

            // stream docs - and upload in batches of size BATCH_SIZE
            var docStream = self.find().stream()

            // elasticsearch commands to perform batch-indexing
            var commandSequence = [];

            docStream.on('data', function (doc) {
                if (!doc || !doc._id) {
                    return
                }

                // get rid of mongoose-added functions
                doc = helpers.serializeModel(doc)

                var selfStream = this
                var strObjectId = doc._id

                var command = {
                    index: {
                        _index: versionedIndexName,
                        _type: options.type,
                        _id: strObjectId
                    }
                }

                // append elasticsearch command and JSON-ified doc to command
                commandSequence.push(command)
                commandSequence.push(doc)

                if (commandSequence.length === BATCH_SIZE) {
                    // pause the stream of incoming docs until we're done
                    // indexing the batch in elasticsearch
                    selfStream.pause()

                    exports.bulkIndexRequest(versionedIndexName, commandSequence, options, function (err) {
                        if (err) {
                            return next(err)
                        }

                        // empty our commandSequence
                        commandSequence = []

                        // keep streaming now that we're ready to accept more
                        selfStream.resume()
                    })
                }
            })

            docStream.on('close', function () {
                // if no documents left in buffer, don't perform a bulk index request
                if (!commandSequence.length) {
                    return next()
                }

                // take care of the rest of the docs left in the buffer
                exports.bulkIndexRequest(versionedIndexName, commandSequence, options, function (err) {
                    if (err) {
                        return next(err)
                    }

                    // empty the commandSequence
                    commandSequence = []

                    return next()
                })
            })
        },
        refreshIndices: function (next) {
            var reqOpts = {
                method: 'POST',
                url: helpers.makeDomainUri(options) + '/_refresh'
            }

            helpers.backOffRequest(reqOpts, function (err, res, body) {
                if (err) {
                    return next(err)
                }

                if (!helpers.elasticsearchBodyOk(body)) {
                    var error = new Error('Elasticsearch index refresh error:'+util.inspect(body, true, 10, true))
                    error.reqOpts = reqOpts
                    error.elasticsearchReply = body

                    return next(error)
                }

                return next()
            })
        },
        // atomically replace existing aliases for this collection with the new one we just created
        replaceAliases: function (next) {

            var reqOpts = {
                method: 'GET',
                url: helpers.makeAliasUri(options)
            }

            helpers.backOffRequest(reqOpts, function (err, res, body) {
                if (err) {
                    return next(err)
                }

                var existingIndices = Object.keys(body)

                // console.log('\nexistingIndices', body)

                var aliasName = helpers.makeIndexName(options)

                // get all aliases pertaining to this collection & are not the index we just created
                // we will remove all of them
                indicesToRemove = existingIndices.filter(function (indexName) {
                    return (indexName.indexOf(aliasName) === 0 && indexName !== versionedIndexName)
                })

                // console.log('\nindicesToRemove', indicesToRemove)

                // generate elasticsearch request body to atomically remove old indices and add the new one
                var requestBody = {
                    actions: [
                        {
                            add: {
                                alias: aliasName,
                                index: versionedIndexName
                            }
                        }
                    ]
                }

                indicesToRemove.forEach(function (indexToRemove) {
                    var removeObj = {
                        remove: {
                            alias: aliasName,
                            index: indexToRemove
                        }
                    }

                    requestBody.actions.unshift(removeObj)
                })

                var reqOpts = {
                    method: 'POST',
                    url: helpers.makeAliasUri(options),
                    body: JSON.stringify(requestBody)
                }

                helpers.backOffRequest(reqOpts, function (err, res, body) {
                    if (err) {
                        return next(err)
                    }

                    if (!helpers.elasticsearchBodyOk(body)) {
                        var error = new Error('Alias deletion error. Elasticsearch reply:'+util.inspect(body, true, 10, true))
                        error.elasticsearchReply = body
                        error.elasticsearchRequestBody = requestBody

                        return next(error)
                    }

                    return next()
                })
            })
        },
        // delete the unused indices for this collection from elasticsearch
        deleteUnusedIndices: function (next) {
            // generate parallel functions to delete unused indices
            var parDeleteFns = indicesToRemove.map(function (indexToRemove) {

                return function (parNext) {
                    var reqOpts = {
                        method: 'DELETE',
                        url: helpers.makeDomainUri(options) + '/' + indexToRemove
                    }

                    // console.log('\nindex deletion reqOpts', reqOpts)

                    helpers.backOffRequest(reqOpts, function (err, res, body) {
                        if (err) {
                            return parNext(err)
                        }

                        if (!helpers.elasticsearchBodyOk(body)) {
                            var error = new Error('Index deletion error for index '+indexToRemove+'. Elasticsearch reply:'+util.inspect(body, true, 10, true))
                            error.elasticsearchReply = body

                            return parNext(error)
                        }

                        return parNext()
                    })
                }
            })

            async.parallel(parDeleteFns, next)
        }
    }, function (err) {
        if (err) {
            return cb(err)
        }

        return cb(null, docsToIndex)
    })
}

/**
 * Run a bulk index request using `commandSequence`, then pass control to `callback`.
 *
 * @param  {Array}      commandSequence array of elasticsearch indexing commands
 * @param  {Function}   callback        completion callback. Signature: function (err)
 * @api private
 */
exports.bulkIndexRequest = function (indexName, commandSequence, options, callback) {
    if (!commandSequence.length) {
        return callback()
    }

    // finalize request body as newline-separated JSON docs
    var body = commandSequence.map(JSON.stringify).join('\n')+'\n'

    var bulkIndexUri = helpers.makeBulkIndexUri(indexName, options)

    var reqOpts = {
        method: 'POST',
        url: bulkIndexUri,
        body: body
    }

    // console.log('bulk index reqOpts', util.inspect(reqOpts, true, 10, true))

    helpers.backOffRequest(reqOpts, function (err, res, body) {
        if (err) {
            return callback(err)
        }

        if (body.error) {
            var error = new Error('Elasticsearch sent an error reply back after bulk indexing.')
            error.elasticsearchReply = body
            error.commandSequence = commandSequence
            error.indexName = indexName
            return callback(error)
        }

        return callback(null)
    })
}
