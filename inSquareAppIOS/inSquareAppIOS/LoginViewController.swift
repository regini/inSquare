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

//serve in modo che se torno a loginVC da gia loggato non fa in automatico segue a home
class LoginViewController: UIViewController, FBSDKLoginButtonDelegate
{
    @IBOutlet weak var enter: UIButton!
    //viewDidLoad
    override func viewDidLoad()
    {
        super.viewDidLoad()
        //print(clearUsersSavedInCoreData())
        print(howManyUserInCoreData())
        
        
        self.enter.enabled = (FBSDKAccessToken.currentAccessToken() != nil)
        self.enter.hidden = (FBSDKAccessToken.currentAccessToken() == nil)
        
        
        if isLoggedInCoreData()
        {
            //print("\(isLoggedInCoreData()) vs \(howManyUserInCoreData())")
            

                //fetchDataFromCoreData()
        }

        self.navigationController?.setNavigationBarHidden(true, animated: true)
        
        //FB LOGIN
        if (FBSDKAccessToken.currentAccessToken() == nil)
        {
            print("Not logged in...")
        }
        else
        {
            print("Logged in...\(FBSDKAccessToken.currentAccessToken())")
        }
        
        //ADD LOGIN/LOGOUT BUTTON TO LOGINVC
        var loginButton = FBSDKLoginButton()
        loginButton.readPermissions = ["public_profile", "email", "user_friends"]
        loginButton.center = self.view.center
        loginButton.delegate = self
        self.view.addSubview(loginButton)

    }
    //END viewDidLoad

    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(animated)
        self.navigationController?.setNavigationBarHidden(true, animated: false)

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


    }
    
    override func viewDidAppear(animated: Bool)
    {
        super.viewDidAppear(animated)
        
        print("did appear")
        
        self.navigationController?.setNavigationBarHidden(true, animated: false)
        
                
//                dispatch_async(dispatch_get_main_queue())
//                    {
//                        self.performSegueWithIdentifier("tableView", sender: self)
//                }
        
    }//END ViewDidAppear
    
    override func viewWillDisappear(animated: Bool)
    {
        super.viewWillDisappear(animated)

        
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
                                //era in viewdidappear credo
                                Answers.logLoginWithMethod("Facebook", success: true, customAttributes: [:])
                                self.enter.enabled = (FBSDKAccessToken.currentAccessToken() != nil)
                                self.enter.hidden = (FBSDKAccessToken.currentAccessToken() == nil)
                                
                                
                                //UIApplication.sharedApplication().registerForRemoteNotifications()
                                
                                //NOTIFICATION
                                let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
                                appDelegate.registerForAllNotifications() // Register this app for notifications
                                
                                
                                self.goToMap()
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
        clearUsersSavedInCoreData()
        
        //UNREGISTER FROM REMOTE NOTIFICATION
        UIApplication.sharedApplication().unregisterForRemoteNotifications()

        
        //TODO TRIGGERA UNA LOCAL NOTIFICATION
        
        self.enter.enabled = (FBSDKAccessToken.currentAccessToken() != nil)
        self.enter.hidden = (FBSDKAccessToken.currentAccessToken() == nil)
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
            let tabBarController = segue.destinationViewController as! TabBarViewController
        }

        
    }
    
    
    func goToMap()
    {
        print("let's go to map")
        var myTabBar = self.storyboard?.instantiateViewControllerWithIdentifier("tabBarController") as! UITabBarController
        UIApplication.sharedApplication().delegate!.window?!.rootViewController = myTabBar
        
        //                dispatch_async(dispatch_get_main_queue())
        //                    {
        //                        self.performSegueWithIdentifier("tableView", sender: self)
        //                }
    }
    
    //customizza er prendere altri dati
    func returnUserData()
    {
        let graphRequest : FBSDKGraphRequest = FBSDKGraphRequest(graphPath: "me", parameters: nil)
        graphRequest.startWithCompletionHandler({ (connection, result, error) -> Void in
            
            if ((error) != nil)
            {
                // Process error
                print("Error: \(error)")
            }
            else
            {
                print("fetched user: \(result)")
                let userName : NSString = result.valueForKey("name") as! NSString
                print("User Name is: \(userName)")
                let userEmail : NSString = result.valueForKey("email") as! NSString
                print("User Email is: \(userEmail)")
            }
        })
    }

 
    
}//ViewController
