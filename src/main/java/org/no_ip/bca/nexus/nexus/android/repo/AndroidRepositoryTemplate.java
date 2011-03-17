package org.no_ip.bca.nexus.nexus.android.repo;

import java.io.IOException;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.MavenShadowRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

public class AndroidRepositoryTemplate extends AbstractRepositoryTemplate {
    private RepositoryPolicy repositoryPolicy;
    
    public AndroidRepositoryTemplate(final AndroidTemplateProvider provider, final String id, final String description) {
        super(provider, id, description, new Maven2ContentClass(), MavenShadowRepository.class);
        setRepositoryPolicy(null);
    }
    
    @Override
    public MavenRepository create() throws ConfigurationException, IOException {
        final MavenRepository mavenRepository = (MavenRepository) super.create();
        if (getRepositoryPolicy() != null) {
            mavenRepository.setRepositoryPolicy(getRepositoryPolicy());
        }
        return mavenRepository;
    }
    
    public RepositoryPolicy getRepositoryPolicy() {
        return repositoryPolicy;
    }
    
    @Override
    protected CRepositoryCoreConfiguration initCoreConfiguration() {
        final CRepository repo = new DefaultCRepository();
        repo.setId("");
        repo.setName("");
        repo.setProviderRole(ShadowRepository.class.getName());
        repo.setProviderHint(AndroidShadowRepository.ID);
        final Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
        repo.setExternalConfiguration(ex);
        final AndroidConfiguration exConf = new AndroidConfiguration(ex);
        repo.externalConfigurationImple = exConf;
        repo.setWritePolicy(RepositoryWritePolicy.READ_ONLY.name());
        final CRepositoryCoreConfiguration result = new CRepositoryCoreConfiguration(getTemplateProvider()
                .getApplicationConfiguration(), repo,
                new CRepositoryExternalConfigurationHolderFactory<AndroidConfiguration>() {
                    public AndroidConfiguration createExternalConfigurationHolder(final CRepository config) {
                        return new AndroidConfiguration((Xpp3Dom) config.getExternalConfiguration());
                    }
                });
        return result;
    }
    
    public void setRepositoryPolicy(final RepositoryPolicy repositoryPolicy) {
        this.repositoryPolicy = repositoryPolicy;
    }
    
    @Override
    public boolean targetFits(final Object clazz) {
        return super.targetFits(clazz) || clazz.equals(getRepositoryPolicy());
    }
    
}
