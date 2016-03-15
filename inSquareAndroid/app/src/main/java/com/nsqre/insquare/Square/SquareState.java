package com.nsqre.insquare.Square;

/**
 * SquareState is the enumerator that represents the concept of State of a square. A square can be in an
 * asleep state if there is no activity, awoken state if there is a light activity, caffeinated state if there
 * is an intense activity.
 */
public enum SquareState {
    AWOKEN,
    ASLEEP,
    CAFFEINATED
}