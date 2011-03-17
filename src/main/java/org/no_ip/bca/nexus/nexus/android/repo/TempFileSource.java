package org.no_ip.bca.nexus.nexus.android.repo;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.FileDeleteStrategy;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = TempFileSource.class)
public class TempFileSource {
    private static class AutoClose extends FileDeleteStrategy {
        private final FileCleaningTracker tracker;
        
        public AutoClose(final FileCleaningTracker tracker) {
            super("Auto close");
            this.tracker = tracker;
        }
        
        @Override
        protected boolean doDelete(final File fileToDelete) throws IOException {
            tracker.exitWhenFinished();
            return super.doDelete(fileToDelete);
        }
    }
    
    private final FileCleaningTracker tracker = new FileCleaningTracker();
    
    public TempFileSource() {
        tracker.track("foo", this, new AutoClose(tracker));
    }
    
    public File getTrackedTempFile(final String prefix, final String suffix) throws IOException {
        final File file = File.createTempFile(prefix, suffix);
        tracker.track(file.getAbsolutePath(), file);
        return file;
    }
}
