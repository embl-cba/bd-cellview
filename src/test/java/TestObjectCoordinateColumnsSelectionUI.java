import de.embl.cba.tables.objects.ObjectCoordinateColumnsSelectionUI;
import de.embl.cba.tables.objects.ObjectTablePanel;
import net.imagej.ImageJ;

import javax.swing.*;
import java.io.IOException;

public class TestObjectCoordinateColumnsSelectionUI
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final JTable table = Examples.loadObjectTableFor2D16BitLabelMask();

		final ObjectTablePanel objectTablePanel = new ObjectTablePanel( table, "Table" );

		objectTablePanel.showPanel();

		new ObjectCoordinateColumnsSelectionUI( objectTablePanel );

	}
}
