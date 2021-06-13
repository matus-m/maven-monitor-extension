import com.binarytale.mavenmonitor.Utils;
import com.binarytale.mavenmonitor.api.PhaseReport;
import com.binarytale.mavenmonitor.api.PluginReport;
import com.binarytale.mavenmonitor.api.ProjectReport;
import com.binarytale.mavenmonitor.timers.MojoTimer;
import com.binarytale.mavenmonitor.timers.PhaseTimer;
import com.binarytale.mavenmonitor.timers.ProjectTimer;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReportTest {
    @Test
    public void testParallelMojo() throws InterruptedException {
        PhaseTimer phaseTimer = new PhaseTimer("test");
        Plugin plugin = new Plugin();
        plugin.setArtifactId("artifact");
        plugin.setGroupId("group");
        MojoExecution me1 = new MojoExecution(plugin, "goal", "exec1");
        MojoExecution me2 = new MojoExecution(plugin, "goal", "exec2");

        MojoTimer mojo1 = new MojoTimer(me1);
        mojo1.start();
        MojoTimer mojo2 = new MojoTimer(me2);
        mojo2.start();
        phaseTimer.getMojoTimers().put("plugin1:mojo1", mojo1);
        phaseTimer.getMojoTimers().put("plugin1.mojo2", mojo2);
        mojo1.setEndTime(mojo1.getStartTime() + 1000);
        mojo2.setEndTime(mojo2.getStartTime() + 1000);
        ProjectReport report = new ProjectReport();
        ProjectTimer projectTimer = new ProjectTimer("group", "testproject");
        projectTimer.getPhaseTimers().put("test", phaseTimer);
        report = report.fillWithTimerInfo(projectTimer);

        Assertions.assertTrue(report.getPhases().size() == 1);
        PhaseReport phaseReport = report.getPhases().get(0);

        Assertions.assertEquals(phaseReport.getDuration(), mojo2.getEndTime() - mojo1.getStartTime());
        Assertions.assertEquals(phaseReport.getStartTime(), Utils.dateFromEpoch(mojo2.getStartTime()));
        Assertions.assertEquals(phaseReport.getEndTime(), Utils.dateFromEpoch(mojo2.getEndTime()));
        Assertions.assertTrue(phaseReport.getDuration() < (mojo1.getDuration() + mojo1.getDuration()));
        Assertions.assertEquals(phaseReport.getPlugins().size() , 1);

        PluginReport pluginReport = phaseReport.getPlugins().get(0);
        Assertions.assertEquals(pluginReport.getExecutions().size() , 2);

    }
}
