package com.linkvault.common.config;

import com.linkvault.folders.Folder;
import com.linkvault.folders.FolderRepository;
import com.linkvault.resources.Resource;
import com.linkvault.resources.ResourceRepository;
import com.linkvault.resources.ResourceType;
import com.linkvault.users.User;
import com.linkvault.users.UserRepository;
import com.linkvault.vaults.Vault;
import com.linkvault.vaults.VaultRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VaultRepository vaultRepository;
    private final FolderRepository folderRepository;
    private final ResourceRepository resourceRepository;

    public DemoDataSeeder(
        UserRepository userRepository,
        VaultRepository vaultRepository,
        FolderRepository folderRepository,
        ResourceRepository resourceRepository
    ) {
        this.userRepository = userRepository;
        this.vaultRepository = vaultRepository;
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
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
        user.setPasswordHash("demo-password-not-for-auth");
        user.setDisplayName("Demo User");
        user = userRepository.save(user);

        Vault vault = new Vault();
        vault.setUser(user);
        vault.setName("Learning");
        vault.setDescription("A small demo vault for LinkVault");
        vault.setIcon("book-open");
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
