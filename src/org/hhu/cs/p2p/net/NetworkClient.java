package org.hhu.cs.p2p.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Path;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

/**
 * Http client to download files.
 * 
 * @author Oliver Schrenk <oliver.schrenk@uni-duesseldorf.de>
 * 
 */
public class NetworkClient {

	private static final String HTTP_SCHEME = "http";

	private static final String NULL_QUERY = null;

	private static final String NULL_FRAGMENT = null;

	private static final Logger logger = Logger.getLogger(NetworkClient.class);

	private Path rootDirectory;
	private HttpClient httpClient;

	/**
	 * Creates a new {@link NetworkClient}
	 * 
	 * @param rootDirectory
	 *            as a base
	 */
	public NetworkClient(Path rootDirectory) {
		this.rootDirectory = rootDirectory;
		this.httpClient = new DefaultHttpClient();
		logger.info(String.format("Created new HttpClient on root \"%1s\"",
				rootDirectory));
	}

	/**
	 * Requests a file from a server under the given adress/path
	 * 
	 * @param socketAdress
	 * @param relativePath
	 *            relative path to the file
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void request(InetSocketAddress socketAdress, Path relativePath)
			throws ClientProtocolException, IOException, URISyntaxException {
		request(getURI(socketAdress, relativePath));

	}

	private void request(URI uri) throws ClientProtocolException, IOException {
		logger.info(String.format("Requesting %1s", uri));
		HttpGet get = new HttpGet(uri);
		httpClient.execute(get, new FileResponseHandler(rootDirectory, uri));
	}

	@Override
	protected void finalize() throws Throwable {
		httpClient.getConnectionManager().shutdown();
		super.finalize();
	}

	private URI getURI(InetSocketAddress socketAdress, Path path)
			throws URISyntaxException {
		try {
			return URIUtils.createURI(HTTP_SCHEME, socketAdress.getAddress()
					.getHostAddress(), socketAdress.getPort(),
					getURLEncodedPath(path), NULL_QUERY, NULL_FRAGMENT);
		} catch (UnsupportedEncodingException e) {
			logger.fatal("Encodig not found. Shouldn't happen", e);
		}
		return null;
	}

	private String getURLEncodedPath(Path path)
			throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		int nameCount = path.getNameCount();
		for (int i = 0; i < nameCount; i++) {
			sb.append(URLEncoder.encode(path.getName(i).toString(), "UTF-8"));
			if (i != nameCount - 1)
				sb.append('/');
		}

		return sb.toString();
	}

}
