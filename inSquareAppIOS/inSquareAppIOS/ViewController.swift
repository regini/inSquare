//
//  ViewController.swift
//  inSquareAppIOS
//
//  Created by Alessandro Steri on 21/01/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import FBSDKCoreKit
import FBSDKLoginKit
import Crashlytics


var cellContent = [String]()
//var notInNameSpace = true




class ViewController: UIViewController, UITableViewDelegate
{
    //values passed by marker press in homeViewController
    var squareId:String = String()
    var squareName:String = String()
    var squareLatitude:Double = Double()
    var squareLongitude:Double = Double()
    
    @IBOutlet weak var textKeyboard: NSLayoutConstraint!
    @IBOutlet var tableView: UITableView!
    
    @IBOutlet weak var squareLabel: UINavigationItem!
    
    
    //Client Socket.io Swift    localhost: http://127.0.0.1:3000
    let socket = SocketIOClient(socketURL: "http://recapp-insquare.rhcloud.com", options: [.Log(true), .ForcePolling(true)])

    
    
    //ButtonPressed: SendMessage
    @IBAction func sendMessage(sender: AnyObject)
    {
        var testo = text.text
        var data:JSON = ["username": username, "room": squareId, "userid" : serverId, "message" : testo!]
        //socket.emit("sendMessage", ["room":"SapienzaDiag", "user":"AlessndroSteri"])
        //socket.emit("chat message", ""+testo!)  //locale
        socket.emit("sendMessage", data.rawValue) //server
        
        //answer custom event 
        Answers.logCustomEventWithName("sendMessage",
            customAttributes: ["username": username, "room": squareId, "userid" : serverId, "message" : testo!])
        
        print("Message Sended")
//check if message correctly sent before clean textfield
//also check if socket is connected, if not make the send button grey and unclicable
        //hardcoded mio messin tableview
        cellContent.append(testo!)
        dispatch_async(dispatch_get_main_queue(), { () -> Void in
            self.tableView.reloadData()})
        text.text=""


    }
    
    @IBOutlet var text: UITextField!

    
    
    //viewDidLoad
    override func viewDidLoad()
    {
        super.viewDidLoad()

        print(squareId)
        print(squareName)
        print(squareLatitude)
        print(squareLongitude)

        self.addHandlers()
        self.socket.connect()
        self.socket.joinNamespace("/squares")

        squareLabel.title = "#\(squareName)"
        
        //text & Keyboard
        NSNotificationCenter.defaultCenter().addObserver(self, selector: "keyboardWillShow:", name: UIKeyboardWillShowNotification, object: nil)
        textKeyboard.constant = 10.0
        //END:text & Keyboard
        
    }//END viewDidLoad
   
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int
    {
        return cellContent.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell
    {
        let cell = UITableViewCell(style: UITableViewCellStyle.Default, reuseIdentifier: "Cell")
        cell.textLabel?.text = cellContent[indexPath.row]
        return cell
    }

     //didRecieveMemoryWarning
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }//ENDdidRecieveMemoryWarning
    
    
        func addHandlers()
        {
            
            self.socket.onAny
            {
                print("Got event: \($0.event), with items: \($0.items)")
            }
            
            
//            self.socket.on("sendMessage") {data, ack in
//                
//                print("Message from you: \(data[0])")
//                //usa data[0] per sender e fai prototipo mess mio e altri o connetti a chat bubble
//                cellContent.append(data[1] as! String)
//                dispatch_async(dispatch_get_main_queue(), { () -> Void in
//                    self.tableView.reloadData()})
//            }

            
            
            self.socket.on("newMessage") {data, ack in
//                let mess = data[0]
//                if mess as! String == "Server"
//                {
//                    cellContent.append(data[1] as! String)
//                    dispatch_async(dispatch_get_main_queue(), { () -> Void in
//                        self.tableView.reloadData()})
//                }
//                else
//                {
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
                    cellContent.append(messJSN[0]["contents"].string!)
                }
                
                dispatch_async(dispatch_get_main_queue(), { () -> Void in
                    self.tableView.reloadData()})
//                }
            }

            socket.on("tellRoom") { dati, ack in
            }
            
            
            socket.on("connect") {data, ack in

                var data:JSON = ["username": username, "room": self.squareId, "user" : serverId]
                print("Socket Connect with data: \(data.rawValue)")
                self.socket.emit("addUser", data.rawValue)


            }
            
//            socket.on("newMessage") {data, ack in
//                print("Message for you! \(data[0])")
//            }
            
        }
    
    //KEY
    func keyboardWillShow(sender: NSNotification) {
        if let userInfo = sender.userInfo {
            if let keyboardHeight = userInfo[UIKeyboardFrameEndUserInfoKey]?.CGRectValue.size.height {
                textKeyboard.constant = keyboardHeight
                UIView.animateWithDuration(0.25, animations: { () -> Void in
                    self.view.layoutIfNeeded()
                })
            }
        }
    }//KEY

    
}//ViewController


