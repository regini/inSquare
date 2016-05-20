//
//  Methods.swift
//  inSquareAppIOS
//
//  Created by Alessandro Steri on 29/01/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation
import UIKit
import FBSDKCoreKit
import FBSDKLoginKit
import Alamofire
import GoogleMaps
import Fabric
import Crashlytics

//saved


func convertSpacesInUrl(url: String) -> String
{
    return url.stringByReplacingOccurrencesOfString(" ", withString: "%20")
}

func updateFavouriteSquares()
{
    request(.GET, "\(serverMainUrl)/favouritesquares/\(serverId)").validate().responseJSON { response in
        switch response.result {
        case .Success:
            if let value = response.result.value {
                userFavouriteSquare = JSON(value)
                print("FAVOURITESQUARES \(userFavouriteSquare)")


            }
        case .Failure(let error):
            print(error)
            //cambia con analitics
            //                Answers.logCustomEventWithName("Error",
            //                    customAttributes: ["Error Debug Description": error.debugDescription])
        }
    }//ENDREQUEST
}

func updateRecentSquares()
{
    request(.GET, "\(serverMainUrl)/recentsquares/\(serverId)").validate().responseJSON { response in
        switch response.result {
        case .Success:
            if let value = response.result.value {
                userRecentSquare = JSON(value)
                print("RECENTSQUARES \(userRecentSquare)")

                
            }
        case .Failure(let error):
            print(error)
            //cambia con analitics
            //                Answers.logCustomEventWithName("Error",
            //                    customAttributes: ["Error Debug Description": error.debugDescription])
        }
    }//ENDREQUEST
}

func isFavourite(squareId: String) -> Bool
{
    for (index, value):(String, JSON) in userFavouriteSquare
    {
        let i:Int=Int(index)!
        
        if userFavouriteSquare[i]["_id"].string == squareId
        {
            return true
        }
        
//        let coordinates = self.jsonSq[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
//        let latitude = (coordinates[0] as NSString).doubleValue
//        let longitude = (coordinates[1] as NSString).doubleValue
//        let title = self.jsonSq[i]["_source"]["name"].string
//        let identifier = self.jsonSq[i]["_id"].string
        
    }
    return false
}

func isRecent(squareId: String) -> Bool
{
    for (index, value):(String, JSON) in userRecentSquare
    {
        let i:Int=Int(index)!
        
        if userFavouriteSquare[i]["_id"].string == squareId
        {
            return true
        }
        
        //        let coordinates = self.jsonSq[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
        //        let latitude = (coordinates[0] as NSString).doubleValue
        //        let longitude = (coordinates[1] as NSString).doubleValue
        //        let title = self.jsonSq[i]["_source"]["name"].string
        //        let identifier = self.jsonSq[i]["_id"].string
        
    }
    return false
}

//seved outside

func getSquare(latitude: Double, longitude: Double, distanceInKm: Int) -> JSON
{
    var jsnResult = JSON(data: NSData())

    request(.GET, "\(serverMainUrl)/squares", parameters:["distance": "\(distanceInKm)km", "lat": latitude, "lon": longitude]).validate().responseJSON { response in
        switch response.result {
        case .Success:
            if let value = response.result.value {
                jsnResult = JSON(value)
                
                
                
            }
        case .Failure(let error):
            print(error)
            
            Answers.logCustomEventWithName("Error",
                customAttributes: ["Error Debug Description": error.debugDescription])
        }
    }
    return jsnResult
}

//ritorna null..perche'???
func getMessagesFromSquare(recent: Bool, size: Int, square: String) -> JSON
{
    var jsnResult = JSON(data: NSData())
    
    request(.GET, "\(serverMainUrl)/messages", parameters:["recent": "\(recent)", "size": "\(size)", "square": "\(square)"]).validate().responseJSON { response in
        switch response.result {
        case .Success:
            if let value = response.result.value {
                jsnResult = JSON(value)
                
                
                
            }
        case .Failure(let error):
            print(error)
            
            Answers.logCustomEventWithName("Error",
                customAttributes: ["Error Debug Description": error.debugDescription])
        }
    }
    return jsnResult
}


//extension UIViewController {
//    func hideKeyboardWhenTappedAround() {
//        let tap: UITapGestureRecognizer = UITapGestureRecognizer(target: self, action: "dismissKeyboard")
//        view.addGestureRecognizer(tap)
//    }
//    
//    func dismissKeyboard() {
//        view.endEditing(true)
//    }
//}

//in view did load del vc copia per extension
//        self.hideKeyboardWhenTappedAround()



//request(.GET, "http://recapp-insquare.rhcloud.com/messages", parameters:["recent": "true", "size": "10", "square": squareId]).validate().responseJSON { response in
//    switch response.result {
//    case .Success:
//        if let value = response.result.value {
//            //jsonSq = JSON(value)
//            print("MESS:")
//            print(value)
//            //                    for (index, value):(String, JSON) in jsonSq
//            //                    {
//            //                        let i:Int=Int(index)!
//            //
//            //
//            //                        let coordinates = jsonSq[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
//            //                        let latitude = (coordinates[0] as NSString).doubleValue
//            //                        let longitude = (coordinates[1] as NSString).doubleValue
//            //                        let title = jsonSq[i]["_source"]["name"].string
//            //                        let snippet = jsonSq[i]["_id"].string
//            //
//            //                        Answers.logContentViewWithName("getSquare",
//            //                            contentType: "Square: \(title)",
//            //                            contentId: "ID: \(snippet)",
//            //                            customAttributes: ["Latitude": latitude, "Longitude": longitude])
//            //
//            //                        let marker = GMSMarker()
//            //                        marker.position = CLLocationCoordinate2DMake(latitude, longitude)
//            //                        marker.title = title
//            //                        marker.snippet = snippet
//            //                        marker.map = self.mapView
//            //                    }
//            //                    self.squareAroundYouTable.reloadData()
//            
//        }
//    case .Failure(let error):
//        print(error)
//        
//        Answers.logCustomEventWithName("Error",
//            customAttributes: ["Error Debug Description": error.debugDescription])
//    }
//}
