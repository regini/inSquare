//
//  SquareMessViewController.swift
//  inSquare
//
//  Created by Alessandro Steri on 27/02/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import JSQMessagesViewController
import Alamofire
import AudioToolbox



class SquareMessViewController: JSQMessagesViewController
{
    var triggeredByPushNotification = false
    var viewControllerNavigatedFrom:AnyObject?
    
    //values passed by marker press in homeViewController, se fai segue vengono comunque sovrascritti
    var squareId:String = String()
    var squareName:String = String()
    var squareLatitude:Double = Double()
    var squareLongitude:Double = Double()
    
    //unwind segues
    @IBAction func backButton(sender: AnyObject)
    {
        if self.viewControllerNavigatedFrom!.isKindOfClass(FavoriteSquareViewController) {
            print("unwindToFavunwindToFav")
            performSegueWithIdentifier("unwindToFav", sender: sender)
        }
        else if self.viewControllerNavigatedFrom!.isKindOfClass(MapViewController) {
            print("unwindToMapunwindToMap")
            performSegueWithIdentifier("unwindToMap", sender: sender)

        }
        else if self.viewControllerNavigatedFrom!.isKindOfClass(RecentsViewController) {
            print("unwindToRecunwindToRec")
            performSegueWithIdentifier("unwindToRec", sender: sender)

        }
        else if self.viewControllerNavigatedFrom!.isKindOfClass(ProfileViewController) {
            print("unwindToProfileunwindToProfile")
            performSegueWithIdentifier("unwindToProfile", sender: sender)
        }
    }
    
    //report abuse
    @IBAction func reportAbuse(sender: AnyObject)
    {
        print("HERE WE ARE")
        //1. Create the alert controller.
        var alert = UIAlertController(title: "REPORT AN ABUSE", message: "Enter The Offensive Username", preferredStyle: .Alert)
        //2. Add the text field. You can configure it however you need.
        alert.addTextFieldWithConfigurationHandler({ (textField) -> Void in
            textField.text = ""
        })
        //3. Grab the value from the text field, and print it when the user clicks OK.
        alert.addAction(UIAlertAction(title: "OK", style: .Default, handler: { (action) -> Void in
            let textField = alert.textFields![0] as UITextField
            if (textField.text == "")
            {
                print("Abuse not reported.")
                return
            }
            else
            {
                let abuseReportText = "ABUSE REPORTED, SQUARE: \(self.squareName) USER:\(textField.text)"
                
                var jsnResult = JSON(data: NSData())
                print("TEXT: \(textField.text)")
                
                var urlPostSquare = "\(serverMainUrl)/feedback?feedback=\(textField.text!)&username=\(username)&activity=ABUSE REPORT IN SQUARE \(self.squareId)"
                urlPostSquare = urlPostSquare.stringByReplacingOccurrencesOfString(" ", withString: "%20")
                
                print("REQUEST URL: \(urlPostSquare)")
                
                var dataForBody:JSON = ["feedback": textField.text!, "username": username, "activity" : "ABUSE REPORT IN SQUARE \(self.squareId)"]
                
                print(dataForBody)
                
                request(.POST, urlPostSquare, parameters: ["feedback": textField.text!, "username": serverId, "activity" : "ABUSE REPORT IN SQUARE \(self.squareId)"]).validate().responseString { response in
                    print("REQUEST POST SQUARE: \(response.request)")
                    switch response.result {
                    case .Success:
                        print("RESULT \(response.result)")
                        print("RESULT \(response.result.value)")
                        
                        if let value = response.result.value
                        {
                            jsnResult = JSON(value)
                            print("qwert \(jsnResult)")
                            
                            
                            
                            //Analitycs
                            var tracker = GAI.sharedInstance().defaultTracker
                            tracker.send(GAIDictionaryBuilder.createEventWithCategory("Reported an abuse", action: "Reporter: \(username), Abuser: \(textField.text!), Square: \(self.squareId), Squarename: \(self.squareName)", label: "User \(serverId) reported an abuse", value: nil).build() as [NSObject : AnyObject])
                            
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
    }
    
    @IBOutlet var likeButtonOutlet: UIBarButtonItem!
    
    //add to favourite
    @IBAction func likeButton(sender: AnyObject)
    {
        if isFavourite(squareId)
        {
            //remove from favourite
            
            //update img
            likeButtonOutlet.image = like

            //add to favourite
            var favUrl = "\(serverMainUrl)/favouritesquares?squareId=\(squareId)&userId=\(serverId)"
            favUrl = convertSpacesInUrl(favUrl)
            print("FAV URL: \(favUrl)")
            
            request(.DELETE, favUrl).validate().responseData { response in
                print("DELETED FAVOURITE: \(response.request)")
                
                switch response.result
                {
                case .Success:
                    if let value = response.result.value
                    {
                        print("DELETED FAVOURITE: \(value)")
                        updateFavouriteSquares()
                    }
                case .Failure(let error):
                    print(error)
                }
            }//ENDREQUEST
        }        
        else
        {
            //update img
            likeButtonOutlet.image = likeFilled
            
            //add to favourite
            var favUrl = "\(serverMainUrl)/favouritesquares?squareId=\(squareId)&userId=\(serverId)"
            favUrl = convertSpacesInUrl(favUrl)
            print("FAV URL: \(favUrl)")
            
            request(.POST, favUrl).validate().responseData { response in
                print("FAVOURITED: \(response.request)")
                
                
                
                switch response.result
                {
                case .Success:
                    if let value = response.result.value
                    {
                        print("FAVOURITED: \(value)")
                        updateFavouriteSquares()
                    }
                case .Failure(let error):
                    print(error)
                }
            }//ENDREQUEST
        }
        
        updateFavouriteSquares()
    }
    
    
    //TODO: doublecheck if pong work
    //data per pong
    var dataPong:JSON = ["beat": 1]

    //Client Socket.io Swift    localhost: http://127.0.0.1:3000
    let socket = SocketIOClient(socketURL: "\(serverSocketMainUrl)", options: [.Log(true), .ForcePolling(true)])


    //JSQ Setting bubble
    let incomingBubble = JSQMessagesBubbleImageFactory().incomingMessagesBubbleImageWithColor(inSquareUiColorQRed) //ORIGINAL COLOR: UIColor(red: 10/255, green: 180/255, blue: 230/255, alpha: 1.0)
    
    let outgoingBubble = JSQMessagesBubbleImageFactory().outgoingMessagesBubbleImageWithColor(UIColor.lightGrayColor())
    
    //Messages of the square joined
    var messages = [JSQMessage]()
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        print("##viewDidLoad")
        updateFavouriteSquares()
        
        print("triggeredByPushNotification", triggeredByPushNotification)
        if triggeredByPushNotification
        {
            let refreshButton = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.Done, target: self, action: "doneAfterCheckingPush")
            navigationItem.leftBarButtonItem = refreshButton
            
            //navigationController?.navigationBar.barTintColor = UIColor.greenColor()
            //title = "Search result"

        }
        
        //socket.io config handlers, connect and joining namespace
        self.addHandlers()
        self.socket.connect()
        self.socket.joinNamespace(serverSocketNamespace)

        //setting up JSQVC: title, and user data (name and id)
        self.setup()
        
        downloadExistingMessagesFromServer()

        //self.automaticallyScrollsToMostRecentMessage = true
        self.showLoadEarlierMessagesHeader = true
        
    }//END VDL
    
    
    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(true)
        print("##viewWillAppear")

        
        updateFavouriteSquares()

        if isFavourite(squareId)
        {
            likeButtonOutlet.image = likeFilled
        }
        else
        {
            likeButtonOutlet.image = like
        }
    }//END VWA

    override func viewWillDisappear(animated:Bool)
    {
        self.socket.disconnect()
        print("##viewWillDisappear")

        super.viewWillDisappear(animated)
    }

    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()

    }//END DRMW
    
    //Back To Map Segues
    override func prepareForSegue(segue: UIStoryboardSegue!, sender: AnyObject!)
    {
        if (segue.identifier == "backToHome") {
            
            //var detailVC = segue!.destinationViewController as! SquareMessViewController
            //var detailVC = segue!.destinationViewController as! NavigationToJSQViewController
            //            let detailVC = detailNC.topViewController as! inSquareViewController
            
            let navVC = segue.destinationViewController as! UITabBarController
            let detailVC = navVC.childViewControllers[2] as! MapViewController
            
            detailVC.backFromSquareLatitude = self.squareLatitude
            detailVC.backFromSquareLongitude = self.squareLongitude
            
            
        }
    }


    func doneAfterCheckingPush() {
        print("Perform action")
        var myTabBar = self.storyboard?.instantiateViewControllerWithIdentifier("tabBarController") as! UITabBarController
        UIApplication.sharedApplication().delegate!.window?!.rootViewController = UIStoryboard(name: "Main", bundle: NSBundle.mainBundle()).instantiateInitialViewController()

    }

    
    //reload JSQ mess display
    func reloadMessagesView()
    {
        self.collectionView?.reloadData()
    }
    
    //avoid inconsistencies between data source and messages view and downloads newest messages
    func reloadAllMessages() {
        self.messages = []
        self.reloadMessagesView()
        self.downloadExistingMessagesFromServer()
    }
    
    func addHandlers()
    {
        
        self.socket.onAny
        {
            print("Got event: \($0.event), with items: \($0.items), in socket Status: \(self.socket.status)")
        }
        
        self.socket.on("newMessage") {data, ack in
            
            let messJSN = JSON(data)
            print("Message for you! \(messJSN[0])")
            print("Message data from server: \(data)")

            if messJSN[0] == "Server"
            {
                //do work with server mess (allert? notification?)
                print("Server Mess" + messJSN[1].string!)
            }
            else if messJSN[0]["userid"].string == "0"
            {
                print("sender userid is equal to 0 check what's happen")
                
            }
            else if messJSN[0]["contents"] != nil
            {
                print(data)
                
                let dateFormatter = NSDateFormatter()
                dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                
                var date = dateFormatter.dateFromString("\(messJSN[0]["createdAt"].string)")
                print("DATA MESS INVIATO formattata: \(date) - data su server= \(messJSN[0]["createdAt"].string)")
                if date == nil
                {
                    date = NSDate()
                    let dateFormatter = NSDateFormatter()
                    dateFormatter.dateStyle = NSDateFormatterStyle.ShortStyle
                    var convertedDate = dateFormatter.stringFromDate(date!)
                    print("Date was nil so now is \(date)")
                    
                }
                self.addMessage("\(messJSN[0]["userid"].string)", sender: "\(messJSN[0]["username"])", date: date!, messageContent: "\(messJSN[0]["contents"].string!)")
                
            }
 
        }
        
        socket.on("ping")
            {
                data, ack in
                self.socket.emit("pong", self.dataPong.rawValue)
                print("emitted pong: \(self.socket.status)")
        }
        
        socket.on("tellRoom") { dati, ack in
        }
        
        socket.on("sendMessage") { dati, ack in
        
            print("Sended Mess with ACK:\(ack)")
        }

        socket.on("pong") { dati, ack in
            print("PONG!! dati: \(dati), ack: \(ack)")
        }
        
        socket.on("connect") {data, ack in
            print("SOCKET ON CONNECT IN STATUS: \(self.socket.status)")

            
            self.inputToolbar.contentView.rightBarButtonItem.enabled = true
            var data:JSON = ["username": username, "room": self.squareId, "user" : serverId]
            print("Socket Connect with data: \(data.rawValue)")
            self.socket.emit("addUser", data.rawValue)
           

        
            
//            var tracker = GAI.sharedInstance().defaultTracker
//            tracker.send(GAIDictionaryBuilder.createEventWithCategory("Soket.onConnect", action: "Data: \(data.rawValue)", label: "User \(serverId) got socket.onConnect event", value: nil).build() as [NSObject : AnyObject])
            
        }
        
        socket.on("reconnect") {data, ack in
            
            print("SOCKET ON RECONNECT IN STATUS: \(self.socket.status)")
            self.inputToolbar.contentView.rightBarButtonItem.enabled = false
            
        }


    }

//    override func viewDidAppear(animated: Bool) {
//        super.viewDidAppear(animated)
//        self.collectionView!.collectionViewLayout.springinessEnabled = true
//
//    }
    
    

    

}//END VC







//MARK - Setup
extension SquareMessViewController {
//    func addDemoMessages() {
//        for i in 1...10 {
//            let sender = (i%2 == 0) ? "Server" : self.senderId
//            let messageContent = "Message nr. \(i)"
//            let message = JSQMessage(senderId: sender, displayName: sender, text: messageContent)
//            self.messages += [message]
//        }
//        self.reloadMessagesView()
//    }
    
    func addMessage(senderId: String, sender: String, date: NSDate, messageContent: String)
    {
        let message = JSQMessage(senderId: senderId, displayName: sender, text: messageContent)
        
        self.messages += [message]
        //RIGA SOTO INUTILE
        let mee = JSQMessage(senderId: senderId, senderDisplayName: sender, date: date, text: messageContent)
        print("ADD MESSAGE in JSQVC, MESS DATE: \(message.date)")
        AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
        self.reloadMessagesView()
    }
    
    //serve? giusto per prima add quando apri viewcontr
    func addMessagesWithoutReload(senderId: String, sender: String, date: NSDate, messageContent: String)
    {
        let message = JSQMessage(senderId: senderId, senderDisplayName: sender, date: date, text: messageContent)
        self.messages += [message]
        print(message.date)

    }
    
    func setup()
    {
        self.title = "#\(squareName)"
        
        self.senderId = serverId
        self.senderDisplayName = username
        
        
//        self.senderId = UIDevice.currentDevice().identifierForVendor?.UUIDString
//        self.senderDisplayName = UIDevice.currentDevice().identifierForVendor?.UUIDString
    }
}


//MARK - Data Source
extension SquareMessViewController {
    
    //how many messages we have (we return message count inside our array)
    override func collectionView(collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return self.messages.count
    }
    
    //which message to display where
    override func collectionView(collectionView: JSQMessagesCollectionView!, messageDataForItemAtIndexPath indexPath: NSIndexPath!) -> JSQMessageData! {
        let data = self.messages[indexPath.row]
        return data
    }
    
    //what to do when a message is deleted (delete if from messages array)
    override func collectionView(collectionView: JSQMessagesCollectionView!, didDeleteMessageAtIndexPath indexPath: NSIndexPath!) {
        self.messages.removeAtIndex(indexPath.row)
    }
    
    //which bubble to choose (outgoing if we are the sender, and incoming otherwise)
    override func collectionView(collectionView: JSQMessagesCollectionView!, messageBubbleImageDataForItemAtIndexPath indexPath: NSIndexPath!) -> JSQMessageBubbleImageDataSource! {
        let data = messages[indexPath.row]
        switch(data.senderId) {
        case self.senderId:
            return self.outgoingBubble
        default:
            return self.incomingBubble
        }
    }
    
    //what to use as an avatar (for now we’ll return nil and will not show avatars yet)
    override func collectionView(collectionView: JSQMessagesCollectionView!, avatarImageDataForItemAtIndexPath indexPath: NSIndexPath!) -> JSQMessageAvatarImageDataSource! {
        //JSQMessagesAvatarImageFactory.circularAvatarImage(insquareMapPin, withDiameter: 2)
        let data = messages[indexPath.row]
        switch(data.senderId) {
        case self.senderId:
            let diameter = UInt(collectionView.collectionViewLayout.outgoingAvatarViewSize.width)
            let image = userAvatar
            return JSQMessagesAvatarImageFactory.avatarImageWithImage(image, diameter: diameter)
        default:
//            var hasPicProfile = false
//            let diameter = UInt(collectionView.collectionViewLayout.outgoingAvatarViewSize.width)
//            var image:UIImage = UIImage()
//            print("IMG BIG: \(image.size)")
//
//            request(.GET, "http://recapp-insquare.rhcloud.com/profilePictures/\(data.senderId)").validate().responseString { response in
//                print("GETURL: \(response.request)")
//                print("RISPOSTA: \(response.result.value)")
//                switch response.result {
//                case .Success:
//                    if let stringURL = response.result.value {
//                        print("USERPICURL:")
//                        
//                        let url = NSURL(string: stringURL)
//                        if let data = NSData(contentsOfURL: url!) //make sure your image in this url does exist, otherwise unwrap in a if let check
//                        {
//                            hasPicProfile = true
//                            image = UIImage(data: data)!
//                            
//                        }
//                        else
//                        {
//                            //ridondante
//                            //hasPicProfile = false
//                            print("User img not found")
//                        }
//                    }
//                case .Failure(let error):
//                    print(error)
//                    
//                    //                Answers.logCustomEventWithName("Error",
//                    //                    customAttributes: ["Error Debug Description": error.debugDescription])
//                }
//            }//END REQ
//            print("IMG BIG: \(image.size)")
//            if hasPicProfile {return JSQMessagesAvatarImageFactory.avatarImageWithImage(image, diameter: diameter)}
//            else {return nil}
            
            //default set as insquare pin
            let diameter = UInt(collectionView.collectionViewLayout.outgoingAvatarViewSize.width)
            let image = opponentDefaultAvatar
            return JSQMessagesAvatarImageFactory.avatarImageWithImage(image, diameter: diameter)
        }

        
        //return nil
    }

    //toplabel
    
    //returns sender display name for each message bubble
    override func collectionView(collectionView: JSQMessagesCollectionView!, attributedTextForMessageBubbleTopLabelAtIndexPath indexPath: NSIndexPath!) -> NSAttributedString! {
        let data = self.collectionView(self.collectionView, messageDataForItemAtIndexPath: indexPath)
        if (self.senderDisplayName == data.senderDisplayName()) {
            return nil
        }
        return NSAttributedString(string: data.senderDisplayName())
    }
    
    //Second functions defines height of the label with sender display name. If we want to show it (we were not the sender of the message) we return default height (value provided by the JSQMessagesViewController library), otherwise we return 0.0 to hide the label.
    override func collectionView(collectionView: JSQMessagesCollectionView!, layout collectionViewLayout: JSQMessagesCollectionViewFlowLayout!, heightForMessageBubbleTopLabelAtIndexPath indexPath: NSIndexPath!) -> CGFloat {
        let data = self.collectionView(self.collectionView, messageDataForItemAtIndexPath: indexPath)
        if (self.senderDisplayName == data.senderDisplayName()) {
            return 0.0
        }
        return kJSQMessagesCollectionViewCellLabelHeightDefault
    }
    
    //bottomlabel
    
    //returns sender display name for each message bubble
    override func collectionView(collectionView: JSQMessagesCollectionView!, attributedTextForCellBottomLabelAtIndexPath indexPath: NSIndexPath!) -> NSAttributedString! {
        let data = self.collectionView(self.collectionView, messageDataForItemAtIndexPath: indexPath)
//        if (self.senderDisplayName == data.senderDisplayName()) {
//            return nil
//        }
        //do operation with date like if in latest week only day, check also if time is set to correct time zone
        let timestamp = data.date()
//        let currentDate = NSDate()
        var dateFormatter = NSDateFormatter()
        //cambia dateform a seconda se stesso giorno o stesso mese ecc
        dateFormatter.dateFormat = "hh:mm (dd/MM)" //format style. Browse online to get a format that fits your needs.
        var dateString = dateFormatter.stringFromDate(timestamp)
        
        return NSAttributedString(string: dateString)
    }
    
    //Second functions defines height of the label with sender display name. If we want to show it (we were not the sender of the message) we return default height (value provided by the JSQMessagesViewController library), otherwise we return 0.0 to hide the label.
    override func collectionView(collectionView: JSQMessagesCollectionView!, layout collectionViewLayout: JSQMessagesCollectionViewFlowLayout!, heightForCellBottomLabelAtIndexPath indexPath: NSIndexPath!) -> CGFloat {
        let data = self.collectionView(self.collectionView, messageDataForItemAtIndexPath: indexPath)
//        if (self.senderDisplayName == data.senderDisplayName()) {
//            return 0.0
//        }
        return kJSQMessagesCollectionViewCellLabelHeightDefault
    }

    
}

//MARK - Toolbar
extension SquareMessViewController {
    override func didPressSendButton(button: UIButton!, withMessageText text: String!, senderId: String!, senderDisplayName: String!, date: NSDate!) {
        if self.socket.status != SocketIOClientStatus.Connected
        {
            //refere to send button
            return
        }
        let message = JSQMessage(senderId: senderId, senderDisplayName: senderDisplayName, date: date, text: text)
        print("SQMVC, DATE DEVE ESSERE DIVERSA DA NIL: \(date)")
        //let mes = JSQMessage(senderId: senderId, senderDisplayName: senderDisplayName, date: date, media: JSQPhotoMediaItem(image: insquareMapPin))
        self.messages += [message]
        self.sendMessageToSquare(message)
        self.finishSendingMessage()
        
    }
    
    override func didPressAccessoryButton(sender: UIButton!)
    {
        print("didPressAccessoryButton")
    }
}


//MARK - ToServer
extension SquareMessViewController {
    
    func sendMessageToSquare(message: JSQMessage) {
        
        print("SOCKET STATUS: \(self.socket.status)")
        if self.socket.status != SocketIOClientStatus.Connected
        {
            print("SOCKET STATUS: \(self.socket.status) == non connesso!")
            
//            self.socket.reconnect()
//            
//            var data:JSON = ["username": username, "room": self.squareId, "user" : serverId]
//            print("Socket Connect with data: \(data.rawValue)")
//            self.socket.emit("addUser", data.rawValue)
            
            //self.socket.connect()
           // self.socket.joinNamespace("/squares")

        }

        if "\(message.senderId)" == serverId
        {
            var data:JSON = ["username": username, "room": squareId, "userid" : serverId, "message" : message.text]
            print("SENDED MESS DATA: \(data)")
            
            //socket.emit("sendMessage", data.rawValue)
            
//            socket.emitWithAck("sendMessage", data.rawValue).onAck {data in
//                print("got ack with data: (data)")
//            }

            socket.emitWithAck("sendMessage", data.rawValue)(timeoutAfter: 0) {data in
                print("got ack")
            }
            
//            socket.emitWithAck("sendMessage", data.rawValue)
            
            /**
             Sends a message to the server, requesting an ack. Use the onAck method of SocketAckHandler to add
             an ack.
             */
//            public func emitWithAck(event: String, _ items: AnyObject...) -> OnAckCallback {
//                return emitWithAck(event, withItems: items)
//            }

                        
            //self.socket.emit("addUser", data.rawValue, ack)
            //self.socket.emitWithAck("", items: AnyObject...)
            
//            //answer custom event
//            Answers.logCustomEventWithName("sendMessage",
//                customAttributes: ["username": username, "room": squareId, "userid" : serverId, "message" : message.content])
//            
//            //Analitycs
//            var tracker = GAI.sharedInstance().defaultTracker
//            tracker.send(GAIDictionaryBuilder.createEventWithCategory("Message Sended", action: "Mess: \(message.content)", label: "User \(serverId) sended a message", value: nil).build() as [NSObject : AnyObject])
        
            print("Message Sended: \(message.text)")
            
        }
        else if "\(message.senderId)" != serverId
        {
            //visualizza e basta
            
            print("Got Message ??: \(message.text)")
            
        }
        
//        let messageToSend = Message()
//        messageToSend.text = message.text
//        messageToSend.senderId = self.senderId
//        messageToSend.channel = syncanoChannelName
//        messageToSend.other_permissions = .Full
//        messageToSend.saveWithCompletionBlock { error in
//            if (error != nil) {
//                //Super cool error handling
//            }
//        }
    }//end smts
    
    
    func downloadExistingMessagesFromServer()
    {

        //ADD O JSQ MESSAGES ALREADY EXISTING ON INSTANCED SQUARE
        request(.GET, "\(serverMainUrl)/messages", parameters:["recent": "true", "size": "100", "square": squareId]).validate().responseJSON { response in
            
            print("REQUEST: \(response.request)")
            print("RESPONSE: \(response.response)")
            print("RESULT: \(response.result)")
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    var jsonMess = JSON(value)
                    print("MESS:")
                    print(jsonMess)
                    
                    //self.chatController.opponentImage = UIImage(named: "User")
                    //self.chatController.title = "#\(self.squareName)"
                    //self.chatController.navigationItem.title = ""
                    //var backButton = UIBarButtonItem(title: "Home", style: UIBarButtonItemStyle.Bordered, target: self, action: "goBackHome")
                    //self.chatController.navigationItem.leftBarButtonItem = backButton
                    
                    //STO INVERTENDO INDICE MESS IN MODO CHE ORDINE CRONOLOGICO! prova con jsonMess.endIndex o jsonMess.count
                    var revIndex = 0
                    for (index, value):(String, JSON) in jsonMess
                    {
                        revIndex = Int(index)!
                    }
                    
                    for (index, value):(String, JSON) in jsonMess
                    {
                        let i:Int = revIndex - Int(index)! //controlla che non perdo qualcosa
                        let text = jsonMess[i]["text"].string
                        
                        let timestamp = jsonMess[i]["createdAt"].string
                        
                        let user = jsonMess[i]["name"].string
                        
                        let userId = jsonMess[i]["from"].string
                        
                        let dateFormatter = NSDateFormatter()
                        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                        var date = dateFormatter.dateFromString(timestamp!)
                        
                        //vedi se risolve crash per date nil
                        if date == nil
                        {
                            print("DATA ERA NIL")
                            date = NSDate()
                        }
                        
                        
                        print("date: \(date)")
                        
                        //let mess:LGChatMessage!
                        
                        
                        print("username: \(username)")
                        
                        if user == String(username)
                        {
                            //mess = LGChatMessage(content: text!, sentBy: .User)
                            
                            self.addMessagesWithoutReload(userId!, sender: user!, date: date!, messageContent: text!)
                        }
                        else
                        {
                            //mess = LGChatMessage(content: "\(user!):\n\(text!)", sentBy: .Opponent)
                            self.addMessagesWithoutReload(userId!, sender: user!, date: date!, messageContent: text!)

                            
                        }
                        //self.chatController.messages.append(mess)
                        self.reloadMessagesView()
                        self.finishReceivingMessage() //scroll to latest mess, in futuro posso implementare to latest unread mess solo per owned o fav square?
                        
                    }
                    
                    //                    self.chatController.delegate = self
                    //                    self.navigationController?.pushViewController(self.chatController, animated: true)
                    //self.presentViewController(self.chatController, animated:true, completion:nil)
                    
                }
            case .Failure(let error):
                print(error)
                
                //                Answers.logCustomEventWithName("Error",
                //                    customAttributes: ["Error Debug Description": error.debugDescription])
            }
        }
        //END ADD ALREADY EXISTING MESS TO JSQ
    }
    
//    func jsqMessageFromSyncanoMessage(message: Message) -> JSQMessage {
//        let jsqMessage = JSQMessage(senderId: message.senderId, senderDisplayName: message.senderId, date: message.created_at, text: message.text)
//        return jsqMessage
//    }
//    
//    func jsqMessagesFromSyncanoMessages(messages: [Message]) -> [JSQMessage] {
//        var jsqMessages : [JSQMessage] = []
//        for message in messages {
//            jsqMessages.append(self.jsqMessageFromSyncanoMessage(message))
//        }
//        return jsqMessages
//    }
    
    
    
    
} //END EXTENSION SQMESSVC


