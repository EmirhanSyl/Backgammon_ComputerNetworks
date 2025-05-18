package com.blackflower.backgammon_computernetworks.model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 *
 * @author emirs
 */
public final class Point {

    private final int index;              // 1-24 (0 = bar, 25 = bear-off)
    private final Deque<Checker> stack = new ArrayDeque<>();

    public Point(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public int size() {
        return stack.size();
    }

    public Checker peek() {
        return stack.peek();
    }

    public void push(Checker c) {
        stack.push(c);
    }

    public Checker pop() {
        return stack.pop();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
