package org.no_ip.bca.nexus.nexus.android.repo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@Component(role = JarFixer.class)
public class JarFixer {
    private final String dxLocation = "/tmp/dx";
    private final Lock dxLock = new ReentrantLock(true);
    @Requirement
    private Logger logger;
    
    @Requirement
    private TempFileSource tempFileSource;
    
    public File fixJar(final InputStream is) throws IOException {
        final File startJar;
        try {
            startJar = tempFileSource.getTrackedTempFile("android-dx", ".jar");
            logger.info("Writing stream to: " + startJar.getAbsolutePath());
            final FileOutputStream fos = new FileOutputStream(startJar);
            try {
                IOUtils.copy(is, fos);
            } finally {
                IOUtils.closeQuietly(fos);
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
        final File endJar = tempFileSource.getTrackedTempFile("android-dx", ".jar");
        FileUtils.deleteQuietly(endJar);
        logger.info("Running dx against " + startJar.getAbsolutePath() + " to produce " + endJar.getAbsolutePath());
        final ProcessBuilder pb = new ProcessBuilder(dxLocation, "--dex", "--output=" + endJar.getAbsolutePath(),
                startJar.getAbsolutePath()).redirectErrorStream(true);
        dxLock.lock();
        try {
            runDx(pb);
        } finally {
            dxLock.unlock();
        }
        return endJar;
    }
    
    private void runDx(final ProcessBuilder pb) throws IOException {
        final Process process = pb.start();
        try {
            IOUtils.closeQuietly(process.getOutputStream());
            final InputStream errorStream = process.getErrorStream();
            final InputStream inputStream = process.getInputStream();
            if (errorStream != inputStream) {
                /*
                 * Sometimes (like the IBM JVM J9) error and "input" are the
                 * same instance. In this case we don't want to close error as
                 * it will close input
                 */
                IOUtils.closeQuietly(errorStream);
            }
            try {
                IOUtils.copy(inputStream, System.out);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
            final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            boolean interrupted = false;
            while (System.nanoTime() < end) {
                try {
                    process.exitValue();
                    break;
                } catch (final IllegalThreadStateException e) {
                }
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            logger.info("dx finished");
        } finally {
            process.destroy();
        }
    }
}
