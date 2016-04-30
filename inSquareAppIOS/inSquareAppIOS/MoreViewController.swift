//
//  MoreViewController.swift
//  inSquare
//
//  Created by Alessandro Steri on 09/03/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit
import Alamofire

class MoreViewController: UIViewController
{
    @IBAction func logOutButton(sender: AnyObject)
    {
        //loggedIn = false
        //clearUsersSavedInCoreData()
    }

    @IBAction func sendFeedback(sender: AnyObject)
    {
        print("send feedback")
        //1. Create the alert controller.
        var alert = UIAlertController(title: "Send Feedback", message: "Enter a Feedback to inSquare", preferredStyle: .Alert)
        //2. Add the text field. You can configure it however you need.
        alert.addTextFieldWithConfigurationHandler({ (textField) -> Void in
            textField.text = ""
        })
        //3. Grab the value from the text field, and print it when the user clicks OK.
        alert.addAction(UIAlertAction(title: "OK", style: .Default, handler: { (action) -> Void in
            let textField = alert.textFields![0] as UITextField
            if (textField.text == "")
            {
                print("Feedback not sended.")
                return
            }
            else
            {
                let feedbackText = "\(textField.text)"
                
                var jsnResult = JSON(data: NSData())
                print("TEXT: \(textField.text)")
                
                var urlPostSquare = "\(serverMainUrl)/feedback?feedback=\(textField.text!)&username=\(username)&activity=moreTab"
                urlPostSquare = urlPostSquare.stringByReplacingOccurrencesOfString(" ", withString: "%20")
                
                print("REQUEST URL: \(urlPostSquare)")
                
                var dataForBody:JSON = ["feedback": textField.text!, "username": username, "activity" : "moreTab"]
                
                print(dataForBody)
                
                request(.POST, urlPostSquare, parameters: ["feedback": textField.text!, "username": serverId, "activity" : "moreTab"]).validate().responseString { response in
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
                            tracker.send(GAIDictionaryBuilder.createEventWithCategory("Sended Feedback", action: "Sender: \(username), Feedback: \(textField.text!)", label: "User \(serverId) dended Feedback", value: nil).build() as [NSObject : AnyObject])
                            
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
    
    
    override func viewDidLoad()
    {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }//END VDL

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }//ENDDRMW
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

}//END CV
