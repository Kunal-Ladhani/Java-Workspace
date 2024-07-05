package com.learn.Builder.v2;

// this is v2 of the builder design pattern
public class Student {
    int studentId;
    String name;
    int rollNo;
    double psp;
    int age;
    String universityName;
    int gradYear;

    // inner class because Student() constructor was to be made private
    // as we don't want anyone to use its constructor

    // because getBuilder() is static, it can only access other static methods.
    // so we make nested inner class Builder as static

    public static class Builder {
        int studentId;
        String name;
        int rollNo;
        double psp;
        int age;
        String universityName;
        int gradYear;

        private Builder() {
            super();
        }

        public int getStudentId() {
            return studentId;
        }

        public String getName() {
            return name;
        }

        public int getRollNo() {
            return rollNo;
        }

        public double getPsp() {
            return psp;
        }

        public int getAge() {
            return age;
        }

        public String getUniversityName() {
            return universityName;
        }

        public int getGradYear() {
            return gradYear;
        }

        public Builder setStudentId(int studentId) {
            this.studentId = studentId;
            return this;
        }
        // setAge() needs Builder object
        // in fact, all the setters work for Builder class object,
        // so we need to return a Builder object from all the setters
        public Builder setName(String name) {
            this.name = name;
            return this;
        }
        public Builder setRollNo(int rollNo) {
            this.rollNo = rollNo;
            return this;
        }
        public Builder setPsp(double psp) {
            this.psp = psp;
            return this;
        }
        public Builder setAge(int age) {
            this.age = age;
            return this;
        }
        public Builder setUniversityName(String universityName) {
            this.universityName = universityName;
            return this;
        }
        public Builder setGradYear(int gradYear) {
            this.gradYear = gradYear;
            return this;
        }
        // build the Student class object in builder only
        // do the validation here only!
        // we have removed the validations even out of the constructor also
        // and put it into build() of Builder
        // so we are not calling the Student constructor until we are done with validations

        public Student build() {
            // do all the validations here.
            // validation starts ....
            if(age > 25) {
                throw new IllegalArgumentException();
            }
            // validation ends .....

            // means, now Builder class build() will create the Student object
            // Student constructor needs a builder object
            // that has the data values,
            // so we pass 'this'
            return new Student(this);
        }
    }

    private Student(Builder builder) {
        // validation starts ....
//        if(builder.getAge() > 25) {
//            throw new IllegalArgumentException();
//        }
        // validation ends .....
        this.studentId = builder.getStudentId();
        this.name = builder.getName();
        this.rollNo = builder.getRollNo();
        this.psp = builder.getPsp();
        this.age = builder.getAge();
        this.gradYear = builder.getGradYear();
        this.universityName = builder.getUniversityName();
    }

    // this breaks Single Resp Principle
    // getter method for builder class object
    // static is used because we don't want to associate it to an object,
    // we want to associate it to Student class, so we can directly create builder with class name only.
    public static Builder getBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "Student{" +
                "studentId=" + studentId +
                ", name='" + name + '\'' +
                ", rollNo=" + rollNo +
                ", psp=" + psp +
                ", age=" + age +
                ", universityName='" + universityName + '\'' +
                ", gradYear=" + gradYear +
                '}';
    }
}
