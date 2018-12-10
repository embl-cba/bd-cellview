package de.embl.cba.tables;

import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import ij.ImagePlus;
import ij.gui.PointRoi;
import net.imagej.table.GenericTable;
import net.imglib2.RealPoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static de.embl.cba.tables.Constants.*;


public class InteractiveTablePanel extends JPanel implements MouseListener, KeyListener
{
    final private JTable table;
    final private GenericTable genericTable;

    private JFrame frame;
    private JScrollPane scrollPane;

    private String coordinateColumnX;
    private String coordinateColumnY;
    private String coordinateColumnZ;
    private String coordinateColumnT;

    private ImagePlus imagePlus;
    private Bdv bdv;
    private double bdvZoom;

    public InteractiveTablePanel( GenericTable genericTable )
    {
        super( new GridLayout(1, 0 ) );

        this.genericTable = genericTable;

        table = TableUtils.asJTable( genericTable );
        table.setPreferredScrollableViewportSize( new Dimension(500, 200) );
        table.setFillsViewportHeight( true );
        table.setAutoCreateRowSorter( true );
        table.setRowSelectionAllowed( true );
        table.addMouseListener(this );
        table.addKeyListener(this );

        this.bdvZoom = 10;

        scrollPane = new JScrollPane( table );
        add( scrollPane );
    }

    public void setBdvZoom( double bdvZoom )
    {
        this.bdvZoom = bdvZoom;
    }

    public void setImagePlus( ImagePlus imagePlus )
    {
        this.imagePlus = imagePlus;
    }

    public void setBdv( Bdv bdv ){ this.bdv = bdv; }

    public void setCoordinateColumnX( String coordinateColumnX )
    {
        this.coordinateColumnX = coordinateColumnX;
    }

    public void setCoordinateColumnY( String coordinateColumnY )
    {
        this.coordinateColumnY = coordinateColumnY;
    }

    public void setCoordinateColumnZ( String coordinateColumnZ )
    {
        this.coordinateColumnZ = coordinateColumnZ;
    }

    public void setCoordinateColumnT( String coordinateColumnT )
    {
        this.coordinateColumnT = coordinateColumnT;
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

        if ( imagePlus != null )
        {
            markSelectedObjectInImagePlus( getXYZT( row ) );
        }

        if ( bdv != null )
        {
            BdvUtils.zoomToPosition( bdv, getXYZT( row ), bdvZoom );
        }
    }


    private double[] getXYZT( int row )
    {
        if ( coordinateColumnX != null && coordinateColumnY != null )
        {
            float x = Float.parseFloat( genericTable.get( coordinateColumnX, row ).toString() );
            float y = Float.parseFloat( genericTable.get( coordinateColumnY, row ).toString() );
            int z = 0;
            int t = 0;

            if ( coordinateColumnZ != null )
            {
                z = Integer.parseInt( genericTable.get( coordinateColumnZ, row ).toString() ) - 1;
            }

            if ( coordinateColumnT != null )
            {
                t = Integer.parseInt( genericTable.get( coordinateColumnT, row ).toString() ) - 1;
            }

            return new double[]{ x, y, z, t };
        }
        else
        {
            return null;
        }
    }

    public void markSelectedObjectInImagePlus( double[] xyzt )
    {
        PointRoi pointRoi = new PointRoi( xyzt[ X ], xyzt[ Y ] );
        pointRoi.setPosition( 0, (int) xyzt[ Z ] + 1, (int) xyzt[ T ] + 1 );
        pointRoi.setSize( 4 );
        pointRoi.setStrokeColor( Color.MAGENTA );

        imagePlus.setPosition( 1, (int) xyzt[ Z ] + 1, (int) xyzt[ T ] + 1 );
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
