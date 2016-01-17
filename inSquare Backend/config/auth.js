// auth.js

// Expose our config directly to our application using module.exports
module.exports = {
	'facebookAuth' : {
		'clientID' : '743420882424724', // App ID
		'clientSecret' : 'f9becf2700ef7d4133c2130335d90890',
		'callbackURL' : 'http://recapp-insquare.rhcloud.com/auth/facebook/callback'
	},
	'twitterAuth' : {
		'consumerKey' : 'vvvXAdodg2mIo9T5hr9fSOu8Q',
		'consumerSecret' : 'CILesV5MD4VtR33gEfVHnXQtGKGdP0LtIa8nul25vYmYdu74BZ',
		'callbackURL' : 'http://recapp-insquare.rhcloud.com/auth/twitter/callback'
	},
	'googleAuth': {
		'clientID' : '231545488769-vebgt9dn7al3bjf6koujod5ir703jv8b.apps.googleusercontent.com',
		'clientSecret' : 'z62Dm7G0UPkWj8SlAuKTosQr',
		'callbackURL' : 'http://recapp-insquare.rhcloud.com/auth/google/callback'
	},
	'googleAuthApp': {
		'clientID' : '231545488769-4d1mcev9vifvlncrern52id2pqqf5u5l.apps.googleusercontent.com',
		'clientSecret' : 'Ufvuj-d6alhwGvKvLh_8Nq0K'
	}
};
