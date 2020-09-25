package de.embl.cba.imflow.cluster;

import de.embl.cba.cats.utils.IOUtils;
import de.embl.cba.cats.utils.IntervalUtils;
import de.embl.cba.cluster.ImageJCommandsSubmitter;
import de.embl.cba.cluster.JobFuture;
import de.embl.cba.cluster.JobSettings;
import de.embl.cba.cluster.SlurmJobMonitor;
import de.embl.cba.log.IJLazySwingLogger;
import de.embl.cba.tables.Logger;
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
import java.util.List;
import java.util.Map;

import static de.embl.cba.cats.CATS.logger;
import static de.embl.cba.cats.utils.IntervalUtils.*;
import static de.embl.cba.cats.utils.Utils.getSimpleString;
import static de.embl.cba.cluster.ImageJCommandsSubmitter.IMAGEJ_EXECTUABLE_ALMF_CLUSTER_HEADLESS;


@Plugin(type = Command.class, menuPath = "Plugins>EMBL>FCCF>BD>Process Images on Slurm" )
public class ProcessBDVulcanImagesOnSlurmCommand implements Command
{
    @Parameter( label = "Username" )
    private String userName = "tischer";
    public static String USER_NAME = "userName";

    @Parameter( label = "Password", style = TextWidget.PASSWORD_STYLE, persist = false )
    private String password;
    public static String PASSWORD = "password";

    @Parameter( label = "Number of jobs" )
    public int numJobs = 100;
    public static String NUM_JOBS = "numJobs";

    @Parameter( label = "Number of CPUs per job" )
    public int numWorkers = 12;
    public static final String NUM_WORKERS = "numWorkers";

    @Parameter( label = "Classifier (must be cluster accessible)" )
    public File classifierFile;
    public static final String CLASSIFIER_FILE = "classifierFile";

    /*
    @Parameter( label = "ImageJ executable (must be linux and cluster accessible)", required = false)
    public File imageJFile;
    public static final String IMAGEJ_FILE = "imageJFile";
    */

    @Parameter( label = "Job status monitoring interval [s]" )
    public int jobStatusMonitoringInterval = 60;
    public static final String JOB_STATUS_MONITORING_INTERVAL = "jobStatusMonitoringInterval";

    @Parameter()
    public String inputModality;

    @Parameter()
    public File inputImageFile;

    @Parameter()
    public File outputDirectory;

    @Parameter()
    public FinalInterval interval;
    public static final String INTERVAL = "interval";

    @Parameter( required = false )
    public String inputImageVSSDirectory;

    @Parameter( required = false )
    public String inputImageVSSScheme;

    @Parameter( required = false )
    public String inputImageVSSPattern;

    @Parameter( required = false )
    public String inputImageVSSHdf5DataSetName;

    public static int memoryFactor = 10;

    public boolean quitAfterRun = true;

    FinalInterval inputImageIntervalXYZT;

    public void run()
    {
        String jobDirectory = "/g/cba/cluster/" + userName;

        List< Path > dataSets = new ArrayList<>();
        dataSets.add( inputImageFile.toPath() );

        ArrayList< JobFuture > jobFutures = submitJobsOnSlurm(
                IMAGEJ_EXECTUABLE_ALMF_CLUSTER_HEADLESS,
                jobDirectory, classifierFile.toPath(), dataSets );

        SlurmJobMonitor slurmJobMonitor = new SlurmJobMonitor( new IJLazySwingLogger() );
        slurmJobMonitor.monitorJobProgress( jobFutures, jobStatusMonitoringInterval, 5 );
    }

    private ArrayList< JobFuture > submitJobsOnSlurm( String imageJ, String jobDirectory, Path classifierPath, List< Path > dataSets )
    {
        ImageJCommandsSubmitter commandsSubmitter = new ImageJCommandsSubmitter(
                ImageJCommandsSubmitter.EXECUTION_SYSTEM_EMBL_SLURM,
                jobDirectory ,
                imageJ,
                userName,
                password );

        ArrayList< JobFuture > jobFutures = new ArrayList<>( );

        int numFeatures = 1500; // TODO: typically too conversative

        ArrayList< FinalInterval > tiles = IntervalUtils.createTiles(
                interval,
                interval,
                numJobs,
                numFeatures,
                false,
                true,
                null );

        for ( Path dataSet : dataSets )
        {
            for ( FinalInterval tile : tiles )
            {
                commandsSubmitter.clearCommands();
                setCommandAndParameterStrings( commandsSubmitter, dataSet, classifierPath, tile );
                JobSettings jobSettings = getJobSettings( tile );
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
        }

        return jobFutures;
    }


    private JobSettings getJobSettings( FinalInterval tile )
    {
        JobSettings jobSettings = new JobSettings();
        jobSettings.numWorkersPerNode = numWorkers;
        jobSettings.queue = JobSettings.DEFAULT_QUEUE;
        jobSettings.memoryPerJobInMegaByte = getApproximatelyNeededMemoryMB( tile );
        jobSettings.timePerJobInMinutes = getApproximatelyNeededTimeInMinutes( tile );
        return jobSettings;
    }

    private int getApproximatelyNeededMemoryMB( FinalInterval tile )
    {
        long memoryB = IntervalUtils.getApproximatelyNeededBytes( tile, memoryFactor );
        int memoryMB = (int) ( 1.0 * memoryB / 1000000L );
        if ( memoryMB < 32000 ) memoryMB = 32000;
        return memoryMB;
    }

    private int getApproximatelyNeededTimeInMinutes( FinalInterval tile )
    {
        long numVoxels = tile.dimension( X ) * tile.dimension( Y ) * tile.dimension( Z );
        long voxelsPerSecond = 20000;
        double secondsNeeded = 1.0 * numVoxels / voxelsPerSecond;
        int minutesNeeded = ( int ) Math.ceil( secondsNeeded / 60.0 );
        minutesNeeded = Math.max( 10, minutesNeeded );
        return minutesNeeded;
    }

    private void setCommandAndParameterStrings(
            ImageJCommandsSubmitter commandsSubmitter,
            Path inputImagePath,
            Path classifierPath,
            FinalInterval tile )
    {

        String dataSetID = getDataSetID( inputImagePath );
        
        Map< String, Object > parameters = new HashMap<>();

        String intervalXYZT = getIntervalAsCsvString( tile );

        parameters.clear();
        parameters.put( ApplyClassifierAdvancedCommand.DATASET_ID, dataSetID );

        parameters.put( IOUtils.INPUT_MODALITY, inputModality );

        parameters.put( IOUtils.INPUT_IMAGE_FILE, PathMapper.asEMBLClusterMounted( inputImagePath ) );

        if ( inputModality.equals( IOUtils.OPEN_USING_LAZY_LOADING_TOOLS) )
        {
            parameters.put( IOUtils.INPUT_IMAGE_VSS_DIRECTORY, PathMapper.asEMBLClusterMounted( inputImageVSSDirectory ) );

            parameters.put( IOUtils.INPUT_IMAGE_VSS_PATTERN, inputImageVSSPattern );

            parameters.put( IOUtils.INPUT_IMAGE_VSS_SCHEME, inputImageVSSScheme );

            parameters.put( IOUtils.INPUT_IMAGE_VSS_HDF5_DATA_SET_NAME, inputImageVSSHdf5DataSetName );
        }

        parameters.put( ApplyClassifierAdvancedCommand.CLASSIFICATION_INTERVAL, intervalXYZT );

        parameters.put( ApplyClassifierAdvancedCommand.CLASSIFIER_FILE, PathMapper.asEMBLClusterMounted( classifierPath ) );

        parameters.put( IOUtils.OUTPUT_MODALITY, IOUtils.SAVE_AS_MULTI_CLASS_TIFF_SLICES );

        parameters.put( ApplyClassifierAdvancedCommand.OUTPUT_DIRECTORY, PathMapper.asEMBLClusterMounted( outputDirectory ) );

        parameters.put( ApplyClassifierAdvancedCommand.NUM_WORKERS, numWorkers );
        parameters.put( ApplyClassifierAdvancedCommand.MEMORY_MB, getApproximatelyNeededMemoryMB( tile ) );

        parameters.put( ApplyClassifierAdvancedCommand.SAVE_RESULTS_TABLE, false );
        parameters.put( ApplyClassifierAdvancedCommand.QUIT_AFTER_RUN, true );

        commandsSubmitter.addIJCommandWithParameters( ApplyClassifierAdvancedCommand.PLUGIN_NAME , parameters );
    }

    private String getDataSetID( Path inputImagePath )
    {
        String dataSetID = getSimpleString( inputImagePath.getFileName().toString()  );

        if ( dataSetID.equals( "" ) )
        {
            dataSetID = "dataSet";
        }
        return dataSetID;
    }


    public static void main(final String... args) throws Exception
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ij.command().run( ProcessBDVulcanImagesOnSlurmCommand.class, true );

    }
}
