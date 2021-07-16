# maven-monitor-extension

Maven extension (based on EventSpy API) that records the execution time of maven builds with optional reporting of these metrics to central collector.

[![License](https://img.shields.io/github/license/yntelectual/maven-monitor-extension?style=for-the-badge&logo=MIT)](https://opensource.org/licenses/MIT)
[![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/yntelectual/maven-monitor-extension/Maven%20Package/main?logo=github&style=for-the-badge)](https://github.com/yntelectual/maven-monitor-extension/actions?query=workflow%3Aci)
[![Maven Central](https://img.shields.io/maven-central/v/com.binarytale/maven-monitor-extension?logo=java&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/com.binarytale/maven-monitor-extension)
[![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/yntelectual/maven-monitor-extension?logo=github&style=for-the-badge)](https://github.com/lorislab/yntelectual/maven-monitor-extension/releases/latest)


## Setup

Create a file `~/.mvn/extensions.xml` with the following contents:
```xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
  <extension>
    <groupId>com.binarytale</groupId>
    <artifactId>maven-monitor-extension</artifactId>
    <version>1.4.0</version>
  </extension>
</extensions>
```
Or add the `maven-monitor-plugin` manually to the extension list if `~/.mvn/extensions.xml` already exists.

## Configuration

You can pass this configuration properties as java System properties to the maven build - via `pom.xml` properties section, or globally via `MAVEN_OPTS` env var(you can define this on OS level or via `~/.mavenrc` file)

* `maven-monitor.debug` - when true, additional info will be printed to console, including report payload
* `maven-monitor.reportApiEnabled` - when true, the report will be sent to remote API, default `false`
* `maven-monitor.reportApiUrl` - if `reportApiEnabled`: report collector API URL. The extension will send the report via HTTP POST to `${maven-monitor.reportApiUrl}/api/projectReports/`. Default  `http://localhost:3000`
* `maven-monitor.reportApiAuth` - value of `Authorization` header that should be sent with the HTTP request to report API(due to issue with passing spaces in JVM props, you can replace them with `##`). Default  `null`
* `maven-monitor.reportApiTimeout` - http request timeout for the report API, in seconds Default  `1`
* `maven-monitor.anonymous` - when true, the current username(system property `user.name`) will be included in report `client` field. When false, the extension will sent a UUID instead and stores it in a file `~/.mvn-monitor-id`. That way you can still find all your reports (using `where client=you_uuid` filter) even if they are anonymous.
* `maven-monitor.ignoreGoals` - comma separate list of maven goals, which when active, will disable the extension
* `maven-monitor.disabled` - when true, the extension will not be active (e.g. when you want to disable the extension per project)
* `maven-monitor.silent` - when true, the extension will not print anything to stdout/logger (in case you only want to submit the report to remote API without polluting stdout)

Example `~/.mavenrc` config that turns on silent reporting to remote collector API:

```
MAVEN_OPTS="-Dmaven-monitor.silent=true -Dmaven-monitor.reportApiEnabled=true -Dmaven-monitor.reportApiUrl=http://mycollector.com/"
```

## Report structure

If you enable `maven-monitor.reportApiUrl` the extension will submit the report as JSON to remote API (`maven-monitor.reportApiUrl`) with the following structure:

```json5
{
   "duration":4442, //total duration of project build in ms
   "startTime":"2021-06-11T16:45:56.693+02:00",
   "endTime":"2021-06-11T16:46:01.135+02:00",
   "client":"matus", //current username, or UUID if anonymous report 
   "version":"1", //report version
   "mavenVersion":"3.6.3", //maven version, from `maven.version` system prop
   "os":"Mac OS X", //os name, from `os.name` system prop
   "java":"11.0.4", //java version, from `java.version` system prop
   "name":"maven-monitor-extension", //name of the project being build
   "goals":"clean,install", //activated goals
   "profiles":"myProfile", //activated profiles
   "commandLine": " clean package -Dmaven-monitor.reportApiEnabled=true -Dmaven-monitor.reportApiUrl=http://localhost:3000/api/", // mvn cmd line arguments
   "cicd":false, //CI env, from env var `CI`
   "phases":[ //for each active phase in the project, sorted by execution start time
      {
         "duration":48, //duration between start of first exec and end of last execution of plugins in this phase
         "startTime":"2021-06-11T16:45:56.778+02:00", //execution start time of the first plugin in this phase
         "endTime":"2021-06-11T16:45:56.826+02:00", //execution end time of the last plugin in this phase
         "name":"clean", //maven lifecycle phase name
         "plugins": [ //for each plugin participating in this phase, sorted by start time
            {
               "duration":48, //duration between start of first exec and end of last execution of this plugin
               "startTime":"2021-06-11T16:45:56.778+02:00",
               "endTime":"2021-06-11T16:45:56.826+02:00",
               "name":"org.apache.maven.plugins:maven-clean-plugin", //plugin group:artifactId
               "executions":[ //for each execution of this plugin
                  {
                     "duration":48, //exec time of this execution
                     "startTime":"2021-06-11T16:45:56.778+02:00",
                     "endTime":"2021-06-11T16:45:56.826+02:00",
                     "name":"default-clean" //execution id
                  }
               ]
            }
         ]
      },
      {
         "duration":97,
         "startTime":"2021-06-11T16:45:56.827+02:00",
         "endTime":"2021-06-11T16:45:56.924+02:00",
         "name":"process-resources",
         "plugins":[
            {
               "duration":97,
               "startTime":"2021-06-11T16:45:56.827+02:00",
               "endTime":"2021-06-11T16:45:56.924+02:00",
               "name":"org.apache.maven.plugins:maven-resources-plugin",
               "executions":[
                  {
                     "duration":97,
                     "startTime":"2021-06-11T16:45:56.827+02:00",
                     "endTime":"2021-06-11T16:45:56.924+02:00",
                     "id":100,
                     "name":"default-resources"
                  }
               ]
            }
         ]
      },
      {
         "duration":1469,
         "startTime":"2021-06-11T16:45:56.924+02:00",
         "endTime":"2021-06-11T16:45:58.393+02:00",
         "name":"compile",
         "plugins":[
            {
               "duration":1469,
               "startTime":"2021-06-11T16:45:56.924+02:00",
               "endTime":"2021-06-11T16:45:58.393+02:00",
               "name":"org.apache.maven.plugins:maven-compiler-plugin",
               "executions":[
                  {
                     "duration":1469,
                     "startTime":"2021-06-11T16:45:56.924+02:00",
                     "endTime":"2021-06-11T16:45:58.393+02:00",
                     "id":101,
                     "name":"default-compile"
                  }
               ]
            }
         ]
      }
     // additional phases truncated for brevity....
   ]
}
```

## Log output

By default, the extension does not report the data to remote collector, instead it prints the summary in the stdout after build completion:

```
[INFO] ---------------< maven-monitor-extension > -----------------
[INFO] Project execution summary:
[INFO] Total duration : [4.069s]
[INFO] Phases :
[INFO] clean : [0.043s]
[INFO]  Plugin: org.apache.maven.plugins:maven-clean-plugin  [0.043s]
[INFO] process-resources : [0.086s]
[INFO]  Plugin: org.apache.maven.plugins:maven-resources-plugin  [0.086s]
[INFO] compile : [1.402s]
[INFO]  Plugin: org.apache.maven.plugins:maven-compiler-plugin  [1.402s]
[INFO] process-test-resources : [0.003s]
[INFO]  Plugin: org.apache.maven.plugins:maven-resources-plugin  [0.003s]
[INFO] test-compile : [0.302s]
[INFO]  Plugin: org.apache.maven.plugins:maven-compiler-plugin  [0.302s]
[INFO] test : [2.008s]
[INFO]  Plugin: org.apache.maven.plugins:maven-surefire-plugin  [2.008s]
[INFO] package : [0.147s]
[INFO]  Plugin: org.apache.maven.plugins:maven-jar-plugin  [0.147s]
[INFO] ------------------------------------------------------------------------
```