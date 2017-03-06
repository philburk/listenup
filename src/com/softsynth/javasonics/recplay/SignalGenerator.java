package com.softsynth.javasonics.recplay;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Generate test signals for debugging.
 * 
 * @author Phil Burk (C) 2006 Mobileer Inc
 */
public class SignalGenerator
{
	double frameRate = 22050.0;
	Vector oscillators;

	public SignalGenerator(String spec)
	{
		oscillators = new Vector();
		if( spec != null )
		{
			parseSpec( spec );
		}
	}

	class SineOscillator
	{
		double frequency;
		double amplitude;
		double phase;
		double scaler;

		public SineOscillator(double frequency, double amplitude)
		{
			this.frequency = frequency;
			this.amplitude = amplitude;
			phase = 0.0;
			scaler = amplitude * 32768.0;
		}

		void mix( short[] buffer, int numSamples )
		{
			double phaseIncrement = frequency * Math.PI * 2.0 / frameRate;
			// Generate fake audio data to test long recordings.
			for( int i = 0; i < numSamples; i++ )
			{
				phase += phaseIncrement;
				buffer[i] += (short) (Math.sin( phase ) * scaler);
			}
			// wrap back so we maintain precision
			phase = phase % (Math.PI * 2.0);
		}
	}

	private void parseSpec( String spec )
	{
		// Parse comma delimited string
		StringTokenizer parser = new StringTokenizer( spec, "," );

		String method = (String) parser.nextElement();
		if( !method.equals( "sines" ) )
		{
			throw new RuntimeException( "Illegal method for testSignalSpec = "
					+ method + ", expected 'sines'" );
		}

		// Parse frequency,amplitude pairs and create oscillators.
		while( parser.hasMoreElements() )
		{
			String frequencyText = (String) parser.nextElement();
			String amplitudeText = (String) parser.nextElement();
			double frequency = Double.valueOf( frequencyText ).doubleValue();
			double amplitude = Double.valueOf( amplitudeText ).doubleValue();
			oscillators.addElement( new SineOscillator( frequency, amplitude ) );
		}
	}

	// Mix each oscillator into the buffer.
	protected void generate( short[] buffer, int numSamples )
	{
		// Clear buffer.
		for( int i = 0; i < numSamples; i++ )
		{
			buffer[i] = 0;
		}
		// Mix in each oscillator.
		Enumeration oscenum = oscillators.elements();
		while( oscenum.hasMoreElements() )
		{
			SineOscillator osc = (SineOscillator) oscenum.nextElement();
			osc.mix( buffer, numSamples );
		}
	}

	/**
	 * @return Returns the frameRate.
	 */
	public double getFrameRate()
	{
		return frameRate;
	}

	/**
	 * @param frameRate
	 *            The frameRate to set.
	 */
	public void setFrameRate( double frameRate )
	{
		this.frameRate = frameRate;
	}

}
