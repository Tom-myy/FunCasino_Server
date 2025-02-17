package com.example.demoSpringInitializrForEvoBJ.myPackage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyPackage<T>  {

    private EMessageType messageType;
    private T message;

    public MyPackage() {}
    public MyPackage(T message, EMessageType messageType) {
        this.message = message;
        this.messageType = messageType;
    }
}
