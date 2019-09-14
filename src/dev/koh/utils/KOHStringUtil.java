package dev.koh.utils;

import dev.koh.utils.enums.StringOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class KOHStringUtil {

    public static String userInputString(String promptMsg, StringOptions stringOptions, MyTimer myTimer) {

        String str;
        boolean invalidInput = false;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        do {
            str = null;
            System.out.println(promptMsg);

            myTimer.pauseTimer();

            try {
                str = bufferedReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            myTimer.continueTimer();

            //  Check for options...
            //  Criteria for deciding whether str is valid or not

            switch (stringOptions) {
                case DEFAULT:
                    invalidInput = false;
                    break;

                case NOWHITESPACE:
                    assert str != null;
                    if (doesContainsOnlyWhiteSpace(str)) {
                        invalidInput = true;
                        System.out.println("Only White Space found!");
                        String promptTryAgain = "Wanna try again? [Y/N] : ";
                        if (!wannaTryAgain(promptTryAgain, myTimer)) return null;
                    } else invalidInput = false;
                    break;

                case YES_OR_NO:
                    assert str != null;
                    String tempStr = str.toLowerCase();
                    switch (tempStr) {
                        case "y":
                        case "yes":
                        case "n":
                        case "no":
                            invalidInput = false;
                            break;
                        default:
                            invalidInput = true;
                            System.out.println("Only [Y/N] allowed!");
                            String promptTryAgain = "Wanna try again? [Y/N] : ";
                            if (!wannaTryAgain(promptTryAgain, myTimer)) return null;
                    }
                    break;  //  break out of outer switch case YES_OR_NO

                case DIR:
                    assert str != null;

                    //  Input is valid if str is an existing Directory
                    if (new File(str).isDirectory()) invalidInput = false;
                    else {
                        invalidInput = true;
                        System.out.println("No Such Directory Found!");
                        String promptTryAgain = "Wanna try again? [Y/N] : ";
                        if (!wannaTryAgain(promptTryAgain, myTimer)) return null;
                    }
                    break;

                case FILE:
                    assert str != null;

                    //  Input is valid if str is an existing regular file
                    if (new File(str).isFile()) invalidInput = false;
                    else {
                        invalidInput = true;
                        System.out.println("No Such File Found!");
                        String promptTryAgain = "Wanna try again? [Y/N] : ";
                        if (!wannaTryAgain(promptTryAgain, myTimer)) return null;
                    }
                    break;

                case DIR_OR_FILE:
                    assert str != null;

                    File temp = new File(str);
                    //  Input is valid if str is an existing regular file or dir
                    if (temp.isFile() || temp.isDirectory()) invalidInput = false;
                    else {
                        invalidInput = true;
                        System.out.println("No Such File/Dir. Found!");
                        String promptTryAgain = "Wanna try again? [Y/N] : ";
                        if (!wannaTryAgain(promptTryAgain, myTimer)) return null;
                    }
                    break;

                default:
                    System.out.println("Unknown UserInputString options!");
            }
        } while (invalidInput);

        return str;
    }

    private static boolean wannaTryAgain(String promptTryAgain, MyTimer myTimer) {

        BufferedReader bufferedReader = new BufferedReader
                (new InputStreamReader(System.in));
        do {
            try {

                myTimer.pauseTimer();
                System.out.println(promptTryAgain);
                String ans = bufferedReader.readLine();
                myTimer.continueTimer();

                switch (ans.toLowerCase().trim()) {
                    case "yes":
                    case "y":
                    case "true":
                        return true;
                    case "n":
                    case "no":
                    case "false":
                        return false;
                    default:
                        //  Keep prompting user to enter a valid input choice
                        System.out.println("Please Enter a valid input");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        } while (true);
    }

    //  Time Stamp: 14th September 2K19, 07:32 PM..!! [Night]
    public static String generateCurrentTimeStamp() {
        String dateTimePattern = "MMMM-dd-yyyy hh-mm-ss-SSS-a";
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateTimePattern));
    }

    private static boolean doesBeginsWithWhiteSpace(String str) {

        //  Return true if str is Empty!
        if (str.isEmpty()) return true;

        //  Return true if first char. of str is found to be a White Space Char.
        return Character.isWhitespace(str.charAt(0));

    }

    //  Time Stamp: 1st June 2K19, 12:20 AM..!! [Mornight]
    private static boolean doesContainsOnlyWhiteSpace(String str) {

        //  Return true if str is Empty!
        if (str.isEmpty()) return true;

        //  Return false if any char. of str is found to be not a White Space Char.
        for (char c : str.toCharArray())
            if (!Character.isWhitespace(c))
                return false;

        return true;

    }

    //  Time Stamp: 8th June 2K19, 11:50 PM..!! [Midnight]
    public static String replaceBackSlashWithForwardSlash(String str) {
        return str.replaceAll("\\\\", "/");
    }

}
