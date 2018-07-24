
import de.embl.cba.metadata.Utils;
import ij.ImageJ;
import loci.common.DateTools;
import loci.common.services.ServiceFactory;
import loci.formats.FormatReader;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

import ome.units.quantity.Length;
import ome.units.quantity.Time;
import ome.units.UNITS;

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
		Utils.log("");
		Utils.log("Pixel dimensions:");
		Utils.log("\tWidth = " + sizeX);
		Utils.log("\tHeight = " + sizeY);
		Utils.log("\tFocal planes = " + sizeZ);
		Utils.log("\tChannels = " + sizeC);
		Utils.log("\tTimepoints = " + sizeT);
		Utils.log("\tTotal planes = " + imageCount);
	}

	/** Outputs global timing details. */
	public static void printPhysicalDimensions(IMetadata meta, int series) {
		Length physicalSizeX = meta.getPixelsPhysicalSizeX(series);
		Length physicalSizeY = meta.getPixelsPhysicalSizeY(series);
		Length physicalSizeZ = meta.getPixelsPhysicalSizeZ(series);
		Time timeIncrement = meta.getPixelsTimeIncrement(series);
		Utils.log("");
		Utils.log("Physical dimensions:");
		Utils.log("\tX spacing = " +
				physicalSizeX.value() + " " + physicalSizeX.unit().getSymbol());
		Utils.log("\tY spacing = " +
				physicalSizeY.value() + " " + physicalSizeY.unit().getSymbol());
		Utils.log("\tZ spacing = " +
				physicalSizeZ.value() + " " + physicalSizeZ.unit().getSymbol());
		Utils.log("\tTime increment = " + timeIncrement.value(UNITS.SECOND).doubleValue() + " seconds");
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
		Utils.log("Initializing " + id);
		reader.setId(id);

		int seriesCount = reader.getSeriesCount();
		if (series < seriesCount) reader.setSeries(series);
		series = reader.getSeries();
		Utils.log("\tImage series = " + series + " of " + seriesCount);

		printPixelDimensions(reader);
		printPhysicalDimensions(meta, series);
	}

}