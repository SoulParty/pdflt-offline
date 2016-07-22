package lt.nortal.pdflt;

import com.itextpdf.text.log.Logger;
import com.itextpdf.text.log.LoggerFactory;
import lt.nortal.rc.unisign.util.DecodeInputStream;
import lt.nortal.rc.unisign.util.EncodeOutputStream;
import lt.webmedia.sigute.service.common.utils.FileUtils;

import java.io.*;
import java.security.InvalidParameterException;

/**
 * Created by DK on 7/7/2016.
 */

public class StorageHelper {
    static final Logger logger = LoggerFactory.getLogger(StorageHelper.class);

    private File storagePath;

    public static File initStorageDir(final String pathname) {
        File storageDir = new File(pathname);
        if (!storageDir.exists()) {
            logger.info("Directory for specified storage path " + pathname + " does not exist. Creating a new one.");
            if (!storageDir.mkdirs()) {
                logger.error("Failed to create directories for path " + pathname + " application may not operate in this environment.");
            }
        }

        if (!storageDir.isDirectory()) {
            logger.error("Specified storage path '" + pathname + "' is not a directory.");
            throw new InvalidParameterException("Specified storage path '" + pathname + "' is not a directory.");
        }
        if (!storageDir.canWrite()) {
            logger.error("Specified storage path '" + pathname + "' is not a writable location.");
            throw new InvalidParameterException("Specified storage path '" + pathname + "' is not a writable location.");
        }
        return storageDir;
    }


    protected File getFile(String fileId, String extension) {
        return new File(storagePath, fileId + extension);
    }

    protected OutputStream getEncodedOutStream(File file, byte[] key) throws FileNotFoundException {
        return new EncodeOutputStream(new FileOutputStream(file), key);
    }

    protected InputStream getDecodedInStream(File file, byte[] key) throws FileNotFoundException {
        return new DecodeInputStream(new FileInputStream(file), key);
    }

    protected byte[] readEncodedFile(File file, byte[] key, int docLength) throws IOException {
        InputStream in = getDecodedInStream(file, key);
        ByteArrayOutputStream out = new ByteArrayOutputStream(docLength);
        FileUtils.copyStreamsAndClose(in, out);
        return out.toByteArray();
    }
}
