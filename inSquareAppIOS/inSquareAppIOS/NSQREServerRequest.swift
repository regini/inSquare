//
//  NSQREServerRequest.swift
//  inSquare
//
//  Created by Corrado Pensa on 27/04/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation
import Alamofire

class NSQREServerRequest: NSObject
{
    
    static let sharedInstance = NSQREServerRequest()
    
    
    //MARK - PATCH
    /** Patch given Apn Token on server by given serverId User  */
    func patchApnTokenOnServer(ApnToken: String, serverIdentifier: String)
    {
        request(.PATCH, "\(serverMainUrl)/apnToken?isApple=true&userId=\(serverIdentifier)&token=\(ApnToken)").validate().responseString { response in
            print("REQUEST PATCH APNS TOKEN: \(response.request)")
            //print("body patch", response.request?.HTTPBody.)
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
    
    func modifySquareName(SquareID: String, NewName: String, OwnerID: String, completionHandler: () -> ())
    {

        var urlPostSquare = "\(serverMainUrl)/squares?name=\(NewName)&squareId=\(SquareID)&ownerId=\(OwnerID)"
        urlPostSquare = urlPostSquare.stringByReplacingOccurrencesOfString(" ", withString: "%20")
        
        //print("REQUEST URL: \(urlPostSquare)")
        request(.PATCH, urlPostSquare).validate().responseString { response in
            //print("REQUEST POST SQUARE: \(response.request)")
            switch response.result {
            case .Success:
                //print("RESULT \(response.result)")
                //print("RESULT \(response.result.value)")
                
                if let value = response.result.value
                {
                    print("MODIFIED SQUARE \(value)")
                }
            case .Failure(let error):
                print("FALLITO \(error)")
                //make analitics
                //                        Answers.logCustomEventWithName("Error",
                //                            customAttributes: ["Error Debug Description": error.debugDescription])
            }
        }
    }


    //MARK - DELETE
    func deleteSquareYouCreated(squareId: String, ownerId: String, completionHandler: () -> ())
    {
        request(.DELETE, "\(serverMainUrl)/squares?&squareId=\(squareId)&ownerId=\(ownerId)").validate().responseString { response in
        
            switch response.result {
                    case .Success:
                        if let value = response.result.value {
                            print("DELETED SQUARE \(value)")
                            completionHandler()
                            
                            
                        }
                    case .Failure(let error):
                        print(error)
            }
                }//ENDREQUEST
    }

    

    //MARK - POST
    
    
    
    
    //MARK - GET
    /** Get squares owned by the given user */
    func getOwnedSquareFromServer(OwnerServerId: String, completionHandler: (JSON) -> ())
    {
        //var ownedSquares = JSON(data: NSData())

        
        request(.GET, "\(serverMainUrl)/squares/?byOwner=true&ownerId=\(OwnerServerId)").validate().responseJSON { response in
            
            print("getOwnedSquareFromServer", response.request)  // original URL request
            print("getOwnedSquareFromServer", response.response) // URL response
            print("getOwnedSquareFromServer", response.data)     // server data
            print("getOwnedSquareFromServer", response.result)   // result of response serialization
            
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    print("profileSQUARES \(value)")
                    completionHandler(JSON(value))
                }
            case .Failure(let error):
                print(error)
            }
        }//ENDREQUEST

    }



}
