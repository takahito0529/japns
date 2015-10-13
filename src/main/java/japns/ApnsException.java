package japns;

/**
 * APNsへのPUSH通知送信処理に失敗したときにスローされる非チェック例外です。
 *
 * @author T.Inukai
 */
public class ApnsException extends RuntimeException {

	/**
	 * 詳細メッセージを指定しないで {@code ApnsException} を構築します。
	 */
	public ApnsException() {
		super();
	}

	/**
	 * 指定された詳細メッセージを持つ {@code ApnsException} を構築します。
	 *
	 * @param message 詳細メッセージ
	 * @param cause 原因
	 */
	public ApnsException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 指定された詳細メッセージおよび原因を使用して {@code ApnsException} を構築します。
	 *
	 * @param message 詳細メッセージ
	 */
	public ApnsException(String message) {
		super(message);
	}

	/**
	 * 指定された原因と詳細メッセージ<tt>(cause==null ? null : cause.toString())</tt>
	 * を使用して {@code ApnsException} を構築します。
	 *
	 * @param cause 原因
	 */
	public ApnsException(Throwable cause) {
		super(cause);
	}

}
