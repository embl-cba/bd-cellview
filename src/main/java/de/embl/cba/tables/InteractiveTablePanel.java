package de.embl.cba.tables;

import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import ij.ImagePlus;
import ij.gui.PointRoi;
import net.imagej.table.GenericTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static de.embl.cba.tables.Constants.*;


public class InteractiveTablePanel extends JPanel implements MouseListener, KeyListener
{
    final private JTable table;

    private JFrame frame;
    private JScrollPane scrollPane;
    
    private ImagePlus imagePlus;
    private Bdv bdv;
    private double bdvZoom;
    private int[] coordinateColumnIndices;
    private String[] coordinateColumns;
    private boolean[] hasCoordinate;

    public InteractiveTablePanel( JTable table )
    {
        super( new GridLayout(1, 0 ) );
        this.table = table;
        init();
    }

    public InteractiveTablePanel( GenericTable genericTable )
    {
        super( new GridLayout(1, 0 ) );
        table = TableUtils.asJTable( genericTable );
        init();
    }

    private void init()
    {
        table.setPreferredScrollableViewportSize( new Dimension(500, 200) );
        table.setFillsViewportHeight( true );
        table.setAutoCreateRowSorter( true );
        table.setRowSelectionAllowed( true );
        table.addMouseListener(this );
        table.addKeyListener(this );

        scrollPane = new JScrollPane( table );
        add( scrollPane );

        coordinateColumnIndices = new int[ 4 ];
        coordinateColumns = new String[ 4 ];
        hasCoordinate = new boolean[ 4 ];

        this.bdvZoom = 10;

        addMenu();
    }

    private void addMenu()
    {
        final JMenu fileMenu = new JMenu( "File" );
        final JMenuItem saveMenuItem = new JMenuItem( "Save as..." );
        saveMenuItem.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                TableUtils.saveTable( table );
            }
        } );
        fileMenu.add( saveMenuItem );
    }

    // TODO: Remove to a listener
    public void setBdvZoom( double bdvZoom )
    {
        this.bdvZoom = bdvZoom;
    }

    // TODO: Remove to a listener
    public void setImagePlus( ImagePlus imagePlus )
    {
        this.imagePlus = imagePlus;
    }

    // TODO: Remove to a listener
    public void setBdv( Bdv bdv ){ this.bdv = bdv; }

    public void setCoordinateColumn( String coordinateColumn, int xyzt )
    {
        this.coordinateColumns[ xyzt ] = coordinateColumn;
        this.coordinateColumnIndices[ xyzt ] = table.getColumn( coordinateColumn ).getModelIndex();
        this.hasCoordinate[ xyzt ] = true;
    }

    public boolean hasXYZT()
    {
        for ( int d = 0; d < 4; ++d )
        {
            if ( hasCoordinate[ d ] == false ) return false;
        }

        return true;
    }

    public void showTable() {

        //Create and set up the window.
        frame = new JFrame("Table");

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

        if ( hasXYZT() )
        {
            final double[] xyzt = getXYZT( row );

            if ( imagePlus != null )
            {
                markSelectedObjectInImagePlus( xyzt );
            }

            if ( bdv != null )
            {
                BdvUtils.zoomToPosition( bdv, xyzt, bdvZoom );
            }
        }

    }

    private double[] getXYZT( int row )
    {
        double[] position = new double[ 4 ];

        for ( int d = 0; d < 4; ++d )
        {
            position[ d ] = Float.parseFloat( table.getValueAt( row, coordinateColumnIndices[ d ] ).toString() );
        }

        return position;
    }

    // TODO: Remove to a listener
    public void markSelectedObjectInImagePlus( double[] xyzt )
    {
        PointRoi pointRoi = new PointRoi( xyzt[ X ], xyzt[ Y ] );
        pointRoi.setPosition( 0, (int) xyzt[ Z ], (int) xyzt[ T ] );
        pointRoi.setSize( 4 );
        pointRoi.setStrokeColor( Color.MAGENTA );

        imagePlus.setPosition( 1, (int) xyzt[ Z ], (int) xyzt[ T ] );
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
