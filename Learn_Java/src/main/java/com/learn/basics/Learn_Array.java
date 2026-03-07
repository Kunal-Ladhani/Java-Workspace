package com.learn.basics;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class Learn_Array {

    public static void main(String[] args) {
        // 1. Static Initialization (shorthand)
        int[] a1 = {1, 2, 3};

        // 2. Dynamic Initialization (default values)
        int[] a2 = new int[5];  // [0, 0, 0, 0, 0]

        // 3. Dynamic with values
        int[] a3 = new int[]{4, 5, 6};

        // 4. Loop-based Initialization
        int[] a4 = new int[5];
        for (int i = 0; i < a4.length; i++) {
            a4[i] = i * 2;
        }

        // 5. Arrays.fill()
        int[] a5 = new int[5];
        Arrays.fill(a5, 42);  // [42, 42, 42, 42, 42]

        // 6. Partial fill
        Arrays.fill(a5, 1, 4, 99);  // [42, 99, 99, 99, 42]

        // 7. Streams
        int[] a6 = IntStream.range(0, 5).toArray();             // [0, 1, 2, 3, 4]
        int[] a7 = IntStream.range(0, 5).map(i -> i * 10).toArray(); // [0, 10, 20, 30, 40]

        // 8. Cloning & Copying
        int[] original = {9, 8, 7};
        int[] clone = original.clone();
        int[] copied = Arrays.copyOf(original, original.length);

        // 9. Multi-dimensional
        int[][] matrix = {
            {1, 2},
            {3, 4}
        };

        int[][] jagged = new int[2][];
        jagged[0] = new int[]{1, 2};
        jagged[1] = new int[]{3};


        // ------------- array sorting -----------

        int[] arr = {3, 5, 1, 2, 0};

        System.out.println(Arrays.toString(arr)); // converts to string and returns

        Arrays.sort(arr);    // O(NlogN)
        System.out.println(Arrays.toString(arr));

        // ------------- binary search -----------
        int idx = Arrays.binarySearch(arr, 2);    // O(logN)
        System.out.println(idx);
        // binary search will work only in sorted collections, if not you need to sort first

        List<Integer> list = Arrays.asList(20, 10, 40, 39, 1);

        int x = Collections.frequency(list, 5);
        System.out.println(x);

        Collections.sort(list);
        System.out.println("Sorted list -> " + list);

        Collections.reverse(list);
        System.out.println("Reversed list -> " + list);

        // if you want to make list synchronized
        List<Integer> syncList = Collections.synchronizedList(list);
        System.out.println(syncList);

        Object ref = null;
        System.out.println(ref instanceof Object);

    }
}
