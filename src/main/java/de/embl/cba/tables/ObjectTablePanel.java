package de.embl.cba.tables;

import bdv.util.Bdv;
import de.embl.cba.bdv.utils.labels.VolatileRealToRandomARGBConverter;
import ij.ImagePlus;
import ij.gui.PointRoi;
import org.scijava.table.GenericTable;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;


public class ObjectTablePanel extends JPanel
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

    public ObjectTablePanel( JTable table )
    {
        super( new GridLayout(1, 0 ) );
        this.table = table;
        init();
    }

    public ObjectTablePanel( String[] columns )
    {
        super( new GridLayout(1, 0 ) );
        DefaultTableModel model = new DefaultTableModel( columns, 0 );
        table = new JTable( model );
        init();
    }

    public ObjectTablePanel( GenericTable genericTable )
    {
        super( new GridLayout(1, 0 ) );
        table = TableUtils.asJTable( genericTable );
        init();
    }

    public void setObjectCoordinateColumn( Coordinate coordinate, String columnName )
    {
        coordinateColumnMap.put( coordinate, columnName );
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

        // TODO: move out of here
        this.bdvZoom = 10;
        this.bdvDurationMillis = 1000;

        initCoordinateColumns();

        initMenus();

    }

    private void initCoordinateColumns()
    {
        this.coordinateColumnMap = new HashMap<>( );

        for ( Coordinate coordinate : Coordinate.values() )
        {
            coordinateColumnMap.put( coordinate, NONE );
        }
    }

    private void initMenus()
    {
        menuBar = new JMenuBar();
        JMenu fileMenu = getFileMenuItem();
        menuBar.add( fileMenu );
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

    public int getSelectedRowIndex()
    {
        return table.convertRowIndexToModel( table.getSelectedRow() );
    }

    public boolean hasObjectCoordinate( Coordinate coordinate )
    {
        if( coordinateColumnMap.get( coordinate ) == NONE ) return false;
        return true;
    }

    public double getObjectCoordinate( Coordinate coordinate, int row )
    {
        if ( coordinateColumnMap.get( coordinate ) != NONE )
        {
            final int columnIndex = table.getColumnModel().getColumnIndex( coordinateColumnMap.get( coordinate ) );
            return ( Double ) table.getValueAt( row, columnIndex );
        }
        else
        {
            return 0;
        }
    }

    /**
     * Informs about row selection.
     *
     * Tip: Use ((ListSelectionEvent)event).getValueIstAdjusting() to avoid multiple notifications.
     *
     * @param listener
     */
    public void addListSelectionListener( ListSelectionListener listener )
    {
        table.getSelectionModel().addListSelectionListener( listener );
    }

}
