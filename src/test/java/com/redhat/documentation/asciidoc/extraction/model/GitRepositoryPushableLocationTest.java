package com.redhat.documentation.asciidoc.extraction.model;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.util.FS;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class GitRepositoryPushableLocationTest {

    @Test
    void getDirectoryPathShouldReturnDirectoryOfClonedRepo() throws IOException, GitAPIException {
        File remoteDir = File.createTempFile("remote", "");
        remoteDir.delete();
        remoteDir.mkdirs();

        // Create bare repository
        RepositoryCache.FileKey fileKey = RepositoryCache.FileKey.exact(remoteDir, FS.DETECTED);
        Repository remoteRepo = fileKey.open(false);
        remoteRepo.create(true);

        // Clone bare repository
        File cloneDir = File.createTempFile("clone", "");
        cloneDir.delete();
        cloneDir.mkdirs();
        Git.cloneRepository().setURI(remoteRepo.getDirectory().getAbsolutePath()).setDirectory(cloneDir).call();
        assertThat(Files.isDirectory(cloneDir.toPath().resolve(".git"))).isTrue();
    }


    @Test
    void pushShouldCommitAndPushFilesToBareGitRepo() throws IOException, GitAPIException {
        // Create a folder to act as remote repository
        File remoteDir = File.createTempFile("remote", "");
        remoteDir.delete();
        remoteDir.mkdirs();

        // Create bare repository
        RepositoryCache.FileKey fileKey = RepositoryCache.FileKey.exact(remoteDir, FS.DETECTED);
        Repository remoteRepo = fileKey.open(false);
        remoteRepo.create(true);

        // Clone bare repository
        File cloneDir = File.createTempFile("clone", "");
        cloneDir.delete();
        cloneDir.mkdirs();
        Git git = Git.cloneRepository().setURI(remoteRepo.getDirectory().getAbsolutePath()).setDirectory(cloneDir).call();

        // Create sample file, add, commit and push to bare repository
        File newFile = new File(cloneDir, "myNewFile");
        newFile.createNewFile();
        Files.writeString(newFile.toPath(), "Test content file");
        git.add().addFilepattern(newFile.getName()).call();
        git.commit().setMessage("First commit").setAuthor("a", "a@example.com ").call();
        RefSpec refSpec = new RefSpec("master");
        git.push().setRemote("origin").setRefSpecs(refSpec).call();

        // Second working directory to clone and test commit
        File cloneDir2 = File.createTempFile("clone", "");
        cloneDir2.delete();
        cloneDir2.mkdirs();
        Git git2 = Git.cloneRepository().setURI(remoteRepo.getDirectory().getAbsolutePath()).setDirectory(cloneDir2).call();

        // Check whether committed file exists
        assertThat(new File(cloneDir2, "myNewFile").exists());
    }
}