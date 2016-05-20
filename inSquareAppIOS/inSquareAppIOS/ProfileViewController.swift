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
    
    var enableActionOnTableViewRow = true
    
    @IBOutlet weak var segmentedControl: UISegmentedControl!
    @IBAction func segmentedControlIndexChanged(sender: UISegmentedControl)
    {
        switch segmentedControl.selectedSegmentIndex
        {
        case 0:
             enableActionOnTableViewRow = true
            request(.GET, "\(serverMainUrl)/squares/?byOwner=true&ownerId=\(serverId)").validate().responseJSON { response in
                
                switch response.result {
                case .Success:
                    if let value = response.result.value {
                        print("profileSQUARES \(value)")
                        self.ownedSquares = JSON(value)
                        self.ownedTableView.reloadData()
                        
                    }
                case .Failure(let error):
                    print(error)
                }
            }//ENDREQUEST
            
            
            
            
            
        case 1:
             enableActionOnTableViewRow = false //disable editing
            request(.GET, "\(serverMainUrl)/favouritesquares/\(serverId)").validate().responseJSON { response in
                switch response.result {
                case .Success:
                    if let value = response.result.value {
                        print("profileSQUARES \(value)")
                        self.ownedSquares = JSON(value)
                        self.ownedTableView.reloadData()
                    }
                case .Failure(let error):
                    print(error)
                }
            }//ENDREQUEST

            
        default:
            break; 
        }
    }
      
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
        blurEffectView.frame = sw.bounds
        blurEffectView.autoresizingMask = [.FlexibleWidth, .FlexibleHeight] // for supporting device rotation
        profileTopView.addSubview(blurEffectView)
        //put name/picture layer upper than blur
        profileTopView.addSubview(sw)
        
//                let rightConstraint = NSLayoutConstraint(
//                    item: sw,
//                    attribute: NSLayoutAttribute.RightMargin,
//                    relatedBy: NSLayoutRelation.Equal,
//                    toItem: profileTopView,
//                    attribute: NSLayoutAttribute.RightMargin,
//                    multiplier: 1,
//                    constant: 0)
//        
//        NSLayoutConstraint.activateConstraints([rightConstraint])
        

        
        
    
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
//        cell.selectionStyle =  UITableViewCellSelectionStyle.None
        //cell.selectedBackgroundView!.backgroundColor = UIColor.whiteColor()
        return cell
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
//        let indexPath = tableView.indexPathForSelectedRow
//        
//        let currentCell = tableView.cellForRowAtIndexPath(indexPath!)! as! ProfileTableViewCell
//        
//        let selectedSquare = self.ownedSquares[indexPath!.row]
//        //let path = tableView.indexPathForRowAtPoint(selectedCell.center)!
//        
//        print("the selected item is \(selectedSquare)")
//        
//        
//        
//        self.squareId = selectedSquare["_id"].string!
//        self.squareName = selectedSquare["_source"]["name"].string!
//        let coordinates = selectedSquare["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
//        let latitude = (coordinates[0] as NSString).doubleValue
//        let longitude = (coordinates[1] as NSString).doubleValue
//        self.squareLatitude = latitude
//        self.squareLongitude = longitude
//        
//        self.performSegueWithIdentifier("chatFromProfile", sender: self)
    }


    // Another way that allows you to change the text of "Delete" and add more buttons when sliding a cell is to use editActionsForRowAtIndexPath.
    //canEditRowAtIndexPath and commitEditingStyle are still required, but you can leave commitEditingStyle empty since deletion is handled in editActionsForRowAtIndexPath
    func tableView(tableView: UITableView, canEditRowAtIndexPath indexPath: NSIndexPath) -> Bool {
        return enableActionOnTableViewRow
        
    }
    
    func tableView(tableView: (UITableView!), commitEditingStyle editingStyle: UITableViewCellEditingStyle, forRowAtIndexPath indexPath: (NSIndexPath!)) {
        
    }
    
    func tableView(tableView: UITableView, editActionsForRowAtIndexPath indexPath: NSIndexPath) -> [UITableViewRowAction]? {
        
        //DELETE
        var deleteAction = UITableViewRowAction(style: .Default, title: "Delete") {action in
            //handle delete
            print("Del")
            
            var refreshAlert = UIAlertController(title: "Are you sure to delete the Square?", message: "All messages will be lost, there no come back dude.", preferredStyle: .Alert)
            
            refreshAlert.addAction(UIAlertAction(title: "Ok", style: .Default, handler: { (action: UIAlertAction!) in
                print("Handle Ok logic here")
                
                let currentCell = tableView.cellForRowAtIndexPath(indexPath)! as! ProfileTableViewCell
                
                let selectedSquare = self.ownedSquares[indexPath.row]
                //let path = tableView.indexPathForRowAtPoint(selectedCell.center)!
                
                print("the selected item is \(selectedSquare)")
                
                
                
                let SquareToDeleteId = selectedSquare["_id"].string!
                
                //solo per far feedback visivo di rimozione automatica
                self.ownedSquares.arrayObject?.removeAtIndex(indexPath.row)
                
                NSQREServerRequest.sharedInstance.deleteSquareYouCreated(SquareToDeleteId, ownerId: serverId, completionHandler: self.completionHandlerForDeleteSquare)

                
            }))
            
            refreshAlert.addAction(UIAlertAction(title: "Cancel", style: .Default, handler: { (action: UIAlertAction!) in
                print("Handle Cancel Logic here")
                
            }))
            
            self.presentViewController(refreshAlert, animated: true, completion: nil)

            
                    }
        
        //EDIT
        var editAction = UITableViewRowAction(style: .Normal, title: "Edit") {action in
            //handle edit
            print("edit")
            
            let currentCell = tableView.cellForRowAtIndexPath(indexPath)! as! ProfileTableViewCell
            
            let selectedSquare = self.ownedSquares[indexPath.row]
            
            print("the selected item is \(selectedSquare)")
            
            //1. Create the alert controller.
            var alert = UIAlertController(title: "Rename Square", message: "Enter a new name.", preferredStyle: .Alert)
            //2. Add the text field. You can configure it however you need.
            alert.addTextFieldWithConfigurationHandler({ (textField) -> Void in
                textField.text = "\(self.ownedSquares[indexPath.row]["_source"]["name"].string!)"
            })
            //3. Grab the value from the text field, and print it when the user clicks OK.
            alert.addAction(UIAlertAction(title: "OK", style: .Default, handler: { (action) -> Void in
                let textField = alert.textFields![0] as UITextField
                if (textField.text == "\(self.ownedSquares[indexPath.row]["_source"]["name"].string!)")
                {
                    print("Name not modified.")
                    return
                }
                else
                {
                    
                    print("TEXT: \(textField.text)")
                
                    let SquareToModify = selectedSquare["_id"].string!
                    self.ownedSquares[indexPath.row]["_source"]["name"].string = textField.text!
                    self.ownedTableView.reloadData()
                    NSQREServerRequest.sharedInstance.modifySquareName(SquareToModify, NewName: textField.text!, OwnerID: serverId, completionHandler: self.completionHandlerForDeleteSquare)
                    //req
                    
                }
            }))
            // 4. Present the alert.
            self.presentViewController(alert, animated: true, completion: nil)
            
            
            
            print("AQAQAQ", self.ownedSquares[indexPath.row]["_source"]["name"].string)
            print("AQAQAQ", self.ownedSquares[indexPath.row]["_source"]["name"].string)

            
            
            

        }
        
        return [deleteAction, editAction]
    }

    func completionHandlerForDeleteSquare()
    {
        print("COMPLETITION HANDLER CALLED")
        self.ownedTableView.reloadData()
        //NSQREServerRequest.sharedInstance.getOwnedSquareFromServer(serverId, completionHandler: completionHandlerForGetSquare)
        
    }
    
    func completionHandlerForGetSquare(resultTodisplay:JSON)
    {
        self.ownedSquares = resultTodisplay
        print("OWNED SQUARES POST DELETE RESULT", resultTodisplay)
        print("OWNED SQUARES POST DELETE", self.ownedSquares)
        self.ownedTableView.reloadData()

    }

    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(true)
        request(.GET, "\(serverMainUrl)/squares/?byOwner=true&ownerId=\(serverId)").validate().responseJSON { response in
            
            print("!!!1", response.request)  // original URL request
            print("!!!2", response.response) // URL response
            print("!!!3", response.data)     // server data
            print("!!!4", response.result)   // result of response serialization
            
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

        

    }
    
    override func viewDidAppear(animated: Bool)
    {
        super.viewDidAppear(true)
        
        self.ownedTableView.reloadData()
        
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
