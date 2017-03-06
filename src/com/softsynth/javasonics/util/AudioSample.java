package com.softsynth.javasonics.util;
import java.util.*;

/**
 * The AudioSample class is a container for digital audio data.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

public class AudioSample
{
	public static final int UNRECOGNIZED = 0;
	public static final int AIFF = 1;
	public static final int WAV = 2;

	int  numFrames;
	double  sustainBegin = -1;
	double  sustainEnd = -1;
	double  releaseBegin = -1;
	double  releaseEnd = -1;
	Vector  cuePoints;
	int    samplesPerFrame;
	double recordedSampleRate = 44100.0;
	double recordedPitch = 60.0;

	protected short[] shortData;

	public  AudioSample()
	{
	}
	
/**
 * Create a digital audio sample containing the given data.
 * @param samplesPerFrame Number of channels, eg. 1 for mono, 2 for stereo.
 */
	public AudioSample( short samples[], int samplesPerFrame )
	{
		shortData = samples;
		this.samplesPerFrame = samplesPerFrame;
		numFrames = samples.length / samplesPerFrame;
	}

/** Determine file type based on suffix, case insensitive.
 * Returns AudioSample.AIFF for ".aiff", ".aif", "AIFF", or "AIF".
 * Returns AudioSample.WAV for ".wave", ".wav", ".WAVE" or ".WAV".
 * Returns AudioSample.UNRECOGNIZED for anything else.
 * @return integer indicating file type.
 */
	public static int getFileType( String fileName )
	{
		if( fileName.endsWith(".aiff") ||
			fileName.endsWith(".aif") ||
			fileName.endsWith(".AIFF") ||
			fileName.endsWith(".AIF") )
		{
			return AIFF;
		}
		else if( fileName.endsWith(".wave") ||
			fileName.endsWith(".wav") ||
			fileName.endsWith(".WAVE") ||
			fileName.endsWith(".WAV") )
		{
			return WAV;
		}
		else return UNRECOGNIZED;
	}

/**
 * Set rate at which sample was recorded.
 */
	public void setSampleRate( double sampleRate )
	{
		this.recordedSampleRate = sampleRate;
	}
	public double getSampleRate()
	{
		return recordedSampleRate;
	}

	public int getSamplesPerFrame()
	{
		return samplesPerFrame;
	}

	/**
	 * Set pitch of this sound.
	 */
	public void setPitch( double pitch )
	{
		recordedPitch = pitch;
	}
	public double getPitch()
	{
		return recordedPitch;
	}

/* Verbose version of toString. */
	public String dump()
	{
		String str = "Sample";
		str += "\n     numFrames = " + numFrames;
		str += "\n     numChannels = " + samplesPerFrame;
		str += "\n     sampleRate = " + recordedSampleRate;
		if( cuePoints != null )
		{
			str += "\n   Cue Points:";
			for( int k=0; k<cuePoints.size(); k++ )
			{
				CuePoint cue = (CuePoint) cuePoints.elementAt(k);
				str += "\n    " + cue.getID() + ", " + cue.getPosition();
			}
		}
		if( sustainBegin >= 0.0 )
		{
			str += "\n     sustainBegin = " + sustainBegin;
			str += "\n     sustainEnd = " + sustainEnd;
		}
		return str;
	}

/** Get short array containing the samples. */
	public short[] getShorts()
	{
		return shortData;
	}
/**
 * @return Current number of frames of data.
 */
	public int getNumFrames() { return numFrames; }
/**
 * @return Current number of samples of data.
 */
	public int getNumSamples() { return numFrames * samplesPerFrame; }
/**
 * @return Maximum number of frames of data.
 */
	public int getMaxFrames()
	{
		return (shortData == null) ? 0 : (shortData.length / samplesPerFrame) ;
	}

/**
 * Set number of frames of data.
 * Input will be clipped to maxFrames.
 * This is useful when  changing the contents of a sample or envelope.
 */
	public void setNumFrames( int numFrames )
	{
		int maxFrames = getMaxFrames();
		if( numFrames > maxFrames ) numFrames = maxFrames;
		this.numFrames = numFrames;
	}

/**
 *	Set location of Sustain Loop in units of Frames. Set SustainBegin to -1 if no Sustain Loop.
 *  SustainEnd value is the frame index of the frame just past the end of the loop.
 *  The number of frames included in the loop is (SustainEnd - SustainBegin).
 */
	public void setSustainBegin( double startFrame )
	{
		this.sustainBegin = startFrame;
	}
	public void setSustainEnd( double endFrame )
	{
		this.sustainEnd = endFrame;
	}
/***
 * @return Beginning of sustain loop or negative if no loop.
 */
	public double getSustainBegin() { return this.sustainBegin; }
/***
 * @return End of sustain loop or negative if no loop.
 */
	public double getSustainEnd() { return this.sustainEnd; }
/***
 * @return Size of sustain loop in frames, 0 if no loop.
 */
	public double getSustainSize() { return (this.sustainEnd - this.sustainBegin); }


/**
 *	Set location of Release Loop in units of Frames. Set ReleaseBegin to -1 if no ReleaseLoop.
 *  ReleaseEnd value is the frame index of the frame just past the end of the loop.
 *  The number of frames included in the loop is (ReleaseEnd - ReleaseBegin).
 * <br>
 * Why would anyone use a Release Loop?
 * Imagine that you want to build a circuit with a complex LFO type filter
 * modulation that starts when a "note" is released and continues as the sound
 * dies away. To do this, hook a regular envelope with no release loop up to the
 * circuit's amplitude. Then hook another envelope with a release loop up to the circuit's
 * filter cutoff. Also put a sustain point or loop before it so that it doesn't
 * modulate before the note off occurs. Then when you call queueOff() for the
 * two envelopes the sound will fade away because of the first envelope, while
 * being complex modulated by the second envelope with the release loop.
 */
	public void setReleaseBegin( double startFrame )
	{
		this.releaseBegin = startFrame;
	}
	public void setReleaseEnd( double endFrame )
	{
		this.releaseEnd = endFrame;
	}
/***
 * @return Beginning of release loop or negative if no loop.
 */
	public double getReleaseBegin() { return this.releaseBegin; }
/***
 * @return End of release loop or negative if no loop.
 */
	public double getReleaseEnd() { return this.releaseEnd; }
/***
 * @return Size of release loop in frames, 0 if no loop.
 */
	public double getReleaseSize() { return (this.releaseEnd - this.releaseBegin); }

/* Insert CuePoint in order of position. */
	public void insertSortedCue( CuePoint cuePoint )
	{
		if( cuePoints == null ) cuePoints = new Vector();
		int idx = cuePoints.size();
		for( int k=0; k<cuePoints.size(); k++ )
		{
			CuePoint cue = (CuePoint) cuePoints.elementAt(k);
			if( cue.getPosition() > cuePoint.getPosition() )
			{
				idx = k;
				break;
			}
		}
		cuePoints.insertElementAt( cuePoint, idx );
	}

	public CuePoint findCuePoint( int uniqueID )
	{
		if( cuePoints == null ) return null;
		int num = cuePoints.size();
		for( int k=0; k<num; k++ )
		{
			CuePoint cue = (CuePoint) cuePoints.elementAt(k);
			if( cue.getID() == uniqueID )
			{
				return cue;
			}
		}
		return null;
	}

	public int findCuePosition( int uniqueID )
	{
		CuePoint cue = findCuePoint( uniqueID );
		if( cue == null ) return -1;
		else return cue.getPosition();
	}
}
