package de.embl.cba.tables;

import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.labels.VolatileRealToRandomARGBConverter;
import ij.ImagePlus;
import ij.gui.PointRoi;
import org.scijava.table.GenericTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static de.embl.cba.tables.Constants.*;


public class InteractiveTablePanel extends JPanel implements MouseListener, KeyListener
{
    public static final String NONE = "None";

    final private JTable table;
    private JFrame frame;
    private JScrollPane scrollPane;

    private ImagePlus imagePlus;
    private Bdv bdv;
    private double bdvZoom;
    private JMenuBar menuBar;

    private int bdvDurationMillis;
    private VolatileRealToRandomARGBConverter bdvSourceConverter;
    private Integer labelColumn;

    private HashMap< Coordinate, String > coordinateColumnMap;


    public InteractiveTablePanel( String[] columns )
    {
        super( new GridLayout(1, 0 ) );
        DefaultTableModel model = new DefaultTableModel( columns, 0 );
        table = new JTable( model );
        init();
    }

    public InteractiveTablePanel( GenericTable genericTable )
    {
        super( new GridLayout(1, 0 ) );
        table = TableUtils.asJTable( genericTable );
        init();
    }

    public InteractiveTablePanel( JTable table )
    {
        super( new GridLayout(1, 0 ) );
        this.table = table;
        init();
    }

    public void setCoordinateColumn( Coordinate coordinate, String columnName )
    {
        coordinateColumnMap.put( coordinate, columnName );
    }

    private void init()
    {
        table.setPreferredScrollableViewportSize( new Dimension(500, 200) );
        table.setFillsViewportHeight( true );
        table.setAutoCreateRowSorter( true );
        table.setRowSelectionAllowed( true );
        table.addMouseListener(this );
        table.addKeyListener(this );

        scrollPane = new JScrollPane( table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add( scrollPane );
        table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

        // TODO: move out of here
        this.bdvZoom = 10;
        this.bdvDurationMillis = 1000;

        this.coordinateColumnMap = new HashMap<>( );

        initMenus();

    }

    private void initMenus()
    {
        menuBar = new JMenuBar();
        JMenu fileMenu = getSaveMenuItem();
        menuBar.add( fileMenu );
    }

    private JMenu getSaveMenuItem()
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

    // TODO: Remove to a listener
    public void setBdvZoom( double bdvZoom )
    {
        this.bdvZoom = bdvZoom;
    }

	// TODO: Remove to a listener
	public void setBdvDurationMillis( int bdvDurationMillis )
	{
		this.bdvDurationMillis = bdvDurationMillis;
	}

    // TODO: Remove to a listener
    public void setImagePlus( ImagePlus imagePlus )
    {
        this.imagePlus = imagePlus;
    }

    // TODO: Remove to a listener
    public void setBdv( Bdv bdv )
    {
	    this.bdv = bdv;
    }

    public void setBdvSourceConverter( VolatileRealToRandomARGBConverter sourceConverter )
    {
        this.bdvSourceConverter = sourceConverter;
    }


    public synchronized void addRow( final Object[] row )
    {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(row);
    }

    public void showTable() {

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

    public void reactToRowSelection()
    {
        int row = table.convertRowIndexToModel( table.getSelectedRow() );

        if ( hasXYPosition() )
        {
            if ( imagePlus != null )
            {
                markSelectedObjectInImagePlus(
                        getCoordinate( Coordinate.X, row ),
                        getCoordinate( Coordinate.Y, row ),
                        getCoordinate( Coordinate.Z, row ) + 1,
                        getCoordinate( Coordinate.T, row ) + 1
                        );
            }

            if ( bdv != null )
            {
                BdvUtils.zoomToPosition(
                        bdv,
                        new double[]{
                            getCoordinate( Coordinate.X, row ),
                            getCoordinate( Coordinate.Y, row ),
                            getCoordinate( Coordinate.Z, row ),
                            getCoordinate( Coordinate.T, row )},
                        bdvZoom,
                        bdvDurationMillis );

                if ( bdvSourceConverter != null && labelColumn != null )
                {
                    final HashSet< Double > selectedLabels = new HashSet<>();
                    selectedLabels.add( ( Double ) table.getValueAt( row, labelColumn ) );
                    bdvSourceConverter.setSelectedLabels( new HashSet<>( selectedLabels ) );
                    bdv.getBdvHandle().getViewerPanel().requestRepaint();
                }
            }
        }

    }


    private boolean hasXYPosition()
    {
        return coordinateColumnMap.containsKey( Coordinate.X ) && coordinateColumnMap.containsKey( Coordinate.Y );
    }


    private double getCoordinate( Coordinate coordinate, int row )
    {
        if ( coordinateColumnMap.containsKey( coordinate ) )
        {
            final int columnIndex = table.getColumnModel().getColumnIndex( coordinateColumnMap.get( coordinate ) );
            return ( Double ) table.getValueAt( row, columnIndex );
        }
        else
        {
            return 0;
        }
    }

    // TODO: Remove to a listener
    public void markSelectedObjectInImagePlus( double x, double y, double z, double t )
    {
        PointRoi pointRoi = new PointRoi( x, y );
        pointRoi.setPosition( 0, (int) z, (int) t );
        pointRoi.setSize( 4 );
        pointRoi.setStrokeColor( Color.MAGENTA );

        imagePlus.setPosition( 1, (int) z, (int) t );
        imagePlus.setRoi( pointRoi );
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        reactToRowSelection();
    }

    @Override
    public void mousePressed(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e)
    {
        reactToRowSelection();
    }



}
