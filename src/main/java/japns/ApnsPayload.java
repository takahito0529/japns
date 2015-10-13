package japns;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * APNsペイロード
 *
 * <p>
 * 各種setメソッドでペイロードの各項目を設定し、<br>
 * {@link #getPayload()}でペイロード文字列を取得できます。<br>
 * {@link #getPayloadBytes()}でByte配列のペイロードを取得することもできます。
 * </p>
 *
 * @author T.Inukai
 */
public class ApnsPayload {

	private static ObjectMapper mapper = new ObjectMapper();

	private Map<String, Object> root;
	private Map<String, Object> aps;
	private Map<String, Object> customAlert;

	/**
	 * コンストラクタ
	 */
	public ApnsPayload() {
		root = new HashMap<String, Object>();
		aps = new HashMap<String, Object>();
		customAlert = new HashMap<String, Object>();

		root.put("aps", aps);
	}

	/**
	 * aps辞書 {@code badge}の設定
	 *
	 * <p>
	 * アプリケーションアイコンのバッジとして表示する数字です。<br>
	 * このプロパティが存在しない場合、バッジは変化しません。
	 * バッジを削除したい場合は、このプロパティの値を0としてください。
	 * </p>
	 *
	 * @param n {@code badge}に設定する値
	 */
	public void setBadge(int n) {
		aps.put("badge", n);
	}

	/**
	 * aps辞書 {@code sound}の設定
	 *
	 * <p>
	 * アプリケーションバンドル内のサウンドファイルの名前です。
	 * このファイル内のサウンドが、警告音として再生されます。
	 * サウンドファイルが存在しないか、値としてdefaultが指定されている場合は、デフォルトの警告音が再生されます。
	 * このオーディオは、システムサウンドと互換性のあるオーディオデータフォーマットのいずれかでなければなりません。
	 * </p>
	 *
	 * @param s {@code sound}に設定する値
	 */
	public void setSound(String s) {
		aps.put("sound", s);
	}

	/**
	 * aps辞書 {@code content-available}の設定
	 *
	 * <p>
	 * このキーの値が1であれば、新しいコンテンツがあることを表します。
	 * このキーと値を設定すると、アプリケーションがバックグラウンドで起動され、あるいは再開したとき、
	 * {@code application: didReceiveRemoteNotification:fetchCompletionHandler:}が呼び出されるようになります。
	 * </p>
	 * @param contentAvilable {@code sound}に設定する値
	 */
	public void setContentAvailable(boolean contentAvilable) {
		if (contentAvilable) {
			aps.put("content-available", 1);
		} else {
			if (aps.containsKey("content-available")) {
				aps.remove("content-available");
			}
		}
	}

	/**
	 * alert辞書 {@code title}の設定
	 *
	 * <p>
	 * 通知の目的を表す短い文字列。
	 * Apple Watchはこの文字列を、通知インターフェイスの一部として表示します。
	 * 実際に表示されるのはごく短時間なので、ひと目で理解できるように記述しなければなりません。
	 * </p>
	 *
	 * @param title {@code sound}に設定する値
	 */
	public void setTitle(String title) {
		customAlert.put("title", title);
	}

	/**
	 * PUSHメッセージ（aps辞書 {@code alert} または alert辞書 {@code body} の設定）
	 *
	 * <p>
	 * 警告メッセージのテキストです。<br>
	 * alert辞書に設定されるキーが"body"のみの場合は、
	 * ここで設定された値はaps辞書の"alert"に設定されます。
	 * alert辞書に設定されるキーが"body"以外にも存在する場合は、
	 * ここで設定された値はalert辞書の"body"に設定されます。
	 * </p>
	 *
	 * @param s PUSHメッセージ
	 */
	public void setAlertBody(String s) {
		customAlert.put("body", s);
	}

	/**
	 * alert辞書 {@code title-loc-key}の設定
	 *
	 * <p>
	 * 現在のローカライズに対応する、{@code Localizable.strings}ファイル内のタイトル文字列のキーです。
	 * このキー文字列を%@指定子や%n $@指定子を使用して書式化すると、
	 * {@code title-loc-args}配列で指定した変数に置き換えることができます。
	 * </p>
	 *
	 * @param s {@code title-loc-key}に設定する値
	 */
	public void setTitleLocKey(String s) {
		customAlert.put("title-loc-key", s);
	}

	/**
	 * alert辞書 {@code title-loc-args}の設定
	 *
	 * <p>
	 * {@code title-loc-key}内の書式指定子の代わりに表示する変数文字列値です。
	 * </p>
	 *
	 * @param s {@code title-loc-args}に設定する値
	 */
	public void setTitleLocArgs(String... s) {
		customAlert.put("title-loc-args", s);
	}

	/**
	 * alert辞書 {@code action-loc-key}の設定
	 *
	 * <p>
	 * 文字列が指定されていれば、システムは「Close」および「View」というボタンがついた警告画面を表示します。
	 * この文字列をキーとして使用して現在のローカライズからローカライズ文字列を取得し、
	 * 「View」の代わりに右ボタンのタイトルとして使用します。
	 * </p>
	 *
	 * @param s "action-loc-key"に設定する値
	 */
	public void setActionLocKey(String s) {
		customAlert.put("action-loc-key", s);
	}

	/**
	 * alert辞書 {@code loc-key}の設定
	 *
	 * <p>
	 * 現在のローカライズ（ユーザの言語環境によって設定される）に対応する
	 * {@code Localizable.strings}ファイル内の警告メッセージ文字列のキーです。
	 * このキー文字列を%@指定子や%n $@指定子を使用して書式化すると、
	 * {@code loc-args}配列で指定した変数に置き換えることができます。
	 * </p>
	 *
	 * @param s {@code loc-key}に設定する値
	 */
	public void setLocKey(String s) {
		customAlert.put("loc-key", s);
	}

	/**
	 * alert辞書 {@code loc-args}の設定
	 *
	 * <p>
	 * {@code loc-key}内の書式指定子の代わりに表示する変数文字列値です。
	 * </p>
	 *
	 * @param s {@code loc-args}に設定する値
	 */
	public void setLocArgs(String... s) {
		customAlert.put("loc-args", s);
	}

	/**
	 * alert辞書 {@code launch-image}の設定
	 *
	 * <p>
	 * アプリケーションバンドル内の画像ファイルのファイル名です。
	 * 拡張子を含んでも省略してもかまいません。
	 * 画像は、ユーザがアクションボタンをタップするか、
	 * アクションスライダを動かしたときの起動画像として使われます。
	 * このプロパティが指定されていない場合、システムは以前のスナップショットを使用するか、
	 * アプリケーションの{@code Info.plist}ファイルの{@code UILaunchImageFile}キーで指定された画像を使用するか、
	 * {@code Default.png}にフォールバックします。
	 * </p>
	 *
	 * @param s {@code launch-image}に設定する値
	 */
	public void setLaunchImage(String s) {
		customAlert.put("launch-image", s);
	}

	/**
	 * カスタムフィールドの設定
	 *
	 * <p>
	 * 任意のカスタムフィールドを設定します。<br>
	 * 設定した値はルート要素として設定されます。
	 * </p>
	 *
	 * @param key キー
	 * @param value 値
	 */
	public void putCustomField(String key, Object value) {
		root.put(key, value);
	}

	/**
	 * カスタムフィールドの設定
	 *
	 * <p>
	 * 任意のカスタムフィールドを設定します。<br>
	 * 設定した値はルート要素として設定されます。
	 * </p>
	 *
	 * @param values カスタムフィールドMap
	 */
	public void putCustomFields(Map<String, ? extends Object> values) {
		root.putAll(values);
	}

	/**
	 * ペイロードの取得
	 * @return ペイロード文字列
	 */
	public String getPayload() {
		try {
			adjustCustomAlert();
			return mapper.writeValueAsString(root);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * ペイロードバイトデータ取得
	 * @return ペイロードデータ
	 */
	public byte[] getPayloadBytes() {
		return ApnsUtil.convertStringToUTF8Bytes(getPayload());
	}

	/**
	 * alert辞書の調整処理
	 */
	private void adjustCustomAlert() {
		switch (customAlert.size()) {
		case 0:
			// alert辞書無しの場合はalertを削除
			aps.remove("alert");
			break;
		case 1:
			// alert辞書が"body"のみの場合は"alert"に"body"の値を設定
			if (customAlert.containsKey("body")) {
				aps.put("alert", customAlert.get("body"));
				break;
			}
		default:
			// それ以外はalert辞書を"alert"に設定
			aps.put("alert", customAlert);
		}
	}

	@Override
	public String toString() {
		return getPayload();
	}

}
