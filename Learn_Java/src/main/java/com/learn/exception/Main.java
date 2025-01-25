package com.learn.exception;


class Temp {
    public static int divide(int a, int b) {
        try {
            int ans =  a/b;
            System.out.println(ans);
            System.exit(1);
            return ans;
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            return Integer.MIN_VALUE;
        }
    }
}

public class Main {
    public static void main(String[] args) {
        int ans = Temp.divide(5, 0);
        System.out.println("ans = "  + ans);
    }   
}
