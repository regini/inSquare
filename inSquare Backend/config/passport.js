// passport.js for managing login Strategies

// loading what we need
var LocalStrategy = require('passport-local').Strategy;
var FacebookStrategy = require('passport-facebook').Strategy;
var TwitterStrategy = require('passport-twitter').Strategy;
var GoogleStrategy = require('passport-google-oauth').OAuth2Strategy;
var FacebookTokenStrategy = require('passport-facebook-token');
var GoogleTokenStrategy = require('passport-google-plus-token');
var TwitterTokenStrategy = require('passport-twitter-token');

// load up the user model
var User = require('../app/models/user');

var configAuth = require('./auth');

// expose this function to our app using module.exports
module.exports = function(passport)
{
	// Session setup
	// =============
	// Required for persistent login sessions
	// Passport needs ability to serialize and unserialize users out of session

	passport.serializeUser(function(user,done){
		done(null, user.id);
	});

	passport.deserializeUser(function(id, done)
	{
		User.findById(id, function(err, user){
			done(err, user);
		});
	});

	// Local Signup
	// ============
	// Using named Strategies (we have one for login and one for signup)
	// by default if there's no name, it'd just be called 'local'
	passport.use('local-signup', new LocalStrategy({
		//by default we have username and password: overriding with email
		usernameField : 'email',
		passwordField : 'password',
		passReqToCallback : true //allows to pass back the entire request to the callback

	},
	function(req, email, password, done)
	{
		if(email)
			email = email.toLowerCase();
		// async
		// User.findOne won't fire unless data is sent back
		process.nextTick(function()
		{
			if(!req.user)
			{
				// find a user whose email is the same as the forms email
				// we are checking to see if the user is trying to login already exists
				User.findOne({ 'local.email' : email }, function(err, user)
				{
					if (err) return done(err);

					if(user)
					{
						return done(null, false, req.flash('signupMessage', 'That email is already taken.'));
					} else
					{
						var newUser = new User();
						newUser.local.email = email;
						newUser.local.password = newUser.generateHash(password);

						newUser.save(function(err)
						{
							if(err) throw err;

							return done(null, newUser);
						});
					}
				});
			}else if(!req.user.local.email)
			{
				User.findOne( {'local.email' : email }, function(err,user)
				{
					if(err)
						return done(err);
					if(user)
					{
						return done(null, false, req.flash('loginMessage', 'That email is already taken.'));
					} else
					{
						var user = req.user;
						user.local.email = email;
						user.local.password = user.generateHash(password);
						user.save(function(err)
						{
							if(err)
								return done(err);

							return done(null, user);
						});
					}
				});
			} else
			{
				return done(null, user);
			}
		});
	}));

	// Local Login
	// ===========
	passport.use('local-login', new LocalStrategy({
		usernameField : 'email',
		passwordField : 'password',
		passReqToCallback : true
	},
	function(req, email, password, done)
	{
		if(email)
			email = email.toLowerCase();
		process.nextTick(function()
		{
			User.findOne( { 'local.email' : email }, function(err, user)
			{
				if(err)  return done(err);

				if(!user)
					return done(null, false, req.flash('loginMessage', 'No User found.'));

				if(!user.validPassword(password))
					return done(null, false, req.flash('loginMessage', 'Oops wrong password!'));


				// Success
				return done(null, user);
			});
		});
	}));

	// Facebook
	// ========

	passport.use(new FacebookTokenStrategy({
		clientID        : configAuth.facebookAuth.clientID,
		clientSecret    : configAuth.facebookAuth.clientSecret
  }, function(accessToken, refreshToken, profile, done) {
	        process.nextTick(function() {

	            // find the user in the database based on their facebook id
	            User.findOne({ 'facebook.id' : profile.id }, function(err, user) {

	            	console.log(profile);
	                // if there is an error, stop everything and return that
	                // ie an error connecting to the database
	                if (err)
	                    return done(err);

	                // if the user is found, then log them in
	                if (user)
	                {
	                	// if there is a user id already but no token (user was linked at one point and then removed)
                        if (!user.facebook.token) {
                            user.facebook.token = accessToken;
                            user.facebook.name  = profile.name.givenName + ' ' + profile.name.familyName;
                            user.facebook.email = (profile.emails[0].value || '').toLowerCase();

                            user.save(function(err) {
                                if (err)
                                    return done(err);

                                return done(null, user);
                            });
                        }
	                    return done(null, user); // user found, return that user
	                } else {
	                    // if there is no user found with that facebook id, create them
	                    var newUser            = new User();

	                    // set all of the facebook information in our user model
	                    newUser.facebook.id    = profile.id; // set the users facebook id
	                    newUser.facebook.token = accessToken; // we will save the token that facebook provides to the user
	                    newUser.facebook.name  = profile.name.givenName + ' ' + profile.name.familyName; // look at the passport user profile to see how names are returned
	                    newUser.facebook.email = (profile.emails[0].value || '').toLowerCase();

	                    // save our user to the database
	                    newUser.save(function(err) {
	                        if (err)
	                            throw err;

	                        // if successful, return the new user
	                        return done(null, newUser);
	                    });
	                }
	            });
	        });
		}));

	passport.use(new FacebookStrategy({

        // pull in our app id and secret from our auth.js file
        clientID        : configAuth.facebookAuth.clientID,
        clientSecret    : configAuth.facebookAuth.clientSecret,
        callbackURL     : configAuth.facebookAuth.callbackURL,
        profileFields: ['id', 'emails', 'name'],
        passReqToCallback: true
    },

    // facebook will send back the token and profile
    function(req, token, refreshToken, profile, done) {

		if(!req.user)
		{
	        process.nextTick(function() {

	            // find the user in the database based on their facebook id
	            User.findOne({ 'facebook.id' : profile.id }, function(err, user) {

	            	console.log(profile);
	                // if there is an error, stop everything and return that
	                // ie an error connecting to the database
	                if (err)
	                    return done(err);

	                // if the user is found, then log them in
	                if (user)
	                {
	                	// if there is a user id already but no token (user was linked at one point and then removed)
                        if (!user.facebook.token) {
                            user.facebook.token = token;
                            user.facebook.name  = profile.name.givenName + ' ' + profile.name.familyName;
                            user.facebook.email = (profile.emails[0].value || '').toLowerCase();

                            user.save(function(err) {
                                if (err)
                                    return done(err);

                                return done(null, user);
                            });
                        }
	                    return done(null, user); // user found, return that user
	                } else {
	                    // if there is no user found with that facebook id, create them
	                    var newUser            = new User();

	                    // set all of the facebook information in our user model
	                    newUser.facebook.id    = profile.id; // set the users facebook id
	                    newUser.facebook.token = token; // we will save the token that facebook provides to the user
	                    newUser.facebook.name  = profile.name.givenName + ' ' + profile.name.familyName; // look at the passport user profile to see how names are returned
	                    newUser.facebook.email = (profile.emails[0].value || '').toLowerCase();

	                    // save our user to the database
	                    newUser.save(function(err) {
	                        if (err)
	                            throw err;

	                        // if successful, return the new user
	                        return done(null, newUser);
	                    });
	                }
	            });
	        });
		} else
		{
			var user = req.user;

			user.facebook.id = profile.id;
			user.facebook.token = token;
			user.facebook.name  = profile.name.givenName + ' ' + profile.name.familyName;
			user.facebook.email = (profile.emails[0].value || '').toLowerCase();

			user.save(function(err) {
                    if (err)
                        throw err;
                    return done(null, user);
                });
		}
    }));

	// Twitter
	// =======
	passport.use(new TwitterTokenStrategy({
		consumerKey : configAuth.twitterAuth.consumerKey,
		consumerSecret : configAuth.twitterAuth.consumerSecret
  }, function(accessToken, tokenSecret, profile, done) {
		process.nextTick(function() {

			// find the user in the database based on their twitter id
			User.findOne({ 'twitter.id' : profile.id }, function(err, user) {

				console.log(profile);
					// if there is an error, stop everything and return that
					// ie an error connecting to the database
					if (err)
							return done(err);

					// if the user is found, then log them in
					if (user)
					{
						// if there is a user id already but no token (user was linked at one point and then removed)
								if (!user.twitter.token) {
										user.twitter.token = accessToken;
										user.twitter.name  = profile.name.givenName + ' ' + profile.name.familyName;
										user.twitter.email = (profile.emails[0].value || '').toLowerCase();

										user.save(function(err) {
												if (err)
														return done(err);

												return done(null, user);
										});
								}
							return done(null, user); // user found, return that user
					} else {
							// if there is no user found with that twitter id, create them
							var newUser = new User();

							// set all of the facebook information in our user model
							newUser.twitter.id    = profile.id; // set the users facebook id
							newUser.twitter.token = accessToken; // we will save the token that facebook provides to the user
							newUser.twitter.name  = profile.name.givenName + ' ' + profile.name.familyName; // look at the passport user profile to see how names are returned
							newUser.twitter.email = (profile.emails[0].value || '').toLowerCase();

							// save our user to the database
							newUser.save(function(err) {
									if (err)
											throw err;

									// if successful, return the new user
									return done(null, newUser);
							});
					}
			});
		});
	}));

	passport.use(new TwitterStrategy({
		consumerKey : configAuth.twitterAuth.consumerKey,
		consumerSecret : configAuth.twitterAuth.consumerSecret,
		callbackURL : configAuth.twitterAuth.callbackURL,
		passReqToCallback: true
	},

	function(req, token, tokenSecret, profile, done)
	{
		process.nextTick(function()
		{
			if(!req.user)
			{
				User.findOne({ 'twitter.id' : profile.id}, function(err,user)
				{
					if(err) return done(err);

					if(user)
					{
						if(!user.twitter.token)
						{
							user.twitter.token = token;
							user.twitter.username = profile.username;
							user.twitter.displayName = profile.displayName;

							user.save(function(err)
							{
								if(err) return done(err);

								return done(null, user);
							});
						}

						return done(null, user);
					} else
					{
						var newUser = new User();

						newUser.twitter.id = profile.id;
						newUser.twitter.token = token;
						newUser.twitter.username = profile.username;
						newUser.twitter.displayName = profile.displayName;

						newUser.save( function (err) {
							if(err) throw err;
							return done(null, newUser);
						});
					}
				});
			} else
			{
				var user = req.user;
				user.twitter.id = profile.id;
				user.twitter.token = token;
				user.twitter.username = profile.username;
				user.twitter.displayName = profile.displayName;

				user.save(function(err)
				{
					if(err) return done(err);

					return done(null, user);
				});
			}
		});
	}
	));

	// Google
	// =======
	passport.use(new GoogleTokenStrategy({
		clientID         : configAuth.googleAuthApp.clientID,
		clientSecret     : configAuth.googleAuthApp.clientSecret,
    passReqToCallback: true
	}, function(req, accessToken, refreshToken, profile, next) {
		process.nextTick(function() {

			// find the user in the database based on their google id
			User.findOne({ 'google.id' : profile.id }, function(err, user) {

				console.log(profile);
					// if there is an error, stop everything and return that
					// ie an error connecting to the database
					if (err)
							return done(err);

					// if the user is found, then log them in
					if (user)
					{
						// if there is a user id already but no token (user was linked at one point and then removed)
								if (!user.google.token) {
										user.google.token = accessToken;
										user.google.name  = profile.name.givenName + ' ' + profile.name.familyName;
										user.google.email = (profile.emails[0].value || '').toLowerCase();

										user.save(function(err) {
												if (err)
														return done(err);

												return done(null, user);
										});
								}
							return done(null, user); // user found, return that user
					} else {
							// if there is no user found with that google id, create them
							var newUser = new User();

							// set all of the facebook information in our user model
							newUser.google.id    = profile.id; // set the users facebook id
							newUser.google.token = accessToken; // we will save the token that facebook provides to the user
							newUser.google.name  = profile.name.givenName + ' ' + profile.name.familyName; // look at the passport user profile to see how names are returned
							newUser.google.email = (profile.emails[0].value || '').toLowerCase();

							// save our user to the database
							newUser.save(function(err) {
									if (err)
											throw err;

									// if successful, return the new user
									return done(null, newUser);
							});
					}
			});
		});
	}));

	passport.use(new GoogleStrategy({
				clientID        : configAuth.googleAuth.clientID,
        clientSecret    : configAuth.googleAuth.clientSecret,
        callbackURL     : configAuth.googleAuth.callbackURL,
        passReqToCallback: true
    },
    function(req, token, refreshToken, profile, done) {

        // make the code asynchronous
        // User.findOne won't fire until we have all our data back from Google
        process.nextTick(function() {
        	if(!req.user)
        	{
	            // try to find the user based on their google id
	            User.findOne({ 'google.id' : profile.id }, function(err, user)
	            {
	                if (err)
	                    return done(err);

	                if (user)
	                {

	                	if(!user.google.token)
	                	{
	                		user.google.token = token;
	                		user.google.name = profile.displayName;
	                		user.google.email = (profile.emails[0].value || '').toLowerCase();

	                		user.save(function(err)
	                		{
		                        if (err)
		                            throw err;
		                        return done(null, newUser);
	                    	});
	                	}

	                    // if a user is found, log them in
	                    return done(null, user);
	                } else {
	                    // if the user isnt in our database, create a new user
	                    var newUser          = new User();

	                    // set all of the relevant information
	                    newUser.google.id    = profile.id;
	                    newUser.google.token = token;
	                    newUser.google.name  = profile.displayName;
	                    newUser.google.email = (profile.emails[0].value || '').toLowerCase();

	                    // save the user
	                    newUser.save(function(err) {
	                        if (err)
	                            throw err;
	                        return done(null, newUser);
	                    });
	                }
	            });
			} else
			{
				var user = req.user;

				user.google.id = profile.id;
				user.google.token = token;
        user.google.name = profile.displayName;
        user.google.email = (profile.emails[0].value || '').toLowerCase();

        user.save(function(err)
        {
          if (err)
            throw err;
          return done(null, user);
        });
			}
        });

    }));
};
