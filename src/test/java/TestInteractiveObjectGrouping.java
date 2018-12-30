import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.RandomAccessibleIntervalSource;
import de.embl.cba.bdv.utils.argbconversion.SelectableRealVolatileARGBConverter;
import de.embl.cba.bdv.utils.argbconversion.VolatileARGBConvertedRealSource;
import de.embl.cba.bdv.utils.behaviour.BehaviourSelectionEventHandler;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.io.IOException;

public class TestInteractiveObjectGrouping
{
	public static void main( String[] args ) throws IOException
	{

		/**
		 * Show image
		 */

		final RandomAccessibleIntervalSource raiSource = Tests.loadTestImage01();

		final SelectableRealVolatileARGBConverter argbConverter = new SelectableRealVolatileARGBConverter();

		final VolatileARGBConvertedRealSource argbSource = new VolatileARGBConvertedRealSource( raiSource,  argbConverter );

		Bdv bdv = BdvFunctions.show( argbSource, BdvOptions.options().is2D() ).getBdvHandle();

		/**
		 * Show interactive table
		 */

		final JTable jTable = Tests.loadTestTable01();

		Tests.createInteractiveTablePanel( jTable, bdv, argbConverter );


		/**
		 * Configure interactive object grouping
		 */

		final BehaviourSelectionEventHandler behaviourSelectionEventHandler = new BehaviourSelectionEventHandler( bdv, argbSource, argbConverter );

		final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "bdv-object-grouping-" + argbSource.getName() );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			System.out.println( "Selected values:" );
			System.out.println( behaviourSelectionEventHandler.getSelectedValues() );

		}, "fetch-curently-selected-objects-" + argbSource.getName(),
				Tests.OBJECT_GROUPING_TRIGGER  );

	}
}
