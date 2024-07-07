package com.learn.L2_Builder.v2;

public class Main {
    public static void main(String[] args) {

//        Builder builder = Student.getBuilder();
//        builder.setAge(21);
//        builder.setName("Kunal");
//        builder.setGradYear(2021);
//        builder.setPsp(80);
//        builder.setRollNo(29);
//        builder.setUniversityName("BITS PILANI");

        // now it feels like everything is being done by Student class
        // until you open getBuilder() method you won't even know that a Builder class is being used
        Student kunal = Student.getBuilder()
                .setStudentId(1)
                .setAge(21)
                .setName("Kunal")
                .setGradYear(2021)
                .setPsp(80)
                .setRollNo(29)
                .setUniversityName("BITS PILANI")
                .build();

//        Student kunal = new Student(builder);
//        Student kunal = builder.build();
        // no one is stopping you to write above code!
        // so we should make the constructors private
        // but, you need to call Student class constructor
        // only someone internal only can access it
        // so, we need to put (nest) Builder class inside Student class

        System.out.println(kunal);

    }
}