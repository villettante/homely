package com.example.homely;

import java.io.Serializable;

import lombok.Getter;

public class Home implements Serializable {
    @Getter
    private String id;
    @Getter
    private String name;
    private boolean isCurrent;

    public Home() {}

    public Home(String id, String name, boolean isCurrent) {
        this.id = id;
        this.name = name;
        this.isCurrent = isCurrent;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }
}

