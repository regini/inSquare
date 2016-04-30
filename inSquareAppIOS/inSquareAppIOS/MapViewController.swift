//
//  MapViewController.swift
//  inSquare
//
//  Created by Alessandro Steri on 01/03/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import GoogleMaps
import CoreLocation
import Alamofire
//import Crashlytics



//mettere get square dentro locationManager func?
//usa         mapView.myLocation instead of locationManager mapView.myLocation.coordinate.latitude
class MapViewController: UIViewController, GMSMapViewDelegate, UITableViewDataSource, UITableViewDelegate, UISearchBarDelegate //, CLLocationManagerDelegate
{
    @IBAction func unwindToMap(segue: UIStoryboardSegue)
    {
    }
    
    @IBOutlet var mapView: GMSMapView!
    
    @IBOutlet var searchBar: UISearchBar!
    
    @IBOutlet weak var searchResultTableView: UITableView!
    
    //var numberOfCharSearched = 0
    
    var searchActive : Bool = false
    var squaresFromSearch = JSON(data: NSData())
//    lazy var urlToSearch = "\(serverMainUrl)/squares?name=\(textSearched)"
//    lazy var urltoSearch2 = "&lat=\(mapCenterLatitude)&lon=\(mapCenterLongitude)&userId=\(serverId)"
//    
//    "\(serverMainUrl)/squares?name=\(textSearched)&lat=\(mapCenterLatitude)&lon=\(mapCenterLongitude)&userId=\(serverId)"
//    

    //var squaresFromSearchFiltered:[String] = []
    
    //SEARCH
    var squareToSearch = [String : GMSMarker]()
//serve^???
    
    
    //back from square
    var backFromSquareLatitude:Double = Double()
    var backFromSquareLongitude:Double = Double()

    //square from get
    var jsonSq = JSON(data: NSData())
    
    //getSquare raggio di azione in km
    var distanceInKmForGetSquare = 20
    
    //mapview
    //var mapView:GMSMapView!
    //lopcationManager toGet Current location
//    var manager:CLLocationManager!
    let locationManager = CLLocationManager()

    
    //in caso di errore centra colosseo, vengono aggiornate da locationManager per centrare in current location
    var latitude:CLLocationDegrees = 41.890466
    var longitude:CLLocationDegrees = 12.492231
    
    var mapCenterLatitude:CLLocationDegrees = CLLocationDegrees()
    var mapCenterLongitude:CLLocationDegrees = CLLocationDegrees()
    
    //values to pass by marker tap, always updated to latest marker tapped
    var squareId:String = String()
    var squareName:String = String()
    var squareLatitude:Double = Double()
    var squareLongitude:Double = Double()
    // usa coordinate passate in modo che quando da chat passa a home centra in square in cui parlavo

    
    //String to pass at marker.snippet
    let snippetString = "Tap to open"
    override func viewDidLoad()
    {
        super.viewDidLoad()
        self.navigationController?.setNavigationBarHidden(true, animated: false)
        //UIApplication.sharedApplication().statusBarStyle = .LightContent

        updateFavouriteSquares()
        
        
        print("SERVER ID: \(serverId)")
        print("USERNAME:", username)
        //search bar change color
        //searchBar.barTintColor = UIColor.blackColor()
        
        searchResultTableView.dataSource = self
        searchResultTableView.delegate = self
        
        self.searchResultTableView.backgroundColor = UIColor.clearColor()
        self.searchResultTableView.opaque = false
        self.searchResultTableView.separatorStyle = .None

        
        //parameters for getSquare
        var distanceInKm:String = "\(distanceInKmForGetSquare)" + "km"
        let latitudeGet:String = "41.890893"
        let longitudeGet:String = "12.504679"
        
//        self.hideKeyboardWhenTappedAround()
        
//        //setUp for location manager -> map centred on current location
//        manager = CLLocationManager()
//        manager.delegate = self
//        manager.desiredAccuracy - kCLLocationAccuracyBest
//        manager.requestWhenInUseAuthorization()
//        manager.startUpdatingLocation()
        locationManager.delegate = self
        locationManager.requestWhenInUseAuthorization()
        
        var camera = GMSCameraPosition.cameraWithLatitude(latitude,
            longitude: longitude, zoom: 15)

        //getting screensize to resize the map
        let screenSize: CGRect = UIScreen.mainScreen().bounds
        let screenWidth = screenSize.width
        let screenHeight = screenSize.height
        let mapWidth = screenSize.width
        let mapHeight = screenSize.height //* 0.5
        //END resizing


        //mapview setup
        //let tabBarHeight = self.tabBarController!.tabBar.frame.height
        //mapView = GMSMapView.mapWithFrame(CGRectMake(0, 64, mapWidth, mapHeight - 2*tabBarHeight), camera: camera)
        
       
        mapView.camera = GMSCameraPosition.cameraWithLatitude(latitude,
            longitude: longitude, zoom: 15)
        
        //attiva-disattiva pallino di dove sono
        mapView.myLocationEnabled = false
        mapView.settings.myLocationButton = true
        
        mapView.delegate = self
        //self.view.addSubview(mapView)

//GETSQUARE REQUEST
//####################################################################################################################################################################
        if (latitudeGet == "\(41.890466)" && longitudeGet == "\(12.492231)") || (latitudeGet == "\(0.0)" && longitudeGet == "\(0.0)")
        {
            //Analitycs
            var tracker = GAI.sharedInstance().defaultTracker
            tracker.send(GAIDictionaryBuilder.createEventWithCategory("1st GetSquare Gone Wrong", action: "Latitude: \(latitudeGet), Longitude: \(longitudeGet), Radius: \(distanceInKm)", label: "User \(serverId) did a new bad 1st get square", value: nil).build() as [NSObject : AnyObject])
            
        }
        else
        {
            //Analitycs
            var tracker = GAI.sharedInstance().defaultTracker
            tracker.send(GAIDictionaryBuilder.createEventWithCategory("1st GetSquare", action: "Latitude: \(latitudeGet), Longitude: \(longitudeGet), Radius: \(distanceInKm)", label: "User \(serverId) did a new 1st get square", value: nil).build() as [NSObject : AnyObject])
            
        }
        
        //controlla se parametri lat e long 0.0 0.0 in caso fai unciclo che finche 0.0 non va avanti o qualcosa di simile
        // use latitude and longitude //sure?seems is working with latget an longet
        request(.GET, "\(serverMainUrl)/squares", parameters:["distance": distanceInKm, "lat": latitudeGet, "lon": longitudeGet]).validate().responseJSON { response in
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    self.jsonSq = JSON(value)
                    
                    for (index, value):(String, JSON) in self.jsonSq
                    {
                        let i:Int=Int(index)!
                        
                        
                        
                        let coordinates = self.jsonSq[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
                        let latitude = (coordinates[0] as NSString).doubleValue
                        let longitude = (coordinates[1] as NSString).doubleValue
                        let title = self.jsonSq[i]["_source"]["name"].string
                        let identifier = self.jsonSq[i]["_id"].string
                        
                        
                        let marker = GMSMarker()
                        marker.position = CLLocationCoordinate2DMake(latitude, longitude)
                        marker.title = title
                        marker.snippet = self.snippetString
                        //marker.snippet = snippet
                        marker.icon = insquareMapPin
                        marker.groundAnchor.x = marker.groundAnchor.x + 0.45
                        marker.map = self.mapView
                        marker.userData = ["markerId": "\(identifier!)", "markerLat": "\(latitude)", "markerLon": "\(longitude)", "favouredBy": "\(self.jsonSq[i]["_source"]["favouredBy"])", "lastMessageDate": "\(self.jsonSq[i]["_source"]["lastMessageDate"].string!)", "views": "\(self.jsonSq[i]["_source"]["views"])", "state": "\(self.jsonSq[i]["_source"]["state"].string!)"]

                        //search
                        self.squareToSearch[marker.userData["markerId"] as! String] = marker
//                        print(self.squareToSearch.count)
                    }
                    //self.squareArHideoundYouTable.reloadData()
                    
                }
            case .Failure(let error):
                print(error)
//cambia con analitics
//                Answers.logCustomEventWithName("Error",
//                    customAttributes: ["Error Debug Description": error.debugDescription])
            }
        }//ENDREQUEST
        //####################################################################################################################################################################
        //END GETSQUARE REQUEST
    }//END VDL
    

//    override func touchesBegan(touches: Set<UITouch>, withEvent event: UIEvent?) {
////        self.view.endEditing(true)
//        self.searchBar.resignFirstResponder()
//        super.touchesBegan(touches, withEvent: event)
//
//    }
    
    //SEARCH E TABLEVIEW PER RESULTS
    
    func searchBarTextDidBeginEditing(searchBar: UISearchBar) {
        print("searchBarTextDidBeginEditing")
        searchActive = true
        //mapView.addSubview(self.searchResultTableView)
        self.searchResultTableView.hidden = false

//        let topConstraint = NSLayoutConstraint(
//            item: searchResultTableView,
//            attribute: NSLayoutAttribute.TopMargin,
//            relatedBy: NSLayoutRelation.Equal,
//            toItem: self.searchBar,
//            attribute: NSLayoutAttribute.BottomMargin,
//            multiplier: 1,
//            constant: 31)
//        
//        NSLayoutConstraint.activateConstraints([topConstraint])
        
        //mapView.addConstraint(topConstraint)
//

    }
    
    func searchBarTextDidEndEditing(searchBar: UISearchBar) {
        print("searchBarTextDidEndEditing")
        searchActive = false
    }
    
    func searchBarCancelButtonClicked(searchBar: UISearchBar) {
        print("searchBarCancelButtonClicked")
        searchActive = false
        self.searchResultTableView.hidden = true
        self.searchBar.resignFirstResponder()

    }
    
    func searchBarSearchButtonClicked(searchBar: UISearchBar) {
        print("searchBarSearchButtonClicked")

        self.searchBar.resignFirstResponder()
        
        searchActive = false
    }
    
    func searchBar(searchBar: UISearchBar, textDidChange searchText: String) {
        print("textDidChange")

        self.searchResultTableView.hidden = false
        
        

        
        if searchText.characters.count > 2
        {
            var urlToSearchWhenTextChange = "\(serverMainUrl)/squares?name=\(searchText)&lat=\(mapCenterLatitude)&lon=\(mapCenterLongitude)&userId=\(serverId)"
            urlToSearchWhenTextChange = urlToSearchWhenTextChange.stringByReplacingOccurrencesOfString(" ", withString: "%20")
            
            request(.GET, urlToSearchWhenTextChange).validate().responseJSON { response in
                switch response.result {
                case .Success:
                    print("REQUEST \(response.request)")
                    print("RESULT \(response.result.value)")
                    if let value = response.result.value {
                        self.squaresFromSearch = JSON(value)
                        
//                        for (index, value):(String, JSON) in self.squaresFromSearch
//                        {
//                            let i:Int=Int(index)!
//                            
//                            
////                            let coordinates = self.squaresFromSearch[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
////                            let latitude = (coordinates[0] as NSString).doubleValue
////                            let longitude = (coordinates[1] as NSString).doubleValue
////                            let title = self.squaresFromSearch[i]["_source"]["name"].string
////                            let identifier = self.squaresFromSearch[i]["_id"].string
//                            
//                        }
                        
                    
                        
                        if(self.squaresFromSearch.count == 0){
                            self.searchActive = false
                            self.searchResultTableView.separatorStyle = .None

                        } else {
                            self.searchActive = true
                            self.searchResultTableView.separatorStyle = .SingleLine

                        }
                        
                        dispatch_async(dispatch_get_main_queue(), { () -> Void in
                            self.searchResultTableView.reloadData()
                        })
                        
                    }
                case .Failure(let error):
                    print("FALLITO SEARCH: \(error)")
                    //cambia con analitics
                    //                Answers.logCustomEventWithName("Error",
                    //                    customAttributes: ["Error Debug Description": error.debugDescription])
                }
            }//ENDREQUEST


        } //end if > 2
        
                //####################################################################################################################################################################
        //END GETSQUARE REQUEST

        
//        filtered = data.filter({ (text) -> Bool in
//            let tmp: NSString = text
//            let range = tmp.rangeOfString(searchText, options: NSStringCompareOptions.CaseInsensitiveSearch)
//            return range.location != NSNotFound
//        })
//        if(self.squaresFromSearch.count == 0){
//            searchActive = false
//        } else {
//            searchActive = true
//        }
        
    } //endtextdidchange
    
    
    
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if(searchActive) {
            print("NUMBEROFROW: \(squaresFromSearch.count)")
            return self.squaresFromSearch.count
        }
        print("NUMBEROFROW: \(squaresFromSearch.count)")
        return self.squaresFromSearch.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        
        
        let cell = searchResultTableView.dequeueReusableCellWithIdentifier("Cell")! as UITableViewCell
        //let cell = self.searchResultTableView.dequeueReusableCellWithIdentifier("Cell", forIndexPath: indexPath) as! UITableViewCell

        
        //let label:UILabel = cell.viewWithTag(1) as! UILabel
        
        let i:Int=Int(indexPath.row)
        
        if(searchActive){

            cell.textLabel?.text = self.squaresFromSearch[i]["_source"]["name"].string
            print(cell.textLabel?.text)
        } else {
            cell.textLabel?.text = self.squaresFromSearch[i]["_source"]["name"].string
            print(cell.textLabel?.text)
        }
        cell.textLabel?.textColor = UIColor.lightGrayColor()
        cell.textLabel?.highlightedTextColor = inSquareUiColorQRed
        
        //cellbackground blurried
        let blur = UIBlurEffect(style: UIBlurEffectStyle.Dark)
        let blurView = UIVisualEffectView(effect: blur)
        cell.backgroundColor = UIColor.clearColor()
        cell.backgroundView = blurView
        //cell.selectedBackgroundView = blurView

        let blurLight = UIBlurEffect(style: UIBlurEffectStyle.Light)
        let blurSelView = UIVisualEffectView(effect: blurLight)

        var myBackView: UIView = UIView(frame: cell.frame)
        myBackView.backgroundColor = UIColor.clearColor()
        cell.selectedBackgroundView = blurSelView

        
//
//        cell.textLabel!.backgroundColor = UIColor.clearColor()
//        cell.detailTextLabel!.backgroundColor = UIColor.clearColor()
//        cell.backgroundColor = UIColor(white: 1, alpha: 0.55)
//
        
        dispatch_async(dispatch_get_main_queue(), {() -> Void in
            //This code will run in the main thread:
            var frame: CGRect = self.searchResultTableView.frame
            frame.size.height = self.searchResultTableView.contentSize.height
            self.searchResultTableView.frame = frame
        })
        
 
        
        return cell
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let indexPath = tableView.indexPathForSelectedRow
        
        let currentCell = tableView.cellForRowAtIndexPath(indexPath!)! as UITableViewCell
        
        let keyForCentermap = currentCell.textLabel!.text
        
        var latToCenter:CLLocationDegrees = CLLocationDegrees()
        var lonToCenter:CLLocationDegrees = CLLocationDegrees()
        
        var i = 0
        checkForKeyLoop: for (index, value):(String, JSON) in self.squaresFromSearch
        {
            i=Int(index)!
            
            if self.squaresFromSearch[i]["_source"]["name"].string == keyForCentermap
            {
                let coordinates = self.squaresFromSearch[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
                latToCenter = (coordinates[0] as NSString).doubleValue
                lonToCenter = (coordinates[1] as NSString).doubleValue

                break checkForKeyLoop
            }
            
            
            //                            let coordinates = self.squaresFromSearch[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
            //                            let latitude = (coordinates[0] as NSString).doubleValue
            //                            let longitude = (coordinates[1] as NSString).doubleValue
            //                            let title = self.squaresFromSearch[i]["_source"]["name"].string
            //                            let identifier = self.squaresFromSearch[i]["_id"].string
            
        }
        if (latToCenter != 0.0 && lonToCenter != 0.0)
        {
            let newCenter:CLLocationCoordinate2D = CLLocationCoordinate2DMake(latToCenter, lonToCenter)
            mapView.camera = GMSCameraPosition(target: newCenter, zoom: 15, bearing: 0, viewingAngle: 0)
            
            let coordinates = self.squaresFromSearch[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
            let latitude = (coordinates[0] as NSString).doubleValue
            let longitude = (coordinates[1] as NSString).doubleValue
            let title = self.squaresFromSearch[i]["_source"]["name"].string
            let identifier = self.squaresFromSearch[i]["_id"].string
                            
            let marker = GMSMarker()
            marker.position = CLLocationCoordinate2DMake(latitude, longitude)
            marker.title = title
            marker.snippet = self.snippetString
            //marker.snippet = snippet
            marker.icon = insquareMapPin
            marker.groundAnchor.x = marker.groundAnchor.x + 0.45
            marker.map = self.mapView
            marker.userData = ["markerId": "\(identifier!)", "markerLat": "\(latitude)", "markerLon": "\(longitude)", "favouredBy": "\(self.jsonSq[i]["_source"]["favouredBy"])", "lastMessageDate": "\(self.jsonSq[i]["_source"]["lastMessageDate"].string!)", "views": "\(self.jsonSq[i]["_source"]["views"])", "state": "\(self.jsonSq[i]["_source"]["state"].string!)"]
            
            self.squareToSearch[marker.userData["markerId"] as! String] = marker
            
            self.mapView.selectedMarker = marker
            
            searchBarCancelButtonClicked(self.searchBar)
            
            

            
//            dispatch_async(dispatch_get_main_queue(), {() -> Void in
//                //This code will run in the main thread:
//                
//                self.searchBar.resignFirstResponder()
//                
//            })
        
            

        }
        
        
        //searchResultTableView.cellForRowAtIndexPath(indexPath: NSIndexPath)
    }

    
    //end search
    
    
    //location manager
    //####################################################################################################################################################################
//    func locationManager(manager: CLLocationManager, didUpdateLocations locations: [CLLocation])
//    {
//        
//        var userLocation:CLLocation = locations[0]
//        
//        self.latitude = userLocation.coordinate.latitude
//        self.longitude = userLocation.coordinate.longitude
//        
//        //if latitude == 0 && longitude == 0
//        //{
//        //update mapview
//        self.mapView.animateToLocation(CLLocationCoordinate2DMake(latitude, longitude))
//        
//        //stop updating location
//        manager.stopUpdatingLocation()
//        
//        //print(locations)
//        
//        
//        //}
//        
//        //Analitycs
//        var tracker = GAI.sharedInstance().defaultTracker
//        tracker.send(GAIDictionaryBuilder.createEventWithCategory("OpenedMap", action: "Center: \(locations)", label: "User \(serverId) opened the map centered in current location", value: nil).build() as [NSObject : AnyObject])
//        
//        
//        
//        
//    }//end location manager
    //####################################################################################################################################################################

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
            if meters > Double(distanceInKmForGetSquare*990)
            {
                //print("newGET")
                
                
                if (mapCenterLatitude == 41.890466 && mapCenterLongitude == 12.492231) || (mapCenterLatitude == 0.0 && mapCenterLongitude == 0.0)
                {
                    //Analitycs
                    var tracker = GAI.sharedInstance().defaultTracker
                    tracker.send(GAIDictionaryBuilder.createEventWithCategory("GetSquare gone wrong", action: "Latitude: \(mapCenterLatitude), Longitude: \(mapCenterLongitude), Radius: \(distanceInKmForGetSquare)", label: "User \(serverId) did a new get square", value: nil).build() as [NSObject : AnyObject])
                    
                    
                }
                else
                {
                    //Analitycs
                    var tracker = GAI.sharedInstance().defaultTracker
                    tracker.send(GAIDictionaryBuilder.createEventWithCategory("GetSquare", action: "Latitude: \(mapCenterLatitude), Longitude: \(mapCenterLongitude), Radius: \(distanceInKmForGetSquare)", label: "User \(serverId) did a new get square", value: nil).build() as [NSObject : AnyObject])
                }
                
                
                
                //centra mappa, controlla se non la centra da solo e allora non si raggiunge mai distanza maggiore
                mapCenterLatitude = position.target.latitude
                mapCenterLongitude = position.target.longitude
                
                //fai get
                //getSquare(mapCenterLatitude, longitude: mapCenterLongitude, distanceInKm: distanceInKmForGetSquare) //ritorna json poi devi lavorarlo
                //mi sa che prima di aggiungere marker nuovi va pulita mappa o non aggiunti quelli presenti
                request(.GET, "\(serverMainUrl)/squares", parameters:["distance": "\(distanceInKmForGetSquare)km", "lat": mapCenterLatitude, "lon": mapCenterLongitude]).validate().responseJSON { response in
                    //print("QWERTYUIO \(response.request)")
                    switch response.result {
                    case .Success:
                        if let value = response.result.value {
                            self.jsonSq = JSON(value)
                            
                            for (index, value):(String, JSON) in self.jsonSq
                            {
                                let i:Int=Int(index)!
                                //cerca se già presente con stesso id prima di aggiungere
                                let coordinates = self.jsonSq[i]["_source"]["geo_loc"].string!.componentsSeparatedByString(",")
                                let latitude = (coordinates[0] as NSString).doubleValue
                                let longitude = (coordinates[1] as NSString).doubleValue
                                let title = self.jsonSq[i]["_source"]["name"].string
                                let identifier = self.jsonSq[i]["_id"].string
                                let marker = GMSMarker()
                                marker.position = CLLocationCoordinate2DMake(latitude, longitude)
                                marker.title = title
                                marker.snippet = self.snippetString
                                marker.userData = ["markerId": "\(identifier!)", "markerLat": "\(latitude)", "markerLon": "\(longitude)", "favouredBy": "\(self.jsonSq[i]["_source"]["favouredBy"])", "lastMessageDate": "\(self.jsonSq[i]["_source"]["lastMessageDate"].string!)", "views": "\(self.jsonSq[i]["_source"]["views"])", "state": "\(self.jsonSq[i]["_source"]["state"].string!)"]
                                marker.icon = insquareMapPin
                                marker.groundAnchor.x = marker.groundAnchor.x + 0.45
                                marker.map = self.mapView
                                
                                //search
                                self.squareToSearch[marker.userData["markerId"] as! String] = marker
//                                print(self.squareToSearch.count)

                                
                            }
                            //self.squareAroundYouTable.reloadData()
                            
                        }
                    case .Failure(let error):
                        print(error)
//make analitics
//                        Answers.logCustomEventWithName("Error",
//                            customAttributes: ["Error Debug Description": error.debugDescription])
                    }
                }
                //reload map cosi vedi piazze // lo fa gia dentro request3
            }
        }
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
            if (textField.text == "")
            {
                print("Not selected a name for the square")
                return
            }
            else
            {
                let marker = GMSMarker()
                marker.position = CLLocationCoordinate2DMake(coordinate.latitude, coordinate.longitude)
                print("MARKER POSITION \(marker.position)")
                marker.title = textField.text
                print("MARKER title \(marker.title)")

                //use reversed geolocalization to add addres for description or use another textfield if needed
                marker.snippet = "Re-open the app to update squarelist"
                marker.icon = insquareMapPin
                marker.groundAnchor.x = marker.groundAnchor.x + 0.45
                marker.map = mapView
                //self.squareAroundYou.append(["Latitude" : coordinate.latitude, "Longitude" : coordinate.longitude, "Name" : textField.text!, "Description" : "Tap to open the Square"]) //mi sa inutile ora
                //self.squareAroundYouTable.reloadData()
                
                let latR = Double(round(1000000*coordinate.latitude)/1000000)
                let longR = Double(round(1000000*coordinate.longitude)/1000000)
                
                var jsnResult = JSON(data: NSData())
                print("TEXT: \(textField.text)")
                
                // http://recapp-insquare.rhcloud.com/squares?name=\(textField.text!)&lat=\(latR)&lon=\(longR)&ownerId=\(serverId)
                var urlPostSquare = "\(serverMainUrl)/squares?name=\(textField.text!)&lat=\(latR)&lon=\(longR)&ownerId=\(serverId)"
                urlPostSquare = urlPostSquare.stringByReplacingOccurrencesOfString(" ", withString: "%20")
                
                //print("REQUEST URL: \(urlPostSquare)")
                request(.POST, urlPostSquare).validate().responseJSON { response in
                    //print("REQUEST POST SQUARE: \(response.request)")
                    switch response.result {
                    case .Success:
                        //print("RESULT \(response.result)")
                        //print("RESULT \(response.result.value)")

                        if let value = response.result.value
                        {
                            jsnResult = JSON(value)
                            //print("qwert \(jsnResult)")
                            //marker.userData = ["markerId": "\(jsnResult["_id"].string!)"]
                            //togli [0]
                            marker.userData = ["markerId": "\(jsnResult["_id"].string!)", "markerLat": "\(coordinate.latitude)", "markerLon": "\(coordinate.longitude)", "favouredBy": "0", "lastMessageDate": "\(NSDate())", "views": "0", "state": "asleep"]
                            //print("qwert \(marker.userData)")
                            
                            //search
                            self.squareToSearch[marker.userData["markerId"] as! String] = marker
                            //print(self.squareToSearch.count)
                            
                            //Analitycs
                            var tracker = GAI.sharedInstance().defaultTracker
                            tracker.send(GAIDictionaryBuilder.createEventWithCategory("Created a Square", action: "Name: \(marker.title), Id: \(marker.snippet), Latitude: \(marker.position.latitude), Longitude: \(marker.position.longitude)", label: "User \(serverId) created a new square", value: nil).build() as [NSObject : AnyObject])
                            
                        }
                    case .Failure(let error):
                        print("FALLITO \(error)")
//make analitics
//                        Answers.logCustomEventWithName("Error",
//                            customAttributes: ["Error Debug Description": error.debugDescription])
                    }
                }
                
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
        self.squareId = marker.userData["markerId"]! as! String
        self.squareLatitude = marker.position.latitude
        self.squareLongitude = marker.position.longitude
        //        let secondViewController = self.storyboard!.instantiateViewControllerWithIdentifier("ViewController")
        //        self.navigationController!.pushViewController(secondViewController, animated: true)
        
        //Analitycs
        var tracker = GAI.sharedInstance().defaultTracker
        tracker.send(GAIDictionaryBuilder.createEventWithCategory("Entered a Square", action: "Name: \(marker.title), Id: \(marker.snippet), Latitude: \(marker.position.latitude), Longitude: \(marker.position.longitude)", label: "User \(serverId) entered a square", value: nil).build() as [NSObject : AnyObject])
        
        
        self.performSegueWithIdentifier("goInTheSquare2", sender: self)
        
        
    }
    //####################################################################################################################################################################
    //END MAPVIEW FUNC

    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(true)
        //analitics get that this view will appear
        //UIApplication.sharedApplication().statusBarStyle = .LightContent

        dispatch_async(dispatch_get_main_queue(), {() -> Void in
            //This code will run in the main thread:
            var frame: CGRect = self.searchResultTableView.frame
            frame.size.height = self.searchResultTableView.contentSize.height
            self.searchResultTableView.frame = frame
        })

        
        var name = "HomeViewController"
      
        var tracker = GAI.sharedInstance().defaultTracker
        tracker.set(kGAIScreenName, value: name)
        
        var builder = GAIDictionaryBuilder.createScreenView()
        tracker.send(builder.build() as [NSObject : AnyObject])
        
        searchResultTableView.hidden = true

        
    }//END VWA

    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()

    }//END DRMW
    
    
    override func prepareForSegue(segue: UIStoryboardSegue!, sender: AnyObject!)
    {
        if (segue.identifier == "goInTheSquare2") {
            //Checking identifier is crucial as there might be multiple
            // segues attached to same view
            print("goInTheSquare2")
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

// MARK: - CLLocationManagerDelegate
//You create a MapViewController extension that conforms to CLLocationManagerDelegate
extension MapViewController: CLLocationManagerDelegate {
    //locationManager(_:didChangeAuthorizationStatus:) is called when the user grants or revokes location permissions.
    func locationManager(manager: CLLocationManager, didChangeAuthorizationStatus status: CLAuthorizationStatus) {
        // Here you verify the user has granted you permission while the app is in use.
        if status == .AuthorizedWhenInUse {
            
            // Once permissions have been established, ask the location manager for updates on the user’s location.
            locationManager.startUpdatingLocation()
            
            //GMSMapView has two features concerning the user’s location: myLocationEnabled draws a light blue dot where the user is located, while myLocationButton, when set to true, adds a button to the map that, when tapped, centers the map on the user’s location
            mapView.myLocationEnabled = true
            mapView.settings.myLocationButton = true
        }
    }
    
    // locationManager(_:didUpdateLocations:) executes when the location manager receives new location data
    func locationManager(manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let location = locations.first {
            
            if backFromSquareLatitude == 0.0 && backFromSquareLongitude == 0.0
            {
                // This updates the map’s camera to center around the user’s current location. The GMSCameraPosition class aggregates all camera position parameters and passes them to the map for display.
                mapView.camera = GMSCameraPosition(target: location.coordinate, zoom: 15, bearing: 0, viewingAngle: 0)
            }
            else
            {
                let latestJoinedSquareCoordinates:CLLocationCoordinate2D = CLLocationCoordinate2DMake(backFromSquareLatitude, backFromSquareLongitude)
                // This updates the map’s camera to last square joined.
                mapView.camera = GMSCameraPosition(target: latestJoinedSquareCoordinates, zoom: 15, bearing: 0, viewingAngle: 0)
            }
            
            
                    //Analitycs
                    var tracker = GAI.sharedInstance().defaultTracker
                    tracker.send(GAIDictionaryBuilder.createEventWithCategory("OpenedMap", action: "Center: \(locations)", label: "User \(serverId) opened the map centered in current location", value: nil).build() as [NSObject : AnyObject])

            
            // Tell locationManager you’re no longer interested in updates; you don’t want to follow a user around as their initial location is enough for you to work with.
            locationManager.stopUpdatingLocation()
        }
        
    }
}


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
