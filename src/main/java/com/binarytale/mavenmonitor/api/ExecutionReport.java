package com.binarytale.mavenmonitor.api;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ExecutionReport {
    private String name;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private long duration;
}
