package com.notifyflow.coreprocessor.exception;

public class CustomerProfileNotFoundException extends RuntimeException{

    public CustomerProfileNotFoundException(String customerId){
        super("Customer profile not found for customerId: "+customerId);
    }
}
