package org.no_ip.bca.nexus.nexus.android.repo;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.nexus.proxy.maven.LayoutConverterShadowRepositoryConfiguration;

public class AndroidConfiguration extends LayoutConverterShadowRepositoryConfiguration {
    public AndroidConfiguration(final Xpp3Dom configuration) {
        super(configuration);
    }
}
