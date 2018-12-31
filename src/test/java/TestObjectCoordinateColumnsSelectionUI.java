import de.embl.cba.tables.ObjectTableCoordinateColumnsSelectionCommand;
import de.embl.cba.tables.objects.ObjectCoordinate;
import de.embl.cba.tables.objects.ObjectCoordinateColumnsSelectionUI;
import de.embl.cba.tables.objects.ObjectTablePanel;
import net.imagej.ImageJ;
import org.scijava.command.CommandModule;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Future;

public class TestObjectCoordinateColumnsSelectionUI
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final JTable table = Tests.loadTestTable01();

		final ObjectTablePanel objectTablePanel = new ObjectTablePanel( table );

		objectTablePanel.showPanel();

		new ObjectCoordinateColumnsSelectionUI( objectTablePanel );

	}
}
