package de.embl.cba.tables.objects.grouping;

import ij.gui.GenericDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class GroupingUI
{
	public static final String NEW_GROUP = "New Group";
	final Grouping grouping;
	private String groupChoice;

	public GroupingUI( Grouping grouping )
	{
		this.grouping = grouping;
		this.groupChoice = null;
	}

	public void assignObjectsToGroup( final Set< Double > objectLabels )
	{
		GenericDialog gd = new GenericDialog( "Choose Group" );
		final ArrayList< Object > groups = new ArrayList<>( grouping.getGroups() );
		groups.add( 0, NEW_GROUP );
		gd.addChoice( "Group", groups.toArray( new String[ groups.size() ]), groupChoice );

		gd.showDialog();
		if ( gd.wasCanceled() ) return;

		groupChoice = gd.getNextChoice();

		if ( groupChoice.equals( NEW_GROUP ) )
		{
			gd = new GenericDialog( "Group name" );
			gd.addStringField( "New group name", "", 15 );
			gd.showDialog();
			if ( gd.wasCanceled() ) return;
			groupChoice = gd.getNextString().trim();
		}

		grouping.assignObjectsToGroup( objectLabels, groupChoice );

	}

}
