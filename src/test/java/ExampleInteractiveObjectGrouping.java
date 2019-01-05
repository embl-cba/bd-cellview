import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.RandomAccessibleIntervalSource;
import de.embl.cba.bdv.utils.behaviour.BdvSelectionEventHandler;
import de.embl.cba.bdv.utils.converters.argb.SelectableVolatileARGBConverter;
import de.embl.cba.bdv.utils.converters.argb.VolatileARGBConvertedRealSource;
import de.embl.cba.tables.models.ColumnClassAwareTableModel;
import de.embl.cba.tables.objects.ObjectCoordinate;
import de.embl.cba.tables.objects.ObjectTablePanel;
import de.embl.cba.tables.objects.grouping.Grouping;
import de.embl.cba.tables.objects.grouping.GroupingUI;
import net.imagej.ImageJ;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;

public class ExampleInteractiveObjectGrouping
{
	public static void main( String[] args ) throws IOException
	{

		/**
		 * Example of interactive object selections and groupings
		 *
		 * Ctrl + Left-Click: Select/Unselect object(s) in image
		 *
		 * Ctrl + Q: Select none
		 *
		 * Ctrl + G: Assign selected object(s) to a group
		 *
		 */

		new ImageJ().ui().showUI();

		/**
		 * Load and show image
		 */

		final RandomAccessibleIntervalSource raiSource = Examples.load2D16BitLabelMask();

		final SelectableVolatileARGBConverter argbConverter = new SelectableVolatileARGBConverter();

		final VolatileARGBConvertedRealSource argbSource = new VolatileARGBConvertedRealSource( raiSource,  argbConverter );

		Bdv bdv = BdvFunctions.show( argbSource, BdvOptions.options().is2D() ).getBdvHandle();

		/**
		 * Load table and add a group column
		 */

		final JTable jTable = Examples.loadObjectTableFor2D16BitLabelMask();

		final ObjectTablePanel objectTablePanel = new ObjectTablePanel( jTable );
		objectTablePanel.showPanel();
		objectTablePanel.setCoordinateColumn( ObjectCoordinate.Label, jTable.getColumnName( 0 ) );
		objectTablePanel.addColumn( "Grouping", "None" );

		/**
		 * Init an table based grouping handler and UI
		 */

		final Grouping grouping = new Grouping( jTable, "Label", "Grouping" );
		final GroupingUI groupingUI = new GroupingUI( grouping );

		/**
		 * Configure interactive object grouping
		 */

		// Add a behaviour to Bdv, enabling selection of labels by Ctrl + Left-Click
		//
		final BdvSelectionEventHandler bdvSelectionEventHandler = new BdvSelectionEventHandler( bdv, argbSource, argbConverter );

		// Define an additional behaviour, enabling grouping by Ctrl + G
		//
		final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "bdv-object-grouping-" + argbSource.getName() );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			groupingUI.assignObjectsToGroup( bdvSelectionEventHandler.getSelectedValues() );
		}
		, "fetch-curently-selected-objects-" + argbSource.getName(), Examples.OBJECT_GROUPING_TRIGGER  );

	}
}
