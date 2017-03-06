/*
 * Created on May 11, 2004
 *
 */
package com.softsynth.javasonics.recplay;

/**
 * Use JSClassLoader.load(classname) instead of Class.forName(classname). <br>
 * JSClassLoader.load() first tries Class.forName(). If this fails, it tries
 * Class.forName() using ClassLoader.getSystemClassLoader() This was developed
 * so that jmsl.jar residing in ~/Library/Java/Extensions on Mac OSX could
 * instantiate classes by name which were outside of this location. Classes in
 * ~/Library/Java/Extensions are loaded by a different ClassLoader than the main
 * Java application.
 * 
 * @author Nick Didkovsky, email: didkovn@mail.rockefeller.edu, (c) 2004 Nick
 *         Didkovsky, all rights reserved.
 *  
 */
public class JSClassLoader
{

    /**
     * First try Class.forName(). If this fails, try Class.forName() using
     * ClassLoader.getSystemClassLoader(). if isApplet, only try Class.forName()
     * else you'll get security exception.
     * 
     * @return Class or null
     */
    public static Class load( String classname, boolean isApplet )
            throws ClassNotFoundException
    {
        //System.out.println("JMSLClassLoader " + classname);
        if( isApplet )
        {
            return Class.forName( classname );
        }

        Class newClass = null;
        try
        {
            //				System.out.println("trying Class.forName()");
            newClass = Class.forName( classname );
            //				System.out.println("Class.forName() succeeded");

        } catch( ClassNotFoundException e )
        {
            // e.printStackTrace();
            System.err.println( "JSClassLoader, Class.forName( " + classname
                    + ") failed, trying System classloader" );
        }
        if( newClass == null )
        {
            System.out.println( "trying System ClassLoader" );
            ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
            //				System.out.println("got System classloader");
            newClass = Class.forName( classname, true, systemLoader );
            //			System.err.println("System classloader succeeded");
        }
        return newClass;
    }

}