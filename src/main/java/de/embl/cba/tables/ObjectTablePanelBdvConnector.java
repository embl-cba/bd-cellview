package de.embl.cba.tables;

import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.behaviour.BdvSelectionEventHandler;
import de.embl.cba.bdv.utils.behaviour.SelectionEventListener;
import de.embl.cba.bdv.utils.converters.argb.CategoricalMappingRandomARGBConverter;
import de.embl.cba.bdv.utils.converters.argb.LinearMappingARGBConverter;
import de.embl.cba.bdv.utils.lut.LinearMappingARGBLut;
import de.embl.cba.bdv.utils.lut.Luts;
import de.embl.cba.bdv.utils.lut.StringMappingRandomARGBLut;
import de.embl.cba.tables.objects.ObjectCoordinate;
import de.embl.cba.tables.objects.ObjectTablePanel;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.volatiles.VolatileARGBType;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.TreeMap;
import java.util.function.Function;

public class ObjectTablePanelBdvConnector
{
	final private ObjectTablePanel objectTablePanel;
	final private BdvSelectionEventHandler bdvSelectionEventHandler;
	private final JTable table;
	private final Converter< RealType, VolatileARGBType > originalConverter;

	public ObjectTablePanelBdvConnector( ObjectTablePanel objectTablePanel,
										 BdvSelectionEventHandler bdvSelectionEventHandler )
	{
		this.bdvSelectionEventHandler = bdvSelectionEventHandler;
		this.objectTablePanel = objectTablePanel;
		this.table = objectTablePanel.getTable();

		originalConverter = bdvSelectionEventHandler.getSelectableConverter().getWrappedConverter();

		configureBdvTableListening();

		configureTableBdvListening();

		objectTablePanel.addMenu( createColoringMenuItem() );
	}

	private void configureTableBdvListening()
	{
		bdvSelectionEventHandler.addSelectionEventListener( new SelectionEventListener()
		{
			@Override
			public void valueSelected( double objectLabel )
			{
				final int row = objectTablePanel.getRow( objectLabel );

				table.setRowSelectionInterval( row, row );
			}
		} );
	}

	private void configureBdvTableListening( )
	{
		table.addMouseListener(new MouseInputAdapter()
		{
			public void mousePressed(MouseEvent me)
			{
				if( me.isControlDown() )
				{
					final int row = table.convertRowIndexToModel( table.getSelectedRow() );

					bdvSelectionEventHandler.addSelection( getObjectLabel( row ) );

					objectTablePanel.getObjectCoordinate( ObjectCoordinate.X, row );

					// TODO: move to xy
				}
			}
		});

	}

	public double getObjectLabel( int selectedRow )
	{
		final Number objectLabelNumber = (Number) table.getModel().getValueAt(
				selectedRow,
				table.getColumnModel().getColumnIndex(
						objectTablePanel.getCoordinateColumn( ObjectCoordinate.Label ) ) );

		return objectLabelNumber.doubleValue();
	}

	private JMenu createColoringMenuItem()
	{
		JMenu coloringMenu = new JMenu( "Coloring" );

		coloringMenu.add( getRestoreOriginalColorMenuItem() );

		for ( int col = 0; col < table.getColumnCount(); col++ )
		{
			coloringMenu.add( getColorByColumnMenuItem( table.getColumnName( col ) ) );
		}

		return coloringMenu;
	}

	private JMenuItem getColorByColumnMenuItem( final String colorByColumn )
	{
		final JMenuItem colorByColumnMenuItem = new JMenuItem( "Color by " + colorByColumn );

		colorByColumnMenuItem.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if ( ! objectTablePanel.isCoordinateColumnSet( ObjectCoordinate.Label ) )
				{
					Logger.warn( "Please specify the object label column:\n" +
							"[ Objects > Select coordinates... ]" );
					return;
				}

				final Object firstValueInColumn = table.getValueAt( 0, table.getColumnModel().getColumnIndex( colorByColumn ) );

				Converter< RealType, VolatileARGBType > converter = createSuitableConverter( firstValueInColumn, colorByColumn );

				bdvSelectionEventHandler.getSelectableConverter().setWrappedConverter( converter );

				BdvUtils.repaint( bdvSelectionEventHandler.getBdv() );
			}

		} );

		return colorByColumnMenuItem;
	}

	public Converter< RealType, VolatileARGBType > createSuitableConverter( Object firstValueInColumn, String colorByColumn )
	{
		Converter< RealType, VolatileARGBType > converter = null;

		if ( firstValueInColumn instanceof Number )
		{
			converter = createLinearMappingARGBConverter( colorByColumn );
		}
		else if ( firstValueInColumn instanceof String )
		{
			converter = createCategoricalMappingRandomARGBConverter( colorByColumn );
		}
		else
		{
			Logger.warn( "Column types must be Number or String");
		}

		return converter;
	}

	public LinearMappingARGBConverter createLinearMappingARGBConverter( String selectedColumn )
	{
		final int selectedColumnIndex = objectTablePanel.getTable().getColumnModel().getColumnIndex( selectedColumn );

		final Function< Double, Double > labelColumnMapper = new Function< Double, Double >()
		{
			@Override
			public Double apply( Double objectLabel )
			{
				final int row = objectTablePanel.getRow( objectLabel );
				return (Double) objectTablePanel.getTable().getValueAt( row, selectedColumnIndex );
			};
		};

		final double[] minMaxValues = objectTablePanel.getMinMaxValues( selectedColumn );

		return new LinearMappingARGBConverter(
				labelColumnMapper,
				minMaxValues[ 0 ],
				minMaxValues[ 1 ],
				Luts.BLUE_WHITE_RED );
	}

	public CategoricalMappingRandomARGBConverter createCategoricalMappingRandomARGBConverter( String selectedColumn )
	{
		final int selectedColumnIndex = objectTablePanel.getTable().getColumnModel().getColumnIndex( selectedColumn );

		final Function< Double, Object > labelColumnMapper = new Function< Double, Object >()
		{
			@Override
			public Object apply( Double objectLabel )
			{
				final int row = objectTablePanel.getRow( objectLabel );
				return objectTablePanel.getTable().getValueAt( row, selectedColumnIndex );
			};
		};

		return new CategoricalMappingRandomARGBConverter(
				labelColumnMapper,
				Luts.BLUE_WHITE_RED
		);
	}

	private JMenuItem getRestoreOriginalColorMenuItem()
	{
		final JMenuItem restoreOriginalColorMenuItem = new JMenuItem( "Restore original coloring");

		restoreOriginalColorMenuItem.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				bdvSelectionEventHandler.getSelectableConverter().setWrappedConverter( originalConverter );
				BdvUtils.repaint( bdvSelectionEventHandler.getBdv() );
		}
		} );
		return restoreOriginalColorMenuItem;
	}


}
