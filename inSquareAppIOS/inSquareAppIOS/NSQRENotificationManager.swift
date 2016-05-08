//
//  NSQRENotificationManager.swift
//  inSquare
//
//  Created by Corrado Pensa on 30/04/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation

class NSQRENotificationManager: NSObject
{
    
    static let sharedInstance = NSQRENotificationManager()
    
    /** Collect all the notification  */
    var notification = [NSNotification]() //JSON(data: NSData())
        {
        didSet {
            print("HI I'M A NOTIFICATION ARRAY: ", notification)
            
        }
    }

    
    /** return tuple (sender, text, squareId)  */
    func getSquareIdFromRemoteNotification(notification: NSNotification) -> String
    {
        var squareId = "Can't Get Sq.Id From Remote Notification"
        if let obj:String? = notification.userInfo!["squareId"]! as! String
        {
            squareId = obj!
        }
        
        return squareId
    }
    
    
//    /** return tuple (sender, text, squareId)  */
//    func remoteNotificationParser(notification: NSNotification) -> (String, String, String)
//    {
//        var message = ""
//        let obj:AnyObject? = notification.userInfo!["aps"]!["alert"]  // [1]
//        // if alert is a flat string
//        if let msg = obj as? String
//        {    // [2]
//            message = msg
//        }
//        else
//        {
//            // [3]
//            // if the alert is a dictionary we need to extract the value of the body key
//            let msg = obj!["body"] as! String
//            message = msg
//        }
//        return ("", message, "")
//    }

    
    
    func addNotificationToNotificationTab(notifica: NSNotification)
    {
        self.notification.append(notifica)
    }


    
//    struct squareInfoForBadge
//    {
//        var id:String
//        var squareInfo:JSON
//        var badge:String
//        var isRecent:Bool
//        var isFavorite:Bool
//    
//        
//    }
    /** Favorite and Recent square  */
    var squareBoardHood:JSON?
    
    func addSquareFromFav(square:JSON)
    {
        
    }
    


}