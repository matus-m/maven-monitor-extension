package com.binarytale.mavenmonitor.timers;

import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Getter
public class PhaseTimer extends AbstractTimer {

    @Getter
    private String name;
    private ConcurrentMap<String, MojoTimer> mojoTimers = new ConcurrentHashMap<>();

    public PhaseTimer(String name) {
        this.name = name;
    }

}
