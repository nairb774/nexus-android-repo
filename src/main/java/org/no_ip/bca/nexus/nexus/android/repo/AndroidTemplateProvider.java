package org.no_ip.bca.nexus.nexus.android.repo;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.templates.TemplateProvider;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;

@Component(role = TemplateProvider.class, hint = "android-repository")
public class AndroidTemplateProvider extends AbstractRepositoryTemplateProvider {
    @Requirement
    private RepositoryTypeRegistry repositoryTypeRegistry;
    
    public TemplateSet getTemplates() {
        final TemplateSet templates = new TemplateSet(null);
        templates.add(new AndroidRepositoryTemplate(this, "android_virtual", "Android Proxy Repository"));
        return templates;
    }
    
    public void initialize() throws InitializationException {
        repositoryTypeRegistry.registerRepositoryTypeDescriptors(new RepositoryTypeDescriptor(ShadowRepository.class
                .getName(), AndroidShadowRepository.ID, "shadows"));
    }
}
