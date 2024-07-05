package com.learn.Prototype.v1;

import java.util.Arrays;

public class Main {

    private static String solver(String s) {
        int n = s.trim().length();
        if (n <= 1) return s;
        String[] words = s.split(" ");
        int k = words.length;
        StringBuilder sb = new StringBuilder();

        for (int i = k-1; i >= 0; i--) {
            String word = words[i].trim();
            if (!word.isEmpty()) {
                sb.append(word).append(' ');
            }
        }

        return sb.toString().trim();
    }

    private static String solver2(String s) {
        int n = s.trim().length();
        if (n <= 1) return s;

        char[] arr = s.toCharArray();
        System.out.println(Arrays.toString(arr));
        return "";
    }

    public static void main(String[] args) {
        String str = "dsa is not     hard";
        String ans = solver(str);
        System.out.println(ans);

        System.out.println(solver2(str));
    }
}
