package japns;

/**
 * Push通知エラーレスポンス
 * @author T.Inukai
 */
public class ApnsNotificationErrorResponse {

	/**
	 * コマンド（8固定）
	 */
	private int command;
	/**
	 * ステータス
	 */
	private int status;
	/**
	 * 識別子
	 */
	private int identifier;
	/**
	 * 例外
	 */
	private Exception e;

	/**
	 * APNsからエラーを受信したか
	 */
	private boolean isApnsErrorNotification;

	/**
	 * コンストラクタ（APNs受信バイナリ指定）
	 * @param bytes APNsからの受信バイナリデータ
	 */
	public ApnsNotificationErrorResponse(byte[] bytes) {
		if (bytes == null || bytes.length < 6) {
			return;
		}
		// 1バイト目:コマンド(8固定)
		command = bytes[0] & 0xff;
		// 2バイト目:ステータス
		status = bytes[1] & 0xff;
		// 3-6バイト目:識別子
		identifier = ApnsUtil.parseBytesToInt(bytes[2], bytes[3], bytes[4], bytes[5]);

		// APNsからのエラー受信
		isApnsErrorNotification = true;
	}

	/**
	 * コンストラクタ（例外指定）
	 * @param e 例外
	 */
	public ApnsNotificationErrorResponse(Exception e) {
		this.e = e;
		// APNsからのエラー受信ではない
		isApnsErrorNotification = false;
	}

	/**
	 * コマンドの取得
	 * @return コマンド APNsからの受信データの場合は8固定のはず
	 */
	public int getCommand() {
		return command;
	}

	/**
	 * ステータスの取得
	 *
	 * <p>
	 * 0 - エラーなし<br>
	 * 1 - 処理エラー<br>
	 * 2 - デバイストークン欠如<br>
	 * 3 - トピック欠如<br>
	 * 4 - ペイロード欠如<br>
	 * 5 - 無効なトークンサイズ<br>
	 * 6 - 無効なトピックサイズ<br>
	 * 7 - 無効なペイロードサイズ<br>
	 * 8 - 無効なトークン<br>
	 * 10 - シャットダウン<br>
	 * 255 - 不明<br>
	 * </p>
	 * @return ステータス
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * 識別子の取得
	 * @return 識別子
	 */
	public int getIdentifier() {
		return identifier;
	}

	/**
	 * APNsからエラーを受信したかどうか
	 *
	 * <p>
	 * {@code true} の場合は {@link #getCommand()}、{@linkplain #getStatus()}、{@link #getIdentifier()} でAPNsからのエラーを取得できます。<br>
	 * {@code false} の場合は {@link #getException()} でExceptionを取得できます。
	 * </p>
	 *
	 * @return true:APNsからエラー受信あり、false:APNsからエラー受信なし
	 */
	public boolean isApnsErrorNotification() {
		return this.isApnsErrorNotification;
	}

	/**
	 * 例外
	 * @return 例外
	 */
	public Exception getException() {
		return e;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		sb.append("isApnsErrorNotification:").append(isApnsErrorNotification).append(",");
		sb.append("command:").append(command).append(",");
		sb.append("status:").append(status).append(",");
		sb.append("identifier:").append(identifier).append(",");
		sb.append("exception:").append(e);
		sb.append("}");
		return sb.toString();
	}
}
