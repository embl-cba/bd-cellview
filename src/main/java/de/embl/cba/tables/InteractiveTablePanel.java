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
import java.util.HashSet;

import static de.embl.cba.tables.Constants.*;


public class InteractiveTablePanel extends JPanel implements MouseListener, KeyListener
{
    final private JTable table;

    private JFrame frame;
    private JScrollPane scrollPane;

    private ImagePlus imagePlus;
    private Bdv bdv;
    private double bdvZoom;
    private int[] coordinateColumsXYZT;
    private JMenuBar menuBar;

    private int bdvDurationMillis;
    private VolatileRealToRandomARGBConverter bdvSourceConverter;
    private Integer labelColumn;


    public InteractiveTablePanel( String[] columns )
    {
        DefaultTableModel model = new DefaultTableModel( columns, 0 );
        table = new JTable( model );
        init();
    }


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

        scrollPane = new JScrollPane( table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add( scrollPane );
        table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

        // TODO: move out of here
        this.bdvZoom = 10;
        this.bdvDurationMillis = 1000;

        initMenus();

        showTable();
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
                    TableUtils.saveTable( table );
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

    // TODO: split up again in xy, z, and t
    public void setCoordinateColumnIndices( int[] xyzt )
    {
        this.coordinateColumsXYZT = xyzt;
    }

    public void setObjectLabelColumnIndex( int labelColumn )
    {
        this.labelColumn = labelColumn;
    }

    public boolean hasPosition()
    {
        if ( coordinateColumsXYZT != null ) return true;

        return false;
    }

    private void showTable() {

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

        if ( hasPosition() )
        {
            final double[] xyzt = getPositionXYZT( row );

            if ( imagePlus != null )
            {
                markSelectedObjectInImagePlus( xyzt );
            }

            if ( bdv != null )
            {
                BdvUtils.zoomToPosition( bdv, xyzt, bdvZoom, bdvDurationMillis );

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

    private double[] getPositionXYZT( int row )
    {
        double[] position = new double[ 4 ];

        for ( int d = 0; d < 4; ++d )
        {
            if ( d == 3  && coordinateColumsXYZT.length < 4 )
            {
                position[ d ] = 0; // time column missing
            }
            else
            {
                position[ d ] = Double.parseDouble( table.getValueAt( row, coordinateColumsXYZT[ d ] ).toString() );
            }
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
