/**
 * 
 */
package com.fanfou.app.hd.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import com.fanfou.app.hd.App;
import com.fanfou.app.hd.App.ApnType;

/**
 * @author mcxiaoke
 * @version 1.0 2011.05.05
 * @version 2.0 2011.10.29
 * 
 */
public final class NetworkHelper {

	public static final int SOCKET_BUFFER_SIZE = 2048;
	public static final int CONNECTION_TIMEOUT_MS = 5000;
	public static final int SOCKET_TIMEOUT_MS = 15000;

	public static HttpURLConnection newHttpURLConnection(ApnType apnType,
			String url) throws IOException {
		HttpURLConnection conn = null;
		URL serverUrl;
		String domain = getDomain(url);
		if (apnType == ApnType.WAP) {
			url = "http://10.0.0.172:80/" + getUrlNoDomain(url);
			serverUrl = new URL(url);
			conn = (HttpURLConnection) serverUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("X-Online-Host", domain);
			conn.setRequestProperty("Host", domain);
		} else {
			serverUrl = new URL(url);
			conn = (HttpURLConnection) serverUrl.openConnection();
		}
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "*/*");
		conn.setConnectTimeout(20 * 1000);
		conn.setRequestProperty("Accept-Language", "zh-CN");
		conn.setRequestProperty("Charset", "UTF-8");
		// conn.setRequestProperty("User-Agent", mContext
		// .getString(R.string.userAgent));
		conn.setDoInput(true);
		conn.setDoOutput(true);
		return conn;
	}

	/**
	 * @param url
	 *            链接地址
	 * @return URL的域名部分
	 */
	public static String getDomain(String url) {
		String domain = "";
		if ((url == null) || ("".equals(url))) {
			return "";
		} else {
			domain = url.replaceAll("http://", "");
			domain = domain.substring(0, domain.indexOf('/'));
			return domain;
		}
	}

	/**
	 * @param url
	 *            链接地址
	 * @return URL的相对链接地址(不包含域名)
	 */
	public static String getUrlNoDomain(String url) {
		String domain = "";
		if ((url == null) || ("".equals(url))) {
			return "";
		} else {
			domain = url.replaceAll("http://", "");
			domain = domain.substring(domain.indexOf('/') + 1);
			return domain;
		}
	}

	/**
	 * Build and return a user-agent string that can identify this application
	 * to remote servers. Contains the package name and version code.
	 */
	@SuppressWarnings("unused")
	private static String buildUserAgent(Context context) {
		try {
			final PackageManager manager = context.getPackageManager();
			final PackageInfo info = manager.getPackageInfo(
					context.getPackageName(), 0);

			// Some APIs require "(gzip)" in the user-agent string.
			return info.packageName + "/" + info.versionName + " ("
					+ info.versionCode + ") (gzip)";
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	public static void setProxy(final HttpParams params, final ApnType type) {
		if (type == ApnType.WAP) {
			if (App.DEBUG) {
				Log.d("setProxy", "set proxy for wap");
			}
			params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(
					"10.0.0.172", 80));
		} else {
			if (App.DEBUG) {
				Log.d("setProxy", "set no proxy");
			}
			params.removeParameter(ConnRoutePNames.DEFAULT_PROXY);
		}
	}

	public static void setProxy(final HttpClient client) {
		if (client == null) {
			return;
		}
		HttpParams params = client.getParams();
		ApnType type = App.getApnType();
		if (type == ApnType.WAP) {
			if (App.DEBUG) {
				Log.d("setProxy", "set proxy for wap");
			}
			params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(
					"10.0.0.172", 80));
		} else {
			if (App.DEBUG) {
				Log.d("setProxy", "set no proxy");
			}
			params.removeParameter(ConnRoutePNames.DEFAULT_PROXY);
		}
	}

	public static ApnType getApnType(Context context) {
		ApnType type = ApnType.NET;
		try {
			ConnectivityManager cm = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = cm.getActiveNetworkInfo();
			if (App.DEBUG) {
				Log.d("App", "NetworkInfo: " + info);
			}
			if (info != null && info.isConnectedOrConnecting()) {
//				App.noConnection = false;
				if (info.getType() == ConnectivityManager.TYPE_WIFI) {
					type = ApnType.WIFI;
				} else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
					String apnTypeName = info.getExtraInfo();
					if (!TextUtils.isEmpty(apnTypeName)) {
						if (apnTypeName.equals("3gnet")) {
							type = ApnType.HSDPA;
						} else if (apnTypeName.contains("wap")) {
							type = ApnType.WAP;
						}
					}
				}
			} else {
//				App.noConnection = true;
			}
		} catch (Exception e) {
			if (App.DEBUG) {
				Log.d("NetworkHelper", e.toString());
			}
		}
		return type;
	}

	static DefaultHttpClient newHttpClient() {
		ConnPerRoute connPerRoute = new ConnPerRoute() {
			@Override
			public int getMaxForRoute(HttpRoute route) {
				return 10;
			}
		};
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		// ConnManagerParams.setTimeout(params, CONNECTION_TIMEOUT_MS);
		ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
		HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
		HttpConnectionParams
				.setConnectionTimeout(params, CONNECTION_TIMEOUT_MS);
		HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT_MS);
		HttpConnectionParams.setSocketBufferSize(params, SOCKET_BUFFER_SIZE);

		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		schReg.register(new Scheme("https",
				SSLSocketFactory.getSocketFactory(), 443));
		ClientConnectionManager manager = new ThreadSafeClientConnManager(
				params, schReg);
		DefaultHttpClient client = new DefaultHttpClient(manager, params);
		// client.addResponseInterceptor(new GzipResponseInterceptor());
		// client.setHttpRequestRetryHandler(new RequestRetryHandler(3));
		return client;
	}

}