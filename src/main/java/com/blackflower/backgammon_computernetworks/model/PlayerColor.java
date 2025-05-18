package com.blackflower.backgammon_computernetworks.model;

/**
 *
 * @author emirs
 */
public enum PlayerColor {
    WHITE( 1,  6, 24),   
    BLACK(-1, 19, 1);

    public final int direction;      
    public final int homeStart;
    public final int bearOffPoint;

    PlayerColor(int direction, int homeStart, int bearOffPoint) {
        this.direction   = direction;
        this.homeStart   = homeStart;
        this.bearOffPoint = bearOffPoint;
    }
}
