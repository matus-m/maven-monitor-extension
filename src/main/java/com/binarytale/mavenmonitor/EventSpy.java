package com.binarytale.mavenmonitor;

import com.alibaba.fastjson.JSON;
import com.binarytale.mavenmonitor.api.PluginReport;
import com.binarytale.mavenmonitor.api.ProjectReport;
import com.binarytale.mavenmonitor.timers.ArtifactTimer;
import com.binarytale.mavenmonitor.timers.ProjectTimer;
import com.binarytale.mavenmonitor.timers.SessionTimer;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Named
@Singleton
public class EventSpy extends AbstractEventSpy {

    public static final String MVN_MONITOR_ID_FILE = ".mvn-monitor-id";
    private static final Logger logger = LoggerFactory.getLogger(EventSpy.class);
    private static final String DEFAULT_IGNORED_GOALS = "quarkus:dev";
    private static final String REPORT_API_PREFIX = "/api/projectReports/";
    private SessionTimer session = new SessionTimer();

    private Map<String, ArtifactTimer> artifactTimerMap = new HashMap<>();

    private ProjectReport report = new ProjectReport();

    private boolean debug = Boolean.getBoolean("maven-monitor.debug");

    private boolean reportApiEnabled = Boolean.getBoolean("maven-monitor.reportApiEnabled");

    private String reportApiUrl = System.getProperty("maven-monitor.reportApiUrl", "http://localhost:3000/");

    private boolean anonymousMetrics = Boolean.getBoolean("maven-monitor.anonymous");

    private boolean skipReport = false;

    private String ignoreList = System.getProperty("maven-monitor.ignoreGoals", DEFAULT_IGNORED_GOALS);

    private boolean disabled = Boolean.getBoolean("maven-monitor.disabled");

    private boolean silent = Boolean.getBoolean("maven-monitor.silent");

    private String reportApiAuth = System.getProperty("maven-monitor.reportApiAuth");

    @Override
    public void init(Context context) throws Exception {
        if (disabled) {
            logger.info("Maven-monitor-extension is disabled (remove 'maven-monitor.disabled') to enable it");
            return;
        }
        logger.info("maven-monitor-extension is registered.");
        report.setCicd(Boolean.valueOf(System.getenv("CI")));
        //we only collect username/id in case we are not in CI mode
        if (!report.isCicd()) {
            if (!anonymousMetrics) {
                //if we can collect username, then set it
                String envUser = System.getProperty("user.name");
                report.setClient(envUser);
            } else {
                //otherwise we are anonymous but identifiable by owner
                try {
                    Path ourMarkerFilePath = Paths.get(System.getProperty("user.home"),
                            MVN_MONITOR_ID_FILE);
                    if (Files.exists(ourMarkerFilePath)) {
                        report.setClient(Files.readString(ourMarkerFilePath));
                    } else {
                        String id = UUID.randomUUID().toString();
                        report.setClient(id);
                        Files.write(ourMarkerFilePath, id.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (Exception e) {
                    logger.warn("Could not create maven-monitor-extension identifier:  {}", e.getMessage());
                    if (debug) {
                        logger.error("Error: ", e);
                    }
                    report.setClient("unknown");
                }

            }
        } else {
            //dont bother on CI
            report.setClient("CI");
        }
        report.setMavenVersion(((Map<String, String>) context.getData().get("systemProperties")).get("maven.version"));
        report.setOs(System.getProperty("os.name"));
        report.setJava(System.getProperty("java.version"));
        report.setCommandLine(System.getenv("MAVEN_CMD_LINE_ARGS"));
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (this.skipReport || this.disabled) {
            return;
        }
        try {
            if (event instanceof ExecutionEvent) {
                ExecutionEvent ee = (ExecutionEvent) event;
                MavenProject project = ee.getProject();
                MojoExecution mojoExec = ee.getMojoExecution();

                switch (ee.getType()) {
                    case ProjectStarted:
                        this.skipReport = shouldSkipReport(ee.getSession());
                        session.projectStarted(project);
                        report.setProfiles(project.getActiveProfiles().stream().map(profile -> profile.getId()).collect(Collectors.joining(",")));
                        report.setGoals(ee.getSession().getGoals().stream().collect(Collectors.joining(",")));
                        break;
                    case ProjectFailed:
                        session.projectEnded(project);
                        report.setStatus("FAILED");
                        break;
                    case ProjectSkipped:
                        session.projectEnded(project);
                        report.setStatus("SKIPPED");
                        break;
                    case ProjectSucceeded:
                        report.setStatus("OK");
                        session.projectEnded(project);
                        break;
                    case MojoStarted:
                        if (mojoExec != null) {
                            session.mojoStarted(ee.getProject(), ee.getMojoExecution());
                        } else {
                            logger.debug("received null mojo execution {}", ee.getType());
                        }
                        break;

                    case MojoFailed:
                        if (mojoExec != null) {
                            session.mojoFailed(ee.getProject(), ee.getMojoExecution());
                        } else {
                            logger.debug("received null mojo execution {}", ee.getType());
                        }

                        break;

                    case MojoSucceeded:
                        if (mojoExec != null) {
                            session.mojoSucceeded(ee.getProject(), ee.getMojoExecution());
                        } else {
                            logger.debug("received null mojo execution {}", ee.getType());
                        }
                        break;

                    case SessionEnded:
                        doReport(session);
                        break;

                    default:
                        //Ignore other events
                }
            } else if (event instanceof RepositoryEvent) {
                RepositoryEvent repo = (RepositoryEvent) event;
                switch (repo.getType()) {
                    case ARTIFACT_DOWNLOADING:
                        this.artifactTimerMap.put(repo.getArtifact().toString(), new ArtifactTimer());
                        break;
                    case ARTIFACT_DOWNLOADED:
                        this.artifactTimerMap.get(repo.getArtifact().toString()).stop();
                        break;
                }

            }
        } catch (Exception e) {
            logger.error("Error in maven-monitor-extension:  {}", event, e);
        }
    }

    private boolean shouldSkipReport(MavenSession session) {
        if (ignoreList == null) {
            return false;
        }
        return session.getGoals().stream().anyMatch(goal -> ignoreList.toLowerCase().contains(goal.toLowerCase()));
    }

    private void doReport(SessionTimer session) {
        for (ProjectTimer projectTimer : session.getProjects().values()) {
            report = report.fillWithTimerInfo(projectTimer);
            if (reportApiEnabled) {
                post(report);
            } else {
                if (debug) {
                    logger.info("Not sending report to api server because 'maven-monitor.reportApiEnabled=' is false or empty");
                }
            }
            if (!this.silent) {
                logReport(report);
            }
        }
    }

    private void logReport(ProjectReport report) {
        logger.info("\n---------------< maven-monitor-extension > -----------------");
        logger.info("Project execution summary:");
        logger.info("Total duration : {}", String.format("%.3fs", (report.getDuration()) / 1000D));
        logger.info("Phases :");

        report.getPhases().forEach(phaseTimer -> {

            List<PluginReport> mojoTimersSorted =
                    phaseTimer.getPlugins().stream().sorted(Comparator.comparing(pr -> pr.getStartTime())).collect(Collectors.toList());
            phaseTimer.setStartTime(mojoTimersSorted.get(0).getStartTime());
            phaseTimer.setEndTime(mojoTimersSorted.get(mojoTimersSorted.size() - 1).getEndTime());

            logger.info("{} : [{}]", phaseTimer.getName(), String.format("%.3fs", (phaseTimer.getDuration()) / 1000D));
            for (PluginReport pluginReport : mojoTimersSorted) {
                logger.info("\tPlugin: {}  [{}]", pluginReport.getName(), String.format("%.3fs",
                        pluginReport.getDuration() / 1000D));
            }

        });

        long downloadTotal = this.artifactTimerMap.values().stream().collect(Collectors.summarizingLong(value -> value.getDuration())).getSum();
        report.setDownloadTimeTotal(downloadTotal);
        report.setDownloadedArtifacts(artifactTimerMap.size());
        if (debug) {
            logger.info("Downloaded {} dependencies in {}", artifactTimerMap.size(), String.format("%.3fs", (downloadTotal) / 1000D));
        }
    }

    private void post(ProjectReport report) {
        try {
            HttpClient client = HttpClient.newBuilder().build();
            String body = JSON.toJSONString(report, true);
            if (debug) {
                logger.info("Maven-monitor-extension will send the following data to report server({}): {}", reportApiUrl, body);
            }
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(constructReportURL(reportApiUrl)))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    //we dont want to slow down the build
                    .timeout(Duration.of(1, ChronoUnit.SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBytes(StandardCharsets.UTF_8)));

            if (reportApiAuth != null) {
                requestBuilder.header("Authorization", reportApiAuth);
            }

            HttpRequest request = requestBuilder.build();


            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                logger.info("Execution report submitted. You can access it via: {}", response.headers().firstValue("Location").get());
            } else {
                logger.warn("Failed to send execution report to API server({}) response code: {}", reportApiUrl, response.statusCode());
                if (debug) {
                    logger.error("maven-extension-monitor failed to send report : {}", response.body());
                }
            }
        } catch (Exception e) {
            logger.error("maven-extension-monitor failed to send report {}", e.getMessage());
            if (debug) {
                logger.error("Exception:  ", e);
            }
        }
    }

    private String constructReportURL(String reportApiUrl) {
        if (reportApiUrl == null) {
            throw new IllegalArgumentException("reportApiUrl can not be null");
        }
        return reportApiUrl.endsWith("/") ? StringUtils.substring(reportApiUrl, 0, -1).concat(REPORT_API_PREFIX) :
                reportApiUrl.concat(REPORT_API_PREFIX);
    }
}
