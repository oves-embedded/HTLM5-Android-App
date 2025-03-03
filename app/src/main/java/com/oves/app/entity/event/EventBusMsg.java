package com.oves.app.entity.event;


import com.oves.app.enums.EventBusEnum;

public class EventBusMsg<T> {
    private EventBusEnum tagEnum;
    private T t;

    public EventBusMsg() {
    }

    public EventBusMsg(EventBusEnum tagEnum, T t) {
        this.tagEnum = tagEnum;
        this.t = t;
    }

    public EventBusEnum getTagEnum() {
        return tagEnum;
    }

    public void setTagEnum(EventBusEnum tagEnum) {
        this.tagEnum = tagEnum;
    }

    public T getT() {
        return t;
    }

    public void setT(T t) {
        this.t = t;
    }
}
