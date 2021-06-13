package com.binarytale.mavenmonitor;

import org.apache.maven.plugin.MojoExecution;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class Utils {
    public static String mojoExecutionId(MojoExecution mojoExecution) {
        return String.format("%s:%s:%s",
                mojoExecution.getArtifactId(),
                mojoExecution.getGoal(),
                mojoExecution.getExecutionId());
    }

    public static String prettyPrintMojo(MojoExecution mojoExecution) {
        return String.format("%s:%s (%s)",
                mojoExecution.getArtifactId(),
                mojoExecution.getGoal(),
                mojoExecution.getExecutionId());
    }

    public static OffsetDateTime dateFromEpoch(long millis) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }
}
