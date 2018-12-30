import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.RandomAccessibleIntervalSource;
import de.embl.cba.bdv.utils.argbconversion.SelectableRealVolatileARGBConverter;
import de.embl.cba.bdv.utils.argbconversion.VolatileARGBConvertedRealSource;

import javax.swing.*;
import java.io.IOException;

public class TestInteractiveAttributeColoring
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
		 * Show interactive table panel
		 * - interactive attribute coloring is available from the table panel menu
		 */

		final JTable jTable = Tests.loadTestTable01();

		Tests.createInteractiveTablePanel( jTable, bdv, argbConverter );
	}

}
