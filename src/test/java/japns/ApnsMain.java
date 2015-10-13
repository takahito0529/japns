package japns;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApnsMain {
	private static Logger logger = LoggerFactory.getLogger(ApnsMain.class);

	/**
	 * メイン
	 * @param args
	 */
	public static void main(String[] args) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

		if (args == null || args.length != 7) {
			logger.error("Usage: japns.ApnsMain <p|s> <cert> <cert-password> <device-token-file> <wait-time> <max-packet> <thread-count>\njapns.ApnsMain p ./cert.pem abc123 token.txt 0 0 3");
			System.exit(1);
		}

		try {
			// 第1引数：製品フラグ
			boolean isProduction = args[0].equals("p");
			// 第2引数：証明書ファイル
			String certFileName = args[1];
			// 第3引数：証明書パスワード
			String certPassword = args[2];
			// 第4引数：デバイストークンファイル
			String tokenFilePath = args[3];
			// 第5引数：通信待機時間
			int waitTime = Integer.parseInt(args[4]);
			// 第6引数：最大パケット数
			int maxPacket = Integer.parseInt(args[5]);
			// 第7引数：スレッド数
			int threadCount = Integer.parseInt(args[6]);

			logger.debug("isProduction={}", isProduction);
			logger.debug("certFileName={}", certFileName);
			logger.debug("certPassword={}", certPassword);
			logger.debug("tokenFilePath={}", tokenFilePath);
			logger.debug("waitTime={}", waitTime);
			logger.debug("maxPacket={}", maxPacket);
			logger.debug("threadCount={}", threadCount);

			// デバイストークンリストファイル読み込み
			List<String> tokenList = getTokenList(tokenFilePath);

			// 通知データを作る
			List<ApnsNotification> apnsNotificationList = new ArrayList<ApnsNotification>();
			for (String deviceToken : tokenList) {
				// 適当にランダムな数値を発行（可変要素であるニックネームを想定）
				int rdm = (int) (Math.random() * 1000000);

				ApnsPayload payload = new ApnsPayload();
				// メッセージ
				payload.setAlertBody("" + rdm + "さん！新しいお知らせが届いているよ！新しいお知らせが届いているよ！");
				// バッジ
				payload.setBadge(1);
				// サウンド
				payload.setSound("default");
				// ボタン
				payload.setActionLocKey("見る");
				ApnsNotification apnsNotificationData = new ApnsNotification(deviceToken, payload.getPayload());
				apnsNotificationList.add(apnsNotificationData);
			}

			//-------------------------------
			// PUSH通知実行
			//-------------------------------
			ApnsNotificationService apnsNotificationService = new ApnsNotificationService(isProduction, certFileName, certPassword, null, waitTime, maxPacket);
			ApnsResult apnsResult = apnsNotificationService.push(apnsNotificationList, threadCount);

			logger.info("PUSH通知送信結果:正常終了フラグ={}", apnsResult.isSuccess());
			logger.info("PUSH通知送信結果:例外={}", apnsResult.getException());
			logger.info("PUSH通知送信結果:未通知={}", apnsResult.getNoneCount());
			logger.info("PUSH通知送信結果:通知済={}", apnsResult.getDoneCount());
			logger.info("PUSH通知送信結果:エラー={}", apnsResult.getErrorCount());

			// 未通知リスト出力
			for (int i = 0; i < apnsResult.getNoneList().size(); i++) {
				logger.info("PUSH通知送信結果:未通知リスト[{}]:token={}, payload={}", i, apnsResult.getNoneList().get(i).getToken(), apnsResult.getNoneList().get(i).getPayload());
			}

			// エラーリスト出力
			for (int i = 0; i < apnsResult.getErrorList().size(); i++) {
				if (apnsResult.getErrorList().get(i).getApnsNotificationErrorData() == null) {
					// APNs通信エラーなし
					logger.info("PUSH通知送信結果:エラーリスト[{}]:token={}, payload={}", i, apnsResult.getErrorList().get(i).getToken(), apnsResult.getErrorList().get(i).getPayload());
				} else {
					// APNs通信エラーあり
					logger.info("PUSH通知送信結果:エラーリスト[{}]:status={}, token={}, payload={}", i, apnsResult.getErrorList().get(i).getApnsNotificationErrorData().getStatus(), apnsResult.getErrorList().get(i).getToken(), apnsResult.getErrorList().get(i).getPayload());
				}
			}

			//-------------------------------
			// フィードバック情報取得
			//-------------------------------
			logger.info("フィードバック情報取得");
			ApnsFeedbackService apnsFeedbackService = new ApnsFeedbackService(isProduction, certFileName, certPassword);
			List<ApnsFeedback> apnsFeedbackList = apnsFeedbackService.feedback();
			logger.info("フィードバック情報件数={}", apnsFeedbackList.size());
			for (ApnsFeedback apnsFeedback : apnsFeedbackList) {
				logger.info("フィードバック情報:Timestamp={}, デバイストークン={}", df.format(apnsFeedback.getTimestamp()), apnsFeedback.getDeviceToken());
			}
		} catch (Exception e) {
			logger.error("バッチ処理エラー", e);
			System.exit(9);
		}
	}

	/**
	 * デバイストークンリストファイルの読み込み
	 * @return
	 * @throws IOException
	 */
	private static List<String> getTokenList(String tokenFilePath) throws IOException {
		// デバイストークンリストファイル読み込み
		List<String> tokenList = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(tokenFilePath));
			while (br.ready()) {
				String line = br.readLine().trim();

				if (line.length() == 0) {
					continue;
				}

				tokenList.add(line);
			}
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {
			}
			br = null;
		}
		return tokenList;
	}

}
