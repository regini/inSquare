//
//  TabBarViewController.swift
//  inSquare
//
//  Created by Alessandro Steri on 01/03/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit

class TabBarViewController: UITabBarController
{

    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        tabBarHeight = self.tabBar.frame.height
        
        

        //start showing map/home
        self.selectedIndex = 2
        
        
        //usefull to set a counter that increments itself eachtime you press a tab, when all tab more than 10 press -> no label
        let tabItems = self.tabBar.items! as [UITabBarItem]
        
        //mapTab
        let mapTab = tabItems[2] as UITabBarItem
        mapTab.title = "Map"
        mapTab.image = insquareMapTab
        mapTab.selectedImage = insquareMapTabHighlightImage
        //make empty when unselected (solo contorno) e full quando selected


        
        //profileTab
        let profileTab = tabItems[0] as UITabBarItem
        profileTab.title = "Profile"
        
        
        //recents
        let recTab = tabItems[1] as UITabBarItem
        //recTab.badgeValue = "\(recBadge)"

        //appearance
        UITabBar.appearance().tintColor = inSquareUiColorQRed

        
        //custom center big button http://idevrecipes.com/2010/12/16/raised-center-tab-bar-button/
//        var button: UIButton = UIButton(type: .Custom)
//        button.frame = CGRectMake(0.0, 0.0, insquareMapTab.size.width, insquareMapTab.size.height)
//        button.setBackgroundImage(insquareMapTab, forState: .Normal)
//        button.setBackgroundImage(insquareMapTabHighlightImage, forState: .Highlighted)
//        var heightDifference: CGFloat = insquareMapTab.size.height - self.tabBar.frame.size.height
//        if heightDifference < 0 {
//            button.center = self.tabBar.center
//        }
//        else {
//            var center: CGPoint = self.tabBar.center
//            center.y = center.y - heightDifference / 2.0
//            button.center = center
//        }
//        self.view!.addSubview(button)
        


//        if !loggedIn {
//            print(2)
//            
//            let viewController:LoginViewController = UIStoryboard(name: "Main", bundle: nil).instantiateViewControllerWithIdentifier("LoginVcID") as! LoginViewController
//            UIApplication.sharedApplication().delegate!.window?!.rootViewController = viewController
//            
//            self.presentViewController(viewController, animated: false, completion: nil)
        
   //     }


        // Do any additional setup after loading the view.
    }//END VDL

    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(true)
            }
    
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }//END DRMW
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // MARK: - Login
    
    func showLoginIfNeededAnimated(animated : Bool, completion : (() -> Void)?) {
        
        // Show login if user is not logged-in, for example
        if !isLoggedInCoreData() && (serverId == "") {
        
            let viewController:LoginViewController = UIStoryboard(name: "Main", bundle: nil).instantiateViewControllerWithIdentifier("LoginVcID") as! LoginViewController
            self.presentViewController(viewController, animated: false, completion: nil)
            
                    }
    }

    
    
}//END TBVC
