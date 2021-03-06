package io.openio.sds;

import static io.openio.sds.http.OioHttp.http;

import java.net.InetSocketAddress;
import java.net.URI;

import io.openio.sds.common.SocketProvider;
import io.openio.sds.common.SocketProviders;
import io.openio.sds.http.OioHttp;
import io.openio.sds.http.OioHttpSettings;
import io.openio.sds.pool.PoolingSettings;
import io.openio.sds.proxy.ProxyClient;
import io.openio.sds.storage.ecd.EcdClient;
import io.openio.sds.storage.rawx.RawxClient;

/**
 * Builder for @link {@link Client} implementations
 * 
 * @author Christopher Dedeurwaerder
 * @author Florent Vennetier
 */
public class ClientBuilder {

	/**
	 * Creates a client using the specified settings
	 * 
	 * @param settings
	 *            the settings to use
	 * @return The build {@link Client}
	 */
	public static DefaultClient newClient(Settings settings) {
		OioHttp proxyHttp = http(settings.proxy().http(),
		        proxySocketProvider(settings.proxy().url(),
		                settings.proxy().http(), settings.proxy().pooling()));
		OioHttp rawxHttp = http(settings.rawx().http(),
		        rawxSocketProvider(settings.rawx().http()));
		ProxyClient proxy = new ProxyClient(proxyHttp, settings.proxy());
		RawxClient rawx = new RawxClient(rawxHttp, settings.rawx());
		EcdClient ecd = null == settings.proxy().ecd() 
				? null
				: new EcdClient(rawxHttp, settings.rawx(), settings.proxy().allEcdHosts());
		return new DefaultClient(proxy, rawx, ecd);
	}

	/**
	 * Creates a client without specific configuration. Useful for testing
	 * purpose
	 *
	 * @param ns
	 *            the OpenIO Namespace
	 * @param proxydUrl
	 *            the url of OpenIO proxyd service
	 * @return The build {@link Client}
	 */
	public static DefaultClient newClient(String ns, String proxydUrl) {
		Settings settings = new Settings();
		settings.proxy().url(proxydUrl).ns(ns);
		return newClient(settings);
	}
	
	/**
	 * Creates a client without specific configuration. Useful for testing
	 * purpose
	 *
	 * @param ns
	 *            the OpenIO Namespace
	 * @param proxydUrl
	 *            the url of OpenIO proxyd service
	 * @param ecdUrl 
	 * 			  url of ECD service to manage Erasure Coding
	 * @return The build {@link Client}
	 */
	public static DefaultClient newClient(String ns, 
			String proxydUrl,
			String ecdUrl) {
		Settings settings = new Settings();
		settings.proxy().url(proxydUrl).ns(ns);
		settings.proxy().ecd(ecdUrl);
		return newClient(settings);
	}

	private static SocketProvider proxySocketProvider(String url,
	        final OioHttpSettings http, PoolingSettings pooling) {
		URI uri = URI.create(url);
		InetSocketAddress target = new InetSocketAddress(uri.getHost(),
		        uri.getPort());
		return pooling.enabled()
		        ? SocketProviders.pooledSocketProvider(pooling, http, target)
		        : SocketProviders.directSocketProvider(http);
	}

	private static SocketProvider rawxSocketProvider(
	        final OioHttpSettings http) {
		return SocketProviders.directSocketProvider(http);
	}
}
