//
//  UserData.swift
//  inSquareAppIOS
//
//  Created by Corrado Pensa on 09/02/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation

var loggedIn = false
var username:String = ""
var accessToken:String = String()
var email:String = String()
var fbId:String = String()
var serverId:String = String()
//colors
var inSquareUiColorQRed:UIColor = UIColor(hue: 0.0028, saturation: 0.62, brightness: 0.63, alpha: 1.0) /* #a3403d */
//UIColor(hue: 1/360, saturation: 62/100, brightness: 63/100, alpha: 1.0) /* #a3403d */
//
//UIColor(red: 0.6392, green: 0.251, blue: 0.2392, alpha: 1.0) /* #a3403d */
//UIColor(red: 163/255, green: 64/255, blue: 61/255, alpha: 1.0) /* #a3403d */

//NSUserDefaults.standardUserDefaults().setObject("Rob", forKey: "name")
//
//var name = NSUserDefaults.standardUserDefaults().objectForKey("name")! as! String
//print(name)


//Immages
let insquareMapPin:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "mapPin-01.png"))!, scale: 4.5)!