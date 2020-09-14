package develop;

import de.embl.cba.fccf.BDOpenTableCommand;
import net.imagej.ImageJ;

import java.io.File;

public class TableBasedBDViewing
{
	public static void main( String[] args )
	{
		// cifs://fccfaurora.embl.de/fccfaurora
		// cifs://bdimsort.embl.de/bdimsort
		// /Volumes/fccfaurora/Daniel Malte Imaging Sorter/09_23_2019/Cell_cycle_1/output/complete_gated.csv

		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final BDOpenTableCommand command = new BDOpenTableCommand();

		command.imageTableFile = new File("/Users/tischer/Documents/fiji-plugin-morphometry/src/test/resources/test-data/bd/minimalgated/output/countpath.csv");
//		command.imageTableFile = new File("/Volumes/bdimsort/09_23_2019/Cell_cycle_1/output/tiffpaths_gated.csv");

		command.gateColumnName = "gate";
		command.imagePathColumnName = "path";
		command.commandService = imageJ.command();
		command.run();
	}
}
