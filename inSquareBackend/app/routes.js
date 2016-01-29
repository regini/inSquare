// router/routes

module.exports = function(router, passport)
{
	//HOME PAGE
	router.get('/', function(req, res)
	{
		console.log("BODY =====\n" + req.body);
		res.render('index.ejs'); //load index.ejs file
	});

	// LOGIN
	// =====
	router.get('/login', function(req,res)
	{
		//flash any data if it exists
		res.render('login.ejs', { message: req.flash('loginMessage') });
	});

	router.post('/login', passport.authenticate('local-login',
	{
		successRedirect : '/profile',
		failureRedirect : '/login',
		failureFlash : true
	}));

	router.get('/signup', function(req,res)
	{
		//flash any data if it exists
		res.render('signup.ejs', {message: req.flash('signupMessage')});
	});

	//process signup form
	router.post('/signup', passport.authenticate('local-signup', {
		successRedirect: '/profile', //redirect to the profile section
		failureRedirect: '/signup', //redirect back to the signup page if there's an error
		failureFlash: true //allow flash messages
		}));

	// PROFILE SECTION
	// ===============
	router.get('/profile', isLoggedIn, function(req,res)
	{
		console.log(req.user);
		res.json(req.user);
		//res.render('profile.ejs', {
		//	user: req.user //get the user from the session and pass to template
		//});
	});

	router.get('/square', isLoggedIn, function(req,res){
		res.render('square.ejs', {message: req.flash('squareMessage')})
	});
	// FACEBOOK ROUTES
	// ===============

	router.post('/auth/facebook/token', passport.authenticate('facebook-token'),
  	function (req, res) {
    // do something with req.user
			res.json({
				id : req.user.facebook.id,
				name : req.user.facebook.name,
				email : req.user.facebook.email});
  	});

	router.get('/auth/facebook', passport.authenticate('facebook', {scope: 'email'}));

	router.get('/auth/facebook/callback', passport.authenticate('facebook', {
		//successRedirect : '/profile',
		failureRedirect : '/'
	}), function(req,res) {
		res.json(req.user);
	});

	// TWITTER ROUTES
	// ==============
	router.post('/auth/twitter/token', passport.authenticate('twitter-token'),
  	function (req, res) {
    // do something with req.user
    	res.send(req.user ? 200 : 401);
  	});

	router.get('/auth/twitter', passport.authenticate('twitter'));

	router.get('/auth/twitter/callback', passport.authenticate('twitter', {
		successRedirect: '/profile',
		failureRedirect: '/'
	}));


	// GOOGLE ROUTES
	// ==============
	router.post('/auth/google/token', passport.authenticate('google-id-token'),
	function (req, res) {
	// do something with req.user
		res.json({
			id : req.user.google.id,
			name : req.user.google.name,
			email : req.user.google.email});
		});

	router.get('/auth/google', passport.authenticate('google', { scope : ['profile', 'email'] }));

    // the callback after google has authenticated the user
    router.get('/auth/google/callback',
            passport.authenticate('google', {
                    successRedirect : '/profile',
                    failureRedirect : '/'
            }));

	// CONNECTION OF ACCOUNTS (AUTHORIZE)
	// ==================================
	// LOCAL
	router.get('/connect/local', function(req, res)
	{
		res.render('connect-local.ejs', {message: req.flash('loginMessage')});
	});
	router.post('/connect/local', passport.authenticate('local-signup',
	{
		successRedirect: '/profile',
		failureRedirect: '/connect/local',
		failureFlash: true
	}));

	// FACEBOOK
	// => authentication
	router.get('/connect/facebook', passport.authorize('facebook', {scope: 'email'}));
	// => handle callback
	router.get('/connect/facebook/callback', passport.authorize('facebook', {
		successRedirect: '/profile',
		failureRedirect: '/'
	}));


	// TWITTER
	// => authentication
	router.get('/connect/twitter', passport.authorize('twitter', {scope: 'email'}));
	// => callback
	router.get('/connect/facebook/callback', passport.authorize('twitter', {
		successRedirect: '/profile',
		failureRedirect: '/'
	}));

	// GOOGLE
	// => authentication
	router.get('/connect/google', passport.authorize('google', {scope: ['profile', 'email']}));
	// => callback
	router.get('/connect/google/callback', passport.authorize('google', {
		successRedirect: '/profile',
		failureRedirect: '/'
	}));

	// LOGOUT
	// ======
	router.get('/logout', function(req, res)
	{
		req.logout();
		res.redirect('/');
	});

	// =============================================================================
	// UNLINK ACCOUNTS =============================================================
	// =============================================================================
	// used to unlink accounts. for social accounts, just remove the token
	// for local account, remove email and password
	// user account will stay active in case they want to reconnect in the future

    // local -----------------------------------
    router.get('/unlink/local', isLoggedIn, function(req, res) {
        var user            = req.user;
        user.local.email    = undefined;
        user.local.password = undefined;
        user.save(function(err) {
            res.redirect('/profile');
        });
    });

    // facebook -------------------------------
    router.get('/unlink/facebook', isLoggedIn, function(req, res) {
        var user            = req.user;
        user.facebook.token = undefined;
        user.save(function(err) {
            res.redirect('/profile');
        });
    });

    // twitter --------------------------------
    router.get('/unlink/twitter', isLoggedIn, function(req, res) {
        var user           = req.user;
        user.twitter.token = undefined;
        user.save(function(err) {
            res.redirect('/profile');
        });
    });

    // google ---------------------------------
    router.get('/unlink/google', isLoggedIn, function(req, res) {
        var user          = req.user;
        user.google.token = undefined;
        user.save(function(err) {
            res.redirect('/profile');
        });
    });
};

function isLoggedIn(req, res, next)
{
	//if user is auth-ed, go on
	if(req.isAuthenticated())
		return next();

	// else redirect to home page
	res.send("unauthorized");
	//res.redirect('/');
}
