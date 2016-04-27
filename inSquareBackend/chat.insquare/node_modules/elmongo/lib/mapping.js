var util = require('util'),
	mongoose = require('mongoose'),
	Schema = mongoose.Schema,
	ObjectId = require('mongodb').ObjectID

/**
 * Deep-traverse a Mongoose schema and generate an elasticsearch mapping object
 * @param  {Object} schema - mongoose schema
 * @return {Object}
 */
exports.generateMapping = function (schema) {
	// console.log('generateMapping schema.tree', schema.tree)
	// console.log('generateMapping schema.paths', util.inspect(schema.paths, true, 3, true))

    var mapping = {
		properties: {

		}
	}

	Object
	.keys(schema.paths)
	.forEach(function (path) {
		// the mongoose information associated to the path
		var pathInfo = schema.paths[path];

		var pathArray = path.split('.')

		var currentLocation = mapping.properties

		// build out the mapping object by traversing the path defined by `pathArray` and building it in `mapping`
		pathArray.forEach(function (pathEntry, i) {
			if (!currentLocation[pathEntry])
				currentLocation[pathEntry] = {}

			if (i === pathArray.length - 1) {
				// we're at the lowest level of the mapping object for this `path`. Set the elasticsearch mapping info for it.

				// determine the type to set on the field in `mapping`
				if (pathInfo.instance) {
					var instanceName = pathInfo.instance.toLowerCase()

				 	if (instanceName === 'objectid') {
						currentLocation[pathEntry].type = 'string'
					} else if (instanceName === 'number') {
						currentLocation[pathEntry].type = 'double'
					} else {
						currentLocation[pathEntry].type = pathInfo.instance.toLowerCase()
					}
				} else if (pathInfo.caster && pathInfo.caster.instance) {
					if (pathInfo.caster.instance.toLowerCase() === 'objectid') {
						currentLocation[pathEntry].type = 'string'
					} else {
						var type = pathInfo.caster.instance.toLowerCase()

						if (type === 'number') {
							currentLocation[pathEntry].type = 'double'
						} else {
							currentLocation[pathEntry].type = type
						}
					}
				} else if (pathInfo.options) {
					// console.log('pathInfo.options', pathInfo.options)
					var typeClass = pathInfo.options.type

					if (Array.isArray(typeClass)) {
						if (!typeClass[0]) {
							currentLocation[pathEntry].type = 'object'
						} else {
							// this low-level property in the schema is an array. Set type of the array entries
							var arrEntryTypeClass = typeClass[0].type

							currentLocation[pathEntry].type = exports.getElasticsearchTypeFromMongooseType(arrEntryTypeClass)
						}
					} else {
						// `options` exists on pathInfo and it's not an array of types
						currentLocation[pathEntry].type = exports.getElasticsearchTypeFromMongooseType(typeClass)
					}
				}

				if (!currentLocation[pathEntry].type) {
					// default to object type
					currentLocation[pathEntry].type = 'object'
				}

				// set autocomplete analyzers if user specified it in the model
				if (
					(pathInfo.options && pathInfo.options.autocomplete) ||
					(pathInfo.caster && pathInfo.caster.options && pathInfo.caster.options.autocomplete)
				) {
					currentLocation[pathEntry].index_analyzer = 'autocomplete_index'
					currentLocation[pathEntry].search_analyzer ='autocomplete_search'
				}
			} else {
				// mark this location in the mapping as an object (only set if it hasn't been set by a previous path already)
				if (!currentLocation[pathEntry].properties) {
					currentLocation[pathEntry].properties = {}
				}

				// keep going deeper into the mapping object - we haven't reached the end of the `path`.
				currentLocation = currentLocation[pathEntry].properties
			}
		})
	})

	// console.log('\ngenerateMapping - mapping:', util.inspect(mapping, true,10, true))

	return mapping
}

exports.getElasticsearchTypeFromMongooseType = function (typeClass) {
	if (typeClass === String) {
		return 'string'
	}
	if (typeClass === Number) {
		return 'double'
	}
	if (typeClass === Date) {
		return 'date'
	}
	if (typeClass === Boolean) {
		return 'boolean'
	}
	if (typeClass === Array) {
		return 'object'
	}
}