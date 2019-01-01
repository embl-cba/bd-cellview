import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.RandomAccessibleIntervalSource;
import de.embl.cba.bdv.utils.argbconversion.SelectableRealVolatileARGBConverter;
import de.embl.cba.bdv.utils.argbconversion.VolatileARGBConvertedRealSource;
import de.embl.cba.bdv.utils.behaviour.BehaviourSelectionEventHandler;
import de.embl.cba.tables.objects.ObjectTableModel;
import de.embl.cba.tables.objects.grouping.Grouping;
import de.embl.cba.tables.objects.grouping.GroupingUI;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		 * Show image
		 */

		final RandomAccessibleIntervalSource raiSource = Tests.load2D16BitLabelMask();

		final SelectableRealVolatileARGBConverter argbConverter = new SelectableRealVolatileARGBConverter();

		final VolatileARGBConvertedRealSource argbSource = new VolatileARGBConvertedRealSource( raiSource,  argbConverter );

		Bdv bdv = BdvFunctions.show( argbSource, BdvOptions.options().is2D() ).getBdvHandle();

		/**
		 * Load table and add a group column
		 */

		final JTable jTable = Tests.loadObjectTableFor2D16BitLabelMask();
		final ObjectTableModel model = (ObjectTableModel) jTable.getModel();

		// add group column
		final String[] groups = new String[ model.getRowCount() ];
		Arrays.fill( groups, "None" );
		model.addColumn( "Group", groups );
		final int groupingColumnIndex = model.getColumnCount() - 1;

		// update column classes (needed for proper sorting)
		model.setColumnClassesFromFirstRow();

		// show panel
		Tests.createInteractiveTablePanel( jTable, bdv, argbConverter );


		/**
		 * Init an object table-based grouping handler and UI
		 */

		final Grouping grouping = new Grouping( jTable, 0, groupingColumnIndex );
		final GroupingUI groupingUI = new GroupingUI( grouping );

		/**
		 * Configure interactive object grouping
		 */


		// Add a behaviour to Bdv, enabling selection of labels by Ctrl + Left-Click
		//
		final BehaviourSelectionEventHandler behaviourSelectionEventHandler = new BehaviourSelectionEventHandler( bdv, argbSource, argbConverter );

		// Define an additional behaviour, enabling grouping by Ctrl + G
		//
		final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "bdv-object-grouping-" + argbSource.getName() );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			groupingUI.assignObjectsToGroup( behaviourSelectionEventHandler.getSelectedValues() );
		}
		, "fetch-curently-selected-objects-" + argbSource.getName(), Tests.OBJECT_GROUPING_TRIGGER  );

	}
}
