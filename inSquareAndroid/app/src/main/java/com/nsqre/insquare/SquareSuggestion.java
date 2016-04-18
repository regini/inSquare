package com.nsqre.insquare;

/**
 * Created by Regini on 30/03/16.
 */


import android.os.Parcel;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.nsqre.insquare.Square.Square;

public class SquareSuggestion implements SearchSuggestion {

    private Square mSquare;

    private String mSquareName;

    private boolean mIsHistory;

    public SquareSuggestion(Square square){

        this.mSquare = square;
        this.mSquareName = mSquare.getName();
    }

    public SquareSuggestion(Parcel source) {
        this.mSquareName = source.readString();
    }

    public Square getSquare(){
        return mSquare;
    }

    @Override
    public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (getClass() != o.getClass()) {
                return false;
            }
           SquareSuggestion other = (SquareSuggestion) o;
            if (other.getSquare().getLat()==this.getSquare().getLat()){
                if (other.getSquare().getLon()==this.getSquare().getLon()){
                    return true;
                }
            }
        return false;
    }


    public void setIsHistory(boolean isHistory){
        this.mIsHistory = isHistory;
    }

    public boolean getIsHistory(){return this.mIsHistory;}

    @Override
    public String getBody() {
        return mSquare.getName();
    }

    @Override
    public Creator getCreator() {
        return CREATOR;
    }

    ///////

    public static final Creator<SquareSuggestion> CREATOR = new Creator<SquareSuggestion>() {
        @Override
        public SquareSuggestion createFromParcel(Parcel in) {
            return new SquareSuggestion(in);
        }

        @Override
        public SquareSuggestion[] newArray(int size) {
            return new SquareSuggestion[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSquareName);
    }
}