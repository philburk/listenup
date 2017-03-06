package com.softsynth.upload;

public class Query
{
	public static double getJavaVersion()
	{
		double javaVersion = 1.1;
		String versionString = System.getProperty( "java.version" );
		try
		{
			// strip off any text after second decimal point
			String goodVersionString = versionString;
			int firstPeriod = versionString.indexOf('.');
			if( firstPeriod > 0 )
			{
				int secondPeriod = versionString.indexOf( '.', firstPeriod + 1 );
				if( secondPeriod > 0 )
				{
					goodVersionString = versionString.substring( 0, secondPeriod );
				}
			}
			javaVersion = Double.valueOf( goodVersionString ).doubleValue();
		} catch( NumberFormatException enf ) {
			System.err.println( enf );
		}
		return javaVersion;
	}
}
