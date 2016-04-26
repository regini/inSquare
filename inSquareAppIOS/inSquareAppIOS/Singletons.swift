//
//  Singletons.swift
//  inSquare
//
//  Created by Corrado Pensa on 23/04/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation

/** Keeps track of User Info and State */
class User: NSObject {
    
    static let sharedInstance = User()
    
    /** The username */
    var username:String = "" //var username: String?
    /** User Mail */
    var email:String = String()
    /** User Facebook Id */
    var fbId:String = String()
    /** User Id on Server */
    var serverId:String = String()
    /** User Img Avatar address */
    var userAvatarUrl = ""
}

class inSquareCatching: NSObject {
    
    static let sharedInstance = inSquareCatching()
    
    /** Catching for Favourite Square */
    var userFavouriteSquare = JSON(data: NSData())
    /** Catching for Recents Square */
    var userRecentSquare = JSON(data: NSData())
    
}

/** App constants */
struct Constants {
    struct ServerURLs {
        static let serverMainUrl = "http://cloud.insquare.it"
        static let serverSocketMainUrl = "http://chat.cloud.insquare.it"
        static let serverSocketNamespace = "/squares"
    }
    
    struct Images {
        static let opponentDefaultAvatar:UIImage = UIImage(named: "opponentAvatar-01.png")!
        static let insquareMapPin:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "mapPinTrace3.png"))!, scale: 4.5)!
        static let insquareMapTab:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "mapTabSelected2.png"))!, scale: 4.5)!
        static let insquareMapTabHighlightImage:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "mapTab.png"))!, scale: 4.5)!
        static var like:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "Like-50.png"))!, scale: 2)!
        static var likeFilled:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "Like Filled-50.png"))!, scale: 2)!


    }
    
    struct Color {
        static let inSquareUiColorQRed:UIColor = UIColor(hue: 0.0028, saturation: 0.62, brightness: 0.63, alpha: 1.0) /* #a3403d */
    }
    
    struct Layout {
        static var tabBarHeight:CGFloat = CGFloat() //49
        static var statusBarHeight:CGFloat = 20
        static var navigationBarHeight:CGFloat = 44

    }
}
