//
//  MoreViewController.swift
//  inSquare
//
//  Created by Alessandro Steri on 09/03/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit

class MoreViewController: UIViewController
{
    @IBAction func logOutButton(sender: AnyObject)
    {
        loggedIn = false
        clearUsersSavedInCoreData()
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
