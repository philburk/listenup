package com.softsynth.javasonics.recplay;

public class ReportToJavaScript implements PlayerListener
{
	private boolean requestStateChanges = false;
	private boolean requestTimeUpdates = false;
	private boolean requestLevelUpdates = false;
	private PlayerApplet applet;
	private String previousState = "stopped";
	// Start out false so initial stopped state is reported.
	// It will get set true after that.
	private boolean blockSameStateReports = false;
	private String stateChangeCallback = "LUPJS_StateChanged";
	private String timeChangeCallback = "LUPJS_TimeChanged";
	private String levelChangeCallback = "LUPJS_LevelChanged";

	public ReportToJavaScript(PlayerApplet applet)
	{
		super();
		this.applet = applet;

		requestStateChanges = applet.getBooleanParameter(
				"requestStateChanges", requestStateChanges );
		requestTimeUpdates = applet.getBooleanParameter( "requestTimeChanges",
				requestTimeUpdates );
		requestLevelUpdates = applet.getBooleanParameter(
				"requestLevelChanges", requestLevelUpdates );

		// Allow override of goofy LUPJS_StateChanged callback etc.
		String temp = applet.getParameter( "stateChangeCallback" );
		if( temp != null )
		{
			stateChangeCallback = temp;
		}
		temp = applet.getParameter( "timeChangeCallback" );
		if( temp != null )
		{
			timeChangeCallback = temp;
		}
		temp = applet.getParameter( "levelChangeCallback" );
		if( temp != null )
		{
			levelChangeCallback = temp;
		}
	}

	/**
	 * Enable or disable GUI buttons based on current mode of operation.
	 */
	public synchronized void playerStateChanged( Player player, int state,
			Throwable thr )
	{
		if( requestStateChanges )
		{
			String newState = player.stateToText( state );
			// Allow report of equal states to only be sent the first time.
			if( !blockSameStateReports || !previousState.equals( newState ) )
			{
				String msg = stateChangeCallback + "( '" + previousState
						+ "', '" + newState + "' );";
				previousState = newState;
				applet.executeJavaScriptCommand( msg );
				blockSameStateReports = true;
			}
		}
	}

	public void playerTimeChanged( Player player, double time )
	{
		if( requestTimeUpdates )
		{
			// These updates are not critical so don't send them if the queue is
			// filling up.
			if( applet.getJavaScriptCommandQueueDepth() < 3 )
			{
				String msg = timeChangeCallback + "( " + time + ", "
						+ player.getMaxTime() + " );";
				applet.executeJavaScriptCommand( msg );
			}
		}
	}

	public void playerLevelChanged( Player player )
	{
		if( requestLevelUpdates )
		{
			if( applet.getJavaScriptCommandQueueDepth() < 3 )
			{
				String msg = levelChangeCallback + "( " + player.getLeftLevel()
						+ " );";
				applet.executeJavaScriptCommand( msg );
			}
		}
	}
}
