package org.no_ip.bca.nexus.nexus.android.repo;

import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.plugins.rest.AbstractNexusIndexHtmlCustomizer;
import org.sonatype.nexus.plugins.rest.NexusIndexHtmlCustomizer;

@Component(role = NexusIndexHtmlCustomizer.class, hint = "AndroidIndexHtmlCustomizer")
public class AndroidIndexHtmlCustomizer extends AbstractNexusIndexHtmlCustomizer {
    @Override
    public String getPostHeadContribution(final Map<String, Object> context) {
        final String version = getVersionFromJarFile("/META-INF/maven/org.no-ip.bca.nexus/nexus-android-repo/pom.properties");
        return "<script src=\"static/js/android-plugin-all.js" + (version == null ? "" : "?" + version)
                + "\" type=\"text/javascript\" charset=\"utf-8\"></script>";
    }
}