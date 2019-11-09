package dev.koh.thedatashredder;

import dev.koh.utils.DirFilesCounter;
import dev.koh.utils.KOHStringUtil;
import dev.koh.utils.MyTimer;
import dev.koh.utils.enums.StringOptions;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class TheDataShredder {

    private static final String SHREDDED_EXTENSION = ".SHREDDED";
    private static long sharedCurrentFilePointer;
    private static final long DMG_PERCENTAGE = 20;
    private static final int BUFFER_SIZE = 8;  //  [MB]
    private static final int ONE_MB = (1 << 20);
    private static final int TEN_MB = ONE_MB * 10;
    private static final long FILE_SIZE_THRESHOLD_LIMIT = ONE_MB * 64;
    private long fileLength;
    private boolean shouldDamageEntireFile;
    private boolean shouldDeleteFiles;
    private int listCode;
    private long origFileCount;

    private MyTimer myTimer;
    private File targetFile;

    public TheDataShredder() {
        this(true, false, null, 0);
    }

    public TheDataShredder(boolean shouldDamageEntireFile, boolean shouldDeleteFiles, File targetFile) {
        this(shouldDamageEntireFile, shouldDeleteFiles, targetFile, 0);
    }

    public TheDataShredder(boolean shouldDamageEntireFile, boolean shouldDeleteFiles, int listCode) {
        this(shouldDamageEntireFile, shouldDeleteFiles, null, listCode);
    }

    public TheDataShredder(boolean shouldDamageEntireFile, boolean shouldDeleteFiles, File targetFile, int listCode) {
        this.shouldDamageEntireFile = shouldDamageEntireFile;
        this.shouldDeleteFiles = shouldDeleteFiles;
        this.listCode = listCode;

        if (targetFile == null) {
            String filePath = KOHStringUtil.userInputString("Enter Src Dir. Path : ", StringOptions.DIR_OR_FILE, new MyTimer());
            assert filePath != null;
            this.targetFile = new File(filePath);
        } else {
            this.targetFile = targetFile;
        }

    }

    public void start() {

        try {

            init();
            beginShredding();

        } catch (IOException e) {
            e.printStackTrace();
        }

        myTimer.stopTimer(true);
        System.out.println("\n|================================|\n");

    }

/*
    private void init() throws IOException {

        //  Initialize MyTimer
        myTimer = new MyTimer();
        myTimer.startTimer();

*/
/*
        //  User Input for filePath
        userInputFilePath();*//*


    }
*/

    /*void start1() {

        try {

            boolean tryAgain = true;
            while (tryAgain) {

                init();
                beginShredding();
                myTimer.stopTimer(true);

                System.out.println("\n|================================|\n");
                String promptFilePath = "Wanna Shred More? [Y/N] : ";
                String strYesOrNo = KOHStringUtil.userInputString(promptFilePath, StringOptions.YES_OR_NO, myTimer);
                char ch = strYesOrNo != null ? strYesOrNo.charAt(0) : 'n';
                tryAgain = ch == 'y';// ? true : false;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/

    private void init() {

        //  Initialize MyTimer
        myTimer = new MyTimer();
        myTimer.startTimer();

    }

    private void beginShredding() throws IOException {

        if (targetFile.isFile()) {

            damageFile(targetFile);

            if (shouldDeleteFiles) {
                boolean hasDeleted = deleteFileNow(targetFile);
                if (hasDeleted)
                    System.out.println("File : [" + targetFile.getAbsolutePath() + "]\t|\tShredded Successfully..!!\n");
                else
                    System.out.println("Unable to Shred File : [" + targetFile.getAbsolutePath() + "]\t|\tShredding Failed..!!\n");
            }

        } else if (targetFile.isDirectory()) {

            DirFilesCounter dirFilesCounter = new DirFilesCounter();
            Files.walkFileTree(targetFile.toPath(), dirFilesCounter);

            origFileCount = dirFilesCounter.getFileCount();

            DirTreeWalker dirTreeWalker = new DirTreeWalker();
            Files.walkFileTree(targetFile.toPath(), dirTreeWalker);

            System.out.println("\nFiles Destroyed : " + dirTreeWalker.filesCount);
            System.out.println("Dirs. Visited : " + dirTreeWalker.dirsCount);
            System.out.println("Failed to Destroy : " + dirTreeWalker.failureCount);
            System.out.println("Deleted Files Count : " + dirTreeWalker.deletedFilesCount);

            //  Display list of files' absolute path
            if (listCode != 0)
                dirTreeWalker.displayLists(listCode);

        }

    }

    private void damageFile(File file) {

        //  TODO : Abstract Print Logs when Shredding Files

        this.fileLength = file.length();
        System.out.println("Currently Processing: [" + file.getAbsolutePath() + "]");

        try (RandomAccessFile raf = new RandomAccessFile(file, "rwd")) {

            //  DAMAGE Entire File
            if (shouldDamageEntireFile || (fileLength < FILE_SIZE_THRESHOLD_LIMIT)) {
                damageEntireFile(raf);
            } else {
                //  Otherwise, Damage DMG_PERCENTAGE % of the File
                damageFileHeaderAndFooter(raf);
                damageMajorFileSegment(raf);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("I/O Exception Occurred!" +
                    "\nProgram Terminated...");
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("Interrupted Exception Occurred!" +
                    "\nProgram Terminated...");
        }

    }

    private boolean renameFileNameToStr(File file, String newFileName) {
        //  TODO : Move this method to KOHFilesUtil

        boolean hasRenamed;/* = file.renameTo(new File(file.getParentFile(), currentTimeStamp + SHREDDED_EXTENSION));*/

        try {
            Files.move(file.toPath(), file.toPath().resolveSibling(newFileName));
            hasRenamed = true;
        } catch (IOException e) {
            e.printStackTrace();
            hasRenamed = false;
        }

        return hasRenamed;

    }

    private boolean deleteFileNow(File file) {
        //  TODO : Move this method to KOHFilesUtil

        boolean hasDeleted;

        try {
            hasDeleted = Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            hasDeleted = false;
        }

        return hasDeleted;

    }

    private void damageEntireFile(RandomAccessFile raf) throws IOException, InterruptedException {

        byte[] buffer = new byte[ONE_MB * 8];

//        System.out.println("\nCurrently Processing : [" + targetFile.getAbsolutePath() + "]");

        Runnable runnable = () -> {
            /*
                Time Stamp : 22nd August 2K19, 12:56 AM..!!
                sharedCurrentFilePointer -> value of i i.e. current Pos.
                        Following Condition :
                (sharedCurrentFilePointer + buffer.length > fileLength) == true
                only when the Main Thread has completed the Processing.
             */
            while (sharedCurrentFilePointer + buffer.length < fileLength) {
                System.out.print((sharedCurrentFilePointer * 100 / fileLength) + "%");

                try {
                    Thread.sleep(1);
//                        this.wait(1000);
                    System.out.print("\b\b\b");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.print("\b\b\b");
            System.out.println("100%\nFile Destroyed Successfully!");

        };
        Thread displayPercentageThread = new Thread(runnable);
        displayPercentageThread.start();

        //  DAMAGE Entire File!
        for (long i = 0; i < fileLength; i += buffer.length) {
            raf.seek(i);
            raf.write(buffer);
            sharedCurrentFilePointer = i;

        }
        Thread.sleep(20);
        sharedCurrentFilePointer += buffer.length * 2;
        displayPercentageThread.join();

    }

    private void damageMajorFileSegment(RandomAccessFile raf) throws IOException, InterruptedException {

        byte[] buffer = new byte[ONE_MB * BUFFER_SIZE];
        long numOfBytesToDestroy = (fileLength * DMG_PERCENTAGE) / 100;
        long numOfPartitions = fileLength * buffer.length / numOfBytesToDestroy;

        Runnable runnable = () -> {
            /*
                Time Stamp : 22nd August 2K19, 12:26 AM..!!
                sharedCurrentFilePointer -> value of i i.e. current Pos.
                        Following Condition :
                (sharedCurrentFilePointer + numOfPartitions > fileLength - TEN_MB) == true
                only when the Main Thread has completed the Processing.
             */
            while (sharedCurrentFilePointer + numOfPartitions < fileLength - TEN_MB) {
                System.out.print(sharedCurrentFilePointer * 100 / fileLength + "%");

                try {
                    Thread.sleep(10);
                    System.out.print("\b\b\b");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//            System.out.print("\n100%\nFile Destroyed Successfully!");
            System.out.print("\b\b\b");
            System.out.println("100%\nFile Destroyed Successfully!");


        };
        Thread displaYPercentageThread = new Thread(runnable);
        displaYPercentageThread.start();

        for (long i = TEN_MB; i < fileLength - TEN_MB; i += numOfPartitions) {

            //  DAMAGE File Header
            raf.seek(i);
            raf.write(buffer);
            sharedCurrentFilePointer = i;

        }
        Thread.sleep(20);
    }

    private void damageFileHeaderAndFooter(RandomAccessFile raf) throws IOException {

        byte[] tenMBBuffer = new byte[TEN_MB];

        //  DAMAGE File Header
        raf.seek(0);
        raf.write(tenMBBuffer);

        //  DAMAGE File Footer
        long footerPos = (fileLength - tenMBBuffer.length);
//        System.out.println("fp: " + footerPos + " | fL : " + fileLength);
        raf.seek(footerPos);
        raf.write(tenMBBuffer);

    }

    public class DirTreeWalker extends SimpleFileVisitor<Path> {

        long filesCount;
        long dirsCount;
        long failureCount;
        int deletedFilesCount;
        List<String> listOfFailedFiles = new ArrayList<>();
        List<String> listOfSuccessfulFiles = new ArrayList<>();

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

            //  Time Stamp : 22nd August 2K19, 01:57 AM..!!
            //  Avoiding Re-traversal of dir tree due to renaming of files
            if (filesCount >= origFileCount)
                return FileVisitResult.SKIP_SUBTREE;

            filesCount++;
            damageFile(file.toFile());

            //  Rename File & Delete It!
            handleRenameAndDeleteFile(file.toFile());

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            failureCount++;
            System.out.println("\nFAILED to Visit File. : " + file.toAbsolutePath() + "\n" + exc.getMessage());
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            dirsCount++;
            handleRenameAndDeleteFile(dir.toFile());
            return FileVisitResult.CONTINUE;
        }

        private void handleRenameAndDeleteFile(File file) {

            if (shouldDeleteFiles) {
                boolean hasDeleted = deleteFileNow(file);
                if (hasDeleted) {
                    deletedFilesCount++;
                    listOfSuccessfulFiles.add(file.getAbsolutePath());
                } else {
                    failureCount++;
                    listOfFailedFiles.add(file.getAbsolutePath());
                    System.out.println("Unable to Shred File : [" + file.getAbsolutePath() + "]\t|\tShredding Failed..!!\n");
                }
            } else {

                String shreddedFileName = KOHStringUtil.generateCurrentTimeStamp() + " - " + System.nanoTime() + SHREDDED_EXTENSION;
                boolean hasRenamed = renameFileNameToStr(file, shreddedFileName);

                if (hasRenamed)
                    listOfSuccessfulFiles.add(file.getAbsolutePath());
                else {
                    System.out.println("Unable to Shred File : [" + file.getAbsolutePath() + "]\t|\tShredding Failed..!!\n");
                    listOfFailedFiles.add(file.getAbsolutePath());
                }
            }

        }

        public void displayLists(int listCode) {

            switch (listCode) {
                case 1:
                    if (listOfFailedFiles.isEmpty())
                        System.out.println("No Failed Files");
                    else {
                        System.out.println("Failed List of Files :");
                        for (String ff : listOfFailedFiles)
                            System.out.println(ff);
                    }
                    break;

                case 2:
                    if (listOfSuccessfulFiles.isEmpty())
                        System.out.println("No Successful Files");
                    else {
                        System.out.println("Successful List of Files :");
                        for (String sf : listOfSuccessfulFiles)
                            System.out.println(sf);
                    }
                    break;

                case 3:
                    if (listOfFailedFiles.isEmpty())
                        System.out.println("No Failed Files");
                    else {
                        System.out.println("Failed List of Files :");
                        for (String ff : listOfFailedFiles)
                            System.out.println(ff);
                    }

                    if (listOfSuccessfulFiles.isEmpty())
                        System.out.println("No Successful Files");
                    else {
                        System.out.println("Successful List of Files :");
                        for (String sf : listOfSuccessfulFiles)
                            System.out.println(sf);
                    }
                    break;

                default:
                    System.out.println("Invalid listCode!\n1 --> listOfFailedFiles" +
                            "\n2 --> listOfSuccessfulFiles\n3 --> Both the lists");
            }

        }
    }

}
