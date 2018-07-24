
import de.embl.cba.metadata.Metadata;
import ij.IJ;
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

	public static void main(String[] args) throws Exception {

		ImageJ.main( args );

		String file = "/Volumes/cba/exchange/OeyvindOedegaard/yaml_project/20180627_LSM780M2_208_ibidi1_fcs_B_Posx96.lsm";

		final Metadata metadata = new Metadata( file );

		final Map< String, Object > map = metadata.getMap();

		final DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle( DumperOptions.FlowStyle.BLOCK );

		String outputPath = "/Volumes/cba/exchange/OeyvindOedegaard/yaml_project/test.yaml";
		Yaml yaml = new Yaml( dumperOptions );
		FileWriter writer = new FileWriter( outputPath );
		yaml.dump(map, writer);

		IJ.open( outputPath );



	}

}