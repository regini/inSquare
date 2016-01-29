#`elmongo`

##The Power of Elasticsearch for Mongoose.


`elmongo` is a [mongoose](http://mongoosejs.com/) plugin that integrates your data with [Elasticsearch](http://www.elasticsearch.org), to give you the full power of highly available, distributed search across your data.

If you have [homebrew](http://brew.sh/), you can install and run Elasticsearch with this one-liner:

```
brew install elasticsearch && elasticsearch
```

Or you can install Elasticsearch and run it in the background with this one-liner (assuming you have a `~/bin` directory):
```
curl http://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-0.90.1.zip -o temp-es.zip && unzip temp-es.zip && rm temp-es.zip && mv elasticsearch-0.90.1 ~/bin/elasticsearch && ~/bin/elasticsearch/bin/elasticsearch
```

#Install

```
npm install elmongo
```

#Usage
```js
var mongoose = require('mongoose'),
    elmongo = require('elmongo'),
    Schema = mongoose.Schema

var CatSchema = new Schema({
    name: String
})

// add the elmongo plugin to your collection
CatSchema.plugin(elmongo)

var Cat = mongoose.model('Cat', CatSchema)
```

Now setup the search index with your data:
```js
Cat.sync(function (err, numSynced) {
  // all cats are now searchable in elasticsearch
  console.log('number of cats synced:', numSynced)
})
```

At this point your Cat schema has all the power of Elasticsearch. Here's how you can search on the model:
```js
Cat.search({ query: 'simba' }, function (err, results) {
 	console.log('search results', results)
})

// Perform a fuzzy search
Cat.search({ query: 'Sphinxx', fuzziness: 0.5 }, function (err, results) {
	// ...
})

// Search in specific fields
Cat.search({ query: 'Siameez', fuzziness: 0.5, fields: [ 'breed'] }, function (err, results) {
    // ...
})

// Paginate through the data
Cat.search({ query: '*', page: 1, pageSize: 25 }, function (err, results) {
 	// ...
})

// Use `where` clauses to filter the data
Cat.search({ query: 'john', where: { age: 25, breed: 'siamese' } }, function (err, results) {
	// ...
})
```

After the initial `.sync()`, any **Cat** models you create/edit/delete with mongoose will be up-to-date in Elasticsearch. Also, `elmongo` reindexes with zero downtime. This means that your data will always be available in Elasticsearch even if you're in the middle of reindexing.

#API

##`Model.sync(callback)`

Re-indexes your collection's data in Elasticsearch. After the first `.sync()` call, Elasticsearch will be all setup with your collection's data. You can re-index your data anytime using this function. Re-indexing is done with zero downtime, so you can keep making search queries even while `.sync()` is running, and your existing data will be searchable.

Example:
```js
Cat.sync(function (err, numSynced) {
	// all existing data in the `cats` collection is searchable now
    console.log('number of docs synced:', numSynced)
})
```

##`Model.search(searchOptions, callback)`

Perform a search query on your model. Any values you provide will override the default search options. The default options are:

```js
{
    query: '*',
    fields: [ '_all' ],	// searches all fields by default
    fuzziness: 0.0,		// exact match by default
    pageSize: 25,
    page: 1
}
```

##`Model.plugin(elmongo[, options])`

Gives your collection `.search()` and `.sync()` methods, and keeps Elasticsearch up-to-date with your data when you insert/edit/delete documents with mongoose. Takes an optional `options` object to tell `elmongo` the url that Elasticsearch is running at. In `options` you can specify:

 * `protocol` - http or https (defaults to `http`)
 * `host` - the host that Elasticsearch is running on (defaults to `localhost`)
 * `port` - the port that Elasticsearch is listening on (defaults to `9200`)
 * `prefix` - adds a prefix to the model's search index, allowing you to have separate indices for the same collection on an Elasticsearch instance (defaults to no prefix)
 * `url` - allows you to specify the protocol, host and port by just passing in a url eg. `https://elasticsearch.mydomain.com:9300`. The provided url must contain at least a host and port.

Suppose you have a test database and a development database both storing models in the `Cats` collection, but you want them to share one Elasticsearch instance. With the `prefix` option, you can separate out the indices used by `elmongo` to store your data for test and development.

For tests, you could do something like:
 ```js
Cat.plugin(elmongo, { host: 'localhost', port: 9200, prefix: 'test' })
 ```
And for development you could do something like:
```js
Cat.plugin(elmongo, { host: 'localhost', port: 9200, prefix: 'development' })
```

This way, you can use the same `mongoose` collections for test and for development, and you will have separate search indices for them (so you won't have situations like test data showing up in development search results).

**Note**: there is no need to specify a `prefix` if you are using separate Elasticsearch hosts or ports. The `prefix` is simply for cases where you are sharing a single Elasticsearch instance for multiple codebases.

##`elmongo.search(searchOptions, callback)`

You can use this function to make searches that are not limited to a specific collection. Use this to search across one or several collections at the same time (without making multiple roundtrips to Elasticsearch). The default options are the same as for `Model.search()`, with one extra key: `collections`. It defaults to searching all collections, but you can specify an array of collections to search on.

```js
elmongo.search({ collections: [ 'cats', 'dogs' ], query: '*' }, function (err, results) {
	// ...
})
```

By default, `elmongo.search()` will use `localhost:9200` (the default Elasticsearch configuration). To configure it to use a different url, use `elmongo.search.config(options)`.

##`elmongo.search.config(options)`

Configure the Elasticsearch url that `elmongo` uses to perform a search when `elmongo.search()` is used. `options` can specify the same keys as `Model.plugin(elmongo, options)`. `elmongo.search.config()` has no effect on the configuration for individual collections - to configure the url for collections, use `Model.plugin()`.

Example:
```js
elmongo.search.config({ host: something.com, port: 9300 })
```

#Autocomplete

To add autocomplete functionality to your models, specify which fields you want autocomplete on in the schema:
```js
var CatSchema = new Schema({
    name: { type: String, autocomplete: true },
    age: { type: Number },
    owner: { type: ObjectId, ref: 'Person' },
    nicknames: [ { type: String, autocomplete: true } ]
})

// add the elmongo plugin to your collection
CatSchema.plugin(elmongo)

var Cat = mongoose.model('Cat', CatSchema)

var kitty = new Cat({ name: 'simba' }).save()
```

Setup the search index using `.sync()`:
```js
Cat.sync(function (err, numSynced) {
  // all cats are now searchable in elasticsearch
  console.log('number of cats synced:', numSynced)
})
```

Now you have autocomplete on `name` and `nicknames` whenever you search on those fields:
```js
Cat.search({ query: 'si', fields: [ 'name' ] }, function (err, searchResults) {
    // any cats having a name starting with 'si' will show up in the search results
})
```

-------

## Running the tests

```
npm test
```

-------

## License

(The MIT License)

Copyright (c) by Sold. <tolga@usesold.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.