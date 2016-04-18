//
//  LoginViewController.swift
//  inSquareAppIOS
//
//  Created by Alessandro Steri on 26/01/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import FBSDKCoreKit
import FBSDKLoginKit
import Alamofire
import CoreData
import Crashlytics

var alreadyLoggedIn = false //serve in modo che se torno a loginVC da gia loggato non fa in automatico segue a home
var settedUpCrashlytics = false
class LoginViewController: UIViewController, FBSDKLoginButtonDelegate
{
    @IBOutlet weak var enter: UIButton!
    
//    @IBAction func unwindToLogin(segue: UIStoryboardSegue)
//    {
//    }
    
//    @IBAction func crashButtonTapped(sender: AnyObject) {
//        Crashlytics.sharedInstance().crash()
//    }

    @IBAction func buttPress(sender: AnyObject)
    {
        var myTabBar = self.storyboard?.instantiateViewControllerWithIdentifier("tabBarController") as! UITabBarController
        
       // var appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
        
        UIApplication.sharedApplication().delegate!.window?!.rootViewController = myTabBar
    }
    
    
    //viewDidLoad
    override func viewDidLoad()
    {
        super.viewDidLoad()
        //print(clearUsersSavedInCoreData())
        print(howManyUserInCoreData())
        
        
        //fatalError("QWERTYUIOP")
      
        if isLoggedInCoreData()
        {
            print("\(isLoggedInCoreData()) vs \(howManyUserInCoreData())")
            loggedIn = true

            //recupera dati da CoreData cosi quando viewne lanciato viewWill appear fa segue to home
            let appDel: AppDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
            let context: NSManagedObjectContext = appDel.managedObjectContext
            
            //var alreadyLoggedUser = NSEntityDescription.insertNewObjectForEntityForName("Users", inManagedObjectContext: context)
            
            let request = NSFetchRequest(entityName: "Users")
            
            // request.predicate = NSPredicate(format: "username = %@", "Ralphie")
            
            request.returnsObjectsAsFaults = false
            
            do {
                
                let results = try context.executeFetchRequest(request)
                
                print("USERS (has to be 0 or 1): \(results.count)")
                print("USERS (details): \(results)")

                
                if results.count == 1
                {
                    for result in results as! [NSManagedObject]
                    {
                    
                                        /*
                    
                                        context.deleteObject(result)
                    
                                        //result.setValue("Ralphie", forKey: "username")
                    
                                        do {
                    
                                        try context.save()
                    
                                        } catch {
                    
                                        }
                    
                                        */
                    
                                    //setting up datas
                                        if let usernameCD = result.valueForKey("username") as? String
                                        {
                                            print(usernameCD)
                                            username = usernameCD
                                        }
                                        if let serverIdCD = result.valueForKey("serverId") as? String
                                        {
                                            print(serverIdCD)
                                            serverId = serverIdCD
                                        }
                                        if let fbIdCD = result.valueForKey("fbId") as? String
                                        {
                                            print(fbIdCD)
                                            fbId = fbIdCD
                                        }
                                        if let emailCD = result.valueForKey("email") as? String
                                        {
                                            print(emailCD)
                                            email = emailCD
                                        }
                                        if let accessTokenCD = result.valueForKey("accessToken") as? String
                                        {
                                            print(accessTokenCD)
                                            accessToken = accessTokenCD
                                        }
                                        if let userAvatarUrlCD = result.valueForKey("userAvatarUrl") as? String
                                        {
                                            //alternativa
//                                            print("AVATAR URL \(userAvatarUrlCD)")
//                                            userAvatarUrl = userAvatarUrlCD
//                                            
//                                            var url : NSString = "\(userAvatarUrl)"
//                                            var urlStr : NSString = url.stringByAddingPercentEscapesUsingEncoding(NSUTF8StringEncoding)!
//                                            var searchURL : NSURL = NSURL(string: urlStr as String)!
//                                            
//                                            //let url = NSURL(string: "\(userAvatarUrl)")
//                                            print("AVATAR URL 2 \(searchURL)")
//                                            
//                                            let data = NSData(contentsOfURL: searchURL) //make sure your image in this url does exist, otherwise unwrap in a if let check

                                            print("AVATAR URL \(userAvatarUrlCD)")
                                            userAvatarUrl = userAvatarUrlCD
                                            
                                            let url = NSURL(string: "\(userAvatarUrl)")
                                            let data = NSData(contentsOfURL: url!) //make sure your image in this url does exist, otherwise unwrap in a if let check
                                            if UIImage(data: data!) != nil
                                            {
                                                userAvatar = UIImage(data: data!)!
                                            }
                                            else
                                            {
                                                print("ERROR: personal FB img URL not working")
                                            }

                                            if userAvatarUrl != ""
                                            {
                                                if let url = NSURL(string: userAvatarUrl)
                                                {
                                                    let data = NSData(contentsOfURL: url) //make sure your image in this url does exist, otherwise unwrap in a if let check
                                                    if UIImage(data: data!) != nil
                                                    {
                                                        userAvatar = UIImage(data: data!)!
                                                    }
                                                    else
                                                    {
                                                        print("ERROR: personal FB img URL not working")
                                                    }
                                                }
                                                else
                                                {
                                                    print("NO IMG URL SOMETHING WENT WRONG")
                                                }
                                            }
                                            
                                        }
                        print("aaaaaaaaaa4")

                    }
                }
            }
            catch
            {
                print("Fetch Failed")
            }
            //


        }

        self.navigationController?.setNavigationBarHidden(true, animated: true)

        if !loggedIn
        {
            enter.hidden = true
            enter.enabled = false

        }
        else if loggedIn
        {
            enter.hidden = false
            enter.enabled = true
        }
        
        //FB LOGIN
        if (FBSDKAccessToken.currentAccessToken() == nil)
        {
            if !loggedIn { print("Not logged in...")}
        }
        else
        {
            print("Logged in...\(FBSDKAccessToken.currentAccessToken())")
        }
        
        if !loggedIn
        {
            print("aaaaaaaaaa1")

            var loginButton = FBSDKLoginButton()
            loginButton.readPermissions = ["public_profile", "email", "user_friends"]
            loginButton.center = self.view.center
            loginButton.delegate = self
            self.view.addSubview(loginButton)
        }
        //end FB
        
//        //crashalitics
//        let button = UIButton(type: UIButtonType.RoundedRect)
//        button.frame = CGRectMake(20, 50, 100, 30)
//        button.setTitle("Crash", forState: UIControlState.Normal)
//        button.addTarget(self, action: "crashButtonTapped:", forControlEvents: UIControlEvents.TouchUpInside)
//        view.addSubview(button)
//        //END CRASHALITICS
        
    }
    //END viewDidLoad

    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(animated)
        
        if (loggedIn == true) && (serverId != "")
        {
            print("aaaaaaaaaa2")
            dispatch_async(dispatch_get_main_queue())
                {
                    self.performSegueWithIdentifier("tableView", sender: self)
            }
        }
        
        
        var name = "LoginViewController"
        
        // The UA-XXXXX-Y tracker ID is loaded automatically from the
        // GoogleService-Info.plist by the `GGLContext` in the AppDelegate.
        // If you're copying this to an app just using Analytics, you'll
        // need to configure your tracking ID here.
        // [START screen_view_hit_swift]
        var tracker = GAI.sharedInstance().defaultTracker
        tracker.set(kGAIScreenName, value: name)
        
        var builder = GAIDictionaryBuilder.createScreenView()
        tracker.send(builder.build() as [NSObject : AnyObject])
        // [END screen_view_hit_swift]

        
        print("will appear")

        self.navigationController?.setNavigationBarHidden(true, animated: false)

    }
    
    override func viewDidAppear(animated: Bool)
    {
        super.viewDidAppear(animated)
        
        print("appear")
        
        self.navigationController?.setNavigationBarHidden(true, animated: false)
        
        if loggedIn
        {
            enter.hidden = false
            enter.enabled = true


            if !alreadyLoggedIn
            {
                Answers.logLoginWithMethod("Facebook",
                    success: true,
                    customAttributes: [:])
                alreadyLoggedIn = true
                
                dispatch_async(dispatch_get_main_queue())
                    {
                        self.performSegueWithIdentifier("tableView", sender: self)
                }
            }
        }
    }//END ViewDidAppear
    
    override func viewWillDisappear(animated: Bool)
    {
        super.viewWillDisappear(animated)
        
        if loggedIn
        {
            updateFavouriteSquares()
        }
    }
    
    //didRecieveMemoryWarning
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    //ENDdidRecieveMemoryWarning

    
    //Facebook Login
    func loginButton(loginButton: FBSDKLoginButton!, didCompleteWithResult result: FBSDKLoginManagerLoginResult!, error: NSError!)
    {
        if error == nil
        {
             //post su server access token FB
            if FBSDKAccessToken.currentAccessToken() != nil
            {
                print("Login complete.\(FBSDKAccessToken.currentAccessToken())")
                loggedIn = true

                request(.POST, "\(serverMainUrl)/auth/facebook/token", parameters:["access_token": FBSDKAccessToken.currentAccessToken().tokenString]).responseJSON { response in
                    if let value = response.result.value
                    {
                        let jsn = JSON(value)
                        serverId = jsn["id"].string!
                        userAvatarUrl = jsn["picture"].string!
                        let url = NSURL(string: jsn["picture"].string!)
                        let data = NSData(contentsOfURL: url!) //make sure your image in this url does exist, otherwise unwrap in a if let check
                        if UIImage(data: data!) != nil
                        {
                            userAvatar = UIImage(data: data!)!

                        }
                        else
                        {
                            print("ERROR: personal FB img URL not working")
                        }
                        print(jsn)
                        //vedi se spostare fuori dalla post rinunciando a info su serverid di utente
                        var tracker = GAI.sharedInstance().defaultTracker
                        tracker.send(GAIDictionaryBuilder.createEventWithCategory("Log-in", action: "Loged-in", label: "User \(serverId) logged in", value: nil).build() as [NSObject : AnyObject])
                        
                        //set up parameters
                        let req = FBSDKGraphRequest(graphPath: "me", parameters: ["fields":"email,name"], tokenString: FBSDKAccessToken.currentAccessToken().tokenString, version: nil, HTTPMethod: "GET")
                        req.startWithCompletionHandler({ (connection, result, error : NSError!) -> Void in
                            if(error == nil)
                            {
                                let jsnUN = JSON(result)
                                
                                print("result \(result)")
                                print("name \(result["name"])")
                                //                    username = "\(result["name"])"
                                username = jsnUN["name"].string!
                                
                                accessToken = FBSDKAccessToken.currentAccessToken().tokenString
                                email = "\(result["email"])"
                                fbId = "\(result["id"])"
                                
                                saveUserDataInCoreData()
                                
                                // TODO: Move this to where you establish a user session
                                self.logUser() //setupcrashlytics
//                                settedUpCrashlytics = true
                            }
                            else
                            {
                                print("error \(error)")
                            }
                        })
                        
                        
                        
                    }//IFLETVALUE
                }//REQUEST
            }//IFFBSDK
            else
            {
                if error != nil
                {
                    print(error.localizedDescription)
                }
            }
        }//ERRORNIL
    }//LOGINBUTT
    
    func loginButtonDidLogOut(loginButton: FBSDKLoginButton!)
    {
        print("User logged out...")
        var tracker = GAI.sharedInstance().defaultTracker
        tracker.send(GAIDictionaryBuilder.createEventWithCategory("Log-in", action: "Loged-out", label: "User \(serverId) logged in", value: nil).build() as [NSObject : AnyObject])
        //clear serverid e clear coredata persistency
        enter.enabled = false
        enter.hidden = true
        loggedIn = false
    }
    

    //Crashalitics User Info
    func logUser() {
        // TODO: Use the current user's information
        // You can call any combination of these three methods
        Crashlytics.sharedInstance().setUserEmail(email)
        Crashlytics.sharedInstance().setUserIdentifier(serverId)
        Crashlytics.sharedInstance().setUserName(username)
        
        print("AA Crashlytics \(email)")
        print("AA Crashlytics \(serverId)")
        print("AA Crashlytics \(username)")
        
    }


    
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
      
        if (segue.identifier == "tableView")
        {
//            if !settedUpCrashlytics
//            {
//                logUser() //setupCrashlytics
//            }
            let tabBarController = segue.destinationViewController as! TabBarViewController
        }

        
    }
    
    
    
    
}//ViewController
