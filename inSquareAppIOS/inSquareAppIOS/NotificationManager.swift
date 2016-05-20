//
//  NotificationManager.swift
//  inSquare
//
//  Created by Corrado Pensa on 22/04/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation
import Alamofire

let notificationCtr = NSNotificationCenter.defaultCenter()

//struct NotificationCounter
//{
//    var countNotificationFromRecent:Int = 0
//        {
//            willSet(newCountNotificationFromRecent)
//            {
//                print("setBadge")
//            }
//    }
//}

var recBadge = 0

enum badgeOperation
{
    case set
    case increment
}

func setRecentTabBadge(badgeNumber: Int, toDo: badgeOperation)
{
    switch toDo {
    case badgeOperation.set:
        recBadge = badgeNumber
    case badgeOperation.increment:
        recBadge = recBadge + badgeNumber
    }
}

func messageReceived(notification: NSNotification) -> String
{
    var message = ""
    let obj:AnyObject? = notification.userInfo!["aps"]!["alert"]  // [1]
    // if alert is a flat string
    if let msg = obj as? String {    // [2]
        message = msg
    } else {                         // [3]
        // if the alert is a dictionary we need to extract the value of the body key
        let msg = obj!["body"] as! String
        message = msg
    }
    return message
    //tableView.reloadData()
}

func postApnTokenOnServer(serverReadyPushNotificationString: String)
{
    request(.PATCH, "\(serverMainUrl)/apnToken?isApple=true&userId=\(serverId)&token=\(serverReadyPushNotificationString)").validate().responseString { response in
        print("REQUEST PATCH APNS TOKEN: \(response.request)")
        
        switch response.result
        {
        case .Success:
            print("RESULT PATCH APNS TOKEN: \(response.result)")
            print("RESULT PATCH APNS TOKEN: \(response.result.value)")
            
            
        case .Failure(let error):
            print("FALLITO \(error)")
        }
    }
    
}

func unRegisterForAllNotifications()
{
    // UnSet notifications for this application
    UIApplication.sharedApplication().unregisterForRemoteNotifications()
    
}

