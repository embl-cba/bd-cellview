import de.embl.cba.tables.InteractiveTablePanel;

import javax.swing.*;

public class TestInteractiveTable
{
	public static void main( String[] args )
	{


		String[] columnNames = {"First Name", "Last Name","Sport","# of Years"};

		Object[][] data = {
				{"Kathy", "Smith", "Snowboarding", "5"},
				{"John", "Doe", "Rowing", "2"},
				{"Sue", "Black", "Knitting", "8"},
				{"Jane", "White", "Speed reading", "10"},
				{"Joe", "Brown", "Pool", "20"}
		};

		JTable table = new JTable(data, columnNames);

		new InteractiveTablePanel( table );

	}
}
