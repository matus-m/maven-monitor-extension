package com.binarytale.mavenmonitor.timers;

import lombok.Getter;
import org.apache.maven.plugin.MojoExecution;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.binarytale.mavenmonitor.Utils.mojoExecutionId;

public class ProjectTimer extends AbstractTimer{
    @Getter
    private final String projectName;

    @Getter
    private final String group;

    @Getter
    private Map<String, PhaseTimer> phaseTimers = new ConcurrentHashMap<>();

    public ProjectTimer(String group, String artifactId) {
        this.projectName = artifactId;
        this.group = group;
    }

    public void stopTimerFor(MojoExecution me) {
        getMojoTimer(me).stop();
    }

    public void startTimerFor(MojoExecution me) {
        getMojoTimer(me).start();
    }

    public MojoTimer getMojoTimer(MojoExecution me) {
        PhaseTimer phaseTimer = phaseTimers.computeIfAbsent(Optional.ofNullable(me.getLifecyclePhase()).orElse("noPhase"), (phase) -> new PhaseTimer(phase));
        return phaseTimer.getMojoTimers().computeIfAbsent(mojoExecutionId(me), s -> new MojoTimer(me));
    }
}