package de.embl.cba.tables;

import ij.ImagePlus;
import ij.gui.PointRoi;
import net.imagej.table.GenericTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


public class InteractiveTablePanel extends JPanel implements MouseListener, KeyListener
{
    final private JTable table;
    final private GenericTable genericTable;

    private JFrame frame;
    private JScrollPane scrollPane;
    private ImagePlus imagePlus;
    private String coordinateColumnX;
    private String coordinateColumnY;
    private String coordinateColumnZ;
    private String coordinateColumnT;

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

        scrollPane = new JScrollPane( table );
        add( scrollPane );
    }

    public void setImagePlus( ImagePlus imagePlus )
    {
        this.imagePlus = imagePlus;
    }

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

        if ( imagePlus != null && coordinateColumnX != null && coordinateColumnY != null )
        {
            float x = Float.parseFloat( genericTable.get( coordinateColumnX, row ).toString() );
            float y = Float.parseFloat( genericTable.get( coordinateColumnY, row ).toString() );
            int slice = 1;
            int frame = 1;

            if ( coordinateColumnZ != null )
            {
                slice = Integer.parseInt( genericTable.get( coordinateColumnZ, row ).toString() );
            }

            if ( coordinateColumnT != null )
            {
                frame = Integer.parseInt( genericTable.get( coordinateColumnT, row ).toString() );
            }

            imagePlus.setPosition( 1, slice, frame );

            markSelectedObject( x, y, slice, frame );
        }
    }

    public void markSelectedObject( float x, float y, int slice, int frame )
    {
        PointRoi pointRoi = new PointRoi( x, y );
        pointRoi.setSize( 4 );
        pointRoi.setStrokeColor( Color.MAGENTA );
        pointRoi.setPosition( 0, slice, frame );
        imagePlus.setRoi( pointRoi );
    }


    @Override
    public void mouseClicked(MouseEvent e)
    {
        reactToRowSelection();
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        reactToRowSelection();
    }
}
