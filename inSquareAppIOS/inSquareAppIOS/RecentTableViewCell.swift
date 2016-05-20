//
//  RecentTableViewCell.swift
//  inSquare
//
//  Created by Alessandro Steri on 06/03/16.
//  Copyright © 2016 Alessandro Steri. All rights reserved.
//

import UIKit

class RecentTableViewCell: UITableViewCell {

    @IBOutlet var squareImg: UIImageView!
    @IBOutlet var squareName: UILabel!
    @IBOutlet var squareActive: UILabel!
    
    var tapped: ((RecentTableViewCell) -> Void)?
    
    
    @IBAction func goInSquare(sender: AnyObject)
    {
        tapped?(self)
        
        print("BUTTON \(sender)")
        
    }
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }
    
    override func setSelected(selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
        
        // Configure the view for the selected state
    }

}
