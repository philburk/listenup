package com.softsynth.dsp;

/**
 * Compress dynamic range of a signal.
 * 
 * @author (C) 2002 Phil Burk, All Rights Reserved
 */

public class DynamicGainControl extends BufferedSignalProcessor
{

	public final static float DEFAULT_ATTACK_TIME = 0.01f;
	public final static float DEFAULT_DECAY_TIME = 0.1f;
	public final static float DEFAULT_THRESHOLD = 0.2f;
	public final static float DEFAULT_NOISE_THRESHOLD = 0.05f;
	public final static float DEFAULT_CURVATURE = 0.05f;
	private final static float MIN_NOISE_THRESHOLD = DEFAULT_NOISE_THRESHOLD * 0.001f;
	private float sampleRate;
	private PeakFollower follower;
	private ThrustLag lag;
	private float noiseCutoff;
	private float noiseThreshold = DEFAULT_NOISE_THRESHOLD;
	private float threshold;
	private float curvature;
	private float noiseSlope;
	private float normalGain;
	private float attackTime = DEFAULT_ATTACK_TIME;
	private float decayTime = DEFAULT_DECAY_TIME;

	public DynamicGainControl(float sampleRate)
	{
		this.sampleRate = sampleRate;
		follower = new PeakFollower( sampleRate, DEFAULT_DECAY_TIME, 0.01f );
		lag = new ThrustLag();

		setAttackTime( DEFAULT_ATTACK_TIME );
		setDecayTime( DEFAULT_DECAY_TIME );
		setThreshold( DEFAULT_THRESHOLD );
		setNoiseThreshold( DEFAULT_NOISE_THRESHOLD );
		setCurvature( DEFAULT_CURVATURE );
	}

	/* Recalculate scalers used in linear piecewise portions of graph. */
	private void update()
	{
		normalGain = (1.0f + curvature) / (threshold + curvature);
		noiseSlope = (normalGain / noiseThreshold) + noiseCutoff;
	}

	/**
	 * Set how quickly it should respond to a rise in input volume.
	 * 
	 * @param time
	 *            Time in seconds.
	 */
	public void setAttackTime( float time )
	{
		attackTime = time;
		lag.setUpAcceleration( 1.0f, (int) (sampleRate * time) );
		lag.setDownAcceleration( 1.0f, (int) (sampleRate * time) );
	}

	public float getAttackTime()
	{
		return attackTime;
	}

	/**
	 * Set how quickly it should respond to a drop in input volume.
	 * 
	 * @param time
	 *            Time in seconds.
	 */
	public void setDecayTime( float time )
	{
		decayTime = time;
		follower.setHalflife( time );
	}

	public float getDecayTime()
	{
		return decayTime;
	}

	/**
	 * Set level below which a constant gain will be applied. Use higher number
	 * for less compression effect. Note: 1.0 is maximum level.
	 * 
	 * @param threshold
	 *            between 0 and 1
	 */
	public void setThreshold( float threshold )
	{
		this.threshold = threshold;
		update();
	}

	public float getThreshold()
	{
		return threshold;
	}

	/**
	 * Level below which there will be no gain because signal is likely just
	 * noise. Use higher number in noisy environments. Use zero for clean signals.
	 * 
	 * @param noiseThreshold between 0 and 1
	 */
	public void setNoiseThreshold( float pNoiseThreshold )
	{
		if( pNoiseThreshold < MIN_NOISE_THRESHOLD )
		{
			pNoiseThreshold = MIN_NOISE_THRESHOLD;
		}
		noiseThreshold = pNoiseThreshold;
		noiseCutoff = pNoiseThreshold / 8.0f;
		update();
	}

	public float getNoiseThreshold()
	{
		return noiseThreshold;
	}
	/** Controls gain curve. Use 0.0 for full compression where everything above the threshold goes to full volume. Higher curvature results in gradual gain change to allow some difference between loud and soft passages for a more natural result.
	 * 
	 * @param curvature
	 */
	public void setCurvature( float curvature )
	{
		this.curvature = curvature;
		update();
	}

	public float getCurvature()
	{
		return curvature;
	}

	private float nextGain( float input )
	{
		float peakAmplitude = follower.next( input );
		float lagAmplitude = lag.next( peakAmplitude );
		float gain;
		if( lagAmplitude > threshold )
		{
			gain = (1.0f + curvature) / (lagAmplitude + curvature);
		}
		else if( lagAmplitude > noiseThreshold )
		{
			gain = normalGain; // between noiseThreshold and threshold
		}
		else if( lagAmplitude > noiseCutoff )
		{
			gain = (lagAmplitude - noiseCutoff) * noiseSlope;
		}
		else
		{
			gain = 0.0f;
		}
		return gain;
	}

	public void flush()
	{
		super.flush();
		follower.reset();
		lag.reset();
	}

	public void write( float[] data, int offset, int numSamples )
	{
		for( int i = 0; i < numSamples; i++ )
		{
			float sample = data[i + offset];
			float gain = nextGain( sample );
			output( gain * sample );
		}
	}
}
