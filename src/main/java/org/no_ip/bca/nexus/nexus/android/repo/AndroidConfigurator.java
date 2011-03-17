package org.no_ip.bca.nexus.nexus.android.repo;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.proxy.repository.AbstractShadowRepositoryConfigurator;

@Component(role = AndroidConfigurator.class)
public class AndroidConfigurator extends AbstractShadowRepositoryConfigurator {
}
