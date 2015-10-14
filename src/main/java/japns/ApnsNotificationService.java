package japns;

import japns.ApnsNotification.PushStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APNsプッシュ通知サービス
 * @author T.Inukai
 */
public class ApnsNotificationService {
	private static Logger logger = LoggerFactory.getLogger(ApnsNotificationService.class);

	/**
	 * デフォルト待機時間（ミリ秒）
	 */
	public static int DEFAULT_PUSH_INTERVAL_MS = 0;
	/**
	 * デフォルト最大パケットサイズ
	 */
	public static int DEFAULT_MAX_PACKET = 0;

	/**
	 * リトライ回数
	 */
	public static int RETRY_COUNT = 3;

	/**
	 * PUSH通信間隔（ミリ秒）
	 */
	private int pushIntervalMs = DEFAULT_PUSH_INTERVAL_MS;
	/**
	 * 最大パケットサイズ
	 */
	private long maxPacket = DEFAULT_MAX_PACKET;
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
	 * PUSH通知ソケット
	 */
	private Socket apnsNotificationSocket;

	/**
	 * パケット通信量
	 */
	private long packetSize = 0;

	/**
	 * APNs入力Streamリーダー
	 */
	private ApnsInputMonitoringThread apnsInputMonitoringThread;

	/**
	 * 実行PUSH通知Map（Key:{@code identifier}, value:{@code ApnsNotification}オブジェクト）
	 */
	private Map<Integer, ApnsNotification> executeApnsNotificationMapIdentifier = new HashMap<Integer, ApnsNotification>();
	/**
	 * 実行PUSH通知Map（Key:{@code ApnsNotification}オブジェクト, value:{@code List<ApnsNotification>内Index）
	 */
	private Map<ApnsNotification, Integer> executeIndexMapApnsNotification = new HashMap<ApnsNotification, Integer>();

	/**
	 * 製品フラグ、証明書ファイル、証明書パスワードを指定してインスタンスを生成します
	 *
	 * <p>
	 * PUSH通信間隔（ミリ秒）、最大パケットサイズはデフォルト、SOCKSプロキシはnull（ダイレクト接続）を設定します
	 * </p>
	 *
	 * @param isProduction 製品フラグ
	 * @param certFileName 証明書ファイル
	 * @param certPassword 証明書パスワード
	 */
	public ApnsNotificationService(boolean isProduction, String certFileName, String certPassword) {
		this(isProduction, certFileName, certPassword, null, DEFAULT_PUSH_INTERVAL_MS, DEFAULT_MAX_PACKET);
	}

	/**
	 * 製品フラグ、証明書ファイル、証明書パスワード、SOCKSプロキシ、PUSH通信間隔（ミリ秒）、最大パケットサイズを指定してインスタンスを生成します
	 *
	 * @param isProduction 製品フラグ
	 * @param certFileName 証明書ファイル
	 * @param certPassword 証明書パスワード
	 * @param socksProxy SOCKSプロキシ
	 * @param pushIntervalMs PUSH通信間隔（ミリ秒）
	 * @param maxPacket 最大パケットサイズ
	 */
	public ApnsNotificationService(boolean isProduction, String certFileName, String certPassword, Proxy socksProxy, int pushIntervalMs, long maxPacket) {
		this.isProduction = isProduction;
		this.certFileName = certFileName;
		this.certPassword = certPassword;
		this.socksProxy = socksProxy;
		this.pushIntervalMs = pushIntervalMs;
		this.maxPacket = maxPacket;
		this.factory = ApnsUtil.getSSLSocketFactory(this.certFileName, this.certPassword);

	}

	/**
	 * PUSH通知（1件送信）
	 *
	 * @param apnsNotification 送信データ
	 * @return 送信結果
	 */
	public synchronized ApnsResult push(ApnsNotification apnsNotification) {
		logger.info("PUSH通知（1件送信） - 開始");
		try {
			List<ApnsNotification> apnsNotificationList = new ArrayList<ApnsNotification>();
			apnsNotificationList.add(apnsNotification);
			return push(apnsNotificationList);
		} finally {
			logger.info("PUSH通知（1件送信） - 終了");
		}
	}

	/**
	 * PUSH通知（マルチスレッド送信）
	 *
	 * <p>
	 * 分割単位ごとにマルチスレッドでPUSH通知します。<br>
	 * </p>
	 *
	 * @param apnsNotificationList 送信データリスト
	 * @param threadCount スレッド数
	 * @return 送信結果
	 */
	public synchronized ApnsResult push(List<ApnsNotification> apnsNotificationList, int threadCount) {
		logger.info("PUSH通知（マルチスレッド送信） - 開始");

		// 送信完了を待機して各スレッドの送信結果を取得
		ApnsResult apnsResult = new ApnsResult();

		try {
			// スレッド数が1の場合はシングルスレッド送信
			if (threadCount == 1) {
				logger.info("スレッド数が1のため、シングルスレッドで実行します。");
				return push(apnsNotificationList);
			}

			// Listをスレッド数に応じて分割
			List<List<ApnsNotification>> apnsNotificationListList = ApnsUtil.splitList(apnsNotificationList, threadCount);

			logger.info("スレッド数:{}", apnsNotificationListList.size());

			// 非同期処理用ExecutorService生成
			ExecutorService service = Executors.newFixedThreadPool(apnsNotificationListList.size());
			List<Future<ApnsResult>> futureList = new ArrayList<Future<ApnsResult>>();

			// 非同期PUSH送信
			for (List<ApnsNotification> apnsNotificationThreadList : apnsNotificationListList) {
				ApnsNotificationPushCaller caller = new ApnsNotificationPushCaller(apnsNotificationThreadList);
				Future<ApnsResult> future = service.submit(caller);
				futureList.add(future);
			}

			// シャットダウン宣言
			service.shutdown();

			apnsResult.setSuccess(true);
			for (int i = 0; i < futureList.size(); i++) {
				// 待機&結果取得
				ApnsResult r = futureList.get(i).get();

				// 1つでも異常終了があれば異常とする
				if (!r.isSuccess()) {
					apnsResult.setSuccess(r.isSuccess());
				}

				// 複数スレッドでExceptionがあった場合は最後のものを格納
				if (r.getException() != null) {
					apnsResult.setException(r.getException());
				}

				// 未実行リスト
				apnsResult.addAllNone(r.getNoneList());
				// 実行済リスト
				apnsResult.addAllDone(r.getDoneList());
				// エラーリスト
				apnsResult.addAllError(r.getErrorList());
			}
		} catch (Exception e) {
			apnsResult.setException(e);
			apnsResult.setSuccess(false);
		} finally {
			logger.info("PUSH通知（マルチスレッド送信） - 終了");
		}
		return apnsResult;
	}

	/**
	 * PUSH通知（複数件送信）
	 *
	 * @param apnsNotificationList 送信データリスト
	 * @return 送信結果
	 */
	public synchronized ApnsResult push(List<ApnsNotification> apnsNotificationList) {
		logger.info("PUSH通知 - 開始");
		try {

			// Socketの生成
			reconnectNotificationSocket();

			// 通知リスト読み込み位置
			int pos = 0;
			packetSize = 0;
			while (true) {
				int i;
				for (i = pos; i < apnsNotificationList.size(); i++) {
					pos = i;

					// 通知情報取り出し
					ApnsNotification apnsNotification = apnsNotificationList.get(i);

					// 実行Mapに格納
					executeApnsNotificationMapIdentifier.put(apnsNotification.getIdentifier(), apnsNotification);
					executeIndexMapApnsNotification.put(apnsNotification, i);

					logger.trace("deviceToken:{}, payload:{}", apnsNotification.getToken(), apnsNotification.getPayload());

					// バイナリデータに変換
					byte[] pushData = apnsNotification.getNotificationBytes();
					if (pushData == null) {
						logger.info("PUSHデータを生成できませんでした。token={}, payload={}", apnsNotification.getToken(), apnsNotification.getPayload());
						// エラーにステータス変更
						apnsNotification.setPushStatus(PushStatus.ERROR);
						// 読み飛ばし
						continue;
					}

					// パケット量制限を超える場合は再接続
					if (this.maxPacket != 0 && this.packetSize > this.maxPacket) {
						reconnectNotificationSocket();
					}

					try {
						// PUSH通知
						OutputStream os = apnsNotificationSocket.getOutputStream();
						os.write(pushData);
						os.flush();

						// パケット量加算
						packetSize += pushData.length;

						// 実行済にステータス設定
						apnsNotification.setPushStatus(PushStatus.DONE);
					} catch (IOException e) {
						// OutputStream書き込みエラー
						logger.debug("APNs通知情報送信エラー。リトライを試行します。", e);
						pos = processPushError(i, apnsNotificationList);
						// forループを抜けてリトライ
						break;
					}

					// 待機
					ApnsUtil.sleep(pushIntervalMs);

					// エラー確認
					if (apnsInputMonitoringThread.hasError()) {
						pos = processPushError(i, apnsNotificationList);
						// forループを抜けてリトライ
						break;
					}
					pos++;
				}

				// 終端まで達していない場合は残りを処理
				if (pos < apnsNotificationList.size()) {
					continue;
				}

				// 1秒間待機（最終通信後のAPNsからの入力を待つ）
				ApnsUtil.sleep(1000);

				// エラー確認
				if (apnsInputMonitoringThread.hasError()) {
					pos = processPushError(i, apnsNotificationList);
					// リトライ
					continue;
				}
				// 全件処理完了
				break;
			}

			return createApnsSendResult(apnsNotificationList, true, null);
		} catch (Exception e) {
			logger.error("PUSH通知処理中にException発生。", e);
			return createApnsSendResult(apnsNotificationList, false, e);
		} finally {
			ApnsUtil.close(apnsNotificationSocket);
			logger.info("PUSH通知 - 終了");
		}
	}

	/**
	 * ソケットの再接続
	 */
	private synchronized void reconnectNotificationSocket() {
		apnsInputMonitoringThread = null;

		// 既存の接続をクローズ
		ApnsUtil.close(apnsNotificationSocket);

		// パケット送信量をリセット
		packetSize = 0;

		// Socket取得
		apnsNotificationSocket = null;
		apnsNotificationSocket = ApnsUtil.createAPNSGatewaySocket(isProduction, factory, socksProxy);

		// APNsからの入力待ちスレッドを開始
		try {
			apnsInputMonitoringThread = new ApnsInputMonitoringThread(apnsNotificationSocket.getInputStream());
			apnsInputMonitoringThread.start();
		} catch (IOException e) {
			logger.info("APNs入力ストリーム取得エラー", e);
		}
		logger.debug("APNsへのSocket通信を構築または再構築しました。");
	}

	/**
	 * PUSH通知エラー発生時処理
	 * @param idx エラー特定時に処理中の notificationInfoList のIndex
	 * @param apnsNotificationList
	 * @return 次に処理すべき notificationInfoList のIndex
	 */
	private synchronized int processPushError(int idx, List<ApnsNotification> apnsNotificationList) {
		ApnsNotification apnsNotification = null;

		// 次回実行Idx
		int nextIdx = idx;
		try {
			// APNsエラー通知チェック
			if (!apnsInputMonitoringThread.hasError()) {
				// APNsエラー通知無し
				apnsNotification = apnsNotificationList.get(idx);
				if (apnsNotification != null) {
					// リトライ回数を加算して取得
					int retryCount = apnsNotification.getAndAddRetryCount();

					if (retryCount > RETRY_COUNT) {
						// リトライ回数オーバーの場合はエラー扱い
						apnsNotification.setPushStatus(PushStatus.ERROR);
						nextIdx = idx + 1;
					} else {
						// リトライ回数以内の場合は未実行扱い
						apnsNotification.setPushStatus(PushStatus.NONE);
						nextIdx = idx;
					}
				}
			} else {
				// APNsエラー通知有り

				// エラーデータ取得
				ApnsNotificationErrorResponse apnsNotificationErrorResponse = apnsInputMonitoringThread.getApnsNotificationErrorResponse();
				logger.debug("APNsサーバエラー:{}", apnsNotificationErrorResponse.toString());

				// APNsのエラー通知でない場合（APNs待受スレッドで例外発生）
				if (!apnsNotificationErrorResponse.isApnsErrorNotification()) {
					// 原因がよくわからないので、ここで処理終了する
					throw new ApnsException(apnsNotificationErrorResponse.getException());
				}

				// エラーとなったPUSH通知情報を取得
				apnsNotification = executeApnsNotificationMapIdentifier.get(apnsNotificationErrorResponse.getIdentifier());
				// エラーとなったPUSH通知情報のIndexを取得
				int idxError = executeIndexMapApnsNotification.get(apnsNotification);
				// リトライカウントを加算して取得
				int retryCount = apnsNotification.getAndAddRetryCount();

				// 次回実行Indexを特定
				// APNsステータスが10以外の場合（PUSH通知データの誤り）・・エラーとなった通知のidentifierがAPNsから渡される
				// APNsステータスが10の場合（APNsシャットダウン）・・・・・最後に正常終了した通知のidentifierがAPNsから渡される
				// いずれの場合も次のindexから再開する
				nextIdx = idxError + 1;

				// 10:シャットダウン以外（通知内容に誤りがある）または同一の通知のリトライ回をオーバーした場合
				if (apnsNotificationErrorResponse.getStatus() != 10 || retryCount > RETRY_COUNT) {
					// エラーデータ格納
					apnsNotification.setApnsNotificationErrorData(apnsNotificationErrorResponse);
					// ステータスをエラーにする
					apnsNotificationList.get(idxError).setPushStatus(PushStatus.ERROR);
				}

				// エラーの通知を送信してからAPNsエラー通知を受け取るまでに送信した通知のステータスを未実行に戻す
				// ※エラー対象以降の通知はAPNs側で破棄されている
				for (int i = nextIdx; i < idx + 1; i++) {
					if (i < apnsNotificationList.size()) {
						apnsNotificationList.get(i).setPushStatus(PushStatus.NONE);
					}
				}
			}

			logger.debug("エラー発生:token={}, payload={}", apnsNotification.getToken(), apnsNotification.getPayload());
			logger.debug("エラー発生Inex:{}, 次回実行Index:{}", idx, nextIdx);
			return nextIdx;
		} finally {
			// ソケットを再接続
			reconnectNotificationSocket();
		}
	}

	/**
	 * APNs通知結果生成
	 *
	 * <p>
	 * 状態に応じた送信結果を生成して返却します
	 * </p>
	 *
	 * @param apnsNotificationList 送信データ
	 * @param isSuccess 正常フラグ
	 * @param e 例外
	 * @return 送信結果
	 */
	private ApnsResult createApnsSendResult(List<ApnsNotification> apnsNotificationList, boolean isSuccess, Exception e) {
		ApnsResult apnsResult = new ApnsResult();
		for (ApnsNotification apnsNotification : apnsNotificationList) {
			switch (apnsNotification.getPushStatus()) {
			case NONE:
				apnsResult.addNone(apnsNotification);
				break;
			case DONE:
				apnsResult.addDone(apnsNotification);
				break;
			case ERROR:
				apnsResult.addError(apnsNotification);
				break;
			}
		}
		apnsResult.setSuccess(isSuccess);
		apnsResult.setException(e);
		return apnsResult;
	}

	/**
	 * PUSH通信間隔の設定
	 * @param pushIntervalMs
	 */
	public void setPushIntervalMs(int pushIntervalMs) {
		this.pushIntervalMs = pushIntervalMs;
	}

	/**
	 * 最大パケットサイズの設定
	 * @param maxPacket
	 */
	public void setMaxPacket(long maxPacket) {
		this.maxPacket = maxPacket;
	}

	/**
	 * SOCKSプロキシの設定
	 * @param socksProxy
	 */
	public void setSocksProxy(Proxy socksProxy) {
		this.socksProxy = socksProxy;
	}

	/**
	 * APNs入力モニタリングスレッド
	 * @author T.Inukai
	 */
	private class ApnsInputMonitoringThread extends Thread {

		/**
		 * APNs接続ソケットのInputStream
		 */
		private InputStream is;

		/**
		 * エラーデータ
		 */
		private ApnsNotificationErrorResponse apnsNotificationErrorResponse;

		/**
		 * コンストラクタ
		 * @param is APNs接続ソケットのInputStream
		 */
		public ApnsInputMonitoringThread(InputStream is) {
			super();
			this.is = is;
		}

		@Override
		public void run() {
			// APNsが返すエラーデータのサイズ
			final int ERROR_DATA_SIZE = 6;
			try {
				byte[] bytes = new byte[ERROR_DATA_SIZE];

				// APNsからの入力があるまで待機
				while (is.read(bytes) == ERROR_DATA_SIZE) {
					// エラーデータを生成、返却
					apnsNotificationErrorResponse = new ApnsNotificationErrorResponse(bytes);
					break;
				}
			} catch (SocketException e) {
				// SocketExceptionはAPNsが通信を切ったとみなす
				// 通信遮断はメインスレッドで検知できるので、エラーなしでスレッド終了とする
				logger.debug("ソケットが閉じられたためスレッドを終了します。", e);
			} catch (IOException e) {
				// APNsからのデータがおかしいなどの例外
				logger.info("エラーデータの読み込みに失敗しました。", e);
				apnsNotificationErrorResponse = new ApnsNotificationErrorResponse(e);
			}
		}

		/**
		 * PUSH通知エラーデータの取得
		 * @return
		 */
		public ApnsNotificationErrorResponse getApnsNotificationErrorResponse() {
			return this.apnsNotificationErrorResponse;
		}

		/**
		 * エラーがあるかどうか
		 * @return
		 */
		public boolean hasError() {
			return this.apnsNotificationErrorResponse != null;
		}
	}

	/**
	 * 非同期PUSH通知用Callable
	 * @author T.Inukai
	 */
	private class ApnsNotificationPushCaller implements Callable<ApnsResult> {

		// PUSH送信クラス
		private ApnsNotificationService apnsNotificationService;
		// PUSH送信データ
		private List<ApnsNotification> apnsNotificationList;

		/**
		 * コンストラクタ
		 * @param apnsNotificationList
		 */
		public ApnsNotificationPushCaller(List<ApnsNotification> apnsNotificationList) {
			// 送信インスタンス生成
			this.apnsNotificationService = new ApnsNotificationService(isProduction, certFileName, certPassword, socksProxy, pushIntervalMs, maxPacket);
			// 対象リスト格納
			this.apnsNotificationList = apnsNotificationList;
		}

		@Override
		public ApnsResult call() throws Exception {
			// 通知処理実行
			try {
				return this.apnsNotificationService.push(this.apnsNotificationList);
			} catch (Exception e) {
				return this.apnsNotificationService.createApnsSendResult(apnsNotificationList, false, e);
			}
		}

	}
}
