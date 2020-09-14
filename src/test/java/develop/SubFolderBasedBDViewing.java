package develop;

import de.embl.cba.morphometry.commands.BDOpenFolderCommand;
import net.imagej.ImageJ;

import java.io.File;

public class SubFolderBasedBDViewing
{
	public static void main( String[] args )
	{
		// cifs://fccfaurora.embl.de/fccfaurora
		// cifs://bdimsort.embl.de/bdimsort
		// /Volumes/fccfaurora/Daniel Malte Imaging Sorter/09_23_2019/Cell_cycle_1/output/complete_gated.csv

		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final BDOpenFolderCommand command = new BDOpenFolderCommand();

		command.inputImagesDirectory = new File("/Users/tischer/Documents/fiji-plugin-morphometry/src/test/resources/test-data/bd.images.subfolder");
		command.commandService = imageJ.command();

		command.run();
	}
}
