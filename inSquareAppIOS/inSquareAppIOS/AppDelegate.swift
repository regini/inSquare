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
import AudioToolbox
import Fabric
import Crashlytics


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate
{
    var window: UIWindow?
    
    func registerForAllNotifications()
    {
        // Set notifications for this application
        let notificationSettings = UIUserNotificationSettings(forTypes: [.Alert, .Badge, .Sound], categories: nil)
        
        UIApplication.sharedApplication().registerUserNotificationSettings(notificationSettings)
        UIApplication.sharedApplication().registerForRemoteNotifications()
    }



    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {

        //UIApplication.sharedApplication().statusBarStyle = .LightContent


        //GOOGLE
        GMSServices.provideAPIKey("AIzaSyBmibIMY4xUPzLe2Sj9Ax4Ne2tNmCNoUic")

        application.applicationIconBadgeNumber = 0
        
        if isLoggedInCoreData()
        {
            //registra notifiche
            //NOTIFICATION
            registerForAllNotifications() // Register this app for notifications
            AGPushAnalytics.sendMetricsWhenAppLaunched(launchOptions) // Send metrics when app is launched due to push notification
            //
            
            print("didFinishLaunchingWithOptions: LOGGEDIN")
            fetchDataFromCoreData()
            
            self.window!.rootViewController = UIStoryboard(name: "Main", bundle: NSBundle.mainBundle()).instantiateInitialViewController()
        }
        else
        {
            print("didFinishLaunchingWithOptions: NOT LOGGED IN")
            //registra notifiche su logincontroller

            
                        var rootController: UIViewController = UIStoryboard(name: "Main", bundle: NSBundle.mainBundle()).instantiateViewControllerWithIdentifier("LoginVcID")
                        var navigation: UINavigationController = UINavigationController(rootViewController: rootController)
                        self.window!.rootViewController = navigation
        }
        // Configure tracker from GoogleService-Info.plist.
        var configureError:NSError?
        GGLContext.sharedInstance().configureWithError(&configureError)
        assert(configureError == nil, "Error configuring Google services: \(configureError)")
        
        // Optional: configure GAI options.
        var gai = GAI.sharedInstance()
        gai.trackUncaughtExceptions = true  // report uncaught exceptions
        gai.logger.logLevel = GAILogLevel.Verbose  // remove before app release
        //GOOGLE

        
    
        
        
        //sposta in fi logged in? considerando no push a logout
        AGPushAnalytics.sendMetricsWhenAppLaunched(launchOptions) // Send metrics when app is launched due to push notification
        
        // Check for launch Options, this could be from Local/Remote Notifications that was used to Open the app!
        if let options = launchOptions as? [String : AnyObject]
        {
            print("LAUNCH OPTIONs: ", launchOptions)
            //Manage Local Notifications that was used to Open the app!
            if let notification = options[UIApplicationLaunchOptionsLocalNotificationKey] as? UILocalNotification
            {
                if let userInfo = notification.userInfo
                {
                    //let foo = userInfo["foo"] as! String
                    print("app launced from local notification")
                    //                    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (Int64)(2 * NSEC_PER_SEC)), dispatch_get_main_queue()) {
                    //                        self.showAlert("App Started from Local Notification", alert: "\(notification.alertBody!) foo=", badgeCount: 0)
                    //                    }
                }
            }
            //Manage Remote Notifications that was used to Open the app!
            if let userInfo = options[UIApplicationLaunchOptionsRemoteNotificationKey] as? [NSObject : AnyObject] {
                
                print("App Started from Push Notification: ")
                
                if let notification = options[UIApplicationLaunchOptionsRemoteNotificationKey] as? NSNotification
                {
                    NSQRENotificationManager.sharedInstance.addNotificationToNotificationTab(notification)
                    let mess = messageReceived(notification)
                    print("App Started from Push Notification: ", mess)
                    
                    //if lanched from push launch the right chat controller
                    var rootController: UIViewController = UIStoryboard(name: "Main", bundle: NSBundle.mainBundle()).instantiateViewControllerWithIdentifier("ChatViewController")
                    
                    var chatRoot = rootController as! SquareMessViewController
                    
                    request(.GET, "\(serverMainUrl)/squares/\(NSQRENotificationManager.sharedInstance.getSquareIdFromRemoteNotification(notification))").validate().responseJSON { response in
                        switch response.result {
                        case .Success:
                            print("REQUEST!!! \(response.request)")
                            print("RESULT!!! \(response.result.value)")
                            if let value = response.result.value {
                                
                                if let title = JSON(value)["name"].string
                                {
                                    chatRoot.squareId = NSQRENotificationManager.sharedInstance.getSquareIdFromRemoteNotification(notification)
                                    chatRoot.squareName = title
                                }
                                if let geoLoc = JSON(value)["geo_loc"].string
                                {
                                    let coordinates = geoLoc.componentsSeparatedByString(",")
                                    print("GEOLOPUSH", geoLoc)
                                    let latitude = (coordinates[0] as NSString).doubleValue
                                    let longitude = (coordinates[1] as NSString).doubleValue
                                    
                                }
                                if (chatRoot.squareId != "" && chatRoot.squareName != "")
                                {
                                    chatRoot.triggeredByPushNotification = true
                                    var navigation: UINavigationController = UINavigationController(rootViewController: rootController)
                                    
                                    print("PAREEENT", navigation.parentViewController)
                                    self.window!.rootViewController = navigation
                                }
                            }
                        case .Failure(let error):
                            print("FALLITO SEARCH: \(error)")
                            
                        }
                    }//ENDREQUEST

                    //end launch chat from puch
                    
                    //inutile?
                    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (Int64)(2 * NSEC_PER_SEC)), dispatch_get_main_queue())
                    {
                        //self.showAlert("App Started from Push Notification", alert: mess, badgeCount: 1)
                        let alertController = UIAlertController(title: "App Started from Push Notification", message: mess, preferredStyle: .ActionSheet)
                        self.getVisibleViewController(self.window?.rootViewController)!.presentViewController(alertController, animated: true, completion: nil)
                    }
                }
            }
            
        }
        
        //Crashlytics has to be the lastone before return
        Fabric.with([Crashlytics.self])
        
        //oppure questo prima del return e return true
        FBSDKApplicationDelegate.sharedInstance().application(application, didFinishLaunchingWithOptions: launchOptions)
        return true
    }
    
    //facebook: The “OpenURL” method allows your app to open again after the user has validated their login credentials. FBSDKAppEvents.activateApp() method allows Facebook to capture events within your application including Ads clicked on from Facebook to track downloads from Facebook and events like how many times your app was opened.
    func application(application: UIApplication, openURL url: NSURL, sourceApplication: String?, annotation: AnyObject?) -> Bool
    {
        FBSDKApplicationDelegate.sharedInstance().application(application, openURL: url, sourceApplication: sourceApplication, annotation: annotation)
        Fabric.with([Crashlytics.self]) //prima era commentato
        return FBSDKApplicationDelegate.sharedInstance().application( application, openURL: url, sourceApplication: sourceApplication, annotation: annotation)
    }

    
    
    // Called if successful registeration for APNS.
    func application(application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: NSData) {
        //send this device token to server
        //print(deviceToken)
        
        print("APNS Success", serverId)
        
    
        
        
        //let registration = AGDeviceRegistration(serverURL: NSURL(string: "https://push-insquareapp.rhcloud.com/ag-push/")!)
//        var registration: AGDeviceRegistration = AGDeviceRegistration(serverURL: NSURL(string: "push-insquareapp.rhcloud.com/ag-push/")!)
        let registration = AGDeviceRegistration(serverURL: NSURL(string: "https://push-insquareapp.rhcloud.com/ag-push/")!)


        
        registration.registerWithClientInfo({ (clientInfo: AGClientDeviceInformation!)  in
            
            // apply the token, to identify this device
            clientInfo.deviceToken = deviceToken
            
            //production variant iTunesConnect and Apple Store
            clientInfo.variantID = "f6c09133-ea75-48ba-9333-c979f93aa95f"
            clientInfo.variantSecret = "787d37ba-f71e-48fc-a015-2acc040bd91b"
            
            
//            //development variant (sandbox, Xcode)
//            clientInfo.variantID = "25e1cc97-f593-4395-944a-5691c6e81a9e"
//            clientInfo.variantSecret = "20294d0b-d1cb-4b42-9878-670580ca0ea5"

            
            // --optional config--
            // set some 'useful' hardware information params
            let currentDevice = UIDevice()
            clientInfo.operatingSystem = currentDevice.systemName
            clientInfo.osVersion = currentDevice.systemVersion
            clientInfo.deviceType = currentDevice.model
            //alias
            clientInfo.alias = serverId
            print("CIVSALIAS: ",clientInfo.alias, serverId)
            
            
            
            
            
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
        
        //save token in userdata
        apnToken = serverReadyPushNotificationString
        
        if isLoggedInCoreData()
        {
            //se sono loggato ho un id server e posso postare token altrimenti lo posto in seguito
            NSQREServerRequest.sharedInstance.patchApnTokenOnServer(serverReadyPushNotificationString, serverIdentifier: serverId)
            
        }
        
    }
    
    // Called if unable to register for APNS.
    func application(application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: NSError) {
        print(error)
    }
    
    func application(application: UIApplication, didReceiveRemoteNotification userInfo: [NSObject: AnyObject], fetchCompletionHandler: (UIBackgroundFetchResult) -> Void) {
        print(userInfo)
        
        // When a message is received, send NSNotification, would be handled by registered ViewController
        let notification:NSNotification = NSNotification(name:"message_received", object:nil, userInfo:userInfo)
        NSNotificationCenter.defaultCenter().postNotification(notification)
        print("UPS message received: \(userInfo)")
        
        NSQRENotificationManager.sharedInstance.addNotificationToNotificationTab(notification)
        
        
        
        
        // Send metrics when app is brpught form backgrpund tp foreground due to push notification
        AGPushAnalytics.sendMetricsWhenAppAwoken(application.applicationState, userInfo: userInfo)
        
        // No additioanl data to fetch
        fetchCompletionHandler(UIBackgroundFetchResult.NoData)
        
        
        let notificationReceivedJSON:JSON = JSON (userInfo)
        
        print("RECEIVED NOTIFICATION IN FOREGROUND: ", notificationReceivedJSON)
        
        if application.applicationState == .Active
        {
            
            let aps = notificationReceivedJSON["aps"]
            print("ALERT", notificationReceivedJSON["aps"]["alert"])
            
            let alert = notificationReceivedJSON["aps"]["alert"]
            let title = notificationReceivedJSON["aps"]["alert"]["title"].string
            let message = notificationReceivedJSON["aps"]["alert"]["body"].string
            var badgeCount = notificationReceivedJSON["aps"]["badge"].int
            
            //usa custom sound qui
            
            if alert == nil {print("alert: ", alert)}
            if title == nil {print("title: ", title)}
            if message == nil {print("message: ", message)}
            if badgeCount == nil
            {
                print("badgeCount: ", badgeCount)
                badgeCount = 0 // PER EVITARE CRASH SE NON MANDO BADGE COUNT
            }
            
            application.applicationIconBadgeNumber = badgeCount!
            
            print("didReceiveRemoteNotification alert message=\(alert) Badge Count=\(badgeCount)")
            
            
            
            request(.GET, "\(serverMainUrl)/squares/\(NSQRENotificationManager.sharedInstance.getSquareIdFromRemoteNotification(notification))").validate().responseJSON { response in
                switch response.result {
                case .Success:
                    print("REQUEST!!! \(response.request)")
                    print("RESULT!!! \(response.result.value)")
                    var SquareNameSa = ""
                    var SquareIdSa = ""
                    var lat = 0.0
                    var lon = 0.0
                    
                    if let value = response.result.value {
                        
                        if let title = JSON(value)["name"].string
                        {
                            SquareIdSa = NSQRENotificationManager.sharedInstance.getSquareIdFromRemoteNotification(notification)
                            SquareNameSa = title
                        }
                        if let geoLoc = JSON(value)["geo_loc"].string
                        {
                            let coordinates = geoLoc.componentsSeparatedByString(",")
                            print("GEOLOPUSH", geoLoc)
                            lat = (coordinates[0] as NSString).doubleValue
                            lon = (coordinates[1] as NSString).doubleValue
                            
                        }
                        if (SquareIdSa != "" && SquareNameSa != "")
                        {
                            // Show Alert to user
                            print("willshowallert")
                            self.showAlert("\(SquareNameSa)", alert: "\(message!)", badgeCount: badgeCount!, squareId: SquareIdSa)
                        }
                    }
                case .Failure(let error):
                    print("FALLITO SEARCH: \(error)")
                    
                }
            }//ENDREQUEST
            
            //end launch chat from puch

            
            
            
            
        }
        //push notification gestita quando non in foreground
        else
        {
            
            //if lanched from push launch the right chat controller
            var rootController: UIViewController = UIStoryboard(name: "Main", bundle: NSBundle.mainBundle()).instantiateViewControllerWithIdentifier("ChatViewController")
            
            var chatRoot = rootController as! SquareMessViewController
            
            request(.GET, "\(serverMainUrl)/squares/\(NSQRENotificationManager.sharedInstance.getSquareIdFromRemoteNotification(notification))").validate().responseJSON { response in
                switch response.result {
                case .Success:
                    print("REQUEST!!! \(response.request)")
                    print("RESULT!!! \(response.result.value)")
                    if let value = response.result.value {
                        
                        if let title = JSON(value)["name"].string
                        {
                            chatRoot.squareId = NSQRENotificationManager.sharedInstance.getSquareIdFromRemoteNotification(notification)
                            chatRoot.squareName = title
                        }
                        if let geoLoc = JSON(value)["geo_loc"].string
                        {
                            let coordinates = geoLoc.componentsSeparatedByString(",")
                            print("GEOLOPUSH", geoLoc)
                            let latitude = (coordinates[0] as NSString).doubleValue
                            let longitude = (coordinates[1] as NSString).doubleValue
                            
                        }
                        if (chatRoot.squareId != "" && chatRoot.squareName != "")
                        {
                            chatRoot.triggeredByPushNotification = true
                            var navigation: UINavigationController = UINavigationController(rootViewController: rootController)
                            
                            print("PAREEENT", navigation.parentViewController)
                            self.window!.rootViewController = navigation
                        }
                    }
                case .Failure(let error):
                    print("FALLITO SEARCH: \(error)")
                    
                }
            }//ENDREQUEST
            
            //end launch chat from puch
        
        }
        
    }

    
    func application(application: UIApplication, didReceiveLocalNotification notification: UILocalNotification) {
        
        print("didReceiveLocalNotification: \(notification)")
        
        
        
        
        if let userInfo = notification.userInfo {
            
            let foo = userInfo["foo"] as! String
            print("didReceiveLocalNotification: foo=\(foo)")
            
            showAlert("Local Notification In Foreground", alert: "\(notification.alertBody!) foo=\(foo)", badgeCount: 0, squareId: "")
            
        }
    }
    
    
    
    func showAlert(title: String, alert: String, badgeCount: Int, squareId: String) {
        print("SHOWALLERT")
        var message = "\(alert)"
        
        if badgeCount > 0 {
            message = "\(alert) badgeCount=\(badgeCount)"
        }
        
        //check se vc visibile e quello che genera notifica
        var isNotificationFromCUrrentChatOpen = false

        let chatContr = self.getVisibleViewController(self.window?.rootViewController)
        if (chatContr != nil)
        {
            print("CC NOT NIL", chatContr)
            print("CC NOT NIL", chatContr!.isKindOfClass(SquareMessViewController))
            if chatContr!.isKindOfClass(SquareMessViewController)
            {
                
                let chatUnboxed = chatContr as! SquareMessViewController
                
                print("sqid!! ", chatUnboxed.squareId)
                if chatUnboxed.squareId == squareId
                {
                    
                    isNotificationFromCUrrentChatOpen = true
                }
                
                
            }
            //check se sono in controller aperto da notifica (quello con done invece di back, poichè e messo in un navigation control quando lo instanzio da notifica serve un altro if
            if chatContr!.isKindOfClass(UINavigationController)
            {
                let navig = chatContr as! UINavigationController
                //prova top invece di visible
                let chatUnboxed = navig.visibleViewController as! SquareMessViewController
                
                print("sqid!!2 ", chatUnboxed.squareId)
                if chatUnboxed.squareId == squareId
                {
                    
                    isNotificationFromCUrrentChatOpen = true
                }
                
                
            }
            
        }
        //fine check
        
        
        
        
        // se non da controller attuale mostro alert
        if !isNotificationFromCUrrentChatOpen
        {
            let alertController = UIAlertController(title: title, message: message, preferredStyle: .ActionSheet)
            
            let okAction = UIAlertAction(title: "View", style: UIAlertActionStyle.Default) {
                UIAlertAction in
                print("OK")
                
                
                
                var rootController: UIViewController = UIStoryboard(name: "Main", bundle: NSBundle.mainBundle()).instantiateViewControllerWithIdentifier("ChatViewController")
                
                var chatRoot = rootController as! SquareMessViewController
                
                chatRoot.triggeredByPushNotification = true
                chatRoot.squareName = title
                chatRoot.squareId = squareId
                var navigation: UINavigationController = UINavigationController(rootViewController: rootController)
                
                print("PAREEENT", navigation.parentViewController)
                self.window!.rootViewController = navigation
                
                
                
                
            }
            
            alertController.addAction(okAction)
            
            let koAction = UIAlertAction(title: "Ignore", style: UIAlertActionStyle.Default) {
                UIAlertAction in
                print("Ignore")
            }
            
            alertController.addAction(koAction)
            
            
            AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
            
            getVisibleViewController(self.window?.rootViewController)!.presentViewController(alertController, animated: true, completion: nil)
        }
        
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
        print("GOING IN BACKGROUND")
    }

    func applicationWillEnterForeground(application: UIApplication) {
        // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
        
        print("COMING BACK FROME BACKGROUND")
    }

    func applicationDidBecomeActive(application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
        print("I'M BACK FROME BACKGROUND")
        application.applicationIconBadgeNumber = 0
        FBSDKAppEvents.activateApp()


    }

    func applicationWillTerminate(application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
        // Saves changes in the application's managed object context before the application terminates.
        print("APP WILL TERMINATE")
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

