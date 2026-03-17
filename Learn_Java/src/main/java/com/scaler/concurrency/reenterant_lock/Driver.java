package com.scaler.concurrency.reenterant_lock;

public class Driver {

    public static void main(String[] args) {
//        multipleSharedResources();
        singleSharedResources();
    }

    private static void singleSharedResources() {
        SharedResource sharedResource = new SharedResource();

        Thread oggy = new Thread(sharedResource::produce, "oggy");
        Thread jack = new Thread(sharedResource::produce, "jack");

        oggy.start();
        jack.start();
    }


    private static void multipleSharedResources() {
        SharedResource sharedResource1 = new SharedResource();
        SharedResource sharedResource2 = new SharedResource();

        Thread oggy = new Thread(sharedResource1::produce, "oggy");
        Thread jack = new Thread(sharedResource2::produce, "jack");

        oggy.start();
        jack.start();
    }

}
