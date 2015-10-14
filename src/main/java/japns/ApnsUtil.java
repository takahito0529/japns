package japns;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APNsユーティリティ
 * @author T.Inukai
 */
public class ApnsUtil {
	private static Logger logger = LoggerFactory.getLogger(ApnsUtil.class);

	/** APNs証明書のキータイプ */
	private static final String KEYSTORE_TYPE = "PKCS12";
	/** APNs証明書のキーアルゴリズム */
	private static final String KEY_ALGORITHM = "sunx509";

	/** APNS Sandboxゲートウェイホスト */
	public static final String SANDBOX_GATEWAY_HOST = "gateway.sandbox.push.apple.com";
	/** APNS Sandboxゲートウェイポート */
	public static final int SANDBOX_GATEWAY_PORT = 2195;

	/** APNS Sandboxフィードバックホスト */
	public static final String SANDBOX_FEEDBACK_HOST = "feedback.sandbox.push.apple.com";
	/** APNS Sandboxフィードバックポート */
	public static final int SANDBOX_FEEDBACK_PORT = 2196;

	/** APNS Productionゲートウェイホスト */
	public static final String PRODUCTION_GATEWAY_HOST = "gateway.push.apple.com";
	/** APNS Productionゲートウェイポート */
	public static final int PRODUCTION_GATEWAY_PORT = 2195;

	/** APNS Productionフィードバックホスト */
	public static final String PRODUCTION_FEEDBACK_HOST = "feedback.push.apple.com";
	/** APNS Productionフィードバックポート */
	public static final int PRODUCTION_FEEDBACK_PORT = 2196;

	/**
	 * スリープ処理
	 * @param millis スリープする時間（ミリ秒）
	 */
	public static void sleep(long millis) {
		try {
			if (millis == 0) {
				return;
			}
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Byte配列をintに変換
	 * @param b1 1バイト目
	 * @param b2 2バイト目
	 * @param b3 3バイト目
	 * @param b4 4バイト目
	 * @return 変換されたint値
	 */
	public static int parseBytesToInt(byte b1, byte b2, byte b3, byte b4) {
		int intValue = 0;
		intValue = (intValue << 8) + (b1 & 0xff);
		intValue = (intValue << 8) + (b2 & 0xff);
		intValue = (intValue << 8) + (b3 & 0xff);
		intValue = (intValue << 8) + (b4 & 0xff);
		return intValue;
	}

	/**
	 * 16進文字列のbyte配列への変換
	 * @param s 16進文字列
	 * @return 変換されたbyte配列
	 */
	public static byte[] convertHexToBytes(String s) {
		String hex = pattern.matcher(s).replaceAll("");

		byte[] bts = new byte[hex.length() / 2];
		for (int i = 0; i < bts.length; i++) {
			bts[i] = (byte) (charval(hex.charAt(2 * i)) * 16 + charval(hex.charAt(2 * i + 1)));
		}
		return bts;
	}

	private static final Pattern pattern = Pattern.compile("[ -]");

	private static int charval(char a) {
		if ('0' <= a && a <= '9') {
			return (a - '0');
		} else if ('a' <= a && a <= 'f') {
			return (a - 'a') + 10;
		} else if ('A' <= a && a <= 'F') {
			return (a - 'A') + 10;
		} else {
			throw new RuntimeException("Invalid hex character: " + a);
		}
	}

	/**
	 * byte配列から16進文字列への変換
	 * @param bytes 変換するbyte配列
	 * @return 変換後の文字列
	 */
	public static String convertBytesToHex(final byte[] bytes) {
		final char[] chars = new char[bytes.length * 2];

		for (int i = 0; i < bytes.length; ++i) {
			final int b = (bytes[i]) & 0xFF;
			chars[2 * i] = base[b >>> 4];
			chars[2 * i + 1] = base[b & 0xF];
		}

		return new String(chars);
	}

	private static final char base[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * 文字列をUTF8のByte配列に変換
	 * @param s 変換する文字列
	 * @return 変換後のbyte配列
	 */
	public static byte[] convertStringToUTF8Bytes(String s) {
		try {
			return s.getBytes("UTF-8");
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Listオブジェクトを分割する
	 *
	 * <p>
	 * {@code lst}で指定したリストを{@code count}で指定した分割数で順々に振り分けます。<br>
	 * 要素数10のリストを3分割する場合、返却されるリストの各要素は、<br>
	 * [0] -> [0][3][6][9]<br>
	 * [1] -> [1][4][7]<br>
	 * [2] -> [2][5][8]<br>
	 * となります。<br>
	 * </p>
	 *
	 * @param lst 分割元のリスト
	 * @param count 分割数
	 * @return 分割されたListオブジェクト
	 */
	public synchronized static <T> List<List<T>> splitList(List<T> lst, int count) {
		// 分割元のリストが分割数より少ない場合
		if (lst.size() < count) {
			// 分割数をリストサイズに調整
			count = lst.size();
		}

		// 結果リストのインスタンスを生成
		List<List<T>> result = new ArrayList<List<T>>();
		for (int i = 0; i < count; i++) {
			List<T> l = new ArrayList<T>();
			result.add(l);
		}

		// 結果リストにデータセット
		for (int i = 0; i < lst.size(); i++) {
			result.get(i % count).add(lst.get(i));
		}
		return result;
	}

	/**
	 * APNs通信用SSLSocketFactoryの取得
	 * @param certFileName 証明書ファイル名
	 * @param certPassword 証明書パスワード
	 * @return 生成された{@link SSLSocketFactory}オブジェクト
	 */
	public static SSLSocketFactory getSSLSocketFactory(String certFileName, String certPassword) {
		InputStream is = null;
		try {
			is = new FileInputStream(certFileName);
			KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
			ks.load(is, certPassword.toCharArray());

			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_ALGORITHM);
			kmf.init(ks, certPassword.toCharArray());

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_ALGORITHM);
			tmf.init((KeyStore) null);

			SSLContext sslc = SSLContext.getInstance("TLS");
			sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			return sslc.getSocketFactory();

		} catch (Exception e) {
			logger.error("SSLSocketFactoryの生成に失敗しました。", e);
			throw new ApnsException(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Push通知ゲートウェイ接続用Socket生成
	 * @param isProduction productionホストに接続するかどうか
	 * @param factory SSL通信用{@link SSLSocketFactory}。{@link #getSSLSocketFactory(String, String)}で生成したものを設定。
	 * @param socksProxy SOCKSプロキシの{@link Proxy}オブジェクト。SOCKS以外のタイプのプロキシを指定した場合は{@link ApnsException}がスローされる
	 * @return Push通知ゲートウェイ接続用Socket
	 */
	public static Socket createAPNSGatewaySocket(boolean isProduction, SSLSocketFactory factory, Proxy socksProxy) {
		String host;
		int port;
		if (isProduction) {
			host = PRODUCTION_GATEWAY_HOST;
			port = PRODUCTION_GATEWAY_PORT;
		} else {
			host = SANDBOX_GATEWAY_HOST;
			port = SANDBOX_GATEWAY_PORT;
		}
		return createSocket(factory, socksProxy, host, port);
	}

	/**
	 * Push通知フィードバック接続用Socket生成
	 * @param isProduction productionホストに接続するかどうか
	 * @param factory SSL通信用{@link SSLSocketFactory}。{@link #getSSLSocketFactory(String, String)}で生成したものを設定。
	 * @param socksProxy SOCKSプロキシの{@link Proxy}オブジェクト。SOCKS以外のタイプのプロキシを指定した場合は{@link ApnsException}がスローされる
	 * @return Push通知フィードバック接続用Socket
	 */
	public static Socket createAPNSFeedbackSocket(boolean isProduction, SSLSocketFactory factory, Proxy socksProxy) {
		String host;
		int port;
		if (isProduction) {
			host = PRODUCTION_FEEDBACK_HOST;
			port = PRODUCTION_FEEDBACK_PORT;
		} else {
			host = SANDBOX_FEEDBACK_HOST;
			port = SANDBOX_FEEDBACK_PORT;
		}
		return createSocket(factory, socksProxy, host, port);
	}

	/**
	 * Socket生成
	 * @param factory SSL通信用{@link SSLSocketFactory}。{@link #getSSLSocketFactory(String, String)}で生成したものを設定。
	 * @param socksProxy SOCKSプロキシの{@link Proxy}オブジェクト。SOCKS以外のタイプのプロキシを指定した場合は{@link ApnsException}がスローされる
	 * @param host 接続先HOST
	 * @param port 接続先PORT
	 * @return {@link Socket}
	 */
	public static Socket createSocket(SSLSocketFactory factory, Proxy socksProxy, String host, int port) {
		Socket socket;
		try {
			if (socksProxy == null) {
				socket = factory.createSocket(host, port);
			} else {
				if (socksProxy.type() != Proxy.Type.SOCKS) {
					throw new ApnsException("プロキシタイプはSOCKSでなければなりません。");
				}
				boolean success = false;
				Socket proxySocket = null;
				try {
					proxySocket = new Socket(socksProxy);
					proxySocket.connect(new InetSocketAddress(host, port));
					socket = ((SSLSocketFactory) factory).createSocket(proxySocket, host, port, false);
					success = true;
				} finally {
					if (!success) {
						ApnsUtil.close(proxySocket);
					}
				}
			}
			return socket;
		} catch (Exception e) {
			logger.error("Socketの生成に失敗しました。", e);
			throw new ApnsException(e);
		}
	}

	/**
	 * Socketのクローズ
	 *
	 * @param socket クローズするSocket
	 * @return クローズを試みたかどうか
	 */
	public static boolean close(Socket socket) {
		try {
			if (socket != null && !socket.isClosed()) {
				socket.close();
				return true;
			}
			return false;
		} catch (Exception e) {
			return true;
		}
	}
}
