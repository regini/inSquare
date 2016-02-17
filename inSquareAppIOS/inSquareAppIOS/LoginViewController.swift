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
import Crashlytics

var alreadyLoggedIn = false

class LoginViewController: UIViewController, FBSDKLoginButtonDelegate
{
    
    
    
    @IBOutlet weak var enter: UIButton!
    
    @IBAction func crashButtonTapped(sender: AnyObject) {
        Crashlytics.sharedInstance().crash()
    }

    
    //viewDidLoad
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        self.navigationController?.setNavigationBarHidden(true, animated: true)
        
        print("load")
        
        if !loggedIn
        {
            enter.hidden = true
            enter.enabled = false
            print(enter.enabled)

        }
        else if loggedIn
        {
            enter.hidden = false
            enter.enabled = true
            print(enter.enabled)

        }
        
        //FB LOGIN
        if (FBSDKAccessToken.currentAccessToken() == nil)
        {
            print("Not logged in...")
        }
        else
        {
            print("Logged in...\(FBSDKAccessToken.currentAccessToken())")

        }
        
        var loginButton = FBSDKLoginButton()
        loginButton.readPermissions = ["public_profile", "email", "user_friends"]
        loginButton.center = self.view.center
        loginButton.delegate = self
        self.view.addSubview(loginButton)
        //end FB
        
        //crashalitics
        let button = UIButton(type: UIButtonType.RoundedRect)
        button.frame = CGRectMake(20, 50, 100, 30)
        button.setTitle("Crash", forState: UIControlState.Normal)
        button.addTarget(self, action: "crashButtonTapped:", forControlEvents: UIControlEvents.TouchUpInside)
        view.addSubview(button)
        //END CRASHALITICS
       
        
    }
    //END viewDidLoad

    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(animated)
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
                        self.performSegueWithIdentifier("showNew", sender: self)
                }
            }
        }
    }//END ViewDidAppear
    
    
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
//                NSUserDefaults.standardUserDefaults().setObject(FBSDKAccessToken.currentAccessToken(), forKey: "FacebookAccessToken")
//                let fbLoginData = NSUserDefaults.standardUserDefaults().objectForKey("FacebookAccessToken")! as! FBSDKAccessToken
//                print(fbLoginData.tokenString)
//                
//                
//                let tokenString = fbLoginData.tokenString!
//                let appID = fbLoginData.appID!
//                let userID = fbLoginData.userID!
//                let expirationDate = fbLoginData.expirationDate!
//                let refreshDate = fbLoginData.refreshDate!
//                
//                
//                let accessToken = FBSDKAccessToken.init(tokenString: tokenString, permissions: fbLoginData.permissions, declinedPermissions: fbLoginData.declinedPermissions, appID: appID, userID: userID, expirationDate: expirationDate, refreshDate: refreshDate)
//                
                
                print("Login complete.\(FBSDKAccessToken.currentAccessToken())")
                loggedIn = true

                
                request(.POST, "http://recapp-insquare.rhcloud.com/auth/facebook/token", parameters:["access_token": FBSDKAccessToken.currentAccessToken().tokenString]).responseJSON { response in
                    //                print(response.request)  // original URL request
                    //                print(response.response) // URL response
                    //                print(response.data)     // server data
                    //                print(response.result)   // result of response serialization
                    if let value = response.result.value {
                        let jsn = JSON(value)
                        serverId = jsn["id"].string!
                    }
                    
                    
                    
                }
                
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
                        
                        // TODO: Move this to where you establish a user session
                        self.logUser()
                    }
                    else
                    {
                        print("error \(error)")
                    }
                })
                
            }
            else
            {
                if error != nil
                {
                    print(error.localizedDescription)
                }
            }
        }
            
            
            
    }
    
    func loginButtonDidLogOut(loginButton: FBSDKLoginButton!)
    {
        print("User logged out...")
    }
    

    //Crashalitics User Info
    func logUser() {
        // TODO: Use the current user's information
        // You can call any combination of these three methods
        Crashlytics.sharedInstance().setUserEmail(email)
        Crashlytics.sharedInstance().setUserIdentifier(serverId)
        Crashlytics.sharedInstance().setUserName(username)
    }


    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

    
    
}//ViewController
