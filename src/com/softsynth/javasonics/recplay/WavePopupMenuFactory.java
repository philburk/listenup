package com.softsynth.javasonics.recplay;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WavePopupMenuFactory
{
	PlayerApplet applet;

	public WavePopupMenuFactory(PlayerApplet applet)
	{
		this.applet = applet;
	}

	class WavePopupMenu extends PopupMenu
	{
		Player player;

		public WavePopupMenu(Player player)
		{
			super( "Wave" );
			this.player = player;
			addDeleteSelection();
			addLoadLast();
		}

		void addDeleteSelection()
		{
			// Delete selection
			MenuItem item = new MenuItem( applet.getUserProperty("delete.selection") );
			item.setEnabled( false );
			if( player instanceof JSRecorder )
			{
				if( player.getRecording().isEditable()
						&& (player.getStopIndex() > player.getStartIndex()) )
				{
					item.addActionListener( new ActionListener()
					{
						public void actionPerformed( ActionEvent e )
						{
							((JSRecorder) player).deleteSelectedRange();
						}
					} );
					item.setEnabled( true );
				}
			}
			add( item );
		}

		void addLoadLast()
		{
			// Delete selection
			MenuItem item = new MenuItem( applet.getUserProperty("load.most.recent") );
			item.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					applet.loadMostRecent( );
				}
			} );
			item.setEnabled( applet.hasStashedRecordings() );
			add( item );
		}
	}

	public PopupMenu createMenu( Player player )
	{
		return (PopupMenu) new WavePopupMenu( player );
	}
}
