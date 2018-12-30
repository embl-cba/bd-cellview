package de.embl.cba.tables;

import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.argbconversion.SelectableRealVolatileARGBConverter;
import de.embl.cba.bdv.utils.lut.ARGBLut;
import de.embl.cba.bdv.utils.lut.LinearMappingARGBLut;
import de.embl.cba.bdv.utils.lut.Luts;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;


public class ObjectTablePanel extends JPanel
{
    public static final Integer NONE = -1;
    final private JTable table;
    private JFrame frame;
    private JScrollPane scrollPane;
    private JMenuBar menuBar;
    private HashMap< ObjectCoordinate, Integer > objectCoordinateColumnIndexMap;

    private Bdv bdv;
	private SelectableRealVolatileARGBConverter selectableConverter;
	private ARGBLut selectableConverterARGBLut;

	public ObjectTablePanel( JTable table )
    {
        super( new GridLayout(1, 0 ) );
        this.table = table;
        init();
    }

	public ObjectTablePanel( JTable table, Bdv bdv, SelectableRealVolatileARGBConverter selectableConverter )
	{
		super( new GridLayout(1, 0 ) );
		this.table = table;
		this.bdv = bdv;
		this.selectableConverter = selectableConverter;
		init();
	}

    public void setObjectCoordinateColumn( ObjectCoordinate objectCoordinate, Integer columnIndex )
    {
        objectCoordinateColumnIndexMap.put( objectCoordinate, columnIndex );
    }

    private void init()
    {
        table.setPreferredScrollableViewportSize( new Dimension(500, 200) );
        table.setFillsViewportHeight( true );
        table.setAutoCreateRowSorter( true );
        table.setRowSelectionAllowed( true );

        scrollPane = new JScrollPane( table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add( scrollPane );
        table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

        initCoordinateColumns();

        initMenus();
    }

    private void initCoordinateColumns()
    {
        this.objectCoordinateColumnIndexMap = new HashMap<>( );

        for ( ObjectCoordinate objectCoordinate : ObjectCoordinate.values() )
        {
            objectCoordinateColumnIndexMap.put( objectCoordinate, NONE );
        }
    }

    private void initMenus()
    {
        menuBar = new JMenuBar();

        JMenu fileMenu = getFileMenuItem();
        menuBar.add( fileMenu );

        if ( ( bdv != null ) & ( selectableConverter != null ) )
		{
			final JMenu coloringMenuItem = getColoringMenuItem();
			menuBar.add( coloringMenuItem );
		}
	}

	private JMenu getColoringMenuItem()
	{
		JMenu coloringMenu = new JMenu( "Coloring" );

		coloringMenu.add( getRestoreOriginalColorMenuItem() );

		for ( int col = 0; col < table.getColumnCount(); col++ )
		{
			coloringMenu.add( getColorByColumnMenuItem( col ) );
		}

		return coloringMenu;
	}

	private JMenuItem getColorByColumnMenuItem( final int col )
	{
		final JMenuItem colorByColumnMenuItem = new JMenuItem( "Color by " + table.getColumnName( col ) );

		colorByColumnMenuItem.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if ( selectableConverter != null
						&& hasObjectCoordinate( ObjectCoordinate.Label ) )
				{

					// TODO: for huge tables this should be implemented more efficiently

					TreeMap< Number, Number > labelValueMap =
							TableUtils.columnsAsTreeMap(
								table,
								objectCoordinateColumnIndexMap.get( ObjectCoordinate.Label ),
								col );

					double min = Double.MAX_VALUE;
					double max = - Double.MAX_VALUE;
					for ( Number value : labelValueMap.values() )
					{
						if ( value.doubleValue() < min ) min = value.doubleValue();
						if ( value.doubleValue() > max ) max = value.doubleValue();
					}

					final LinearMappingARGBLut mappingARGBLut = new LinearMappingARGBLut(
							labelValueMap,
							Luts.BLUE_WHITE_RED_LUT,
							min,
							max
					);

					selectableConverter.setARGBLut( mappingARGBLut );

					BdvUtils.repaint( bdv );

				}
			}
		} );

		return colorByColumnMenuItem;
	}

	private JMenuItem getRestoreOriginalColorMenuItem()
	{
		final JMenuItem restoreOriginalColorMenuItem = new JMenuItem( "Restore original coloring");

		restoreOriginalColorMenuItem.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				selectableConverter.setARGBLut( selectableConverterARGBLut );
			}
		} );
		return restoreOriginalColorMenuItem;
	}


	private JMenu getFileMenuItem()
    {
        JMenu fileMenu = new JMenu( "File" );
        final JMenuItem saveMenuItem = new JMenuItem( "Save as..." );
        saveMenuItem.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                try
                {
                    TableUtils.saveTableUI( table );
                }
                catch ( IOException e1 )
                {
                    e1.printStackTrace();
                }
            }
        } );
        fileMenu.add( saveMenuItem );
        return fileMenu;
    }

    public void showPanel() {

        //Create and set up the window.
        frame = new JFrame("Table");

        frame.setJMenuBar( menuBar );

        //Show the table
        //frame.add( scrollPane );

        //Create and set up the content pane.
        this.setOpaque(true); //content panes must be opaque
        frame.setContentPane(this);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

//    // TODO: Remove to a listener
//    public void markSelectedObjectInImagePlus( double x, double y, double z, double t )
//    {
//        PointRoi pointRoi = new PointRoi( x, y );
//        pointRoi.setPosition( 0, (int) z, (int) t );
//        pointRoi.setSize( 4 );
//        pointRoi.setStrokeColor( Color.MAGENTA );
//
//        imagePlus.setPosition( 1, (int) z, (int) t );
//        imagePlus.setRoi( pointRoi );
//    }

    public int getSelectedRowIndex()
    {
        return table.convertRowIndexToModel( table.getSelectedRow() );
    }

    public boolean hasObjectCoordinate( ObjectCoordinate objectCoordinate )
    {
        if( objectCoordinateColumnIndexMap.get( objectCoordinate ) == NONE ) return false;
        return true;
    }

    public double getObjectCoordinate( ObjectCoordinate objectCoordinate, int row )
    {
        if ( objectCoordinateColumnIndexMap.get( objectCoordinate ) != NONE )
        {
            final int columnIndex = table.getColumnModel().getColumnIndex( objectCoordinateColumnIndexMap.get( objectCoordinate ) );
            return ( Double ) table.getValueAt( row, columnIndex );
        }
        else
        {
            return 0;
        }
    }

    public DefaultTableModel getTableModel()
    {
        return ( DefaultTableModel ) table.getModel();
    }

}
