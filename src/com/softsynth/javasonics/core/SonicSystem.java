package com.softsynth.javasonics.core;
import com.softsynth.javasonics.AudioDevice;
import com.softsynth.javasonics.DeviceUnavailableException;
import com.softsynth.javasonics.sundev.AudioDeviceSun;

/**
 * Determines the availability of JavaSound and uses it or a native equivalent.
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

public class SonicSystem
{
	public final static int VERSION = 7;
	public final static int NOT_SPECIFIED = -1;
	public final static int SILENT = 0;
	public final static int TERSE = 1;
	public final static int VERBOSE = 2;
	public static int verbosity = SILENT;

	static Class loadClassByName( String name )
	{
		Class cl = null;
		try
		{
			cl = Class.forName( name );
		} catch (Throwable thr) {
            //System.out.println("loadClassByName caught " + thr );
			cl = null;
		}
		return cl;
	}

/** Calls getDevice( true )
 */
	public static AudioDevice getDevice() throws DeviceUnavailableException
	{
		return getDevice( true );
	}

/** If preferNative is true, and the native device is available then it
 *  returns an AudioDevice implemented using a custom native code.
 *  Otherwise it tries to return an AudioDevice implemented using JavaSound.
 *  If neither JavaSound, nor the native device are supported, then it returns null.
 */
	public static AudioDevice getDevice( boolean preferNative ) throws DeviceUnavailableException
	{
		boolean haveJavaSound = isJavaSoundSupported();
		boolean haveNative = isNativeSupported();
		
		if( preferNative )
		{
			if( haveNative ) return getNativeDevice();
			else if( haveJavaSound ) return getJavaSoundDevice();
		}
		else
		{
			if( haveJavaSound ) return getJavaSoundDevice();
			else if( haveNative ) return getNativeDevice();
		}
		return null;
	}

	public static AudioDevice getDevice( boolean preferNative, int minimalVersion ) throws DeviceUnavailableException
	{
		if( preferNative )
		{
			if( isNativeSupported() ) return getNativeDevice( minimalVersion );
			else if( isJavaSoundSupported() ) return getJavaSoundDevice();
		}
		else
		{
			if( isJavaSoundSupported() ) return getJavaSoundDevice();
			else if( isNativeSupported() ) return getNativeDevice( minimalVersion );
		}
		return null;
	}

/** Returns true if JavaSound is supported and not Microsoft Java.
 *  JavaSound is generally available in JDK 1.3 and later JVMs.
 */
	public static boolean isJavaSoundSupported()
	{
		// try to load a simple JavaSound class
		String vendor = System.getProperty("java.vendor");
		if( vendor.indexOf("Microsoft") >= 0 ) return false;
		else return (loadClassByName( "javax.sound.sampled.AudioFormat" ) != null);
	}

/** Returns true if JavaSonic native library or plugin is available.
 */
	public static boolean isNativeSupported()
	{
		// try to load a native plugin class
//		boolean haveAudio = (loadClassByName( "com.softsynth.javasonics.natdev.SonicNativeSystem" ) != null);
//		return haveAudio;
		return false;
	}

/** Returns true if JavaSonic native library or plugin version is the latest version.
 */
	public static boolean isNativeCurrent()
	{
//		if( isNativeSupported() )
//		{
//			return (SonicNativeSystem.getVersion() >= VERSION );
//		}
//		else
		{
			return false;
		}
	}

	public static AudioDevice getNativeDevice() throws DeviceUnavailableException
	{
//		if( verbosity >= TERSE ) System.out.println("Create new AudioDeviceNative");
//        AudioDevice device = SonicNativeSystem.getDevice();
//		return device;
		return null;
	}

	public static AudioDevice getNativeDevice( int minimalVersion ) throws DeviceUnavailableException
	{
//		if( verbosity >= TERSE ) System.out.println("getNativeDevice version " + minimalVersion );
//		if( SonicNativeSystem.getVersion() < minimalVersion )
//		{
//			throw new DeviceUnavailableException( "Upgrade JavaSonics Plugin, need " +
//				 minimalVersion + ", got " + SonicNativeSystem.getVersion() );
//		}
		return getNativeDevice();
	}

	public static AudioDevice getJavaSoundDevice()
	{
		if( verbosity >= TERSE ) System.out.println("JavaSonics using JavaSound library.");
		return new AudioDeviceSun();
	}

    public static int getNativeVersion()
    {
//        if( isNativeSupported() )
//        {
//            return SonicNativeSystem.getVersion();
//        }
//        else
        {
            return 0;
        }
    }
}
