//
//  AlamofireExtension.swift
//  inSquare
//
//  Created by Corrado Pensa on 22/04/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation
//import Alamofire
//
////Extend Alamofire so it can do POSTs with a JSON body from passed object
//extension Alamofire.Manager {
//    public class func request(
//        method: Alamofire.Method,
//        _ URLString: URLStringConvertible,
//          bodyObject: EVObject)
//        -> Request
//    {
//        return Manager.sharedInstance.request(
//            method,
//            URLString,
//            parameters: [:],
//            encoding: .Custom({ (convertible, params) in
//                let mutableRequest = convertible.URLRequest.copy() as! NSMutableURLRequest
//                mutableRequest.HTTPBody = bodyObject.toJsonString().dataUsingEncoding(NSUTF8StringEncoding, allowLossyConversion: false)
//                return (mutableRequest, nil)
//            })
//        )
//    }
//}