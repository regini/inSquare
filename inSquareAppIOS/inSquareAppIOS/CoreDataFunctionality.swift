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
