package dev.koh.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DirFilesCounter extends SimpleFileVisitor<Path> {

    private long fileCount;
    private long dirCount;
    private long dirSize;

    public long getFileCount() {
        return fileCount;
    }

    public long getDirCount() {
        return dirCount;
    }

    public long getDirSize() {
        return dirSize;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

        //  Increment the filesCount by 1
        fileCount++;

        //  Update dirSize by adding the currentFileSize
        double currentFileSize = attrs.size();
        dirSize += currentFileSize;

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        System.out.println("\nFAILED to Visit File. : " + file.toAbsolutePath() + "\n");
        System.out.println(exc.getMessage());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        dirCount++;
        return FileVisitResult.CONTINUE;
    }

}

