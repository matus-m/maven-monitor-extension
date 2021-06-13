package com.binarytale.mavenmonitor.api;

import com.binarytale.mavenmonitor.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.binarytale.mavenmonitor.timers.MojoTimer;
import com.binarytale.mavenmonitor.timers.PhaseTimer;
import com.binarytale.mavenmonitor.timers.ProjectTimer;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
public class ProjectReport {
    private String name;
    private String group;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private long duration;
    private String client;
    private String commandLine;
    private String reportVersion = "1";
    private String mavenVersion; //maven.version
    private String os;// os.name
    private String java;
    private String goals;
    private String profiles;
    private boolean cicd;
    private String status;
    private long downloadTimeTotal;
    private long downloadedArtifacts;
    private List<PhaseReport> phases = new ArrayList<>();

    public ProjectReport fillWithTimerInfo(ProjectTimer timer) {
        this.setStartTime(Utils.dateFromEpoch(timer.getStartTime()));
        this.setEndTime(Utils.dateFromEpoch(timer.getEndTime()));
        this.setDuration(timer.getDuration());
        this.setName(timer.getProjectName());

        timer.getPhaseTimers().values().stream().forEach(phaseTimer -> {
            long phaseStart = Long.MAX_VALUE;
            long phaseEnd = Long.MIN_VALUE;
            List<MojoTimer> mojoTimersSorted =
                    phaseTimer.getMojoTimers().values().stream().sorted(Comparator.comparingLong(MojoTimer::getStartTime)).collect(Collectors.toList());
            for (MojoTimer mojoTimer : mojoTimersSorted) {
                phaseStart = mojoTimer.getStartTime() < phaseStart ? mojoTimer.getStartTime() : phaseStart;
                phaseEnd = mojoTimer.getEndTime() > phaseEnd ? mojoTimer.getEndTime() : phaseEnd;
            }
            phaseTimer.setEndTime(phaseEnd);
            phaseTimer.setStartTime(phaseStart);

        });

        this.setPhases(timer.getPhaseTimers().values().stream().sorted(Comparator.comparingLong(PhaseTimer::getStartTime)).map(phaseTimer -> {
            PhaseReport phaseReport = new PhaseReport();
            phaseReport.setName(phaseTimer.getName());
            phaseReport.setDuration(phaseTimer.getDuration());
            phaseReport.setStartTime(Utils.dateFromEpoch(phaseTimer.getStartTime()));
            phaseReport.setEndTime(Utils.dateFromEpoch(phaseTimer.getEndTime()));
            phaseReport.setPlugins(phaseTimer.getMojoTimers().values().stream()
                    .collect(Collectors.groupingBy(mojoTimer -> mojoTimer.getMojoExecution().getGroupId() + ":" + mojoTimer.getMojoExecution().getArtifactId()))
                    .entrySet().stream()
                    .map(pluginMapEntry -> {
                        PluginReport pluginReport = new PluginReport();
                        pluginReport.setName(pluginMapEntry.getKey());
                        long pluginStartTime  = Long.MAX_VALUE;
                        long pluginEndTime = Long.MIN_VALUE;
                        for (MojoTimer mojoTimer : pluginMapEntry.getValue()) {
                            ExecutionReport executionReport = new ExecutionReport();
                            executionReport.setDuration(mojoTimer.getDuration());
                            executionReport.setEndTime(Utils.dateFromEpoch(mojoTimer.getEndTime()));
                            executionReport.setStartTime(Utils.dateFromEpoch(mojoTimer.getStartTime()));
                            executionReport.setName(mojoTimer.getMojoExecution().getExecutionId());
                            if (pluginStartTime > mojoTimer.getStartTime()) {
                                pluginStartTime = mojoTimer.getStartTime();
                            }
                            if (pluginEndTime < mojoTimer.getEndTime()) {
                                pluginEndTime = mojoTimer.getEndTime();
                            }
                            pluginReport.getExecutions().add(executionReport);
                        }
                        pluginReport.setStartTime(Utils.dateFromEpoch(pluginStartTime));
                        pluginReport.setEndTime(Utils.dateFromEpoch(pluginEndTime));
                        pluginReport.setDuration(pluginEndTime - pluginStartTime);
                        return pluginReport;
                    })
                    .collect(Collectors.toList()));
            return phaseReport;
        }).collect(Collectors.toList()));

        return this;
    }

}
