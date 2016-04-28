//
//  RecentsViewController.swift
//  inSquare
//
//  Created by Alessandro Steri on 05/03/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import Alamofire

class RecentsViewController: UIViewController, UITableViewDelegate
{
    var recentSquares = JSON(data: NSData())
    
    //values to pass by cell chatButton tap, updated when the button is tapped
    var squareId:String = String()
    var squareName:String = String()
    var squareLatitude:Double = Double()
    var squareLongitude:Double = Double()
    
    @IBOutlet var recentTableView: UITableView!
    
    @IBAction func unwindToRec(segue: UIStoryboardSegue)
    {
    }

    
    override func viewDidLoad() {
        super.viewDidLoad()
        //UIApplication.sharedApplication().statusBarStyle = .Default

        
        request(.GET, "\(serverMainUrl)/recentsquares/\(serverId)").validate().responseJSON { response in
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    print("RECSQUARES \(value)")
                    self.recentSquares = JSON(value)

                    //                    for (index, value):(String, JSON) in self.jsonSq
                    //                    {
                    //                    }
                    self.recentTableView.reloadData()

                }
            case .Failure(let error):
                print(error)
                //cambia con analitics
                //                Answers.logCustomEventWithName("Error",
                //                    customAttributes: ["Error Debug Description": error.debugDescription])
            }
        }//ENDREQUEST

        
        // Do any additional setup after loading the view.
    }//END VDL
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
        
        //inutile?
        updateRecentSquares()
    }

    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        
        //inutile?
        recentTableView.reloadData()
    }

    
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int
    {
        return recentSquares.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell
    {
        let cell = self.recentTableView.dequeueReusableCellWithIdentifier("recCell", forIndexPath: indexPath) as! RecentTableViewCell
        //print("qwerty \(self.favouriteSquare[indexPath.row]["_source"]["lastMessageDate"])")
        cell.squareName.text = self.recentSquares[indexPath.row]["_source"]["name"].string
        let dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        let date = dateFormatter.dateFromString("\(self.recentSquares[indexPath.row]["_source"]["lastMessageDate"].string!)")
        print(date)
        let newDateFormatter = NSDateFormatter()
        newDateFormatter.locale = NSLocale.currentLocale()
        newDateFormatter.dateFormat = "hh:mm (dd-MMM)"
        var convertedDate = newDateFormatter.stringFromDate(date!)
        cell.squareActive.text = "Active: \(convertedDate)"
        //cell.goInSquare(cell)
        cell.tapped = { [unowned self] (selectedCell) -> Void in
            let path = tableView.indexPathForRowAtPoint(selectedCell.center)!
            let selectedSquare = self.recentSquares[path.row]
            
            print("the selected item is \(selectedSquare)")
            
            
            
            self.squareId = selectedSquare["_id"].string!
            self.squareName = selectedSquare["_source"]["name"].string!
            let coordinates = selectedSquare["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
            let latitude = (coordinates[0] as NSString).doubleValue
            let longitude = (coordinates[1] as NSString).doubleValue
            self.squareLatitude = latitude
            self.squareLongitude = longitude
            
            self.performSegueWithIdentifier("chatFromRec", sender: self)
        }
        
        return cell
    }
    
    
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }//END DRMW
    
    

    // MARK: - Navigation
    override func prepareForSegue(segue: UIStoryboardSegue!, sender: AnyObject!)
    {
        if (segue.identifier == "chatFromRec") {
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


    
}//END VC
