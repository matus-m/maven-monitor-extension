package com.binarytale.mavenmonitor.timers;

import lombok.Getter;
import org.apache.maven.plugin.MojoExecution;

public class MojoTimer extends  AbstractTimer{

    @Getter
    private MojoExecution mojoExecution;

    public MojoTimer(MojoExecution mojoExecution) {
        this.mojoExecution = mojoExecution;
    }
}