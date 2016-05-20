//
//  AppState.swift
//  inSquare
//
//  Created by Corrado Pensa on 27/04/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation

/** Keeps track of the application state */
class AppState: NSObject {
    
    static let sharedInstance = AppState()
    
    /** ServerID If the user ever logged in on server  */
    var serverId:String?
//        {
//        didSet {
//            //NSQREServerRequest.sharedInstance.patchApnTokenOnServer(token e servid) 
//        }
//        }
    /** True if user ever signedin on server */
    var hasAServerID = false

    /** Return True if user ever signed in on server, else false */
    func isFirstLoginEver() -> Bool
    {
        return (serverId == nil) ? true : false
    }
    
    
}