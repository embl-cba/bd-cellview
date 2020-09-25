package de.embl.cba.imflow.cluster;

import de.embl.cba.cluster.ImageJCommandsSubmitter;
import de.embl.cba.cluster.JobFuture;
import de.embl.cba.cluster.JobSettings;
import de.embl.cba.cluster.SlurmJobMonitor;
import de.embl.cba.imflow.BDVulcanHeadlessProcessorCommand;
import de.embl.cba.log.IJLazySwingLogger;
import de.embl.cba.util.PathMapper;
import net.imagej.ImageJ;
import net.imglib2.FinalInterval;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.TextWidget;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Plugin(type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Process Images on Slurm" )
public class ProcessBDVulcanImagesOnSlurmCommand implements Command
{
    public static final String FIJI_HEADLESS = "/g/almf/software/Fiji-versions/Fiji-BDVulcan.app/ImageJ-linux64 --mem=MEMORY_MB --ij2 --allow-multiple --headless --run";

    /*
    @Parameter( label = "ImageJ executable (must be linux and cluster accessible)", required = false)
    public File imageJFile;
    public static final String IMAGEJ_FILE = "imageJFile";
    */

    @Parameter( label = "Username" )
    private String userName = "tischer";
    public static String USER_NAME = "userName";

    @Parameter( label = "Password", style = TextWidget.PASSWORD_STYLE, persist = false )
    private String password;
    public static String PASSWORD = "password";

    @Parameter( label = "Maxmimal number of jobs" )
    public int numJobs = 100;
    public static String NUM_JOBS = "numJobs";

    @Parameter( label = "Number of CPUs per job" )
    public int numWorkers = 12;
    public static final String NUM_WORKERS = "numWorkers";

    @Parameter( label = "Batch files (must be cluster accessible)" )
    public File[] batchFiles;
    public static final String BATCH_FILES = "batchFiles";

    @Parameter( label = "Job status monitoring interval [s]" )
    public int jobStatusMonitoringInterval = 60;
    public static final String JOB_STATUS_MONITORING_INTERVAL = "jobStatusMonitoringInterval";

    public boolean quitAfterRun = true;

    public void run()
    {
        String jobDirectory = "/g/cba/cluster/" + userName;

        ArrayList< JobFuture > jobFutures = submitJobsOnSlurm( FIJI_HEADLESS, jobDirectory, batchFiles );

        SlurmJobMonitor slurmJobMonitor = new SlurmJobMonitor( new IJLazySwingLogger() );
        slurmJobMonitor.monitorJobProgress( jobFutures, jobStatusMonitoringInterval, 5 );
    }

    private ArrayList< JobFuture > submitJobsOnSlurm( String imageJ, String jobDirectory, File[] batchFiles )
    {
        ImageJCommandsSubmitter commandsSubmitter = new ImageJCommandsSubmitter(
                ImageJCommandsSubmitter.EXECUTION_SYSTEM_EMBL_SLURM,
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
            jobFutures.add( commandsSubmitter.submitCommands( jobSettings ) );

            try
            {
                Thread.sleep( 500 );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        }

        return jobFutures;
    }

    private JobSettings getJobSettings( File settingsFile )
    {
        // BDVulcanProcessorCommand.createBdVulcanProcessorCommandFromJson( settingsFile );

        JobSettings jobSettings = new JobSettings();
        jobSettings.numWorkersPerNode = numWorkers;
        jobSettings.queue = JobSettings.DEFAULT_QUEUE;
        jobSettings.memoryPerJobInMegaByte = 8000; // TODOL  getApproximatelyNeededMemoryMB( settingsFile );
        jobSettings.timePerJobInMinutes = 60; // getApproximatelyNeededTimeInMinutes( settingsFile );
        return jobSettings;
    }

//    private int getApproximatelyNeededMemoryMB( FinalInterval tile )
//    {
//        long memoryB = IntervalUtils.getApproximatelyNeededBytes( tile, memoryFactor );
//        int memoryMB = (int) ( 1.0 * memoryB / 1000000L );
//        if ( memoryMB < 32000 ) memoryMB = 32000;
//        return memoryMB;
//    }

//    private int getApproximatelyNeededTimeInMinutes( FinalInterval tile )
//    {
//        long numVoxels = tile.dimension( X ) * tile.dimension( Y ) * tile.dimension( Z );
//        long voxelsPerSecond = 20000;
//        double secondsNeeded = 1.0 * numVoxels / voxelsPerSecond;
//        int minutesNeeded = ( int ) Math.ceil( secondsNeeded / 60.0 );
//        minutesNeeded = Math.max( 10, minutesNeeded );
//        return minutesNeeded;
//    }

    private void setCommandAndParameterStrings( ImageJCommandsSubmitter commandsSubmitter, File batchFile )
    {
        Map< String, Object > parameters = new HashMap<>();

        parameters.clear();

        parameters.put( BDVulcanHeadlessProcessorCommand.SETTINGS_FILE, PathMapper.asEMBLClusterMounted( batchFile) );

        commandsSubmitter.addIJCommandWithParameters( BDVulcanHeadlessProcessorCommand.COMMAND_NAME , parameters );
    }

//    private String getDataSetID( Path inputImagePath )
//    {
//        String dataSetID = getSimpleString( inputImagePath.getFileName().toString()  );
//
//        if ( dataSetID.equals( "" ) )
//        {
//            dataSetID = "dataSet";
//        }
//        return dataSetID;
//    }

    public static void main(final String... args) throws Exception
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ij.command().run( ProcessBDVulcanImagesOnSlurmCommand.class, true );

    }
}
