package com.linkvault.common.config;

import com.linkvault.common.response.ApiResponse;
import com.linkvault.folders.entity.Folder;
import com.linkvault.folders.repository.FolderRepository;
import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.enums.ResourceType;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.users.entity.User;
import com.linkvault.users.repository.UserRepository;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.vaults.repository.VaultRepository;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.service.WorkspaceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app", name = "seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VaultRepository vaultRepository;
    private final FolderRepository folderRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkspaceService workspaceService;

    public DemoDataSeeder(
        UserRepository userRepository,
        VaultRepository vaultRepository,
        FolderRepository folderRepository,
        ResourceRepository resourceRepository,
        PasswordEncoder passwordEncoder,
        WorkspaceService workspaceService
    ) {
        this.userRepository = userRepository;
        this.vaultRepository = vaultRepository;
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
        this.passwordEncoder = passwordEncoder;
        this.workspaceService = workspaceService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User user = new User();
        user.setUsername("demo");
        user.setEmail("demo@linkvault.local");
        user.setPasswordHash(passwordEncoder.encode("demo123456"));
        user.setDisplayName("Demo User");
        user.setIsVerified(true);
        user.setIsEnabled(true);
        user.setAuthProvider("LOCAL");
        user = userRepository.save(user);
        Workspace workspace = workspaceService.createDefaultWorkspaceForUser(user);

        Vault vault = new Vault();
        vault.setUser(user);
        vault.setWorkspace(workspace);
        vault.setName("Learning");
        vault.setDescription("A small demo vault for LinkVault");
        vault.setIcon("menu_book");
        vault.setColor("#2f7d6d");
        vault = vaultRepository.save(vault);

        Folder folder = new Folder();
        folder.setVault(vault);
        folder.setName("Spring Boot");
        folder.setDescription("Backend learning notes");
        folder.setIcon("folder");
        folder.setSortOrder(1);
        folder = folderRepository.save(folder);

        Resource link = new Resource();
        link.setVault(vault);
        link.setFolder(folder);
        link.setTitle("Spring Boot Documentation");
        link.setResourceType(ResourceType.LINK);
        link.setUrl("https://docs.spring.io/spring-boot/");
        link.setSourceName("Spring Docs");
        resourceRepository.save(link);

        Resource note = new Resource();
        note.setVault(vault);
        note.setFolder(folder);
        note.setTitle("JPA note");
        note.setResourceType(ResourceType.NOTE);
        note.setContent("Use repositories from services and return DTOs to the frontend.");
        resourceRepository.save(note);

        Resource snippet = new Resource();
        snippet.setVault(vault);
        snippet.setFolder(folder);
        snippet.setTitle("Small Java snippet");
        snippet.setResourceType(ResourceType.SNIPPET);
        snippet.setCodeLanguage("java");
        snippet.setContent("record ApiResponse<T>(boolean success, String message, T data) {}");
        resourceRepository.save(snippet);
    }
}