package com.learn.L2_Builder.v0;

public class Main {
    public static void main(String[] args) {
        // client is responsible to create builder object
        // this part doesn't even inform that builder is for Student object
        // feels out of context
        Builder builder = new Builder();
        builder.setAge(21);
        builder.setName("Kunal");
        builder.setGradYear(2021);
        builder.setPsp(80);
        builder.setRollNo(29);
        builder.setUniversityName("BITS PILANI");
        // client is passing the builder object to Student
        // Student should be used to create Student object not builder(directly)
        // also client should not have to deal with builder directly

        Student kunal = new Student(builder);
        System.out.println(kunal.toString());
    }
}