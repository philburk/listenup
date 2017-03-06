package com.softsynth.javasonics.error;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

import com.softsynth.xml.XMLWriter;

public class CoreDump
{
	private XMLWriter xmlWriter;
	private ByteArrayOutputStream outputStream;
	private long timestamp;

	public CoreDump()
	{
		this( System.currentTimeMillis() );
	}
	
	public CoreDump(long timestamp)
	{
		this.timestamp = timestamp;
	}

	public void open()
	{
		outputStream = new ByteArrayOutputStream();
		Writer outputStreamWriter = null;
		try
		{
			outputStreamWriter = new OutputStreamWriter( outputStream, "UTF-8" );
		} catch( UnsupportedEncodingException e )
		{
			e.printStackTrace();
		}
		xmlWriter = new XMLWriter( outputStreamWriter );
		xmlWriter.beginTag( "coredump" );
		xmlWriter.writeTag( "date", new Date(timestamp).toString() );
		xmlWriter.writeTag( "time", ""+timestamp );
	}

	public void addThreads()
	{
		xmlWriter.beginTag( "threads" );
		int threadCount = Thread.activeCount();
		Thread[] threads = new Thread[threadCount + 3];
		threadCount = Thread.enumerate( threads );
		for( int i = 0; i < threadCount; i++ )
		{
			addThread( threads[i] );
		}
		xmlWriter.endTag();
	}

	private void addThread( Thread thread )
	{
		xmlWriter.beginTag( "thread" );
		xmlWriter.writeContent( thread.toString() );
		xmlWriter.endTag();
	}
	
	public void addAudio()
	{
		xmlWriter.beginTag( "javasound" );
		Mixer.Info[] mi = AudioSystem.getMixerInfo();
		for( int i = 0; i < mi.length; i++ )
		{
			xmlWriter.beginTag( "mixer" );
			xmlWriter.writeAttribute( "name", mi[i].getName() );
			xmlWriter.writeAttribute( "description", mi[i].getDescription() );
			xmlWriter.writeAttribute( "vendor", mi[i].getVendor() );
			xmlWriter.writeAttribute( "version", mi[i].getVersion() );
			xmlWriter.endTag();
		}
		xmlWriter.endTag();
	}

	public void close()
	{
		xmlWriter.endTag();
	}

	public void addProperties()
	{
		xmlWriter.beginTag( "system" );
		addSystemProperty( "java.version" );
		addSystemProperty( "java.vendor" );
		addSystemProperty( "os.arch" );
		addSystemProperty( "os.name" );
		addSystemProperty( "os.version" );

		xmlWriter.beginTag( "memory" );
		xmlWriter.writeAttribute( "free", Runtime.getRuntime().freeMemory() );
		xmlWriter.writeAttribute( "total", Runtime.getRuntime().totalMemory() );
		xmlWriter.endTag();
		xmlWriter.endTag();
	}

	private void addSystemProperty( String prop )
	{
		xmlWriter.beginTag( "property" );
		xmlWriter.writeAttribute( "name", prop );
		xmlWriter.writeAttribute( "value", System.getProperty( prop ) );
		xmlWriter.endTag();
	}

	public byte[] getBytes()
	{
		return outputStream.toByteArray();
	}

	public XMLWriter getXMLWriter()
	{
		return xmlWriter;
	}

	public void addThrowable( Throwable e )
	{
		String tag = "exception";
		addThrowable( e, tag );
	}

	private void addThrowable( Throwable e, String tag )
	{
		xmlWriter.beginTag( tag );
		xmlWriter.writeAttribute( "class", e.getClass().getName() );
		xmlWriter.writeAttribute( "message", e.getMessage() );

		xmlWriter.beginTag( "stacktrace" );
		xmlWriter.beginContent();
		StackTraceElement[] traces = e.getStackTrace();
		for( int i=0; i<traces.length; i++ )
		{
			xmlWriter.writeTag( "line", traces[i].toString() );
		}
		xmlWriter.endContent();
		xmlWriter.endTag();
		
		if( e.getCause() != null )
		{
			addThrowable( e.getCause(), "cause" );
		}
		
		xmlWriter.endTag();
	}

	public void addApplication( String name, String version, int build )
	{
		xmlWriter.beginTag( "application" );
		xmlWriter.writeAttribute( "name", name );
		xmlWriter.writeAttribute( "version", version );
		xmlWriter.writeAttribute( "build", build );
		xmlWriter.endTag();
	}

	public void addCommon()
	{
		addProperties();
		addAudio();
		addThreads();
	}

	public void writeTag( String tag, String content )
	{
		xmlWriter.writeTag( tag, content );
	}

	public String toString()
	{
		return new String( getBytes() );
	}

}
