package com.nsqre.insquare.Utilities;

/**
 * A basic Navigation Item class
 */
public class NavItem {
    private String mTitle;
    private String mSubtitle;
    private int mIcon;
    private int mNotificationCounter;

    public NavItem(String title, String subtitle, int icon, int notificationCounter) {
        mTitle = title;
        mSubtitle = subtitle;
        mIcon = icon;
        mNotificationCounter = notificationCounter;
    }

    public String getmTitle() {
        return mTitle;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getmSubtitle() {
        return mSubtitle;
    }

    public void setmSubtitle(String mSubtitle) {
        this.mSubtitle = mSubtitle;
    }

    public int getmIcon() {
        return mIcon;
    }

    public void setmIcon(int mIcon) {
        this.mIcon = mIcon;
    }

    public int getmNotificationCounter() {
        return mNotificationCounter;
    }

    public void setmNotificationCounter(int mNotificationCounter) {
        this.mNotificationCounter = mNotificationCounter;
    }
}