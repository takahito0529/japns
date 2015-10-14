package japns;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APNsへのPUSH通信データ、および、通知状態を管理する
 * @author T.Inukai
 */
public class ApnsNotification {
	private static Logger logger = LoggerFactory.getLogger(ApnsNotification.class);

	/**
	 * 優先度列挙体
	 *
	 * <p>
	 * {@code HIGH} - 10：プッシュメッセージをすぐに送信します。その結果、デバイスには、アラート、サウンド、バッジのいずれかが現れます。<br>
	 * {@code LOW} - 5：送信先デバイスが節電モードのときにプッシュメッセージを送信します。
	 * </p>
	 */
	public enum Priority {
		HIGH {
			@Override
			int getCode() {
				return 10;
			}

		},
		LOW {
			@Override
			int getCode() {
				return 5;
			}
		};
		abstract int getCode();
	}

	/**
	 * デフォルト有効期限
	 *
	 * <p>
	 * {@code Integer.MAX_VALUE}を設定します。
	 * </p>
	 */
	private final static int DEFAULT_EXPIRY = Integer.MAX_VALUE;

	/**
	 * デフォルト優先度
	 *
	 * <p>
	 * {@code Priority.HIGH}を設定します。
	 * </p>
	 */
	private final static Priority DEFAULT_PRIORITY = Priority.HIGH;
	/**
	 * Identifier管理変数
	 *
	 * <p>
	 * このクラスのインスタンスが生成されるたびにインクリメントされます。
	 * </p>
	 */
	private static int nextId = 0;

	/**
	 * PUSH通知送信ステータス
	 *
	 * <p>
	 * NONE - 未送信<br>
	 * DONE - 送信済<br>
	 * ERROR - エラー
	 * </p>
	 */
	public enum PushStatus {
		// 未実行
		NONE,
		// 実行済
		DONE,
		// エラー
		ERROR
	}

	/**
	 * コマンド（2固定）
	 */
	private int command = 2;
	/**
	 * デバイストークン
	 */
	private String token;
	/**
	 * ペイロード
	 */
	private String payload;
	/**
	 * 通知の識別子
	 */
	private int identifier;
	/**
	 * 有効期限
	 */
	private int expiry;
	/**
	 * 優先度
	 */
	private Priority priority;

	/**
	 * リトライ回数
	 */
	private int retryCount;

	/**
	 * PUSHステータス
	 */
	private PushStatus pushStatus = PushStatus.NONE;

	/**
	 * PUSH通知エラー情報
	 */
	private ApnsNotificationErrorResponse apnsNotificationErrorResponse;

	/**
	 * PUSH通知バイナリデータ
	 */
	private byte[] notificationBytes;

	/**
	 * デバイストークン、ペイロードを指定してインスタンスを生成します。
	 *
	 * <p>
	 * 有効期限、優先度はでデフォルトになります。
	 * </p>
	 *
	 * @param token デバイストークン
	 * @param payload ペイロード
	 */
	public ApnsNotification(String token, String payload) {
		this(token, payload, DEFAULT_EXPIRY, DEFAULT_PRIORITY);
	}

	/**
	 * デバイストークン、ペイロード、有効期限、優先度を指定してインスタンスを生成します。
	 * @param token デバイストークン
	 * @param payload ペイロード
	 * @param expiry 有効期限
	 * @param priority 優先度
	 */
	public ApnsNotification(String token, String payload, int expiry, Priority priority) {
		this.token = token;
		this.payload = payload;

		this.identifier = getSecId();

		this.expiry = expiry;
		this.priority = priority;
	}

	/**
	 * デバイストークンの取得
	 * @return デバイストークン
	 */
	public String getToken() {
		return this.token;
	}

	/**
	 * ペイロードの取得
	 * @return ペイロード
	 */
	public String getPayload() {
		return this.payload;
	}

	/**
	 * 通知識別子の取得
	 * @return 通知識別子
	 */
	public int getIdentifier() {
		return this.identifier;
	}

	/**
	 * リトライ回数を加算して返却
	 *
	 * <p>
	 * 初回の呼び出しで1が返る。<br>
	 * </p>
	 *
	 * @return リトライ回数
	 */
	public int getAndAddRetryCount() {
		retryCount++;
		return retryCount;
	}

	/**
	 * PUSHステータスの設定
	 * @param pushStatus PUSHステータス
	 */
	public void setPushStatus(PushStatus pushStatus) {
		this.pushStatus = pushStatus;
	}

	/**
	 * PUSHステータスの取得
	 * @return PUSHステータス
	 */
	public PushStatus getPushStatus() {
		return this.pushStatus;
	}

	/**
	 * PUSH通知エラー情報の取得
	 * @return PUSH通知エラー情報
	 */
	public ApnsNotificationErrorResponse getApnsNotificationErrorData() {
		return apnsNotificationErrorResponse;
	}

	/**
	 * PUSH通知エラー情報の設定
	 * @param apnsNotificationErrorResponse PUSH通知エラー情報
	 */
	public void setApnsNotificationErrorData(ApnsNotificationErrorResponse apnsNotificationErrorResponse) {
		this.apnsNotificationErrorResponse = apnsNotificationErrorResponse;
	}

	/**
	 * 通知バイナリデータ取得
	 *
	 * <p>
	 * APNsに送信するバイナリデータを生成して返却します。<br>
	 * 初回呼び出し時にメモリに保持されるため、生成処理が実行されるのは初回のみ。
	 * </p>
	 *
	 * @return PUSH通知バイナリデータ
	 */
	public byte[] getNotificationBytes() {
		if (notificationBytes != null) {
			return notificationBytes;
		}

		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(boas);
		try {
			// フレームデータの生成
			int frameDataLength = 0;

			// デバイストークン
			int tokenCommand = 1;
			byte[] tokenData = ApnsUtil.convertHexToBytes(token);
			int tokenDataLength = tokenData.length;
			frameDataLength += 1 + 2 + tokenDataLength;

			// ペイロード
			int payloadCommad = 2;
			byte[] payloadData = ApnsUtil.convertStringToUTF8Bytes(payload);
			int payloadDataLength = payloadData.length;
			frameDataLength += 1 + 2 + payloadDataLength;

			// 通知の識別子
			int identifierCommad = 3;
			int identifierData = this.identifier;
			int identifierDataLength = 4;
			frameDataLength += 1 + 2 + identifierDataLength;

			// 有効期限
			int expiryCommad = 4;
			int expiryData = this.expiry;
			int expiryDataLength = 4;
			frameDataLength += 1 + 2 + expiryDataLength;

			// 優先度
			int priorityCommad = 5;
			int priorityData = this.priority.getCode();
			int priorityDataLength = 1;
			frameDataLength += 1 + 2 + priorityDataLength;

			// Streamに書き出し

			dos.writeByte(command);
			dos.writeInt(frameDataLength);

			dos.writeByte(tokenCommand);
			dos.writeShort(tokenDataLength);
			dos.write(tokenData);

			dos.writeByte(payloadCommad);
			dos.writeShort(payloadDataLength);
			dos.write(payloadData);

			dos.writeByte(identifierCommad);
			dos.writeShort(identifierDataLength);
			dos.writeInt(identifierData);

			dos.writeByte(expiryCommad);
			dos.writeShort(expiryDataLength);
			dos.writeInt(expiryData);

			dos.writeByte(priorityCommad);
			dos.writeShort(priorityDataLength);
			dos.writeByte(priorityData);

			notificationBytes = boas.toByteArray();
		} catch (Exception e) {
			logger.info("PUSH通知バイナリデータを生成できませんでした。", e);

			notificationBytes = null;
		} finally {
			try {
				dos.close();
				boas.close();
			} catch (IOException e) {
			} finally {
				dos = null;
				boas = null;
			}
		}
		return notificationBytes;
	}

	/**
	 * identifierの取得
	 * @return インクリメントしたidentifier
	 */
	private synchronized int getSecId() {
		nextId++;
		return nextId;
	}
}
