package de.embl.cba.tables.objects.attributes;

import com.sun.org.apache.bcel.internal.generic.NEW;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.objects.ObjectTablePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class AssignObjectAttributesUI extends JPanel
{
	public static final String NEW_ATTRIBUTE = "None";
	final ObjectTablePanel objectTablePanel;
	Set< Double > objectLabels;
	private JComboBox attributeComboBox;
	private JComboBox columnComboBox;
	private JFrame frame;

	private String selectedColumn;
	private Object selectedAttribute;
	private Set< Object > selectedAttributes;
	private Point location;


	public AssignObjectAttributesUI( ObjectTablePanel objectTablePanel )
	{
		this.objectTablePanel = objectTablePanel;
		selectedAttributes = new HashSet<>();

		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

		final JPanel attributeSelectionUI = createAttributeSelectionUI();
		final JPanel columnSelectionUI = createColumnSelectionUI();
		final JButton okButton = createOkButton();

		this.add( columnSelectionUI );
		this.add( attributeSelectionUI );
		this.add( okButton );
	}

	public void showUI( Set< Double > objectLabels )
	{
		this.objectLabels = objectLabels;
		showFrame();
	}

	private JButton createOkButton()
	{
		final JButton okButton = new JButton( "OK" );
		okButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				selectedColumn = ( String ) columnComboBox.getSelectedItem();
				selectedAttribute = attributeComboBox.getSelectedItem();

				assignAttributes(
						selectedColumn,
						objectLabels,
						selectedAttribute
						);

				updateUIComponents();
				frame.dispose();
			}
		} );
		return okButton;
	}

	public void updateUIComponents()
	{
		location = frame.getLocation();

		if ( ! selectedAttributes.contains( selectedAttribute ) )
		{
			selectedAttributes.add( selectedAttribute );
			attributeComboBox.removeItem( NEW_ATTRIBUTE );
			attributeComboBox.addItem( selectedAttribute );
		}

		columnComboBox.setSelectedItem( selectedColumn );
	}

	private void showFrame()
	{
		this.revalidate();
		this.repaint();
		frame = new JFrame();
		if ( location != null ) frame.setLocation( location );
		frame.add( this );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );
	}

	private JPanel createAttributeSelectionUI()
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "Attribute: " ) );

		attributeComboBox = new JComboBox();
		attributeComboBox.setEditable( true );
		attributeComboBox.addItem( NEW_ATTRIBUTE );

		horizontalLayoutPanel.add( attributeComboBox );

		// TODO: maybe add more items, depending on column

		return horizontalLayoutPanel;
	}

	private JPanel createColumnSelectionUI()
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "Column: " ) );

		columnComboBox = new JComboBox();

		horizontalLayoutPanel.add( columnComboBox );

		for ( String name : objectTablePanel.getColumnNames() )
		{
			columnComboBox.addItem( name );
		}

		if ( selectedColumn != null )
		{
			columnComboBox.setSelectedItem( selectedColumn );
		}

		columnComboBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				// TODO: maybe change content of attributeComboBox
			}
		} );

		return horizontalLayoutPanel;
	}

	private void assignAttributes( final String column, final Set< Double > objectLabels, final Object attribute )
	{
		for ( Double objectLabel : objectLabels )
		{
			assignAttribute( column, objectLabel, attribute );
		}
	}

	private void assignAttribute( String column, Double objectLabel, Object attribute )
	{
//		if ( ! exists( attribute ) ) groups.add( attribute );

		objectTablePanel.getTable().getModel().setValueAt(
				attribute,
				objectTablePanel.getRowIndex( objectLabel ),
				getColumnIndex( column ) );
	}

	private int getColumnIndex( String column )
	{
		return objectTablePanel.getTable().getColumnModel().getColumnIndex( column );
	}
}
