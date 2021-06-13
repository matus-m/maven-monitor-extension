package com.binarytale.mavenmonitor.timers;

import lombok.Getter;
import lombok.Setter;

public class AbstractTimer {
    @Getter
    @Setter
    private long startTime = System.currentTimeMillis();
    @Getter
    @Setter
    private long endTime = 0;

    public Long getDuration() {
        return endTime - startTime;
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
    }

    public void stop() {
        this.endTime = System.currentTimeMillis();
    }
}
