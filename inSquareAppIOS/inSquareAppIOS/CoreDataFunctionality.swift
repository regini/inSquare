//
//  CoreDataFunctionality.swift
//  inSquare
//
//  Created by Alessandro Steri on 09/03/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation
import CoreData


func isLoggedInCoreData() -> Bool
{
    let appDel: AppDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    
    let context: NSManagedObjectContext = appDel.managedObjectContext
    
    
    let request = NSFetchRequest(entityName: "Users")
    
    // request.predicate = NSPredicate(format: "username = %@", "Ralphie")
    
    request.returnsObjectsAsFaults = false
    
    do {
        
        let results = try context.executeFetchRequest(request)
        
        print("USERS (has to be 0 or 1): \(results.count)")
        
        if results.count > 0
        {
            print("isLoggedInCoreData == true because of this users: \(results)")

            return true
        }
        
    } catch {
        
        print("Fetch Failed")
    }
    
    return false

}

func howManyUserInCoreData() -> Int
{
    let appDel: AppDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    let context: NSManagedObjectContext = appDel.managedObjectContext
    let request = NSFetchRequest(entityName: "Users")
    request.returnsObjectsAsFaults = false
    do
    {
        let results = try context.executeFetchRequest(request)
        print("USERS (has to be 0 or 1): \(results.count)")
        return results.count
    }
    catch
    {
        print("Fetch Failed")
    }
    return -1
}

func clearUsersSavedInCoreData() -> Int //converti in bool
{
    let appDel: AppDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    let context: NSManagedObjectContext = appDel.managedObjectContext
    let request = NSFetchRequest(entityName: "Users")
    request.returnsObjectsAsFaults = false
    do
    {
        let results = try context.executeFetchRequest(request)
        if results.count > 0
        {
            for result in results as! [NSManagedObject]
            {
                context.deleteObject(result)
                do
                {
                try context.save()
                
                } catch
                {
                    return -2
                }
            }
        }
    }
    catch
    {
        print("Fetch Failed")
    }
    return howManyUserInCoreData()
}

func saveUserDataInCoreData() -> Bool
{
    //TODO: per ora ho solo un user in coredata, quando voglio piu di uno togli clear quando salvo
    clearUsersSavedInCoreData()
    
    let appDel: AppDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    let context: NSManagedObjectContext = appDel.managedObjectContext
    
    var newUser = NSEntityDescription.insertNewObjectForEntityForName("Users", inManagedObjectContext: context)
    newUser.setValue("\(username)", forKey: "username")
    newUser.setValue("\(serverId)", forKey: "serverId")
    newUser.setValue("\(fbId)", forKey: "fbId")
    newUser.setValue("\(email)", forKey: "email")
    newUser.setValue("\(accessToken)", forKey: "accessToken")
    newUser.setValue("\(userAvatarUrl)", forKey: "userAvatarUrl")
    do
    {
        try context.save()
    }
    catch
    {
        print("There was a problem!")
        return false
    }
    
    let request = NSFetchRequest(entityName: "Users")
    request.returnsObjectsAsFaults = false
    do
    {
        let results = try context.executeFetchRequest(request)
        print("saveUserDataInCoreData: \(results)")
    }
    catch
    {
        print("Fetch Failed")
        //return false
    }
    return true
}

func fetchDataFromCoreData()
{
    //recupera dati da CoreData cosi quando viewne lanciato viewWill appear fa segue to home
    let appDel: AppDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    let context: NSManagedObjectContext = appDel.managedObjectContext
    
    let request = NSFetchRequest(entityName: "Users")
    
    request.returnsObjectsAsFaults = false
    do
    {
        let results = try context.executeFetchRequest(request)
        print("USERS (has to be 0 or 1): \(results.count)")
        print("USERS (details): \(results)")
        
        if results.count == 1
        {
            for result in results as! [NSManagedObject]
            {
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
            } //for result in result
        }// if res.count = 1
    }//do
    catch
    {
        print("Fetch Failed")
    }
    
}

//use it for store access token
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
