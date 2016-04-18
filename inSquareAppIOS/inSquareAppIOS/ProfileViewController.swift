//
//  ProfileViewController.swift
//  inSquare
//
//  Created by Alessandro Steri on 05/03/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import Alamofire


class ProfileViewController: UIViewController, UITableViewDelegate
{

    @IBOutlet var profileImg: UIImageView!
    @IBOutlet var profileImgHeigt: NSLayoutConstraint!
    @IBOutlet var profileImgWidth: NSLayoutConstraint!
    @IBOutlet var profileTopViewHeightConstraints: NSLayoutConstraint!
    @IBOutlet var profileTopView: UIView!
    @IBOutlet var name: UILabel!
    @IBOutlet var activeLabel: UILabel!
    
    @IBOutlet var sw: UIView!
    
    @IBOutlet var favButt: UIBarButtonItem!
    @IBOutlet var ownedTableView: UITableView!
    
    @IBAction func unwindToProfile(segue: UIStoryboardSegue)
    {
    }
    
    var ownedSquares = JSON(data: NSData())
    
    //values to pass by cell chatButton tap, updated when the button is tapped
    var squareId:String = String()
    var squareName:String = String()
    var squareLatitude:Double = Double()
    var squareLongitude:Double = Double()

    
    override func viewDidLoad() {
        super.viewDidLoad()
        UIApplication.sharedApplication().statusBarStyle = .Default
        
        favButt.enabled = false
        
        //set profile topview height
        let screenSize: CGRect = UIScreen.mainScreen().bounds
        let screenWidth = screenSize.width
        let screenHeight = screenSize.height
        //profileTopView.frame.height = screenSize.height / 4
        //        profileTopView.frame = CGRectMake(0 , 0, self.profileTopView.frame.width, screenSize.height / 10)
        
        profileTopViewHeightConstraints.constant = (screenSize.height - tabBarHeight - statusBarHeight) / 5
        
        //profileImg.frame.size = CGSizeMake(profileTopViewHeightConstraints.constant / 1.3 , profileTopViewHeightConstraints.constant / 1.3)
        //set profile img 70% of topview height
        profileImgHeigt.constant = profileTopViewHeightConstraints.constant * 0.7
        profileImgWidth.constant = profileTopViewHeightConstraints.constant * 0.7

        //profile img circular with white border
        profileImg.image = userAvatar
        profileImg.layer.borderWidth = 3
        profileImg.layer.masksToBounds = false
        profileImg.layer.borderColor = UIColor.whiteColor().CGColor
        profileImg.layer.cornerRadius = profileImgHeigt.constant / 2
        profileImg.clipsToBounds = true
        
        //name & active
        name.text = username
        
        //profile background blurried
        profileTopView.backgroundColor = UIColor(patternImage: userAvatar)
        
        var blurEffect = UIBlurEffect(style: UIBlurEffectStyle.Dark)
        var blurEffectView = UIVisualEffectView(effect: blurEffect)
        blurEffectView.frame = view.bounds
        blurEffectView.autoresizingMask = [.FlexibleWidth, .FlexibleHeight] // for supporting device rotation
        profileTopView.addSubview(blurEffectView)
        //put name/picture layer upper than blur
        profileTopView.addSubview(sw)

        request(.GET, "\(serverMainUrl)/squares/?byOwner=true&ownerId=\(serverId)").validate().responseJSON { response in
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    print("profileSQUARES \(value)")
                    self.ownedSquares = JSON(value)
                    
                    //                    for (index, value):(String, JSON) in self.jsonSq
                    //                    {
                    //                    }
                    self.ownedTableView.reloadData()
                    
                }
            case .Failure(let error):
                print(error)
                //cambia con analitics
                //                Answers.logCustomEventWithName("Error",
                //                    customAttributes: ["Error Debug Description": error.debugDescription])
            }
        }//ENDREQUEST

        
    
    }//end vdl
    
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int
    {
        return ownedSquares.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell
    {
        let cell = self.ownedTableView.dequeueReusableCellWithIdentifier("profileCell", forIndexPath: indexPath) as! ProfileTableViewCell
        //print("qwerty \(self.favouriteSquare[indexPath.row]["_source"]["lastMessageDate"])")
        cell.squareName.text = self.ownedSquares[indexPath.row]["_source"]["name"].string
        let dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        let date = dateFormatter.dateFromString("\(self.ownedSquares[indexPath.row]["_source"]["lastMessageDate"].string!)")
        print(date)
        let newDateFormatter = NSDateFormatter()
        newDateFormatter.locale = NSLocale.currentLocale()
        newDateFormatter.dateFormat = "hh:mm (dd-MMM)"
        var convertedDate = newDateFormatter.stringFromDate(date!)
        cell.squareActive.text = "Active: \(convertedDate)"
        //cell.goInSquare(cell)
        cell.tapped = { [unowned self] (selectedCell) -> Void in
            let path = tableView.indexPathForRowAtPoint(selectedCell.center)!
            let selectedSquare = self.ownedSquares[path.row]
            
            print("the selected item is \(selectedSquare)")
            
            
            
            self.squareId = selectedSquare["_id"].string!
            self.squareName = selectedSquare["_source"]["name"].string!
            let coordinates = selectedSquare["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
            let latitude = (coordinates[0] as NSString).doubleValue
            let longitude = (coordinates[1] as NSString).doubleValue
            self.squareLatitude = latitude
            self.squareLongitude = longitude
            
            self.performSegueWithIdentifier("chatFromProfile", sender: self)
        }
        
        return cell
    }


    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    

    // MARK: - Navigation
    override func prepareForSegue(segue: UIStoryboardSegue!, sender: AnyObject!)
    {
        if (segue.identifier == "chatFromProfile") {
            //Checking identifier is crucial as there might be multiple
            // segues attached to same view
            print("chatFromProfile")
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


}
