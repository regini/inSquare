//
//  AppDelegate.swift
//  inSquareAppIOS
//
//  Created by Alessandro Steri on 21/01/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import CoreData
//import FBSDKCoreKit
//import FBSDKCoreKit.h
import GoogleMaps
//import Google.Analytics.h
import Alamofire
import AeroGearPush
import Fabric
import Crashlytics


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate
{

    var window: UIWindow?
    
    func registerForAllNotifications() {
        
        // Set notifications for this application
        let notificationSettings = UIUserNotificationSettings(forTypes: [.Alert, .Badge, .Sound], categories: nil)
        
        UIApplication.sharedApplication().registerUserNotificationSettings(notificationSettings)
        
        UIApplication.sharedApplication().registerForRemoteNotifications()
        
        
    }



    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
        // Override point for customization after application launch.

        GMSServices.provideAPIKey("AIzaSyBmibIMY4xUPzLe2Sj9Ax4Ne2tNmCNoUic")
        
        //UIApplication.sharedApplication().statusBarStyle = .LightContent
        
        // Configure tracker from GoogleService-Info.plist.
        var configureError:NSError?
        GGLContext.sharedInstance().configureWithError(&configureError)
        assert(configureError == nil, "Error configuring Google services: \(configureError)")
        
        // Optional: configure GAI options.
        var gai = GAI.sharedInstance()
        gai.trackUncaughtExceptions = true  // report uncaught exceptions
        gai.logger.logLevel = GAILogLevel.Verbose  // remove before app release
        
        // Register this app for notifications
        registerForAllNotifications()
        
        // Check for launch Options, this could be from Local or Remote Notifications that was used to Open the app!
        if let options = launchOptions as? [String : AnyObject] {
            
            if let notification = options[UIApplicationLaunchOptionsLocalNotificationKey] as? UILocalNotification {
                
                if let userInfo = notification.userInfo {
                    
                    let foo = userInfo["foo"] as! String
                    print("app launced from local notification")
                    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (Int64)(2 * NSEC_PER_SEC)), dispatch_get_main_queue()) {
                        
                        self.showAlert("App Started from Local Notification", alert: "\(notification.alertBody!) foo=\(foo)", badgeCount: 0)
                    }
                }
            }
            
            
            // Remote Push Notifications
            if let userInfo = options[UIApplicationLaunchOptionsRemoteNotificationKey] as? [NSObject : AnyObject] {
                
                
                if let aps = userInfo["aps"] {
                    
                    let badgeCount = aps["badge"] as! Int
                    
                    application.applicationIconBadgeNumber = badgeCount
                    
                    let alert = aps["alert"] as! String
                    
                    var message = alert
                    
                    if let messageId = userInfo["messageId"] {
                        
                        message = "\(alert) messageId=\(messageId)"
                    }
                    
                    
                    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (Int64)(2 * NSEC_PER_SEC)), dispatch_get_main_queue()) {
                        
                        self.showAlert("App Started from Push Notification", alert: message, badgeCount: badgeCount)
                        
                    }
                    
                    
                }
                
                
                
                
            }
            
        }
        
        //Crashlytics has to be the lastone before return
        Fabric.with([Crashlytics.self])
        return true
    }
    
    //facebook: The “OpenURL” method allows your app to open again after the user has validated their login credentials. FBSDKAppEvents.activateApp() method allows Facebook to capture events within your application including Ads clicked on from Facebook to track downloads from Facebook and events like how many times your app was opened.
    func application(application: UIApplication,
        openURL url: NSURL,
        sourceApplication: String?,
        annotation: AnyObject?) -> Bool {
            //Fabric.with([Crashlytics.self])
            return FBSDKApplicationDelegate.sharedInstance().application(
                application,
                openURL: url,
                sourceApplication: sourceApplication,
                annotation: annotation)
    }
    
    
    // Called if successful registeration for APNS.
    func application(application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: NSData) {
        //send this device token to server
        //print(deviceToken)
        
        print("APNS Success")
        
        let registration = AGDeviceRegistration(serverURL: NSURL(string: "https://aerogear-insquaretest.rhcloud.com/ag-push/")!)
        
        registration.registerWithClientInfo({ (clientInfo: AGClientDeviceInformation!)  in
            
            // apply the token, to identify this device
            clientInfo.deviceToken = deviceToken
            
            clientInfo.variantID = "b79a94bc-f440-46c2-b7f6-aa6b6f2e0452"
            clientInfo.variantSecret = "291509e2-bcaf-40d9-9dbc-8e28331b6118"
            
            // --optional config--
            // set some 'useful' hardware information params
            let currentDevice = UIDevice()
            clientInfo.operatingSystem = currentDevice.systemName
            clientInfo.osVersion = currentDevice.systemVersion
            clientInfo.deviceType = currentDevice.model
            clientInfo.alias = serverId
            
            

            
            }, success:
            {
                print("UPS registration worked")
                
                
                
            }, failure: { (error:NSError!) -> () in
                print("UPS registration Error: \(error.localizedDescription)")
        })
        
        
        
    

        
        let rawTokenString = NSString(format: "%@", deviceToken)
        print("didRegisterForRemoteNotificationsWithDeviceToken rawTokenString = \(rawTokenString)")
        
        var cleanedString = rawTokenString.stringByReplacingOccurrencesOfString("<", withString: "")
        cleanedString = cleanedString.stringByReplacingOccurrencesOfString(">", withString: "")
        print("didRegisterForRemoteNotificationsWithDeviceToken APN Tester Free Device Token String = \(cleanedString)")
        
        let serverReadyPushNotificationString = cleanedString.stringByReplacingOccurrencesOfString(" ", withString: "")
        print("didRegisterForRemoteNotificationsWithDeviceToken Server Device Token String = \(serverReadyPushNotificationString)")
        
        
        request(.PATCH, "\(serverMainUrl)/apnToken?isApple=true&userId=\(serverId)&token=\(serverReadyPushNotificationString)").validate().responseString { response in
            print("REQUEST PATCH APNS TOKEN: \(response.request)")
            //print("body patch", response.request?.HTTPBody.)
            switch response.result
            {
            case .Success:
                print("RESULT PATCH APNS TOKEN: \(response.result)")
                print("RESULT PATCH APNS TOKEN: \(response.result.value)")
                
            case .Failure(let error):
                print("FALLITO \(error)")
            }
        }
        
//        // ...register device token ON SERVER
//        var urlPostSquare = "\(serverMainUrl)/apnToken"
//        urlPostSquare = urlPostSquare.stringByReplacingOccurrencesOfString(" ", withString: "%20")
//        
//        print("REQUEST URL: \(urlPostSquare)")
//        
//        var dataForBody:JSON = ["apn": serverReadyPushNotificationString, "userId": serverId]
//        
//        request(.PATCH, urlPostSquare, parameters: ["apn": serverReadyPushNotificationString, "userId": serverId]).validate().responseString { response in
//            print("REQUEST PATCH APNS TOKEN: \(response.request)")
//            //print("body patch", response.request?.HTTPBody.)
//            switch response.result
//            {
//            case .Success:
//                print("RESULT PATCH APNS TOKEN: \(response.result)")
//                print("RESULT PATCH APNS TOKEN: \(response.result.value)")
//                
//            case .Failure(let error):
//                print("FALLITO \(error)")
//            }
//        }
        
        
    }

    
    // Called if unable to register for APNS.
    func application(application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: NSError) {
        print(error)
    }
    
    func application(application: UIApplication, didReceiveRemoteNotification userInfo: [NSObject : AnyObject]) {
        print(userInfo)
        
        let notificationReceivedJSON:JSON = JSON (userInfo)
        
        print("RECEIVED NOTIFICATION IN FOREGROUND: ", notificationReceivedJSON)
        
        if application.applicationState == .Active {
            
            let alert = notificationReceivedJSON["aps"]["alert"].string
            let title = notificationReceivedJSON["aps"]["alert"]["title"].string
            let message = notificationReceivedJSON["aps"]["alert"]["body"].string
            let badgeCount = notificationReceivedJSON["aps"]["badge"].int
            
            //usa custom sound qui
            
            //MESS ID O ALTRI CAMPI
            //            if let messageId = userInfo["messageId"] {
            //                message = "\(alert) messageId=\(messageId)"
            //            }


        
            if alert == nil {print("alert")}
            if title == nil {print("title")}
            if message == nil {print("message")}
            if badgeCount == nil {print("badgeCount")}

            application.applicationIconBadgeNumber = badgeCount!
        
            print("didReceiveRemoteNotification alert message=\(alert) Badge Count=\(badgeCount)")
            
            // Show Alert to user
            showAlert("Push Notification In Foreground", alert: message!, badgeCount: badgeCount!)
            
            
        }
        
    }
    
    func application(application: UIApplication, didReceiveLocalNotification notification: UILocalNotification) {
        
        print("didReceiveLocalNotification: \(notification)")
        
        
        
        
        if let userInfo = notification.userInfo {
            
            let foo = userInfo["foo"] as! String
            print("didReceiveLocalNotification: foo=\(foo)")
            
            showAlert("Local Notification In Foreground", alert: "\(notification.alertBody!) foo=\(foo)", badgeCount: 0)
            
        }
    }
    
    
    
    func showAlert(title: String, alert: String, badgeCount: Int) {
        
        var message = "\(alert)"
        
        if badgeCount > 0 {
            message = "\(alert) badgeCount=\(badgeCount)"
        }
        
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .ActionSheet)
        
        let okAction = UIAlertAction(title: "OK", style: UIAlertActionStyle.Default) {
            UIAlertAction in
        }
        
        alertController.addAction(okAction)
        
//        dispatch_async(dispatch_get_main_queue(), {
//            let importantAlert: UIAlertController = UIAlertController(title: "Action Sheet", message: "Hello I was presented from appdelegate ;)", preferredStyle: .ActionSheet)
//            self.window?.rootViewController?.presentViewController(importantAlert, animated: true, completion: nil)
//
//        })
        
        getVisibleViewController(self.window?.rootViewController)!.presentViewController(alertController, animated: true, completion: nil)
        
        
    }
    

    func applicationWillResignActive(application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
        let currentViewController = getVisibleViewController(nil)

    }
    
    func getVisibleViewController(var rootViewController: UIViewController?) -> UIViewController? {
        
        if rootViewController == nil {
            rootViewController = UIApplication.sharedApplication().keyWindow?.rootViewController
        }
        
        if rootViewController?.presentedViewController == nil {
            return rootViewController
        }
        
        if let presented = rootViewController?.presentedViewController {
            if presented.isKindOfClass(UINavigationController) {
                let navigationController = presented as! UINavigationController
                return navigationController.viewControllers.last!
            }
            
            if presented.isKindOfClass(UITabBarController) {
                let tabBarController = presented as! UITabBarController
                return tabBarController.selectedViewController!
            }
            
            return getVisibleViewController(presented)
        }
        return nil
    }

    func applicationDidEnterBackground(application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(application: UIApplication) {
        // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
        // Saves changes in the application's managed object context before the application terminates.
        self.saveContext()
    }

    // MARK: - Core Data stack

    lazy var applicationDocumentsDirectory: NSURL = {
        // The directory the application uses to store the Core Data store file. This code uses a directory named "com.recapp.inSquareAppIOS" in the application's documents Application Support directory.
        let urls = NSFileManager.defaultManager().URLsForDirectory(.DocumentDirectory, inDomains: .UserDomainMask)
        return urls[urls.count-1]
    }()

    lazy var managedObjectModel: NSManagedObjectModel = {
        // The managed object model for the application. This property is not optional. It is a fatal error for the application not to be able to find and load its model.
        let modelURL = NSBundle.mainBundle().URLForResource("inSquareAppIOS", withExtension: "momd")!
        return NSManagedObjectModel(contentsOfURL: modelURL)!
    }()

    lazy var persistentStoreCoordinator: NSPersistentStoreCoordinator = {
        // The persistent store coordinator for the application. This implementation creates and returns a coordinator, having added the store for the application to it. This property is optional since there are legitimate error conditions that could cause the creation of the store to fail.
        // Create the coordinator and store
        let coordinator = NSPersistentStoreCoordinator(managedObjectModel: self.managedObjectModel)
        let url = self.applicationDocumentsDirectory.URLByAppendingPathComponent("SingleViewCoreData.sqlite")
        var failureReason = "There was an error creating or loading the application's saved data."
        do {
            try coordinator.addPersistentStoreWithType(NSSQLiteStoreType, configuration: nil, URL: url, options: nil)
        } catch {
            // Report any error we got.
            var dict = [String: AnyObject]()
            dict[NSLocalizedDescriptionKey] = "Failed to initialize the application's saved data"
            dict[NSLocalizedFailureReasonErrorKey] = failureReason

            dict[NSUnderlyingErrorKey] = error as! NSError
            let wrappedError = NSError(domain: "YOUR_ERROR_DOMAIN", code: 9999, userInfo: dict)
            // Replace this with code to handle the error appropriately.
            // abort() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.
            NSLog("Unresolved error \(wrappedError), \(wrappedError.userInfo)")
            abort()
        }
        
        return coordinator
    }()

    lazy var managedObjectContext: NSManagedObjectContext = {
        // Returns the managed object context for the application (which is already bound to the persistent store coordinator for the application.) This property is optional since there are legitimate error conditions that could cause the creation of the context to fail.
        let coordinator = self.persistentStoreCoordinator
        var managedObjectContext = NSManagedObjectContext(concurrencyType: .MainQueueConcurrencyType)
        managedObjectContext.persistentStoreCoordinator = coordinator
        return managedObjectContext
    }()

    // MARK: - Core Data Saving support

    func saveContext () {
        if managedObjectContext.hasChanges {
            do {
                try managedObjectContext.save()
            } catch {
                // Replace this implementation with code to handle the error appropriately.
                // abort() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.
                let nserror = error as NSError
                NSLog("Unresolved error \(nserror), \(nserror.userInfo)")
                abort()
            }
        }
    }

    
    //facebook monitoring installation of app
    
//    - (void)applicationDidBecomeActive:(UIApplication *)application {
//    [FBSDKAppEvents activateApp];
//    }
//    
//    - (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
//    [[FBSDKApplicationDelegate sharedInstance] application:application
//    didFinishLaunchingWithOptions:launchOptions];
//    return YES;
//    }
//    
//    - (BOOL)application:(UIApplication *)application
//    openURL:(NSURL *)url
//    sourceApplication:(NSString *)sourceApplication
//    annotation:(id)annotation {
//    return [[FBSDKApplicationDelegate sharedInstance] application:application
//    openURL:url
//    sourceApplication:sourceApplication
//    annotation:annotation];
//    }
    
    
}

