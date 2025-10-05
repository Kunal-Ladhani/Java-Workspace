package com.learn.collection;

import java.util.ArrayDeque;
import java.util.Deque;

public class LearnDeque {

    public static void main(String[] args) {

        Deque<Integer> deque = new ArrayDeque<>();

        deque.add(1);
        deque.add(2);
        deque.add(3);
        deque.add(4);

        for (int x : deque) {
            System.out.println(x);
        }

    }

}
