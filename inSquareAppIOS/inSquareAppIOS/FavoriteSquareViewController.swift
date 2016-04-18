//
//  FavoriteSquareViewController.swift
//  inSquare
//
//  Created by Alessandro Steri on 02/03/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import GoogleMaps
import Alamofire


class FavoriteSquareViewController: UIViewController, UITableViewDelegate
{
    var favouriteSquare = JSON(data: NSData())

    //values to pass by cell chatButton tap, updated when the button is tapped
    var squareId:String = String()
    var squareName:String = String()
    var squareLatitude:Double = Double()
    var squareLongitude:Double = Double()

    
    @IBOutlet var tableView: UITableView!


    @IBOutlet var tabBar: UITabBarItem!
    
    @IBAction func unwindToFav(segue: UIStoryboardSegue)
    {
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        //UIApplication.sharedApplication().statusBarStyle = .Default

//        let tabBarHeight = self.tabBarController!.tabBar.frame.height
//        con.constant = viewSquare.frame.height
        
        
        request(.GET, "\(serverMainUrl)/favouritesquares/\(serverId)").validate().responseJSON { response in
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    print("FAVOURITESQUARES \(value)")
                    self.favouriteSquare = JSON(value)
                    print("FAVOURITESQUARES2 \(self.favouriteSquare[0]["_source"])")

                    

                    
                    
//                    for (index, value):(String, JSON) in self.jsonSq
//                    {
//                    }
                    self.tableView.reloadData()
                }
            case .Failure(let error):
                print(error)
                //cambia con analitics
                //                Answers.logCustomEventWithName("Error",
                //                    customAttributes: ["Error Debug Description": error.debugDescription])
            }
        }//ENDREQUEST

        
        
    }//END VDL

    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int
    {
        return favouriteSquare.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell
    {
        //let cell = FavouriteCellTableViewCell(style: UITableViewCellStyle.Default, reuseIdentifier: "favCell")
        
        let cell = self.tableView.dequeueReusableCellWithIdentifier("favCell", forIndexPath: indexPath) as! FavouriteCellTableViewCell
        print("qwerty \(self.favouriteSquare[indexPath.row]["_source"]["lastMessageDate"])")
        cell.squareName.text = self.favouriteSquare[indexPath.row]["_source"]["name"].string
        let dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        let date = dateFormatter.dateFromString("\(self.favouriteSquare[indexPath.row]["_source"]["lastMessageDate"].string!)")
        print(date)
        let newDateFormatter = NSDateFormatter()
        newDateFormatter.locale = NSLocale.currentLocale()
        newDateFormatter.dateFormat = "hh:mm (dd-MMM)"
        var convertedDate = newDateFormatter.stringFromDate(date!)
        cell.squareActive.text = "Active: \(convertedDate)"
        //cell.goInSquare(cell)
        cell.tapped = { [unowned self] (selectedCell) -> Void in
            let path = tableView.indexPathForRowAtPoint(selectedCell.center)!
            let selectedSquare = self.favouriteSquare[path.row]
            
            print("the selected item is \(selectedSquare)")
            
          
            
            self.squareId = selectedSquare["_id"].string!
            self.squareName = selectedSquare["_source"]["name"].string!
            let coordinates = selectedSquare["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
            let latitude = (coordinates[0] as NSString).doubleValue
            let longitude = (coordinates[1] as NSString).doubleValue
            self.squareLatitude = latitude
            self.squareLongitude = longitude

            self.performSegueWithIdentifier("chatFromFav", sender: self)

            
            
        }
        
        return cell
    }

    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
        
        //inutile?
        updateFavouriteSquares()
    }
    
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }//END DRMW
    

    override func prepareForSegue(segue: UIStoryboardSegue!, sender: AnyObject!)
    {
        if (segue.identifier == "chatFromFav") {
            //Checking identifier is crucial as there might be multiple
            // segues attached to same view
            print("chatFromFav")
            print(squareId)
            print(squareName)
            print(squareLatitude)
            print(squareLongitude)
            //var detailVC = segue!.destinationViewController as! SquareMessViewController
            //var detailVC = segue!.destinationViewController as! NavigationToJSQViewController
            //            let detailVC = detailNC.topViewController as! inSquareViewController
            
            let navVC = segue.destinationViewController as! UINavigationController
            
            let detailVC = navVC.viewControllers.first as! SquareMessViewController
            
            detailVC.viewControllerNavigatedFrom = segue.sourceViewController

            detailVC.squareName = self.squareName
            detailVC.squareId = self.squareId
            detailVC.squareLatitude = self.squareLatitude
            detailVC.squareLongitude = self.squareLongitude
            
            
        }
    }

}// END VC
