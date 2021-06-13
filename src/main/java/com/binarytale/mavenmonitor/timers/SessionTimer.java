package com.binarytale.mavenmonitor.timers;

import lombok.Getter;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SessionTimer extends AbstractTimer{
    @Getter
    private ConcurrentMap<String, ProjectTimer> projects;

    public SessionTimer() {
        this(new ConcurrentHashMap<String, ProjectTimer>());
    }

    public SessionTimer(ConcurrentMap<String, ProjectTimer> projects) {
        this.projects = projects;
    }

    public ProjectTimer getProject(MavenProject project) {
        return getProject(project.getGroupId(), project.getArtifactId());
    }

    private ProjectTimer getProject(String group, String projectArtifactId) {
        if (!projects.containsKey(projectArtifactId))
            projects.putIfAbsent(projectArtifactId, new ProjectTimer(group, projectArtifactId));

        return projects.get(projectArtifactId);
    }

    public void mojoStarted(MavenProject project, MojoExecution mojoExecution) {
        getProject(project).startTimerFor(mojoExecution);
    }

    public void mojoSucceeded(MavenProject project, MojoExecution mojoExecution) {
        getProject(project).stopTimerFor(mojoExecution);
    }

    public void mojoFailed(MavenProject project, MojoExecution mojoExecution) {
        getProject(project).stopTimerFor(mojoExecution);
    }

    public void projectStarted(MavenProject project) {
        getProject(project).start();
    }

    public void projectEnded(MavenProject project) {
        getProject(project).stop();
    }

}