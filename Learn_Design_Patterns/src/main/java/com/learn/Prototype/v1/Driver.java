package com.learn.Prototype.v1;

import java.util.Arrays;

public class Driver {

    private static char[] solverAlt(char[] s) {
        if (s.length == 0) {
            return s;
        }
        int[] freq = new int['z'-'a'+1];

        for (char value : s) {
            freq[value - 'a']++;
        }
        System.out.println(Arrays.toString(freq));

        int idx = 0;
        for(char ch='a'; ch <= 'z' && idx < s.length; ch++) {
            int count = freq[ch-'a'];
            for(int j=0; j<count; j++) {
                s[idx++] = ch;
            }
        }
        return s;
    }

    private static char[] solver(char[] s) {
        if (s.length == 0) {
            return s;
        }
        int[] arr = new int[26];

        for (int i = 0; i < s.length; i++) {
            arr[s[i] - 'a']++;
        }
        System.out.println(Arrays.toString(arr));

        int i=0, k=0;
        while (i < 26 && k < s.length) {
            int count = arr[i];
            char c = (char)('a' + i);
            while(count-- > 0) {
                s[k++] = c;
            }
            i++;
        }
        return s;
    }

    private static char convertLowerToUpper(char c) {
        if (c >= 97 && c <= 122) {
            return (char) (c - 32);
        }
        return '0';
    }

    private static char convertUpperToLower(char c) {
        if (c >= 65 && c <= 90) {
            return (char) (c ^ (1 << 5));
        }
        return '0';
    }

    private static boolean checkLowercase(char c) {
        return (c >= 97 && c <= 122);
    }

    public static void main(String[] args) {
        char c1 = 'a';
        c1++;
        System.out.println(c1);

        char c2 = 'b';
        if (c1 > c2) {
            System.out.println('1');
        } else {
            System.out.println("2");
        }

        System.out.println(checkLowercase('a'));
        System.out.println(convertLowerToUpper('a'));
        System.out.println((int) 'Z');
        System.out.println(convertUpperToLower('Z'));

        char[] s = {'d', 'a', 'b', 'a', 'c', 'd', 'b'};

        char[] ans = solver(s);
        System.out.println(ans);

        char[] ans1 = solverAlt("dabacdb".toCharArray());
        System.out.println(ans1);

        Arrays.sort(s);
        System.out.println(s);
    }
}
