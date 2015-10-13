package japns;

import java.util.Date;

/**
 * APNsフィードバック
 * @author T.Inukai
 */
public class ApnsFeedback {

	/** Timestamp(int型) */
	private int nTimestamp;
	/** デバイストークン(byte配列) */
	private byte[] bDeviceToken;

	/** Timestamp */
	private Date timestamp;
	/** デバイストークン */
	private String deviceToken;

	/**
	 * APNsフィードバックのバイナリ
	 * （タイムスタンプ、デバイストークン）を設定してインスタンスを生成する
	 * @param timestamp Timestamp
	 * @param deviceToken デバイストークン
	 */
	public ApnsFeedback(int timestamp, byte[] deviceToken) {
		this.nTimestamp = timestamp;
		this.bDeviceToken = deviceToken;
	}

	/**
	 * Timestampの取得
	 * @return timestamp
	 */
	public Date getTimestamp() {
		if (timestamp == null) {
			timestamp = new Date(nTimestamp * 1000L);
		}
		return timestamp;
	}

	/**
	 * デバイストークンの取得
	 * @return デバイストークン
	 */
	public String getDeviceToken() {
		if (deviceToken != null) {
			return deviceToken;
		}
		if (bDeviceToken == null) {
			return null;
		}
		deviceToken = ApnsUtil.convertBytesToHex(bDeviceToken);
		return deviceToken;
	}

}
