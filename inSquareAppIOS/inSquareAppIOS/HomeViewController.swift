//
//  HomeViewController.swift
//  inSquareAppIOS
//
//  Created by Alessandro Steri on 30/01/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import GoogleMaps
import CoreLocation
import Alamofire
import Crashlytics


var jsonSq = JSON(data: NSData())
var squareAroundYou = [["_index":"squares","_type":"square","_id":"56abd75edbae0310a14c045c","_score":1,"_source":["name":"Università degli Studi di Roma La Sapienza","geo_loc":"41.890893,12.504679"]],["_index":"squares","_type":"square","_id":"56abd75edbae0310a14c0475","_score":1,"_source":["name":"Ponte Tazio","geo_loc":"41.935064,12.533812"]]]

class HomeViewController: UIViewController, GMSMapViewDelegate, UITableViewDelegate , CLLocationManagerDelegate
{
    @IBOutlet weak var myNavigationBar: UINavigationBar!
    //getSquare raggio di azione in km
    var distanceInKmForGetSquare = 20

    var mapView:GMSMapView!
    var manager:CLLocationManager!

    var latitude:CLLocationDegrees = CLLocationDegrees()
    var longitude:CLLocationDegrees = CLLocationDegrees()
    
    var mapCenterLatitude:CLLocationDegrees = CLLocationDegrees()
    var mapCenterLongitude:CLLocationDegrees = CLLocationDegrees()


    //values to pass by marker tap, always updated to latest marker tapped
    var squareId:String = String()
    var squareName:String = String()
    var squareLatitude:Double = Double()
    var squareLongitude:Double = Double()
    
    //constraint of tableview against navigation bar
    @IBOutlet var tableViewVsMapSize: NSLayoutConstraint!

    //tableView that display square outlet reference
    @IBOutlet weak var squareAroundYouTable: UITableView!

//    override func loadView()
//    {
//        super.loadView()
//    }
    
    
    override func viewDidLoad()
    {
        super.viewDidLoad()

        
        
        //navugationBar SetUP
        //hide navigationbar
        self.navigationController?.setNavigationBarHidden(false, animated: true)
        myNavigationBar.hidden = true
//        myNavigationBar.backgroundColor = UIColor.blackColor()
//        myNavigationBar.barTintColor = UIColor.blackColor()
//        let logo = UIImage(named: "logoApp-01.png")
//        let imageView = UIImageView(image:logo)
//        myNavigationBar.shadowImage = logo
        
        
        
        //self.squareAroundYouTable.backgroundColor = UIColor.blackColor()

        var titleView : UIImageView
        // set the dimensions you want here
        titleView = UIImageView(frame:CGRectMake(0, 0, 40, 30))
        // Set how do you want to maintain the aspect
        titleView.contentMode = .ScaleAspectFit
        titleView.image = UIImage(named: "logoApp-01.png")
        self.navigationItem.titleView = titleView
        self.navigationItem.backBarButtonItem?.title = "Log-Out"
        print(self.navigationItem.rightBarButtonItems)
        
        
        //parameters for getSquare
        var distanceInKm:String = "\(distanceInKmForGetSquare)" + "km"
        let latitudeGet:String = "41.890893"
        let longitudeGet:String = "12.504679"
        
        //setUp for location manager -> map centred on current location
        manager = CLLocationManager()
        manager.delegate = self
        manager.desiredAccuracy - kCLLocationAccuracyBest
        manager.requestWhenInUseAuthorization()
        manager.startUpdatingLocation()

        var camera = GMSCameraPosition.cameraWithLatitude(latitude,
            longitude: longitude, zoom: 13)

        //getting screensize to resize the map
        let screenSize: CGRect = UIScreen.mainScreen().bounds
        let screenWidth = screenSize.width
        let screenHeight = screenSize.height
        let mapWidth = screenSize.width
        let mapHeight = screenSize.height * 0.5
        //END resizing
        
        
        //mapview setup
        mapView = GMSMapView.mapWithFrame(CGRectMake(0, 64, mapWidth, mapHeight), camera: camera)
        //attiva-disattiva pallino di dove sono
        mapView.myLocationEnabled = true
        mapView.settings.myLocationButton = true
        mapView.delegate = self
        self.view.addSubview(mapView)
        
//GETSQUARE REQUEST
//####################################################################################################################################################################
        // use latitude and longitude //sure?seems is working with latget an longet
        request(.GET, "http://recapp-insquare.rhcloud.com/squares", parameters:["distance": distanceInKm, "lat": latitudeGet, "lon": longitudeGet]).validate().responseJSON { response in
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    jsonSq = JSON(value)

            for (index, value):(String, JSON) in jsonSq
            {
                let i:Int=Int(index)!

                
                let coordinates = jsonSq[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
                let latitude = (coordinates[0] as NSString).doubleValue
                let longitude = (coordinates[1] as NSString).doubleValue
                let title = jsonSq[i]["_source"]["name"].string
                let snippet = jsonSq[i]["_id"].string
                
                Answers.logContentViewWithName("getSquare",
                    contentType: "Square: \(title)",
                    contentId: "ID: \(snippet)",
                    customAttributes: ["Latitude": latitude, "Longitude": longitude])
            
                let marker = GMSMarker()
                marker.position = CLLocationCoordinate2DMake(latitude, longitude)
                marker.title = title
                marker.snippet = snippet
                marker.icon = insquareMapPin
                marker.groundAnchor.x = marker.groundAnchor.x + 0.45
                marker.map = self.mapView
                
            }
                self.squareAroundYouTable.reloadData()

                }
            case .Failure(let error):
                print(error)
                
                Answers.logCustomEventWithName("Error",
                    customAttributes: ["Error Debug Description": error.debugDescription])
            }
        }//ENDREQUEST
//####################################################################################################################################################################
//END GETSQUARE REQUEST
        
        
        //resizing Table view to fill screen against map
        tableViewVsMapSize.constant = mapHeight
        UIView.animateWithDuration(0.25, animations: { () -> Void in
            self.view.layoutIfNeeded()
        })

        
    }   //END VIEWDIDLOAD

    
//location manager
//####################################################################################################################################################################
    func locationManager(manager: CLLocationManager, didUpdateLocations locations: [CLLocation])
    {
        
        var userLocation:CLLocation = locations[0]
        
        self.latitude = userLocation.coordinate.latitude
        self.longitude = userLocation.coordinate.longitude
        
        //if latitude == 0 && longitude == 0
        //{
            //update mapview
            self.mapView.animateToLocation(CLLocationCoordinate2DMake(latitude, longitude))
            manager.stopUpdatingLocation()

            print(locations)
        //}
        

//        self.course.text = "\(userLocation.course)"
//        self.speed.text = "\(userLocation.speed)"
//        self.altitude.text = "\(userLocation.altitude)"
        
//        //geocoding addre->coordinate, reverse e' l'opposto - placeMark e' set di indirizzi
//        CLGeocoder().reverseGeocodeLocation(userLocation, completionHandler: { (placemarks, error) -> Void in
//            if (error != nil )
//            {
//                print(error)
//                
//            }
//            else
//            {
//                // a meno che utente non si move molto velocemente va bene
//                if let p = placemarks?[0]                {
//                    var subThoroughfare:String = ""
//                    
//                    if (p.subThoroughfare != nil)
//                    {
//                        subThoroughfare = p.subThoroughfare!
//                    }
//                    
//                    print(p.country)
//                    self.address.text = "\(subThoroughfare) \n \(p.thoroughfare)\(p.subLocality) \n \(p.subAdministrativeArea) \n \(p.postalCode) \n \(p.country)"
//                    
//                }
//            }
//            
//            
//        })
        
    }//end location manager
//####################################################################################################################################################################
//end location manager

//MAPVIEW FUNC
//####################################################################################################################################################################
    
    func mapView(mapView: GMSMapView!, willMove gesture: Bool) {
        //mapView.clear()
    }

    
    func mapView(mapView: GMSMapView!, didChangeCameraPosition position: GMSCameraPosition!) {
//        print("POSITION \(position)")

        if true
        {
            let oldMapCenter:CLLocation = CLLocation(latitude: mapCenterLatitude, longitude: mapCenterLongitude)
            let newMapCenter:CLLocation = CLLocation(latitude: position.target.latitude, longitude: position.target.longitude)
            let meters:CLLocationDistance = oldMapCenter.distanceFromLocation(newMapCenter)
            if meters > Double(distanceInKmForGetSquare*1000)
            {
                //centra mappa, controlla se non la centra da solo e allora non si raggiunge mai distanza maggiore
                mapCenterLatitude = position.target.latitude
                mapCenterLongitude = position.target.longitude

                //fai get
                //getSquare(mapCenterLatitude, longitude: mapCenterLongitude, distanceInKm: distanceInKmForGetSquare) //ritorna json poi devi lavorarlo
                //mi sa che prima di aggiungere marker nuovi va pulita mappa o non aggiunti quelli presenti
                request(.GET, "http://recapp-insquare.rhcloud.com/squares", parameters:["distance": "\(distanceInKmForGetSquare)km", "lat": mapCenterLatitude, "lon": mapCenterLongitude]).validate().responseJSON { response in
                    switch response.result {
                    case .Success:
                        if let value = response.result.value {
                            jsonSq = JSON(value)
                            
                            for (index, value):(String, JSON) in jsonSq
                            {
                                let i:Int=Int(index)!
                                
                                
                                let coordinates = jsonSq[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
                                let latitude = (coordinates[0] as NSString).doubleValue
                                let longitude = (coordinates[1] as NSString).doubleValue
                                let title = jsonSq[i]["_source"]["name"].string
                                let snippet = jsonSq[i]["_id"].string
                                
                                Answers.logContentViewWithName("getSquare",
                                    contentType: "Square: \(title)",
                                    contentId: "ID: \(snippet)",
                                    customAttributes: ["Latitude": latitude, "Longitude": longitude])
                                
                                let marker = GMSMarker()
                                marker.position = CLLocationCoordinate2DMake(latitude, longitude)
                                marker.title = title
                                marker.snippet = snippet //cerca se già presente con stesso id
                                marker.icon = insquareMapPin
                                marker.groundAnchor.x = marker.groundAnchor.x + 0.45
                                marker.map = self.mapView
                            }
                            self.squareAroundYouTable.reloadData()
                            
                        }
                    case .Failure(let error):
                        print(error)
                        
                        Answers.logCustomEventWithName("Error",
                            customAttributes: ["Error Debug Description": error.debugDescription])
                    }
                }

                
                //reload map cosi vedi piazze // lo fa gia dentro request3

                
            }
//            print("DISTANZA IN METRI: \(meters)")
        }
        
        self.mapCenterLatitude = position.target.latitude
        self.mapCenterLongitude = position.target.latitude
    }
    
    func mapView(mapView: GMSMapView!, idleAtCameraPosition cameraPosition: GMSCameraPosition!)
    {
        
        let handler = { (response : GMSReverseGeocodeResponse!, error: NSError!) -> Void in
            print(error)
            print("IDLE: \(response.firstResult().coordinate.longitude)")

            if let result = response.firstResult() {
//                let marker = GMSMarker()
                
                print("IDLE position is:\(cameraPosition.target.latitude),\(cameraPosition.target.longitude)")
                
//                marker.position = cameraPosition.target
//                marker.title = result.lines[0] as! String
//                marker.snippet = result.lines[1] as! String
//                marker.map = mapView!
            }
        }
    }

    
    
    //Gesture: LongPres on map creates a marker and add it to marker list
//make sure to add the new marker (Square) to the server
    func mapView(mapView: GMSMapView!, didLongPressAtCoordinate coordinate: CLLocationCoordinate2D)
    {
        //1. Create the alert controller.
        var alert = UIAlertController(title: "Create a New Square", message: "Enter a name for the Square you wish to create here", preferredStyle: .Alert)
        //2. Add the text field. You can configure it however you need.
        alert.addTextFieldWithConfigurationHandler({ (textField) -> Void in
            textField.text = ""
        })
        //3. Grab the value from the text field, and print it when the user clicks OK.
        alert.addAction(UIAlertAction(title: "OK", style: .Default, handler: { (action) -> Void in
            let textField = alert.textFields![0] as UITextField
            print("Text field: \(textField.text)")
            if (textField.text == "")
            {
                print("Not selected a name for the square")
                return
            }
            else
            {
                let marker = GMSMarker()
                marker.position = CLLocationCoordinate2DMake(coordinate.latitude, coordinate.longitude)
                marker.title = textField.text
                //use reversed geolocalization to add addres for description or use another textfield if needed
                marker.snippet = "Tap to open the Square"
                marker.map = mapView
                squareAroundYou.append(["Latitude" : coordinate.latitude, "Longitude" : coordinate.longitude, "Name" : textField.text!, "Description" : "Tap to open the Square"])
                self.squareAroundYouTable.reloadData()
                
                let latR = Double(round(1000000*coordinate.latitude)/1000000)
                let longR = Double(round(1000000*coordinate.longitude)/1000000)
                
                request(.POST, "http://recapp-insquare.rhcloud.com/squares", parameters:["name": textField.text!, "lat": latR, "lon": longR])
                print(latR)
                print(longR)

            }
        }))
        // 4. Present the alert.
        self.presentViewController(alert, animated: true, completion: nil)
        
        print("You tapped at \(coordinate.latitude), \(coordinate.longitude)")
        //self.viewDidLoad() //serve per aggiornare la mappa
    }
    
    //Gesture: Add event to marker tap - Market Tap Function
    func mapView(mapView: GMSMapView!, didTapInfoWindowOfMarker marker: GMSMarker!)
    {
        print("tappped")
        self.squareName = marker.title
        self.squareId = marker.snippet
        self.squareLatitude = marker.position.latitude
        self.squareLongitude = marker.position.longitude
        //        let secondViewController = self.storyboard!.instantiateViewControllerWithIdentifier("ViewController")
//        self.navigationController!.pushViewController(secondViewController, animated: true)
        
        self.performSegueWithIdentifier("goInTheSquare", sender: self)
        
    }
//####################################################################################################################################################################
//END MAPVIEW FUNC
    
//TABLEVIEW FUNC
//####################################################################################################################################################################
//fill table with square vicine
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int
    {
        return jsonSq.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell
    {
        //let cell = UITableViewCell(style: UITableViewCellStyle.Default, reuseIdentifier: "Cell")
        let cell = tableView.dequeueReusableCellWithIdentifier("SquareCell")! as UITableViewCell
        cell.textLabel?.text = jsonSq[indexPath.row]["_source"]["name"].string //squareAroundYou[indexPath.row]["Name"] as! String
        cell.imageView?.image = insquareMapPin
    
        //cell.textLabel?.tintColor = UIColor.whiteColor()
        //cell.textLabel!.textColor = UIColor.whiteColor()
        //cell.selectionStyle = UITableViewCellSelectionStyleNone
        //cell.textLabel.highlightedTextColor = UIColor orangeColor
        
        // this is where you set your color view
        var customColorView = UIView()
        customColorView.backgroundColor = inSquareUiColorQRed
        cell.selectedBackgroundView =  customColorView;
        
        return cell
    }
//ENDfill table with square vicine

    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        
        tableView.deselectRowAtIndexPath(indexPath, animated: true)
        
        let row = indexPath.row
        print("Row: \(row)")
    }
    
    //change background tableview
    func tableView(tableView: UITableView, willDisplayCell cell: UITableViewCell, forRowAtIndexPath indexPath: NSIndexPath) {
        //tableView.backgroundColor = inSquareUiColorQRed //non funge
        //cell.backgroundColor = UIColor.blackColor()
        cell.textLabel!.textColor = UIColor.blackColor()
        //cell.selectionStyle = UITableViewCellSelectionStyle.None
        //cell.textLabel!.highlightedTextColor = inSquareUiColorQRed
        
    }
//####################################################################################################################################################################
//END TABLEVIEW FUNC
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    /*
    // MARK: - Navigation
    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */
    
    override func prepareForSegue(segue: UIStoryboardSegue!, sender: AnyObject!)
    {
        if (segue.identifier == "goInTheSquare") {
            //Checking identifier is crucial as there might be multiple
            // segues attached to same view
            var detailVC = segue!.destinationViewController as! inSquareViewController
            //            let detailVC = detailNC.topViewController as! inSquareViewController
            detailVC.squareName = self.squareName
            detailVC.squareId = self.squareId
            detailVC.squareLatitude = self.squareLatitude
            detailVC.squareLongitude = self.squareLongitude
        }
    }
   

}//ENDVIEWCONTROLLER
//####################################################################################################################################################################


//EXTENSION
//####################################################################################################################################################################


extension String
{
    func substringWithRange(start: Int, end: Int) -> String
    {
        if (start < 0 || start > self.characters.count)
        {
            print("start index \(start) out of bounds")
            return ""
        }
        else if end < 0 || end > self.characters.count
        {
            print("end index \(end) out of bounds")
            return ""
        }
        let range = Range(start: self.startIndex.advancedBy(start), end: self.startIndex.advancedBy(end))
        return self.substringWithRange(range)
    }
    
    func substringWithRange(start: Int, location: Int) -> String
    {
        if (start < 0 || start > self.characters.count)
        {
            print("start index \(start) out of bounds")
            return ""
        }
        else if location < 0 || start + location > self.characters.count
        {
            print("end index \(start + location) out of bounds")
            return ""
        }
        let range = Range(start: self.startIndex.advancedBy(start), end: self.startIndex.advancedBy(start + location))
        return self.substringWithRange(range)
    }
}




