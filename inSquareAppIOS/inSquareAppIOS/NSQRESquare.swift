//
//  NSQRESquare.swift
//  inSquare
//
//  Created by Corrado Pensa on 30/04/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation


//enum SquareState {
//    case Mercury, awoken, Earth, Mars, Jupiter, Saturn, Uranus, Neptune
//}


class NSQRESquare: NSObject
{
    /** Square id  */
    var id:String
    /** Square State  */
    var state:String
    /** Who favorite this Square  */
    var favorers:JSON
    /** Square Messages, as Database id  */
    var messages:JSON
    /** if user is on place  */
    var userLocated:String
    /** id of Square creator  */
    var ownerId:String
    /** Square State  */
    var name:String
    /** Square State  */
    var type:Int
    /** Last Message Date  */
    var lastMessageDate:String
    /** Square latitude  */
    var latitude:Double
    /** Square Longitude  */
    var longitude:Double
    /** Created Date  */
    var createdAt:String
    /** Last Message Date  */
    var views:Int
    
    
    
    init(jsonSq: JSON)
    {
        let coordinates = jsonSq["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
        self.latitude = (coordinates[0] as NSString).doubleValue
        self.longitude = (coordinates[1] as NSString).doubleValue
        self.name = jsonSq["_source"]["name"].string!
        self.id = jsonSq["_id"].string!
        self.state = jsonSq["_source"]["state"].string!
        self.favorers = jsonSq["_source"]["favorers"]
        self.messages = jsonSq["_source"]["messages"]
        self.userLocated = "\(jsonSq["_source"]["userLocated"])"
        self.ownerId = jsonSq["_source"]["ownerId"].string!
        self.type = jsonSq["_source"]["type"].int!
        self.lastMessageDate = jsonSq["_source"]["lastMessageDate"].string!
        self.createdAt = jsonSq["_source"]["createdAt"].string!
        self.views = jsonSq["_source"]["views"].int!
        print(self.id)
    }
    
    /** How many users favorited this square  */
    func favotiredBy() -> Int
    {
        return self.favorers.count
    }

    
}