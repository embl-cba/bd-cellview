package de.embl.cba.cellview.cluster;

import de.embl.cba.cluster.*;
import de.embl.cba.cellview.command.CellViewHeadlessProcessorCommand;
import de.embl.cba.cellview.command.CellViewProcessorCommand;
import de.embl.cba.log.IJLazySwingLogger;
import de.embl.cba.morphometry.Utils;
import de.embl.cba.util.PathMapper;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.TextWidget;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins>CellView>Process CellView Images on Slurm Cluster" )
public class CellViewSlurmProcessorCommand implements Command
{
    @Parameter( label = "Execution host" )
    private String hostName = "login.cluster.embl.de";

    @Parameter( label = "Username" )
    private String userName = "tischer";

    @Parameter( label = "Password", style = TextWidget.PASSWORD_STYLE, persist = false )
    private String password;

    @Parameter( label = "Path to Fiji on execution host")
    public String clusterFijiPath = "/g/almf/software/Fiji-versions/Fiji-BDVulcan.app/ImageJ-linux64";

    @Parameter( label = "Number of CPUs per job" )
    public int numWorkers = 1;

    @Parameter( label = "Memory per job [MB]" )
    public int memory = 32000;

    @Parameter( label = "Processing settings files" )
    public File[] settingsFiles;

    @Parameter( label = "Job status monitoring interval [s]" )
    public int jobStatusMonitoringInterval = 60;

    @Parameter( label = "Maximum number of failed job resubmissions" )
    public int maxNumResubmissions = 0;

    private static final String FIJI_OPTIONS = " --mem=MEMORY_MB --ij2 --allow-multiple --headless --run";

    public void run()
    {
        String jobDirectory = "/g/cba/cluster/" + hostName;

        ArrayList< JobFuture > jobFutures = submitJobsOnSlurm( clusterFijiPath + FIJI_OPTIONS, jobDirectory, settingsFiles );

        JobMonitor jobMonitor = new JobMonitor( new IJLazySwingLogger() );
        jobMonitor.monitorJobProgress( jobFutures, jobStatusMonitoringInterval, maxNumResubmissions );
    }

    private ArrayList< JobFuture > submitJobsOnSlurm( String imageJ, String jobDirectory, File[] batchFiles )
    {
        final JobExecutor jobExecutor = new JobExecutor();
        jobExecutor.hostName = hostName;
        jobExecutor.scriptType = JobExecutor.ScriptType.SlurmJob;

        JobSubmitter commandsSubmitter = new JobSubmitter(
                jobExecutor,
                jobDirectory ,
                imageJ,
                userName,
                password );

        ArrayList< JobFuture > jobFutures = new ArrayList<>( );

        for ( File batchFile : batchFiles )
        {
            commandsSubmitter.clearCommands();
            setCommandAndParameterStrings( commandsSubmitter, batchFile );
            JobSettings jobSettings = getJobSettings( batchFile );
            jobFutures.add( commandsSubmitter.submitJob( jobSettings ) );

            Utils.wait( 500 );
        }

        return jobFutures;
    }

    private JobSettings getJobSettings( File settingsFile )
    {
        try
        {
            CellViewProcessorCommand command = CellViewProcessorCommand.createBdVulcanProcessorCommandFromJson( settingsFile );

            IJ.log( "Table: " + command.selectedTableFile );
            IJ.log( "Gate: " + command.selectedGate );
            IJ.log( "Number of images to be processed: " + command.numImagesToBeProcessed );

            JobSettings jobSettings = new JobSettings();
            jobSettings.numWorkersPerNode = numWorkers;
            jobSettings.queue = JobSettings.DEFAULT_QUEUE;
            jobSettings.memoryPerJobInMegaByte = memory;
            jobSettings.timePerJobInMinutes = getApproximatelyNeededTimeInMinutes( command.numImagesToBeProcessed );

            return jobSettings;

        } catch ( IOException e )
        {
            e.printStackTrace();
            throw new RuntimeException(  );
        }
    }

    private int getApproximatelyNeededTimeInMinutes( int numImages )
    {
        // less than 500 milliseconds per image
        return (int) Math.ceil( numImages * ( 0.500 / 60.0 ) ) ;
    }

    private void setCommandAndParameterStrings( JobSubmitter commandsSubmitter, File batchFile )
    {
        Map< String, Object > parameters = new HashMap<>();
        parameters.clear();
        parameters.put( CellViewHeadlessProcessorCommand.SETTINGS_FILE, PathMapper.asEMBLClusterMounted( batchFile) );

        commandsSubmitter.addIJCommandWithParameters( CellViewHeadlessProcessorCommand.COMMAND_NAME , parameters );
    }

    public static void main(final String... args) throws Exception
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ij.command().run( CellViewSlurmProcessorCommand.class, true );
    }
}
