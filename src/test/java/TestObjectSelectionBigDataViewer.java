import bdv.util.*;
import de.embl.cba.bdv.utils.transformhandlers.BehaviourTransformEventHandler3DLeftMouseDrag;
import de.embl.cba.tables.ObjectTablePanel;
import de.embl.cba.tables.TableUtils;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.type.volatiles.VolatileARGBType;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestObjectSelectionBigDataViewer
{
	public static void main( String[] args ) throws SpimDataException, IOException
	{
//		SpimData labelData = new XmlIoSpimData().load( new File( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr/em-segmented-cells-labels.xml" ).toString() );
//
//		final VolatileRealToRandomARGBConverter volatileRealToRandomARGBConverter = new VolatileRealToRandomARGBConverter();
//		final ARGBConvertedRealSource ARGBConvertedRealSource = new ARGBConvertedRealSource( labelData, 0, volatileRealToRandomARGBConverter );
//
//		final BdvStackSource< VolatileARGBType > bdvStackSource =
//				BdvFunctions.show( ARGBConvertedRealSource,
//						BdvOptions.options().transformEventHandlerFactory(
//								new BehaviourTransformEventHandler3DLeftMouseDrag.BehaviourTransformEventHandler3DFactory() ) );
//
//		final Bdv bdv = bdvStackSource.getBdvHandle();
//
//		SpimData emData = new XmlIoSpimData().load( new File( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr/em-raw-full-res.xml" ).toString() );
//
//		final List< BdvStackSource< ? > > show = BdvFunctions.show( emData, BdvOptions.options().addTo( bdv ) );
//		show.get( 0 ).setDisplayRange( 0, 255 );
//
//		final JTable jTable = TableUtils.loadTable( new File( "/Users/tischer/Desktop/filtered.csv" ), "\t" );
//
//		final ObjectTablePanel objectTablePanel = new ObjectTablePanel( jTable );
//
//		// TODO: set coordinate columns
//		objectTablePanel.showPanel();
//
//		// TODO: add a row selection listener
//		BdvUtils.zoomToPosition(
//				bdv,
//				new double[]{
//						getObjectCoordinate( Coordinate.X, row ),
//						getObjectCoordinate( Coordinate.Y, row ),
//						getObjectCoordinate( Coordinate.Z, row ),
//						getObjectCoordinate( Coordinate.T, row )},
//				bdvZoom,
//				bdvDurationMillis );
//
//		if ( bdvSourceConverter != null && labelColumn != null )
//		{
//			final HashSet< Double > selectedLabels = new HashSet<>();
//			selectedLabels.add( ( Double ) table.getValueAt( row, labelColumn ) );
//			bdvSourceConverter.setSelectedLabels( new HashSet<>( selectedLabels ) );
//			bdv.getBdvHandle().getViewerPanel().requestRepaint();
//		}


		// TODO: add below code back based on columnNames
//		interactiveTablePanel.setBdv( bdv );
//		interactiveTablePanel.setObjectLabelColumnIndex( 0 );
//		interactiveTablePanel.setBdvSourceConverter( volatileRealToRandomARGBConverter );
//		interactiveTablePanel.setCoordinateColumnIndices( new int[]{ 3, 4, 5} );

	}
}
