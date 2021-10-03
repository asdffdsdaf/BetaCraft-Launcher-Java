package pl.betacraft.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.betacraft.launcher.BC;
import org.betacraft.launcher.DownloadResult;

import pl.betacraft.auth.CustomRequest.RequestType;

public class DownloadRequest extends Request {
	public String destination;
	public RequestType type;
	public String hash_sha1;
	public boolean force;

	public DownloadRequest(String url, String destination, String expected_sha1, boolean force) {
		this(url, destination, expected_sha1, force, null, null, RequestType.GET);
	}

	public DownloadRequest(String url, String destination, String expected_sha1, boolean force, String post_data, Map<String, String> properties, RequestType type) {
		this.destination = destination;
		if (expected_sha1 == null) expected_sha1 = "";
		this.hash_sha1 = expected_sha1;
		this.type = type;
		this.REQUEST_URL = url;
		if (post_data != null) this.POST_DATA = post_data;
		if (properties != null) this.PROPERTIES = properties;
	}

	public DownloadResponse perform() {
		File file = new File(this.destination);
		File backupfile = new File(BC.get() + "launcher" + File.separator + "backup.tmp");
		boolean dl_failed = false;
		String err = null;

		try {
			file.getParentFile().mkdirs();
			if (file.isDirectory()) {
				file.delete();
			}

			// Save a copy of the current file in case of failure
			if (!file.createNewFile()) {
				backupfile.createNewFile();
				IOUtils.copy(new FileInputStream(file.getPath()), new FileOutputStream(backupfile.getPath()));
			}

			byte[] data = RequestUtil.performRawGETRequest(this);
			if (data != null) {
				IOUtils.write(data, new FileOutputStream(file.getPath()));
			} else {
				dl_failed = true;
			}
		} catch (Throwable t) {
			t.printStackTrace();
			dl_failed = true;
			err = t.getMessage();
		}

		DownloadResult result = DownloadResult.OK;

		// Restore the copy if existed
		if (backupfile.exists()) {
			if (dl_failed) {
				try {
					IOUtils.copy(new FileInputStream(backupfile.getPath()), new FileOutputStream(file.getPath()));
					result = DownloadResult.FAILED_WITH_BACKUP;
				} catch (IOException e) {
					result = DownloadResult.FAILED_WITHOUT_BACKUP;
					file.delete();
				}
			}
			backupfile.delete();
		} else if (dl_failed) {
			result = DownloadResult.FAILED_WITHOUT_BACKUP;
			file.delete();
		}
		return new DownloadResponse(result, err);
	}
}
