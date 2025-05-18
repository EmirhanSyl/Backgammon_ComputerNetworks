package com.blackflower.backgammon_computernetworks.model;

import java.util.Random;

/**
 *
 * @author emirs
 */
public class Dice {

    private Random random = new Random();

    private int value;

    public void roll() {
        value = random.nextInt(1, 7);
    }

    public int get() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
