//
//  inSquareViewController.swift
//  inSquareAppIOS
//
//  Created by Alessandro Steri on 13/02/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import Alamofire
import Crashlytics


//contenuto dei messaggi, prendi poi da server
//var cellContent = [String]()

//var now = NSDate()
//var formatter = NSDateFormatter()
//formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
//formatter.timeZone = NSTimeZone(forSecondsFromGMT: 0)
//print(formatter.stringFromDate(now))

class inSquareViewController: UIViewController, LGChatControllerDelegate
{
    
    //values passed by marker press in homeViewController
    var squareId:String = String()
    var squareName:String = String()
    var squareLatitude:Double = Double()
    var squareLongitude:Double = Double()
 


    
    //data per pong
    var dataPong:JSON = ["beat": 1]

    
    let chatController = LGChatController()
    //    /messages?recent=true&size=<quanti ne vuoi>&square=<id della square>
    
    //Client Socket.io Swift    localhost: http://127.0.0.1:3000
    let socket = SocketIOClient(socketURL: "http://recapp-insquare.rhcloud.com", options: [.Log(true), .ForcePolling(true)])
    
    //VDL
    override func viewDidLoad()
    {
        super.viewDidLoad()
        self.navigationController?.setNavigationBarHidden(false, animated: true)
        self.navigationController?.title = ""
        
        
        
        print(squareId)
        print(squareName)
        print(squareLatitude)
        print(squareLongitude)
        
        self.addHandlers()
        self.socket.connect()
        self.socket.joinNamespace("/squares")
        
        self.launchChatController()
        
    }//END VDL
    
    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(true)
        print("CHATCONTROLLER\(self.chatController)")
    }
    

//        func stylizeChatInput() {
//            LGChatInput.Appearance.backgroundColor = UIColor.blackColor()
////            LGChatInput.Appearance.includeBlur = true
////            LGChatInput.Appearance.textViewFont = <#UIFont#>
////            LGChatInput.Appearance.textViewTextColor = <#UIColor#>
////            LGChatInput.Appearance.tintColor = <#UIColor#>
////            LGChatInput.Appearance.textViewBackgroundColor = <#UIColor#>
//        }
    
//        func stylizeMessageCell() {
////            LGChatMessageCell.Appearance.font = <#UIFont#>
//            LGChatMessageCell.Appearance.opponentColor = inSquareUiColorQRed
////            LGChatMessageCell.Appearance.userColor = <#UIColor#>
//        }

    
    // MARK: Launch Chat Controller
    func launchChatController()
    {
//        //let chatController = LGChatController()
//        chatController.opponentImage = UIImage(named: "User")
//        chatController.title = "#\(squareName)"
//        let helloWorld = LGChatMessage(content: "Hello World!", sentBy: .User)
//        chatController.messages = [helloWorld]
//        let helloWorld2 = LGChatMessage(content: "Hello World!", sentBy: .Opponent)
//        chatController.messages.append(helloWorld2)
//        chatController.delegate = self
//        self.navigationController?.pushViewController(chatController, animated: true)
        
        request(.GET, "http://recapp-insquare.rhcloud.com/messages", parameters:["recent": "true", "size": "100", "square": squareId]).validate().responseJSON { response in
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    var jsonMess = JSON(value)
                    print("MESS:")
                    print(jsonMess)
                    
                    self.chatController.opponentImage = UIImage(named: "User")
                    self.chatController.title = "#\(self.squareName)"
                    //self.chatController.navigationItem.title = ""
                    var backButton = UIBarButtonItem(title: "Home", style: UIBarButtonItemStyle.Bordered, target: self, action: "goBackHome")
                    self.chatController.navigationItem.leftBarButtonItem = backButton
                    //prova con jsonMess.endIndex o jsonMess.count
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

                        let mess:LGChatMessage!
                        
                
                        print("username: \(username)")
                        
                        if user == String(username)
                        {
                            
//                            mess = LGChatMessage(content: "\(user!):\n\(text!)", sentBy: .User)
                            mess = LGChatMessage(content: text!, sentBy: .User)


                        }
                        else
                        {
                            mess = LGChatMessage(content: "\(user!):\n\(text!)", sentBy: .Opponent)

                        }
                        self.chatController.messages.append(mess)

                    }
                    
                    self.chatController.delegate = self
                    self.navigationController?.pushViewController(self.chatController, animated: true)
                    //self.presentViewController(self.chatController, animated:true, completion:nil)
                    
                }
            case .Failure(let error):
                print(error)
                
                Answers.logCustomEventWithName("Error",
                    customAttributes: ["Error Debug Description": error.debugDescription])
            }
        }

        
        
    }

    // MARK: LGChatControllerDelegate
    func chatController(chatController: LGChatController, didAddNewMessage message: LGChatMessage)
    {
        print("Did Add Message: \(message.content)")
        
    }
    
    func shouldChatController(chatController: LGChatController, addMessage message: LGChatMessage) -> Bool {
        /*
        Use this space to prevent sending a message, or to alter a message.  For example, you might want to hold a message until its successfully uploaded to a server.
        */
        if "\(message.sentByString)" == LGChatMessage.SentByUserString()
        {
            var data:JSON = ["username": username, "room": squareId, "userid" : serverId, "message" : message.content]
            socket.emit("sendMessage", data.rawValue)
            
            //answer custom event
            Answers.logCustomEventWithName("sendMessage",
                customAttributes: ["username": username, "room": squareId, "userid" : serverId, "message" : message.content])
            
            //Analitycs
            var tracker = GAI.sharedInstance().defaultTracker
            tracker.send(GAIDictionaryBuilder.createEventWithCategory("Message Sended", action: "Mess: \(message.content)", label: "User \(serverId) sended a message", value: nil).build() as [NSObject : AnyObject])
            
            print("Message Sended: \(message.content)")
            
        }
        else if "\(message.sentByString)" == LGChatMessage.SentByUserString()
        {
            //visualizza e basta
            
            print("Got Message: \(message.content)")

        }
        return true
    }
    
    
    
    //DRMW
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }//END DRMW
    
    
    
    
    func addHandlers()
    {
        
        self.socket.onAny
            {
                print("Got event: \($0.event), with items: \($0.items)")
                
                var tracker = GAI.sharedInstance().defaultTracker
                tracker.send(GAIDictionaryBuilder.createEventWithCategory("Soket.onAny", action: "Event: \($0.event) \nItems: \($0.items)", label: "User \(serverId) got socket.onAny event", value: nil).build() as [NSObject : AnyObject])

        }
        
        self.socket.on("newMessage") {data, ack in

            let messJSN = JSON(data)
            print("Message for you! \(messJSN[0])")
            if messJSN[0] == "Server"
            {
                print("Server Mess" + messJSN[1].string!)
                //cellContent.append(messJSN[0]["message"].string!)
            }
            else if messJSN[0]["userid"].string == "0"
            {
                print("userid")
                //cellContent.append(messJSN[0]["message"].string!)
                
            }
            else if messJSN[0]["contents"] != nil
            {
                let newMess = LGChatMessage(content: "\(messJSN[0]["username"]):\n\(messJSN[0]["contents"].string!)", sentBy: .Opponent)
                //self.chatController.messages.append(newMess)
                self.chatController.addNewMessage(newMess)
                
                
                //self.chatController.reloadInputViews()
//                self.shouldChatController(self.chatController, addMessage: newMess)
                //cellContent.append(messJSN[0]["contents"].string!)
            }
            
            dispatch_async(dispatch_get_main_queue(), { () -> Void in
                //self.tableView.reloadData()
            })
            //             }
        
            //Analitycs; in realta basta monitorare gli inviati, i ricevuti non ha senso
            var tracker = GAI.sharedInstance().defaultTracker
            tracker.send(GAIDictionaryBuilder.createEventWithCategory("Soket.onNewMessage", action: "Mess: \(messJSN.rawValue)", label: "User \(serverId) got socket.onNewMessage event", value: nil).build() as [NSObject : AnyObject])
        
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
            
            var tracker = GAI.sharedInstance().defaultTracker
            tracker.send(GAIDictionaryBuilder.createEventWithCategory("Soket.onConnect", action: "Data: \(data.rawValue)", label: "User \(serverId) got socket.onConnect event", value: nil).build() as [NSObject : AnyObject])
            
        }
        
        //            socket.on("newMessage") {data, ack in
        //                print("Message for you! \(data[0])")
        //            }
        
    }

    
    func goBackHome(){
        
        //Initiate newViewController this way
        let homeViewC = self.storyboard?.instantiateViewControllerWithIdentifier("HomeViewController") as! HomeViewController
        
        self.navigationController?.pushViewController(homeViewC, animated: true)
    }

    
    

}//ENDVIEWCONTROLLER
