package com.binarytale.mavenmonitor.api;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PhaseReport {
    private String name;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private long duration;
    private List<PluginReport> plugins = new ArrayList<>();

}
