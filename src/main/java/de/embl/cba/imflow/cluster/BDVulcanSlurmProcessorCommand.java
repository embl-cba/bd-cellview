package de.embl.cba.imflow.cluster;

import de.embl.cba.cluster.ImageJCommandsSubmitter;
import de.embl.cba.cluster.JobFuture;
import de.embl.cba.cluster.JobSettings;
import de.embl.cba.cluster.SlurmJobMonitor;
import de.embl.cba.imflow.BDVulcanHeadlessProcessorCommand;
import de.embl.cba.imflow.BDVulcanProcessorCommand;
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


@Plugin(type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Process Images on Slurm" )
public class BDVulcanSlurmProcessorCommand implements Command
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

    @Parameter( label = "Number of CPUs per job" )
    public int numWorkers = 1;
    public static final String NUM_WORKERS = "numWorkers";

    @Parameter( label = "Batch files" )
    public File[] batchFiles;
    public static final String BATCH_FILES = "batchFiles";

    @Parameter( label = "Job status monitoring interval [s]" )
    public int jobStatusMonitoringInterval = 60;
    public static final String JOB_STATUS_MONITORING_INTERVAL = "jobStatusMonitoringInterval";

    @Parameter( label = "Maximum number of failed job resubmissions" )
    public int maxNumResubmissions = 0;

    public void run()
    {
        String jobDirectory = "/g/cba/cluster/" + userName;

        ArrayList< JobFuture > jobFutures = submitJobsOnSlurm( FIJI_HEADLESS, jobDirectory, batchFiles );

        SlurmJobMonitor jobMonitor = new SlurmJobMonitor( new IJLazySwingLogger() );
        jobMonitor.monitorJobProgress( jobFutures, jobStatusMonitoringInterval, maxNumResubmissions );
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

            Utils.wait( 500 );
        }

        return jobFutures;
    }

    private JobSettings getJobSettings( File settingsFile )
    {
        try
        {
            BDVulcanProcessorCommand command = BDVulcanProcessorCommand.createBdVulcanProcessorCommandFromJson( settingsFile );
            int numImages = command.getNumberOfImagesToBeProcessed();

            IJ.log( "Table: " + command.selectedTableFile );
            IJ.log( "Gate: " + command.selectedGate );
            IJ.log( "Number of images: " + numImages );

            JobSettings jobSettings = new JobSettings();
            jobSettings.numWorkersPerNode = numWorkers;
            jobSettings.queue = JobSettings.DEFAULT_QUEUE;
            jobSettings.memoryPerJobInMegaByte = getApproximatelyNeededMemoryMB();
            jobSettings.timePerJobInMinutes = getApproximatelyNeededTimeInMinutes( numImages );
            return jobSettings;

        } catch ( IOException e )
        {
            e.printStackTrace();
            throw new RuntimeException(  );
        }
    }

    private int getApproximatelyNeededMemoryMB( )
    {
        return 1000;
    }

    private int getApproximatelyNeededTimeInMinutes( int numImages )
    {
        // less than 500 milliseconds per image
        return (int) Math.ceil( numImages * ( 0.500 / 60.0 ) ) ;
    }

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

        ij.command().run( BDVulcanSlurmProcessorCommand.class, true );

    }
}
