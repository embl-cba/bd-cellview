
import de.embl.cba.metadata.MetaData;
import ij.ImageJ;
import loci.common.services.ServiceFactory;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

import ome.units.quantity.Length;
import ome.units.quantity.Time;
import ome.units.UNITS;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;


import java.io.FileWriter;
import java.util.*;

import static de.embl.cba.metadata.Utils.log;

/**
 * Uses Bio-Formats to extract some basic standardized
 * (format-independent) metadata.
 */
public class BioFormatsMetadataExtractionTest
{

	/** Outputs dimensional information. */
	public static void printPixelDimensions(IFormatReader reader) {
		// output dimensional information
		int sizeX = reader.getSizeX();
		int sizeY = reader.getSizeY();
		int sizeZ = reader.getSizeZ();
		int sizeC = reader.getSizeC();
		int sizeT = reader.getSizeT();
		int imageCount = reader.getImageCount();
		log("");
		log("Pixel dimensions:");
		log("\tWidth = " + sizeX);
		log("\tHeight = " + sizeY);
		log("\tFocal planes = " + sizeZ);
		log("\tChannels = " + sizeC);
		log("\tTimepoints = " + sizeT);
		log("\tTotal planes = " + imageCount);
	}

	/** Outputs global timing details. */
	public static void printPhysicalDimensions(IMetadata meta, int series) {
		Length physicalSizeX = meta.getPixelsPhysicalSizeX(series);
		Length physicalSizeY = meta.getPixelsPhysicalSizeY(series);
		Length physicalSizeZ = meta.getPixelsPhysicalSizeZ(series);
		Time timeIncrement = meta.getPixelsTimeIncrement(series);
		log("");
		log("Physical dimensions:");
		log("\tX spacing = " +
				physicalSizeX.value() + " " + physicalSizeX.unit().getSymbol());
		log("\tY spacing = " +
				physicalSizeY.value() + " " + physicalSizeY.unit().getSymbol());
		log("\tZ spacing = " +
				physicalSizeZ.value() + " " + physicalSizeZ.unit().getSymbol());
		log("\tTime increment = " + timeIncrement.value(UNITS.SECOND).doubleValue() + " seconds");
	}


	public static void main(String[] args) throws Exception {

		ImageJ.main( args );

		String id = "/Volumes/cba/exchange/OeyvindOedegaard/yaml_project/20180627_LSM780M2_208_ibidi1_fcs_B_Posx96.lsm";
		int series = args.length > 1 ? Integer.parseInt(args[1]) : 0;

		// create OME-XML metadata store
		ServiceFactory factory = new ServiceFactory();
		OMEXMLService service = factory.getInstance(OMEXMLService.class);
		IMetadata meta = service.createOMEXMLMetadata();

		// create format reader
		IFormatReader reader = new ImageReader();
		reader.setMetadataStore(meta);

		// initialize file
		log("Initializing " + id);
		reader.setId(id);

		int seriesCount = reader.getSeriesCount();
		if (series < seriesCount) reader.setSeries(series);
		series = reader.getSeries();
		log("\tImage series = " + series + " of " + seriesCount);

		printPixelDimensions(reader);
		printPhysicalDimensions(meta, series);

		final MetaData metaData = new MetaData( reader, meta, series );

		final Map< String, Object > map = metaData.getMap();

		final DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle( DumperOptions.FlowStyle.BLOCK );

		Yaml yaml = new Yaml( dumperOptions );
		FileWriter writer = new FileWriter("/Volumes/cba/exchange/OeyvindOedegaard/yaml_project/test.yaml");
		yaml.dump(map, writer);



	}

}