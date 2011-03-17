package org.no_ip.bca.nexus.nexus.android.repo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.GavCalculator;
import org.sonatype.nexus.artifact.IllegalArtifactCoordinateException;
import org.sonatype.nexus.artifact.M2ArtifactRecognizer;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.mime.MimeUtil;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.maven.ArtifactPackagingMapper;
import org.sonatype.nexus.proxy.maven.ArtifactStoreHelper;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.MavenShadowRepository;
import org.sonatype.nexus.proxy.maven.MetadataManager;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractShadowRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.local.fs.FileContentLocator;

@Component(role = ShadowRepository.class, hint = AndroidShadowRepository.ID, instantiationStrategy = "per-lookup", description = "Android DX Shadow")
public class AndroidShadowRepository extends AbstractShadowRepository implements MavenRepository {
    /** This "mimics" the @Named("android-shadow") */
    public static final String ID = "android-shadow";
    
    @Requirement
    private AndroidConfigurator androidConfigurator;
    
    @Requirement
    private ArtifactPackagingMapper artifactPackagingMapper;
    
    private ArtifactStoreHelper artifactStoreHelper;
    
    @Requirement(hint = Maven2ContentClass.ID)
    private ContentClass contentClass;
    
    @Requirement
    private JarFixer jarFixer;
    
    @Requirement(hint = "maven2")
    private GavCalculator m2GavCalculator;
    
    @Requirement
    private MetadataManager metadataManager;
    
    @Requirement
    private MimeUtil mimeUtil;
    private final RepositoryKind repositoryKind = new DefaultRepositoryKind(MavenShadowRepository.class, Arrays
            .asList(new Class<?>[] { MavenRepository.class }));
    
    @Override
    protected StorageLinkItem createLink(final StorageItem item) throws UnsupportedStorageOperationException,
            IllegalOperationException, StorageException {
        final String path = item.getPath();
        if (path.startsWith("/.meta/repository-metadata.xml")) {
            return null;
        }
        final String mimeType = mimeUtil.getMimeType(path);
        Gav gav;
        try {
            gav = getGavCalculator().pathToGav(path);
        } catch (final IllegalArtifactCoordinateException e) {
            gav = null;
        }
        
        if (gav != null && "application/java-archive".equals(mimeType)) {
            getLogger().info("Converting: " + item.getPath() + " " + mimeType);
            final StorageFileItem cachedItem;
            try {
                cachedItem = getCachedItem(item);
            } catch (final ItemNotFoundException e) {
                throw new LocalStorageException(e);
            }
            try {
                final File fixedJar = jarFixer.fixJar(cachedItem.getInputStream());
                final ResourceStoreRequest req = new ResourceStoreRequest(item);
                final DefaultStorageFileItem fileItem = new DefaultStorageFileItem(this, req, true, false,
                        new FileContentLocator(fixedJar, mimeType));
                getArtifactStoreHelper().storeItemWithChecksums(false, fileItem);
            } catch (final IOException e) {
                throw new LocalStorageException(e);
            }
        } else {
            getLogger().info("Pass: " + item.getPath() + " " + mimeType);
            final ResourceStoreRequest req = new ResourceStoreRequest(item);
            final DefaultStorageLinkItem link = new DefaultStorageLinkItem(this, req, true, true, item
                    .getRepositoryItemUid());
            storeItem(false, link);
            return link;
        }
        return null;
    }
    
    public void deleteItemWithChecksums(final boolean fromTask, final ResourceStoreRequest request)
            throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException,
            StorageException {
        getArtifactStoreHelper().deleteItemWithChecksums(fromTask, request);
    }
    
    public void deleteItemWithChecksums(final ResourceStoreRequest request)
            throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
            StorageException, AccessDeniedException {
        getArtifactStoreHelper().deleteItemWithChecksums(request);
    }
    
    @Override
    protected void deleteLink(final StorageItem item) throws UnsupportedStorageOperationException,
            IllegalOperationException, ItemNotFoundException, StorageException {
        deleteItem(false, new ResourceStoreRequest(item));
    }
    
    @Override
    protected StorageItem doRetrieveItem(final ResourceStoreRequest request) throws IllegalOperationException,
            ItemNotFoundException, StorageException {
        try {
            return super.doRetrieveItem(request);
        } catch (final ItemNotFoundException e) {
            final StorageItem result;
            request.pushRequestPath(request.getRequestPath());
            try {
                result = doRetrieveItemFromMaster(request);
            } finally {
                request.popRequestPath();
            }
            
            try {
                final StorageLinkItem link = createLink(result);
                
                if (link != null) {
                    return link;
                } else {
                    return result;
                }
            } catch (final Exception e1) {
                return result;
            }
        }
    }
    
    public ArtifactPackagingMapper getArtifactPackagingMapper() {
        return artifactPackagingMapper;
    }
    
    public ArtifactStoreHelper getArtifactStoreHelper() {
        if (artifactStoreHelper == null) {
            artifactStoreHelper = new ArtifactStoreHelper(this) {
            };
        }
        
        return artifactStoreHelper;
    }
    
    private StorageFileItem getCachedItem(final StorageItem item) throws LocalStorageException, ItemNotFoundException {
        final RepositoryItemUid uid = item.getRepositoryItemUid();
        final Repository repository = uid.getRepository();
        final ResourceStoreRequest request = new ResourceStoreRequest(uid.getPath());
        return (StorageFileItem) repository.getLocalStorage().retrieveItem(repository, request);
    }
    
    @Override
    protected Configurator getConfigurator() {
        return androidConfigurator;
    }
    
    @Override
    protected AndroidConfiguration getExternalConfiguration(final boolean forWrite) {
        return (AndroidConfiguration) super.getExternalConfiguration(forWrite);
    }
    
    @Override
    protected CRepositoryExternalConfigurationHolderFactory<AndroidConfiguration> getExternalConfigurationHolderFactory() {
        return new CRepositoryExternalConfigurationHolderFactory<AndroidConfiguration>() {
            public AndroidConfiguration createExternalConfigurationHolder(final CRepository config) {
                return new AndroidConfiguration((Xpp3Dom) config.getExternalConfiguration());
            }
        };
    }
    
    public GavCalculator getGavCalculator() {
        return m2GavCalculator;
    }
    
    @Override
    public MavenRepository getMasterRepository() {
        return super.getMasterRepository().adaptToFacet(MavenRepository.class);
    }
    
    public ContentClass getMasterRepositoryContentClass() {
        return contentClass;
    }
    
    public MetadataManager getMetadataManager() {
        return metadataManager;
    }
    
    public ContentClass getRepositoryContentClass() {
        return contentClass;
    }
    
    public RepositoryKind getRepositoryKind() {
        return repositoryKind;
    }
    
    public RepositoryPolicy getRepositoryPolicy() {
        return getMasterRepository().getRepositoryPolicy();
    }
    
    public boolean isMavenArtifact(final StorageItem item) {
        return isMavenArtifactPath(item.getPath());
    }
    
    public boolean isMavenArtifactPath(final String path) {
        try {
            return getGavCalculator().pathToGav(path) != null;
        } catch (final IllegalArtifactCoordinateException e) {
        }
        
        return false;
    }
    
    public boolean isMavenMetadata(final StorageItem item) {
        return isMavenMetadataPath(item.getPath());
    }
    
    public boolean isMavenMetadataPath(final String path) {
        return M2ArtifactRecognizer.isMetadata(path);
    }
    
    public boolean recreateMavenMetadata(final ResourceStoreRequest request) {
        return false;
    }
    
    public void setRepositoryPolicy(final RepositoryPolicy repositoryPolicy) {
        throw new UnsupportedOperationException("This method is not supported on Repository of type SHADOW");
    }
    
    public void storeItemWithChecksums(final boolean fromTask, final AbstractStorageItem item)
            throws UnsupportedStorageOperationException, IllegalOperationException, StorageException {
        throw new UnsupportedStorageOperationException("Not implemented");
    }
    
    public void storeItemWithChecksums(final ResourceStoreRequest request, final InputStream is,
            final Map<String, String> userAttributes) throws UnsupportedStorageOperationException,
            ItemNotFoundException, IllegalOperationException, StorageException, AccessDeniedException {
        throw new UnsupportedStorageOperationException("Not implemented");
    }
}
