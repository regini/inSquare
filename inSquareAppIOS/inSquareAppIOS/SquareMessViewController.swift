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


class SquareMessViewController: JSQMessagesViewController
{
    var viewControllerNavigatedFrom:AnyObject?
    
    var squareId:String = String()
    var squareName:String = String()
    var squareLatitude:Double = Double()
    var squareLongitude:Double = Double()
    
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
    
    @IBOutlet var likeButtonOutlet: UIBarButtonItem!
    @IBAction func likeButton(sender: AnyObject)
    {
        
        request(.POST, "\(serverMainUrl)/favouritesquares", parameters:["userId": "\(serverId)", "squareId": "\(squareId)"]).validate().responseData { response in
            print("FAVOURITED \(response.request)")

            switch response.result {
            case .Success:
                if let value = response.result.value {
                    print("FAVOURITED \(value)")

                }
            case .Failure(let error):
                print(error)
                //cambia con analitics
                //                Answers.logCustomEventWithName("Error",
                //                    customAttributes: ["Error Debug Description": error.debugDescription])
            }
        }//ENDREQUEST
        likeButtonOutlet.image = likeFilled
        //updateFavouriteSquares()
    }
    //values passed by marker press in homeViewController, se fai segue vengono comunque sovrascritti
//    var squareId:String = "56bfa2cc8b4cc88bba9b32c2"
//    var squareName:String = "Comoleanno giulia"
//    var squareLatitude:Double = 41.888005
//    var squareLongitude:Double = 12.504009
    //ViewController that seugued to this VC


    

//check if pong work
    //data per pong
    var dataPong:JSON = ["beat": 1]

    //Client Socket.io Swift    localhost: http://127.0.0.1:3000
    let socket = SocketIOClient(socketURL: "\(serverMainUrl)", options: [.Log(true), .ForcePolling(true)])


    //JSQ Setting bubble
    let incomingBubble = JSQMessagesBubbleImageFactory().incomingMessagesBubbleImageWithColor(inSquareUiColorQRed)
    //    let incomingBubble = JSQMessagesBubbleImageFactory().incomingMessagesBubbleImageWithColor(UIColor(red: 10/255, green: 180/255, blue: 230/255, alpha: 1.0))
    let outgoingBubble = JSQMessagesBubbleImageFactory().outgoingMessagesBubbleImageWithColor(UIColor.lightGrayColor())
    var messages = [JSQMessage]()
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        updateFavouriteSquares()
        
        
        
        self.addHandlers()
        self.socket.connect()
        self.socket.joinNamespace("/squares")

        
        self.setup()
        //self.addDemoMessages()
        downloadExistingMessagesFromServer()
        //non funziona..capisci come aprire su ultimo mess
        //self.automaticallyScrollsToMostRecentMessage = true
    }//END VDL
    
    
    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(true)
        
        if isFavourite(squareId)
        {
            likeButtonOutlet.image = likeFilled
        }
        else
        {
            likeButtonOutlet.image = like
        }

      
        
        
    }//END VWA

    

    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()

    }//END DRMW
    
    
    override func prepareForSegue(segue: UIStoryboardSegue!, sender: AnyObject!)
    {
        if (segue.identifier == "backToHome") {
            //Checking identifier is crucial as there might be multiple
            // segues attached to same view
            //var detailVC = segue!.destinationViewController as! SquareMessViewController
            //var detailVC = segue!.destinationViewController as! NavigationToJSQViewController
            //            let detailVC = detailNC.topViewController as! inSquareViewController
            
            let navVC = segue.destinationViewController as! UITabBarController
            
            let detailVC = navVC.childViewControllers[2] as! MapViewController
            
            
            detailVC.backFromSquareLatitude = self.squareLatitude
            detailVC.backFromSquareLongitude = self.squareLongitude
            
            
        }
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
                print("Got event: \($0.event), with items: \($0.items)")
                
//                var tracker = GAI.sharedInstance().defaultTracker
//                tracker.send(GAIDictionaryBuilder.createEventWithCategory("Soket.onAny", action: "Event: \($0.event) \nItems: \($0.items)", label: "User \(serverId) got socket.onAny event", value: nil).build() as [NSObject : AnyObject])
                
        }
        
        self.socket.on("newMessage") {data, ack in
            
            let messJSN = JSON(data)
            print("Message for you! \(messJSN[0])")
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
                let dateFormatter = NSDateFormatter()
                dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                let date = dateFormatter.dateFromString("\(messJSN[0]["createdAt"].string)")
                print("DATA MESS INVIATO: \(date)")
                self.addMessage("\(messJSN[0]["userid"].string)", sender: "\(messJSN[0]["username"])", date: date!, messageContent: "\(messJSN[0]["contents"].string!)")
                //old chatt controller
                //let newMess = LGChatMessage(content: "\(messJSN[0]["username"]):\n\(messJSN[0]["contents"].string!)", sentBy: .Opponent)
                //self.chatController.messages.append(newMess)
                //self.chatController.addNewMessage(newMess)
                
                //self.chatController.reloadInputViews()
                //                self.shouldChatController(self.chatController, addMessage: newMess)
            }
            
            dispatch_async(dispatch_get_main_queue(), { () -> Void in
                //self.tableView.reloadData()
            })
            //             }
            
            //Analitycs; in realta basta monitorare gli inviati, i ricevuti non ha senso
//            var tracker = GAI.sharedInstance().defaultTracker
//            tracker.send(GAIDictionaryBuilder.createEventWithCategory("Soket.onNewMessage", action: "Mess: \(messJSN.rawValue)", label: "User \(serverId) got socket.onNewMessage event", value: nil).build() as [NSObject : AnyObject])
            
        }
        
        socket.on("ping")
            {
                data, ack in
                self.socket.emit("pong", self.dataPong.rawValue)
        }
        
        socket.on("tellRoom") { dati, ack in
        }
        
        
        socket.on("connect") {data, ack in
            
            var data:JSON = ["username": username, "room": self.squareId, "user" : serverId]
            print("Socket Connect with data: \(data.rawValue)")
            self.socket.emit("addUser", data.rawValue)
            
//            var tracker = GAI.sharedInstance().defaultTracker
//            tracker.send(GAIDictionaryBuilder.createEventWithCategory("Soket.onConnect", action: "Data: \(data.rawValue)", label: "User \(serverId) got socket.onConnect event", value: nil).build() as [NSObject : AnyObject])
            
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
        let mee = JSQMessage(senderId: senderId, senderDisplayName: sender, date: date, text: messageContent)
        print(message.date)
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
        let message = JSQMessage(senderId: senderId, senderDisplayName: senderDisplayName, date: date, text: text)
        print("SQMVC, DATE DEVE ESSERE DIVERSA DA NIL: \(date)")
        
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
        
        if "\(message.senderId)" == serverId
        {
            var data:JSON = ["username": username, "room": squareId, "userid" : serverId, "message" : message.text]
            socket.emit("sendMessage", data.rawValue)
            
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
            
            print("Got Message: \(message.text)")
            
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


