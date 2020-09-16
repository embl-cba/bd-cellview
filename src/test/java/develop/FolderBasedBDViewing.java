package develop;

import de.embl.cba.imflow.devel.deprecated.BDOpenFolderCommand;
import net.imagej.ImageJ;

import java.io.File;

public class FolderBasedBDViewing
{
	public static void main( String[] args )
	{
		// cifs://fccfaurora.embl.de/fccfaurora
		// cifs://bdimsort.embl.de/bdimsort
		// /Volumes/fccfaurora/Daniel Malte Imaging Sorter/09_23_2019/Cell_cycle_1/output/complete_gated.csv

		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final BDOpenFolderCommand command = new BDOpenFolderCommand();

		command.inputImagesDirectory = new File("/Users/tischer/Documents/fccf/src/test/resources/minimalgated");
		command.commandService = imageJ.command();

		command.run();
	}
}
