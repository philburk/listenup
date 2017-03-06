package com.softsynth.ssl;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;

import javax.net.SocketFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLTools
{
	private final static boolean testing = true;

	private static SocketFactory getTrustingSocketFactory()
	{
		SocketFactory factory = null;
		// TODO WARNING - using socket with certificate validation disabled.
		// Create a trust manager that does not validate certificate chains.
		// This may be OK because we are only uploading to the customers website.
		TrustManager[] trustAllCerts =
			new TrustManager[] { new X509TrustManager()
			{ public java.security.cert.X509Certificate[] getAcceptedIssuers()
				{ return null;
				}
				public void checkClientTrusted(
					java.security.cert.X509Certificate[] certs,
					String authType)
				{
				}
				public void checkServerTrusted(
					java.security.cert.X509Certificate[] certs,
					String authType)
				{
				}
			}
		};

		try
		{
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			factory = sc.getSocketFactory();
		} catch (Exception e)
		{
		}
		return factory;
	}
	/** For testing, create a factory that does not validate certificates.
	 * To prevent:
	 *    javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: No trusted certificate found
	 * http://javaalmanac.com/egs/javax.net.ssl/TrustAll.html?l=rel
	 *
	 */
	private static SocketFactory getSocketFactory()
	{
		SocketFactory factory = null;

		if (testing)
		{
			factory = getTrustingSocketFactory();
		} else
		{
			factory = SSLSocketFactory.getDefault();
		}

		return factory;
	}

	/** For testing.
	 * To prevent:
	 *    javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: No trusted certificate found
	 * http://javaalmanac.com/egs/javax.net.ssl/TrustAll.html?l=rel
	 *
	 */
	public static void disableCertificateValidation()
	{
		// Install the all-trusting trust manager
		try
		{
			HttpsURLConnection.setDefaultSSLSocketFactory(
				(SSLSocketFactory) getTrustingSocketFactory());
		} catch (Exception e)
		{
		}
	}
	
	public static Socket createSocket(URL url)
		throws IOException
	{
		// Get the default SSL socket factory
		SocketFactory factory = getSocketFactory();
		// Using socket factory, get SSL socket to port on host
		int port = url.getPort();
		if (port < 0)
			port = url.getDefaultPort();
		Socket skt = factory.createSocket(url.getHost(), port);
		return skt;
	}

}
