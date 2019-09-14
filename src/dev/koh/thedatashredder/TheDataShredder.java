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
    private long origFileCount;

    private MyTimer myTimer;
    private File targetFile;

    public TheDataShredder() {
        this(true, null);
    }

    public TheDataShredder(boolean shouldDamageEntireFile, File targetFile) {
        this.shouldDamageEntireFile = shouldDamageEntireFile;
        this.targetFile = targetFile;
    }

    public static void main(String[] args) {

        System.out.println("Begin.");

        TheDataShredder theDataShredder = new TheDataShredder();
        theDataShredder.start();

        System.out.println("\nEnd.");

    }

    void start() {

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

    }

    private void init() throws IOException {

        //  Initialize MyTimer
        myTimer = new MyTimer();
        myTimer.startTimer();

        //  User Input for filePath
        userInputFilePath();

    }

    private void userInputFilePath() throws IOException {

        String promptFilePath = "Enter Valid File/Dir. Path : ";
        String filePath = KOHStringUtil.userInputString(promptFilePath, StringOptions.DIR_OR_FILE, myTimer);

        if (filePath == null) {
            System.out.println("No Valid File/Dir. Path Entered!\n" +
                    "Program Terminating...");
            System.exit(-17);
        } else targetFile = new File(filePath);

        String promptDeleteEntireFile = "Wanna Delete Entire File? [Y/N] : ";
        String strYesOrNo = KOHStringUtil.userInputString(promptDeleteEntireFile, StringOptions.YES_OR_NO, myTimer);
        char ch = strYesOrNo != null ? strYesOrNo.charAt(0) : 'n';

        if (ch == 'y' || ch == 'Y')
            shouldDamageEntireFile = true;

        String targetType = targetFile.isDirectory() ? "Directory" : "File";
        System.out.println(targetType + " To Be Destroyed: \n" + targetFile.getCanonicalPath());

    }

    public void beginShredding() throws IOException {

        if (targetFile.isFile()) {
            damageFile(targetFile);
        } else if (targetFile.isDirectory()) {

            DirFilesCounter dirFilesCounter = new DirFilesCounter();
            Files.walkFileTree(targetFile.toPath(), dirFilesCounter);

            origFileCount = dirFilesCounter.getFileCount();

            DirTreeWalker dirTreeWalker = new DirTreeWalker();
            Files.walkFileTree(targetFile.toPath(), dirTreeWalker);

            System.out.println("\nFiles Destroyed : " + dirTreeWalker.filesCount);
            System.out.println("Dirs. Visited : " + dirTreeWalker.dirsCount);

        }

    }

    private void damageFile(File file) {

        this.fileLength = file.length();
        String tempFileAbsPath = file.getAbsolutePath();
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

        //  Rename File & Delete It!
        renameFileToCurrentTimeStamp(file, tempFileAbsPath);

    }

    private void renameFileToCurrentTimeStamp(File file, String fileAbsPath) {

        String currentTimeStamp = KOHStringUtil.generateCurrentTimeStamp();
        boolean hasRenamed = file.renameTo(new File(file.getParent(), currentTimeStamp + SHREDDED_EXTENSION));
//        boolean hasDeleted = file.delete();
        if (hasRenamed) System.out.println("File : [" + fileAbsPath + "] Shredded Successfully..!!\n");
        else System.out.println("Unable to Shred File : [" + fileAbsPath + "]   |   Shredding Failed..!!\n");

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

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

            //  Time Stamp : 22nd August 2K19, 01:57 AM..!!
            if (filesCount >= origFileCount)
                return FileVisitResult.SKIP_SUBTREE;

            filesCount++;
            damageFile(file.toFile());
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
            dirsCount++;
            renameFileToCurrentTimeStamp(dir.toFile(), dir.toAbsolutePath().toString());
            return FileVisitResult.CONTINUE;
        }
    }

}

/*
 *  Date Created : 21st August 2K19, 09:44 PM..!!
 *  Time Stamp : 14th September 2K19, 08:36 PM..!!
 *
 *  Change Log:
 *
 *  Init Commit - The Data Shredder [CLI]
 *  1. Shred Entire Files & Dirs. as well as Segments / Portions of Data.
 *
 *  Code Developed By,
 *  ~K.O.H..!! ^__^
 */
