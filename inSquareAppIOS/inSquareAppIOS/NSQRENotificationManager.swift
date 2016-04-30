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