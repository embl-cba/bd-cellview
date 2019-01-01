package de.embl.cba.tables.objects.ui;

import de.embl.cba.tables.TableUtils;
import de.embl.cba.tables.objects.ObjectCoordinate;
import de.embl.cba.tables.objects.ObjectTablePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;


import static de.embl.cba.tables.SwingUtils.horizontalLayoutPanel;

public class ObjectCoordinateColumnsSelectionUI extends JPanel
{
	private final ObjectTablePanel objectTablePanel;

	private ArrayList< String > choices;
	private JFrame frame;
	private static Point frameLocation;

	public ObjectCoordinateColumnsSelectionUI( ObjectTablePanel objectTablePanel )
	{
		this.objectTablePanel = objectTablePanel;

		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

		initColumnChoices();

		for ( ObjectCoordinate coordinate : ObjectCoordinate.values())
		{
			addColumnSelectionUI( this, coordinate );
		}

		addOKButton();

		showUI();
	}

	private void addOKButton()
	{
		final JButton okButton = new JButton( "OK" );
		okButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				frameLocation = frame.getLocation();
				frame.dispose();
			}
		} );

		add( okButton );
	}

	private void initColumnChoices()
	{
		choices = new ArrayList<>( );
		choices.add( ObjectTablePanel.NO_COLUMN_SELECTED );
		choices.addAll( TableUtils.getColumnNames( objectTablePanel.getTable() ) );
	}

	private void addColumnSelectionUI( final JPanel panel, final ObjectCoordinate coordinate )
	{
		final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( coordinate.toString() ) );

		final JComboBox jComboBox = new JComboBox();
		horizontalLayoutPanel.add( jComboBox );

		for ( String choice : choices )
		{
			jComboBox.addItem( choice );
		}

		// +/- 1 is due to the option to select no column

		jComboBox.setSelectedItem( objectTablePanel.getCoordinateColumn( coordinate ) + 1 );

		jComboBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				objectTablePanel.setCoordinateColumn( coordinate, ( String ) jComboBox.getSelectedItem() );
			}
		} );

		panel.add( horizontalLayoutPanel );
	}


	private void showUI()
	{
		//Create and set up the window.
		frame = new JFrame("Manual editing");

		//Create and set up the content pane.
		this.setOpaque(true); //content panes must be opaque
		frame.setContentPane(this);

		//Display the window.
		frame.pack();
		if ( frameLocation != null ) frame.setLocation( frameLocation );
		frame.setVisible( true );
	}



}
