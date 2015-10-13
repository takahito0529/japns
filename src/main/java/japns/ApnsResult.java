package japns;

import java.util.ArrayList;
import java.util.List;

/**
 * APNs送信結果
 * @author T.Inukai
 */
public class ApnsResult {

	/**
	 * 正常終了フラグ
	 */
	private boolean success = false;

	/**
	 * 例外
	 */
	private Exception exception;

	/**
	 * 未送信リスト
	 */
	private List<ApnsNotification> noneList = new ArrayList<ApnsNotification>();
	/**
	 * 送信済リスト
	 */
	private List<ApnsNotification> doneList = new ArrayList<ApnsNotification>();
	/**
	 * エラーリスト
	 */
	private List<ApnsNotification> errorList = new ArrayList<ApnsNotification>();

	/**
	 * 正常終了フラグの取得
	 * @return success true:正常、false:異常
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * 正常終了フラグの設定
	 * @param success 正常終了フラグに設定する値
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/**
	 * 未送信件数取得
	 * @return 未送信件数
	 */
	public int getNoneCount() {
		return noneList.size();
	}

	/**
	 * 送信済件数取得
	 * @return 送信済件数
	 */
	public int getDoneCount() {
		return doneList.size();
	}

	/**
	 * 送信エラー数取得
	 * @return 送信エラー数
	 */
	public int getErrorCount() {
		return errorList.size();
	}

	/**
	 * 未送信リスト追加
	 * @param d
	 */
	public void addNone(ApnsNotification d) {
		this.noneList.add(d);
	}

	/**
	 * 未送信リスト一括追加
	 * @param l 設定する未送信リスト
	 */
	public void addAllNone(List<ApnsNotification> l) {
		this.noneList.addAll(l);
	}

	/**
	 * 未送信リストの取得
	 *
	 * @return 未送信の{@link ApnsNotification}のリスト
	 */
	public List<ApnsNotification> getNoneList() {
		return this.noneList;
	}

	/**
	 * 送信済リスト追加
	 * @param d 設定する送信済の{@link ApnsNotification}
	 */
	public void addDone(ApnsNotification d) {
		this.doneList.add(d);
	}

	/**
	 * 送信済リスト一括追加
	 * @param l 設定する送信済リスト
	 */
	public void addAllDone(List<ApnsNotification> l) {
		this.doneList.addAll(l);
	}

	/**
	 * 送信済リストの取得
	 * @return 送信済の{@link ApnsNotification}のリスト
	 */
	public List<ApnsNotification> getDoneList() {
		return this.doneList;
	}

	/**
	 * 送信エラーリスト追加
	 * @param d 設定する送信エラーの{@link ApnsNotification}
	 */
	public void addError(ApnsNotification d) {
		this.errorList.add(d);
	}

	/**
	 * 送信エラーリスト一括追加
	 * @param l 設定する送信エラーリスト
	 */
	public void addAllError(List<ApnsNotification> l) {
		this.errorList.addAll(l);
	}

	/**
	 * 送信エラーリストの取得
	 *
	 * @return 送信エラーの{@link ApnsNotification}のリスト
	 */
	public List<ApnsNotification> getErrorList() {
		return this.errorList;
	}

	/**
	 * 例外の設定
	 * @param exception 接位する例外
	 */
	public void setException(Exception exception) {
		this.exception = exception;
	}

	/**
	 * 例外の取得
	 * @return 例外
	 */
	public Exception getException() {
		return this.exception;
	}
}
