package com.learn.L2_Builder.v1;

public class Main {
    public static void main(String[] args) {
        // is Builder actually building the student object ??
        // no, not as of now, its just taking inputs and,
        // acting as a data structure and holding values for Student
        // it should be builder's responsibility to create Student object.

        Builder builder = Student.getBuilder();
        builder.setAge(21);
        builder.setName("Kunal");
        builder.setGradYear(2021);
        builder.setPsp(80);
        builder.setRollNo(29);
        builder.setUniversityName("BITS PILANI");

//        Student kunal = new Student(builder);
        Student kunal = builder.build();
        System.out.println(kunal);

//        Sequence is like this -
//        1. getBuilder() will create object of the Builder class
//        2. Pass the values to Builder Object - done by user/client
//        3. Data will get Validated in the Builder Class build() method
//        4. Student object will get created in Builder Class build() method
    }
}