//
//  NotificationManager.swift
//  inSquare
//
//  Created by Corrado Pensa on 22/04/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import Foundation

var recBadge = 0

enum badgeOperation
{
    case set
    case increment

}

func setRecentTabBadge(badgeNumber: Int, toDo: badgeOperation)
{
    switch toDo {
    case badgeOperation.set:
        recBadge = badgeNumber
    case badgeOperation.increment:
        recBadge = recBadge + badgeNumber
    }
}