package de.embl.cba.metadata;

import ij.IJ;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins>Metadata>Metadata to YAML" )
public class MetadataCommand implements Command
{
	@Parameter
	public UIService uiService;

	@Parameter
	public DatasetService datasetService;

	@Parameter
	public LogService logService;

	@Parameter
	public OpService opService;

	@Parameter
	public StatusService statusService;

	@Parameter( style = FileWidget.OPEN_STYLE )
	public File dataset;

	public void run()
	{

		final Metadata metadata = new Metadata( dataset.getAbsolutePath() );
		final Map< String, Object > map = metadata.getMap();

		String outputPath = dataset.getAbsolutePath()  + ".yaml";

		createYamlFile( map, outputPath );

		IJ.open( outputPath );
		
	}

	public void createYamlFile( Map< String, Object > map, String outputPath )
	{
		try
		{
			final DumperOptions dumperOptions = new DumperOptions();
			dumperOptions.setDefaultFlowStyle( DumperOptions.FlowStyle.BLOCK );

			Yaml yaml = new Yaml( dumperOptions );
			FileWriter writer = new FileWriter( outputPath );
			yaml.dump( map, writer );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}

}