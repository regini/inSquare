//
//  UserData.swift
//  inSquareAppIOS
//
//  Created by Corrado Pensa on 09/02/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation

//http://cloud.insquare.it al posto di http://recapp-insquare.rhcloud.com
let serverMainUrl = "http://cloud.insquare.it"
let serverSocketMainUrl = "http://chat.cloud.insquare.it"
let serverSocketNamespace = "/squares"

//var loggedIn = false
var userAvatarUrl = ""
var userAvatar:UIImage = UIImage(named: "logo insq.png")!
var opponentDefaultAvatar:UIImage = UIImage(named: "opponentAvatar-01.png")!

var apnToken = ""


var username:String = ""
var accessToken:String = String()
var email:String = String()
var fbId:String = String()
//var serverId:String = "56bf626fbf2f63918b56efeb"

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

//CATCHING
var userFavouriteSquare = JSON(data: NSData())
var userRecentSquare = JSON(data: NSData())


//Immages
let insquareMapPin:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "mapPinTrace3.png"))!, scale: 4.5)!

let insquareMapTab:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "mapTabSelected2.png"))!, scale: 4.5)!
let insquareMapTabHighlightImage:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "mapTab.png"))!, scale: 4.5)!

var like:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "Like-50.png"))!, scale: 2)!
var likeFilled:UIImage = UIImage(data: UIImagePNGRepresentation(UIImage(imageLiteral: "Like Filled-50.png"))!, scale: 2)!


//LAYOUT
var tabBarHeight:CGFloat = CGFloat() //49
var statusBarHeight:CGFloat = 20
var navigationBarHeight:CGFloat = 44


//all the info http://www.idev101.com/code/User_Interface/sizes.html




//http://recapp-insquare.rhcloud.com/ log:
//rhc tail recapp --namespace insquare 
// new tail rhc tail insquarerecapp --namespace insquare
//rhc tail -a insquare
//chat tail: rhc tail chat --namespace recappapp

//pw v*******


//ALAMOFIRE DEBUGGER
//                print(response.request)  // original URL request
//                print(response.response) // URL response
//                print(response.data)     // server data
//                print(response.result)   // result of response serialization