package japns;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APNsフィードバックサービス
 * @author T.Inukai
 */
public class ApnsFeedbackService {
	private static Logger logger = LoggerFactory.getLogger(ApnsFeedbackService.class);

	/**
	 * 製品フラグ
	 */
	private final boolean isProduction;
	/**
	 * 証明書ファイル名
	 */
	private final String certFileName;
	/**
	 * 証明書パスワード
	 */
	private final String certPassword;
	/**
	 * SOCKSプロキシ
	 */
	private Proxy socksProxy;

	/**
	 * ソケットファクトリ
	 */
	private final SSLSocketFactory factory;

	/**
	 * 製品フラグ、証明書ファイル、証明書パスワードを指定してインスタンスを生成します
	 *
	 * <p>
	 * SOCKSプロキシはnull（ダイレクト接続）を設定します
	 * </p>
	 *
	 * @param isProduction 製品フラグ
	 * @param certFileName 証明書ファイル
	 * @param certPassword 証明書パスワード
	 */
	public ApnsFeedbackService(boolean isProduction, String certFileName, String certPassword) {
		this(isProduction, certFileName, certPassword, null);
	}

	/**
	 * 製品フラグ、証明書ファイル、証明書パスワード、SOCKSプロキシを指定してインスタンスを生成します
	 *
	 * @param isProduction 製品フラグ
	 * @param certFileName 証明書ファイル
	 * @param certPassword 証明書パスワード
	 * @param socksProxy SOCKSプロキシ
	 */
	public ApnsFeedbackService(boolean isProduction, String certFileName, String certPassword, Proxy socksProxy) {
		this.isProduction = isProduction;
		this.certFileName = certFileName;
		this.certPassword = certPassword;
		this.socksProxy = socksProxy;
		this.factory = ApnsUtil.getSSLSocketFactory(this.certFileName, this.certPassword);

	}

	/**
	 * フィードバックサービスに接続し、フィードバック情報を取得する
	 * @return フィードバック情報リスト
	 */
	public synchronized List<ApnsFeedback> feedback() {
		Socket apnsFeedbackSocket = null;

		int tryCnt = 0;

		while (true) {
			try {
				tryCnt++;
				apnsFeedbackSocket = ApnsUtil.createAPNSFeedbackSocket(isProduction, factory, socksProxy);

				List<ApnsFeedback> apnsFeedbackList = readFeedbackStream(apnsFeedbackSocket.getInputStream());

				return apnsFeedbackList;
			} catch (Exception e) {
				if (tryCnt >= 3) {
					// リトライ回数オーバー
					logger.info("フィードバックサービスとの接続に失敗しました。", e);
					throw new ApnsException("リトライ回数オーバー", e);
				}
			} finally {
				ApnsUtil.close(apnsFeedbackSocket);
			}
		}

	}

	/**
	 * {@code InputStream}からフィードバック情報を読み込む
	 * @param is フィードバック情報のInputStream
	 * @return フィードバック情報リスト
	 */
	private synchronized List<ApnsFeedback> readFeedbackStream(InputStream is) {
		List<ApnsFeedback> apnsFeedbackList = new ArrayList<ApnsFeedback>();

		DataInputStream dis = new DataInputStream(is);
		while (true) {
			try {
				// Timestamp
				int timestamp = dis.readInt();
				// トークン長
				int tokenLength = dis.readUnsignedShort();
				// デバイストークン
				byte[] deviceToken = new byte[tokenLength];
				dis.readFully(deviceToken);

				apnsFeedbackList.add(new ApnsFeedback(timestamp, deviceToken));
			} catch (EOFException e) {
				// 読み込み終了
				break;
			} catch (Exception e) {
				// 例外発生
				logger.debug("フィードバックサービスのストリーム読み込みエラー", e);
				throw new ApnsException(e);
			}
		}
		return apnsFeedbackList;
	}

	/**
	 * SOCKSプロキシの設定
	 * @param socksProxy
	 */
	public void setSocksProxy(Proxy socksProxy) {
		this.socksProxy = socksProxy;
	}

}
